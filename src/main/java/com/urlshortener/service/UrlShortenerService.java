package com.urlshortener.service;

import com.urlshortener.model.Url;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.util.Base62Encoder;
import org.springframework.stereotype.Service;

@Service
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final RedisService redisService;

    public UrlShortenerService(
            UrlRepository urlRepository,
            RedisService redisService) {

        this.urlRepository = urlRepository;
        this.redisService = redisService;
    }

    public String shortenUrl(String originalUrl) {

        String temporaryCode = "TEMP-" + System.nanoTime();

        Url url = new Url(temporaryCode, originalUrl);

        url = urlRepository.save(url);

        long id = url.getId();

        String shortCode = Base62Encoder.encode(id);

        url.setShortCode(shortCode);

        urlRepository.save(url);

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {

        // 1. Check Redis first
        String cachedUrl = redisService.get(shortCode);

        if (cachedUrl != null) {
            System.out.println("CACHE HIT: " + shortCode);
            return cachedUrl;
        }

        // 2. Redis miss → check PostgreSQL
        System.out.println("CACHE MISS: " + shortCode);

        String originalUrl = urlRepository
                .findByShortCode(shortCode)
                .map(Url::getOriginalUrl)
                .orElse(null);

        // 3. If found in database, put it into Redis
        if (originalUrl != null) {
            redisService.save(shortCode, originalUrl);
        }

        return originalUrl;
    }
}