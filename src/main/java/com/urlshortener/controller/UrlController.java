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

        // Prefer X-Forwarded-For (set by our load balancer) so rate limiting
        // sees the real client IP, not the load balancer's IP.
        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
        String clientIp = (forwardedFor != null && !forwardedFor.isBlank())
                ? forwardedFor.split(",")[0].trim()
                : httpRequest.getRemoteAddr();

        if (!rateLimitService.isAllowed(clientIp)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded. Try again later.");
        }

        String shortCode =
                urlShortenerService.shortenUrl(
                        request.getOriginalUrl()
                );

        // Build the short URL dynamically from the actual incoming request,
        // instead of hardcoding localhost — this way it's correct whether
        // running locally, on a single Render instance, or behind the LB.
        String scheme = httpRequest.getScheme();
        String host = httpRequest.getHeader("Host");
        String shortUrl = scheme + "://" + host + "/" + shortCode;

        return ResponseEntity.ok(
                new ShortenUrlResponse(shortCode, shortUrl)
        );
    }
}