package com.jainish.ratelimiter.service;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.jainish.ratelimiter.entity.RateLimitResponse;

@Service
public class RateLimiterService {
	
	@Autowired
	public StringRedisTemplate redisTemplate;
	
	public RateLimitResponse checkLimit(String clientKey, int maxReq, long resetInSeconds) {
		String redisKey = "rate:" + clientKey;
		
		Long count = redisTemplate.opsForValue().increment(redisKey);
		
		if(count == 1) {
			redisTemplate.expire(redisKey, resetInSeconds, TimeUnit.SECONDS);
		}
		
        long remaining = Math.max(0, maxReq - count);
        boolean allowed = count <= maxReq;
        
        return new RateLimitResponse(allowed, remaining, resetInSeconds);
		
	}

}
