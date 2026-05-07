package com.jainish.ratelimiter.controller;

import java.net.http.HttpHeaders;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jainish.ratelimiter.entity.RateLimitResponse;
import com.jainish.ratelimiter.entity.RequestLog;
import com.jainish.ratelimiter.service.LogService;
import com.jainish.ratelimiter.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {
	
    private final RateLimiterService rateLimiterService;
    private final LogService logService;

    public RateLimitController(RateLimiterService rateLimiterService, LogService logService) {
        this.rateLimiterService = rateLimiterService;
        this.logService = logService;
    }
    
    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> check(@RequestParam String clientKey, HttpServletRequest request){
    	
        RateLimitResponse response = rateLimiterService.checkLimit(clientKey);
        logService.log(clientKey, request.getRemoteAddr(), response.isAllowed(), response.getRemainingReq());

        HttpStatus status = response.isAllowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS;
        return ResponseEntity.status(status).body(response);
    }
    
    @GetMapping("/logs/{clientKey}")
    public List<RequestLog> getLogs(@PathVariable String clientKey) {
        return logService.getLogsForClient(clientKey);
    }

}
