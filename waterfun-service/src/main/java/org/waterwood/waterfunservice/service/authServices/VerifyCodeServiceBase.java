package org.waterwood.waterfunservice.service.authServices;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservice.repository.RedisRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public abstract class VerifyCodeServiceBase implements VerifyCodeService {
    private final String redisKeyPrefix;
    private final RedisRepository<String> redisRepository;
    protected VerifyCodeServiceBase(String redisKeyPrefix, RedisRepository<String> redisRepository) {
        this.redisKeyPrefix = redisKeyPrefix;
        this.redisRepository = redisRepository;
    }

    @Override
    public void saveCode(String uuid,String code){
        redisRepository.save(redisKeyPrefix + uuid, code, 2, TimeUnit.MINUTES);
    }

    @Override
    public String getCode(String uuid){
        return redisRepository.get(redisKeyPrefix + uuid);
    }

    @Override
    public void removeCode(String uuid){
        redisRepository.delete(redisKeyPrefix + uuid);
    }

    /**
     * Get a new uuid by
     * @see UUID#randomUUID()
     * @return uuid string
     */
    public static String getNewUUID(){
        return UUID.randomUUID().toString();
    }
}
