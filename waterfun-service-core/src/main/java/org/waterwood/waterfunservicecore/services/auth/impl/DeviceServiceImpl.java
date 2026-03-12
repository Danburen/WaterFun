package org.waterwood.waterfunservicecore.services.auth.impl;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.waterwood.common.cache.RedisHelperHolder;
import org.waterwood.waterfunservicecore.entity.user.User;
import org.waterwood.utils.codec.HashUtil;
import org.waterwood.waterfunservicecore.infrastructure.persistence.user.UserRepository;
import org.waterwood.waterfunservicecore.services.auth.DeviceService;
import org.waterwood.waterfunservicecore.services.auth.UserKeyBuilder;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
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
        String deviceId = this.generateDeviceId(userUid,dfp);
        redisHelper.hSet(getDeviceKey(userUid),deviceId, String.valueOf(System.currentTimeMillis()));
        return deviceId;
    }

    @Override
    public void removeUserDevice(Long userUid, String deviceId){
        redisHelper.hDel(getDeviceKey(userUid),deviceId);
    }

    @Override
    public String generateDeviceId(long userUid, String dfp){
        return HashUtil.hashWithSalt(dfp+userUid, deviceHashSalt);
    }

    @Async
    @Override
    public void cleanZombieDevicesBatch(int batchSize){
        Page<User> users = userRepository.findAll(PageRequest.of(0, batchSize));
        while(!users.getContent().isEmpty()){
            users.forEach(user->{
                Map<Object,Object> devices = redisHelper.hGetAll(getDeviceKey(user.getUid()));
                // Get all the devices that are older than the max expire time
                devices.entrySet().removeIf(
                        entry-> (System.currentTimeMillis() - (long) entry.getValue()) < deviceExpireMaxTimeMillis);
                redisHelper.hDel(getDeviceKey(user.getUid()), devices.keySet().toArray(new String[0]));
            });
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Override
    public void scheduledCleanup() {
        cleanZombieDevicesBatch(1000);
    }

    @Override
    public List<String> getUserDeviceIds(Long userUid) {
        return redisHelper.hGetAll(getDeviceKey(userUid)).keySet().stream().map(Object::toString).toList();
    }

    private String getDeviceKey(Long userUid){
        return UserKeyBuilder.userDevice(userUid);
    }
}