package com.jainish.ratelimiter.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class RequestLog {
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientKey;
    private String ipAddress;
    private boolean allowed;
    private long remainingReq;

    private LocalDateTime timestamp;
    
    

    public RequestLog() {
		super();
	}

	public RequestLog(Long id, String clientKey, String ipAddress, boolean allowed, long remainingReq,
			LocalDateTime timestamp) {
		super();
		this.id = id;
		this.clientKey = clientKey;
		this.ipAddress = ipAddress;
		this.allowed = allowed;
		this.remainingReq = remainingReq;
		this.timestamp = timestamp;
	}

	public RequestLog(String clientKey,
            String ipAddress,
            boolean allowed,
            long remainingReq) {

			this.clientKey = clientKey;
			this.ipAddress = ipAddress;
			this.allowed = allowed;
			this.remainingReq = remainingReq;
	}

	@PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getClientKey() {
		return clientKey;
	}

	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}

	public long getRemainingReq() {
		return remainingReq;
	}

	public void setRemainingReq(long remainingReq) {
		this.remainingReq = remainingReq;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
    
}
