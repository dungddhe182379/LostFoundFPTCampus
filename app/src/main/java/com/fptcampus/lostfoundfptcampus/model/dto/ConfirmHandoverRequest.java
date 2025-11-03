package com.fptcampus.lostfoundfptcampus.model.dto;

import com.google.gson.annotations.Expose;

/**
 * Request DTO for confirming handover via QR code
 * Used in POST /api/lostfound/items/{itemId}/confirm-handover
 */
public class ConfirmHandoverRequest {
    @Expose
    private String qrToken;
    
    // Constructors
    public ConfirmHandoverRequest() {
    }
    
    public ConfirmHandoverRequest(String qrToken) {
        this.qrToken = qrToken;
    }
    
    // Getters and Setters
    public String getQrToken() {
        return qrToken;
    }
    
    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}
