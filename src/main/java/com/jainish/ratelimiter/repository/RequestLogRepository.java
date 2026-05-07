package com.jainish.ratelimiter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jainish.ratelimiter.entity.RequestLog;

import java.util.*;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    List<RequestLog> findByClientKeyOrderByTimestampDesc(String clientKey);
}