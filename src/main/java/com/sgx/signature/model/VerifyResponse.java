package com.sgx.signature.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyResponse {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private String requestId;
    private String status; // OK veya ERROR
    private boolean valid;
    private long processingTimeMicros;

    // Hata durumu için alanlar
    private String errorCode;
    private String errorMessage;

    // Getter ve Setter metodları
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public long getProcessingTimeMicros() { return processingTimeMicros; }
    public void setProcessingTimeMicros(long processingTimeMicros) { this.processingTimeMicros = processingTimeMicros; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
