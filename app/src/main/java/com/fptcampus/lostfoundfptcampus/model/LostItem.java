package com.fptcampus.lostfoundfptcampus.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

@Entity(
    tableName = "items",
    indices = {@Index("user_id"), @Index("status")}
)
public class LostItem {
    @PrimaryKey(autoGenerate = true)
    @Expose(serialize = false, deserialize = true) // Don't send to API, but receive from API
    private long id;

    @ColumnInfo(name = "uuid")
    @Expose
    private String uuid;

    @ColumnInfo(name = "user_id")
    @Expose
    @SerializedName("userId")
    private long userId;

    @ColumnInfo(name = "title")
    @Expose
    private String title;

    @ColumnInfo(name = "description")
    @Expose
    private String description;

    @ColumnInfo(name = "category")
    @Expose
    private String category;

    @ColumnInfo(name = "status")
    @Expose
    private String status; // lost, found, returned

    @ColumnInfo(name = "latitude")
    @Expose
    private Double latitude;

    @ColumnInfo(name = "longitude")
    @Expose
    private Double longitude;

    @ColumnInfo(name = "image_url")
    @Expose
    @SerializedName("imageUrl")
    private String imageUrl;

    @ColumnInfo(name = "created_at")
    @Expose(serialize = false, deserialize = true) // Server generates this
    @SerializedName("createdAt")
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    @Expose(serialize = false, deserialize = true) // Server generates this
    @SerializedName("updatedAt")
    private Date updatedAt;

    // Local field only - NOT sent to API
    @ColumnInfo(name = "synced")
    private boolean synced; // Để track offline sync

    // Constructors
    public LostItem() {
        this.status = "lost";
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.synced = false;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }
}
