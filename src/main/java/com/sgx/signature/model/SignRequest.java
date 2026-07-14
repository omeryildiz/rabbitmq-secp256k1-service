package com.sgx.signature.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignRequest {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private String requestId;
    private String keyId;
    private String algorithm;
    private String curve;
    private String payloadEncoding;
    private String payload;
    private long timestamp;

    // Getter ve Setter metodları
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }
    
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    
    public String getCurve() { return curve; }
    public void setCurve(String curve) { this.curve = curve; }
    
    public String getPayloadEncoding() { return payloadEncoding; }
    public void setPayloadEncoding(String payloadEncoding) { this.payloadEncoding = payloadEncoding; }
    
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
