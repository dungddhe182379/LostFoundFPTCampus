package com.fptcampus.lostfoundfptcampus.model;

/**
 * UserChat model - represents a chat in user's chat list
 */
public class UserChat {
    private String chatId;
    private long itemId;
    private long otherUserId;
    private String otherUserName;
    private String lastMessage;
    private long lastMessageTime;
    private int unreadCount;
    private boolean isAnonymous; // Chat ẩn danh hay không

    public UserChat() {
        this.unreadCount = 0;
        this.isAnonymous = false;
    }

    public UserChat(String chatId, long itemId, long otherUserId, String otherUserName) {
        this.chatId = chatId;
        this.itemId = itemId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.unreadCount = 0;
        this.isAnonymous = false;
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

    public long getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(long otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
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

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }
}
