package com.fptcampus.lostfoundfptcampus.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
    tableName = "histories",
    foreignKeys = {
        @ForeignKey(
            entity = LostItem.class,
            parentColumns = "id",
            childColumns = "item_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "giver_id",
            onDelete = ForeignKey.SET_NULL
        ),
        @ForeignKey(
            entity = User.class,
            parentColumns = "id",
            childColumns = "receiver_id",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {@Index("item_id"), @Index("giver_id"), @Index("receiver_id")}
)
public class History {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "item_id")
    private long itemId;

    @ColumnInfo(name = "giver_id")
    private Long giverId;

    @ColumnInfo(name = "receiver_id")
    private Long receiverId;

    @ColumnInfo(name = "qr_token")
    private String qrToken;

    @ColumnInfo(name = "confirmed_at")
    private Date confirmedAt;

    // Constructors
    public History() {
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

    public Long getGiverId() {
        return giverId;
    }

    public void setGiverId(Long giverId) {
        this.giverId = giverId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public Date getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Date confirmedAt) {
        this.confirmedAt = confirmedAt;
    }
}
