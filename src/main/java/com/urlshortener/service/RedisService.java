package com.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String key, String value) {
        redisTemplate.opsForValue().set(
                key,
                value,
                Duration.ofHours(1)
        );
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
}