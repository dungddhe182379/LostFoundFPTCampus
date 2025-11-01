package com.fptcampus.lostfoundfptcampus.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

@Entity(tableName = "users", indices = {@Index(value = "email", unique = true)})
public class User {
    @PrimaryKey(autoGenerate = true)
    @Expose
    private long id;

    @ColumnInfo(name = "uuid")
    @Expose
    private String uuid;

    @ColumnInfo(name = "name")
    @Expose
    private String name;

    @ColumnInfo(name = "email")
    @Expose
    private String email;

    @ColumnInfo(name = "password_hash")
    @SerializedName("passwordHash")
    private String passwordHash; // Don't expose password

    @ColumnInfo(name = "phone")
    @Expose
    private String phone;

    @ColumnInfo(name = "avatar_url")
    @Expose
    @SerializedName("avatarUrl")
    private String avatarUrl;

    @ColumnInfo(name = "karma")
    @Expose
    private int karma;

    @ColumnInfo(name = "created_at")
    @Expose
    @SerializedName("createdAt")
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    @Expose
    @SerializedName("updatedAt")
    private Date updatedAt;

    // Constructors
    public User() {
        this.karma = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getKarma() {
        return karma;
    }

    public void setKarma(int karma) {
        this.karma = karma;
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
}
