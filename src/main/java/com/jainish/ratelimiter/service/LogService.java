package com.jainish.ratelimiter.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.jainish.ratelimiter.entity.RequestLog;
import com.jainish.ratelimiter.repository.RequestLogRepository;

@Service
public class LogService {
    @Autowired
    private RequestLogRepository repository;

    @Async
    public void log(String clientKey, String ip, boolean allowed, long remainingReq) {
        RequestLog log = new RequestLog(clientKey, ip, allowed, remainingReq);
        repository.save(log);
    }

	public List<RequestLog> getLogsForClient(String clientKey) {
		List<RequestLog> log = repository.findByClientKeyOrderByTimestampDesc(clientKey);
		if(log.isEmpty()) {
			return null;
		}else {
			return log;
		}
	}
}
