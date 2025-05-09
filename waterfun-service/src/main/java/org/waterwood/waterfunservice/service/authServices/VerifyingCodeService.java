package org.waterwood.waterfunservice.service.authServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.waterwood.waterfunservice.repository.RedisRepository;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class VerifyingCodeService {
    @Autowired
    private RedisRepository<String> redisRepository;

    public String generateVerifyingCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

}
