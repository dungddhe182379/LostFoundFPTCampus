package com.fptcampus.lostfoundfptcampus.model.dto;

import com.google.gson.annotations.Expose;

/**
 * DTO for updating item fields
 * Only include fields that need to be updated
 */
public class UpdateItemRequest {
    @Expose
    private String title;
    
    @Expose
    private String description;
    
    @Expose
    private String category;
    
    @Expose
    private String status;
    
    @Expose
    private Double latitude;
    
    @Expose
    private Double longitude;
    
    @Expose
    private String imageUrl;

    // Constructors
    public UpdateItemRequest() {
    }

    // Builder pattern for easy construction
    public static class Builder {
        private UpdateItemRequest request = new UpdateItemRequest();

        public Builder setTitle(String title) {
            request.title = title;
            return this;
        }

        public Builder setDescription(String description) {
            request.description = description;
            return this;
        }

        public Builder setCategory(String category) {
            request.category = category;
            return this;
        }

        public Builder setStatus(String status) {
            request.status = status;
            return this;
        }

        public Builder setLatitude(Double latitude) {
            request.latitude = latitude;
            return this;
        }

        public Builder setLongitude(Double longitude) {
            request.longitude = longitude;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            request.imageUrl = imageUrl;
            return this;
        }

        public UpdateItemRequest build() {
            return request;
        }
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
