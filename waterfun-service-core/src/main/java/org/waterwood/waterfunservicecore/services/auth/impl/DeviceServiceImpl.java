package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservicecore.infrastructure.RedisHelperHolder;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.auth.DeviceService;
import org.waterwood.common.constratin.UserKeyBuilder;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
/**
 * A service aiming to manager devices
 * we use redis keys like these:
 * "user:[userUid]:device:[deviceId] -> jti" store the basic one-token-one-device relations
 * "user:[userUid]:devices -> set(deviceId)" store all the user's devices
 * "user:[userUid]:device:[deviceId]:last_active -> timestamp(string)" store single
 * - user's single device's last active time, used for clean up of zombie devices
 *
 */
public class DeviceServiceImpl implements DeviceService {
    private final RedisHelperHolder redisHelper;
    @Getter
    private final String deviceHashSalt;

    private final UserRepository userRepository;

    @Value("${device.temp.ttl:3600}")
    private Long device_temp_ttl;
    @Value("${device.short.ttl:604800}")
    private Long device_short_ttl;
    @Value("${device.long.ttl:777600}")
    private Long device_long_ttl;
    @Value("#{${clean-up.device.max-days} * 24 * 60 * 60 * 1000}")
    private Long deviceExpireMaxTimeMillis;
    protected DeviceServiceImpl(@Value("${device.salt}")String salt,RedisHelperHolder redisHelper, UserRepository userRepository) {
        this.deviceHashSalt = Base64.getEncoder().encodeToString(salt.getBytes());
        this.redisHelper = redisHelper;
        this.userRepository = userRepository;
    }

    @Override
    public String generateAndStoreDeviceId(Long userUid, String dfp) {
        String deviceId = this.calculaateDid(userUid,dfp);
        redisHelper.sAdd(getDevicesKey(userUid),deviceId, String.valueOf(System.currentTimeMillis()));
        return deviceId;
    }

    @Override
    public void removeUserDevice(Long userUid, String deviceId){
        redisHelper.del(UserKeyBuilder.userAccessDevice(userUid, deviceId));
    }

    @Override
    public String calculaateDid(long userUid, String dfp){
        return HashUtil.hashWithSalt(dfp+userUid, deviceHashSalt);
    }

    @Async
    @Override
    public void cleanZombieDevicesBatch(int batchSize){
        Page<User> users = userRepository.findAll(PageRequest.of(0, batchSize));
        while(!users.getContent().isEmpty()){
            users.forEach(user->{
                Map<String, Long> devices = getUserDeviceLastActiveTime(user.getUid());
                // Get all the devices that are older than the max expire time
                devices.entrySet().removeIf(
                        entry-> (System.currentTimeMillis() - entry.getValue()) < deviceExpireMaxTimeMillis);
                redisHelper.hDel(getDevicesKey(user.getUid()), devices.keySet().toArray(new String[0]));
            });
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Override
    public void scheduledCleanup() {
        cleanZombieDevicesBatch(1000);
    }

    @Override
    public Set<String> getUserDeviceIds(Long userUid) {
        return redisHelper.sMem(getDevicesKey(userUid));
    }

    @Override
    public void updateUserDeviceActive(long userUid, String did){
        redisHelper.set(getDeviceLastActiveKey(userUid,did), String.valueOf(System.currentTimeMillis()), Duration.ofDays(30));
    }

    @Override
    public boolean isNewDeviceDid(long userUid, String calculatedHashDid) {
        Set<String> userDeviceIds = getUserDeviceIds(userUid);
        if(userDeviceIds.contains(calculatedHashDid)){
            return true;
        }else{
            // TODO: AUDIT log for new device login, consider to add more info like IP, device type etc.
            log.info("New Device detected: userUid={}, deviceId={}", userUid, calculatedHashDid);
            return false;
        }
    }

    private Map<String, Long> getUserDeviceLastActiveTime(Long userUid){
        HashMap<String, Long> res = new HashMap<>();
        Set<String> deviceIds = getUserDeviceIds(userUid);
        if (deviceIds == null || deviceIds.isEmpty()) {
            return res;
        }

        List<String> keys = deviceIds.stream().map(id -> String.format(getDeviceLastActiveKey(userUid, id))).toList();
        List<String> values = redisHelper.mget(keys);
        int index = 0;
        for (String deviceId : deviceIds) {
            String value = values.get(index++);
            if (value != null) {
                try {
                    res.put(deviceId, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    log.warn("Invalid lastActiveTime format, userUid={}, deviceId={}, value={}",
                            userUid, deviceId, value);
                }
            }
        }
        return res;
    }

    private String getDeviceLastActiveKey(Long userUid, String did){
        return "user:" + userUid + ":device:" + did + ":last_active";
    }
    private String getDevicesKey(Long userUid){
        return UserKeyBuilder.userDevices(userUid);
    }
}