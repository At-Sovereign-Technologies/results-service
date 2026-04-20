package com.electoral.results_service.cache;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisCacheAdapter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(RedisCacheAdapter.class);

    @CircuitBreaker(name = "redisCache", fallbackMethod = "getFallback")
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Object getFallback(String key, Throwable t) {
        log.warn("CACHE FALLBACK (GET) - key={} - {}", key, t.getMessage());
        return null;
    }

    @CircuitBreaker(name = "redisCache", fallbackMethod = "setFallback")
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(5));
    }

    public void setFallback(String key, Object value, Throwable t) {
        log.warn("CACHE FALLBACK (SET) - key={} - {}", key, t.getMessage());
    }
}