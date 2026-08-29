package com.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String clientIp) {

        // Create Redis key
        String key = "rate_limit:" + clientIp;

        // Increase request counter
        Long requestCount =
                redisTemplate.opsForValue().increment(key);

        // Debug information
        System.out.println("RATE LIMIT KEY: " + key);
        System.out.println("REQUEST COUNT: " + requestCount);

        // Set expiration for the first request
        if (requestCount != null && requestCount == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        // Allow maximum 10 requests
        return requestCount != null && requestCount <= MAX_REQUESTS;
    }
}