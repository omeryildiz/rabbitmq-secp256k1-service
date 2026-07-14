package com.sgx.signature.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignResponse {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private String requestId;
    private String status; // OK veya ERROR
    private String algorithm;
    private String curve;
    private String signatureEncoding;
    private String signature;
    private String publicKey;
    private long processingTimeMicros;
    
    // Hata durumu için alanlar
    private String errorCode;
    private String errorMessage;

    // Getter ve Setter metodları
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getCurve() { return curve; }
    public void setCurve(String curve) { this.curve = curve; }

    public String getSignatureEncoding() { return signatureEncoding; }
    public void setSignatureEncoding(String signatureEncoding) { this.signatureEncoding = signatureEncoding; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public long getProcessingTimeMicros() { return processingTimeMicros; }
    public void setProcessingTimeMicros(long processingTimeMicros) { this.processingTimeMicros = processingTimeMicros; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
