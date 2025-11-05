package com.fptcampus.lostfoundfptcampus.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat model - represents a chat conversation between two users about an item
 */
public class Chat {
    private String chatId;
    private long itemId;
    private long lostUserId;
    private long foundUserId;
    private long createdAt;
    private String lastMessage;
    private long lastMessageTime;
    private Map<String, Boolean> participants; // userId -> true

    public Chat() {
        this.participants = new HashMap<>();
    }

    public Chat(String chatId, long itemId, long lostUserId, long foundUserId) {
        this.chatId = chatId;
        this.itemId = itemId;
        this.lostUserId = lostUserId;
        this.foundUserId = foundUserId;
        this.createdAt = System.currentTimeMillis();
        this.participants = new HashMap<>();
        this.participants.put(String.valueOf(lostUserId), true);
        this.participants.put(String.valueOf(foundUserId), true);
    }

    // Getters and Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public long getItemId() {
        return itemId;
    }

    public void setItemId(long itemId) {
        this.itemId = itemId;
    }

    public long getLostUserId() {
        return lostUserId;
    }

    public void setLostUserId(long lostUserId) {
        this.lostUserId = lostUserId;
    }

    public long getFoundUserId() {
        return foundUserId;
    }

    public void setFoundUserId(long foundUserId) {
        this.foundUserId = foundUserId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Map<String, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, Boolean> participants) {
        this.participants = participants;
    }
}
