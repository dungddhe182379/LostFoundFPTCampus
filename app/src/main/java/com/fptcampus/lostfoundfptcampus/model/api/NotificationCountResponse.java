package com.fptcampus.lostfoundfptcampus.model.api;

import com.google.gson.annotations.SerializedName;

public class NotificationCountResponse {
    @SerializedName("count")
    private int count;

    // Constructors
    public NotificationCountResponse() {
    }

    public NotificationCountResponse(int count) {
        this.count = count;
    }

    // Getters and Setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
