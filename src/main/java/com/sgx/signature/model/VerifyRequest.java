package com.sgx.signature.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyRequest {
    private static final Logger log = LoggerFactory.getLogger(java.lang.invoke.MethodHandles.lookup().lookupClass());
    private String requestId;
    private String algorithm;
    private String curve;
    private String payloadEncoding;
    private String payload;
    private String signatureEncoding;
    private String signature;
    private String publicKey;

    // Getter ve Setter metodları
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getCurve() { return curve; }
    public void setCurve(String curve) { this.curve = curve; }

    public String getPayloadEncoding() { return payloadEncoding; }
    public void setPayloadEncoding(String payloadEncoding) { this.payloadEncoding = payloadEncoding; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getSignatureEncoding() { return signatureEncoding; }
    public void setSignatureEncoding(String signatureEncoding) { this.signatureEncoding = signatureEncoding; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}
