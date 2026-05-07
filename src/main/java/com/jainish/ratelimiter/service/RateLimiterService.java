package com.jainish.ratelimiter.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.jainish.ratelimiter.entity.RateLimitResponse;

@Service
public class RateLimiterService {
	
	@Autowired
	public StringRedisTemplate redisTemplate;
	
    @Value("${rate.limiter.max-requests}")
    private int maxRequests;

    @Value("${rate.limiter.window-seconds}")
    private long windowSeconds;

	public RateLimitResponse checkLimit(String clientKey) {
		String redisKey = "rate:" + clientKey;
		
		Long count = redisTemplate.opsForValue().increment(redisKey);
		
		if(count == 1) {
			redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
		}
		
        long remaining = Math.max(0, maxRequests - count);
        boolean allowed = count <= maxRequests;
        
        return new RateLimitResponse(allowed, remaining, windowSeconds);
		
	}

}
