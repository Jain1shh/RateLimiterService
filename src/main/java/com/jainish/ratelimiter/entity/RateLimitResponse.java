package com.jainish.ratelimiter.entity;



public class RateLimitResponse {
	private boolean allowed;
    private long remainingRequests;
    private long resetInSeconds;
    
	public RateLimitResponse() {

	}
    
    
	public RateLimitResponse(boolean allowed, long remainingRequests, long resetInSeconds) {
		super();
		this.allowed = allowed;
		this.remainingRequests = remainingRequests;
		this.resetInSeconds = resetInSeconds;
	}
	public boolean isAllowed() {
		return allowed;
	}
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}
	public long getRemainingReq() {
	    return remainingRequests;
	}
	public void setRemainingReq(long remainingReq) {
	    this.remainingRequests = remainingReq;
	}
	
	public long getResetInSeconds() {
		return resetInSeconds;
	}
	public void setResetInSeconds(long resetInSeconds) {
		this.resetInSeconds = resetInSeconds;
	}
    

}
