package com.fptcampus.lostfoundfptcampus.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "photos",
    foreignKeys = @ForeignKey(
        entity = LostItem.class,
        parentColumns = "id",
        childColumns = "item_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("item_id")}
)
public class Photo {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "url")
    private String url;

    @ColumnInfo(name = "is_primary")
    private boolean isPrimary;

    // Constructors
    public Photo() {
        this.isPrimary = false;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
