package org.waterwood.waterfunservice.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisRepository<T> {
    private final RedisTemplate<String,T> redisTemplate;
    public RedisRepository(RedisTemplate<String,T> redisTemplate) {
        this.redisTemplate =  redisTemplate;
    }
    public void save(String key, T value , long timeout , TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }
    public T get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
