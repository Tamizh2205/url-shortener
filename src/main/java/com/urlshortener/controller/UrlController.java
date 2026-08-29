package com.urlshortener.controller;

import com.urlshortener.model.ShortenUrlRequest;
import com.urlshortener.model.ShortenUrlResponse;
import com.urlshortener.service.RateLimitService;
import com.urlshortener.service.UrlShortenerService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimitService rateLimitService;

    public UrlController(
            UrlShortenerService urlShortenerService,
            RateLimitService rateLimitService) {

        this.urlShortenerService = urlShortenerService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            @RequestBody ShortenUrlRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        if (!rateLimitService.isAllowed(clientIp)) {

            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }

        String shortCode =
                urlShortenerService.shortenUrl(
                        request.getOriginalUrl()
                );

        String shortUrl =
                "http://localhost:9000/" + shortCode;

        return ResponseEntity.ok(
                new ShortenUrlResponse(shortCode, shortUrl)
        );
    }
}