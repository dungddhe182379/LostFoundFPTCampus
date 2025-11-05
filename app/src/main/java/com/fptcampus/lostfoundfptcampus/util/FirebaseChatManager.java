package com.fptcampus.lostfoundfptcampus.util;

import androidx.annotation.NonNull;

import com.fptcampus.lostfoundfptcampus.model.Chat;
import com.fptcampus.lostfoundfptcampus.model.Message;
import com.fptcampus.lostfoundfptcampus.model.UserChat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Chat Manager - handles all chat operations with Firebase Realtime Database
 */
public class FirebaseChatManager {
    private static FirebaseChatManager instance;
    private final DatabaseReference databaseRef;
    private final DatabaseReference chatsRef;
    private final DatabaseReference messagesRef;
    private final DatabaseReference userChatsRef;

    private FirebaseChatManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://lost-and-found-fpt-campus-default-rtdb.asia-southeast1.firebasedatabase.app"
        );
        this.databaseRef = database.getReference();
        this.chatsRef = databaseRef.child("chats");
        this.messagesRef = databaseRef.child("messages");
        this.userChatsRef = databaseRef.child("userChats");
    }

    public static synchronized FirebaseChatManager getInstance() {
        if (instance == null) {
            instance = new FirebaseChatManager();
        }
        return instance;
    }

    /**
     * Generate chat ID from item and users
     */
    public String generateChatId(long itemId, long userId1, long userId2) {
        long minId = Math.min(userId1, userId2);
        long maxId = Math.max(userId1, userId2);
        return "item_" + itemId + "_users_" + minId + "_" + maxId;
    }

    /**
     * Create or get existing chat
     */
    public void createChat(long itemId, long lostUserId, long foundUserId, 
                          String lostUserName, String foundUserName,
                          boolean isAnonymous, ChatCallback callback) {
        String chatId = generateChatId(itemId, lostUserId, foundUserId);

        // Check if chat exists
        chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Chat exists
                    callback.onSuccess(chatId);
                } else {
                    // Create new chat
                    Chat chat = new Chat(chatId, itemId, lostUserId, foundUserId);
                    chatsRef.child(chatId).setValue(chat);

                    // Create userChats for both users
                    UserChat lostUserChat = new UserChat(chatId, itemId, foundUserId, 
                        isAnonymous ? "Người dùng ẩn danh" : foundUserName);
                    lostUserChat.setAnonymous(isAnonymous);
                    userChatsRef.child(String.valueOf(lostUserId)).child(chatId).setValue(lostUserChat);

                    UserChat foundUserChat = new UserChat(chatId, itemId, lostUserId, 
                        isAnonymous ? "Người dùng ẩn danh" : lostUserName);
                    foundUserChat.setAnonymous(isAnonymous);
                    userChatsRef.child(String.valueOf(foundUserId)).child(chatId).setValue(foundUserChat);

                    callback.onSuccess(chatId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Send message
     */
    public void sendMessage(String chatId, long senderId, String senderName, 
                           String messageText, MessageCallback callback) {
        Message message = new Message(senderId, senderName, messageText);
        
        // Generate message ID
        String messageId = messagesRef.child(chatId).push().getKey();
        if (messageId == null) {
            callback.onError("Failed to generate message ID");
            return;
        }

        message.setMessageId(messageId);

        // Save message
        messagesRef.child(chatId).child(messageId).setValue(message)
            .addOnSuccessListener(aVoid -> {
                // Update chat last message
                Map<String, Object> updates = new HashMap<>();
                updates.put("lastMessage", messageText);
                updates.put("lastMessageTime", message.getTimestamp());
                chatsRef.child(chatId).updateChildren(updates);

                // Update userChats for both users
                updateUserChatLastMessage(chatId, senderId, messageText, message.getTimestamp());
                
                callback.onSuccess(message);
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Update last message in userChats
     */
    private void updateUserChatLastMessage(String chatId, long senderId, String message, long timestamp) {
        // Get chat participants
        chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null) {
                    // Update for both users
                    long user1 = chat.getLostUserId();
                    long user2 = chat.getFoundUserId();

                    updateUserChat(user1, chatId, message, timestamp, senderId != user1);
                    updateUserChat(user2, chatId, message, timestamp, senderId != user2);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateUserChat(long userId, String chatId, String message, long timestamp, boolean incrementUnread) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", message);
        updates.put("lastMessageTime", timestamp);
        
        DatabaseReference ref = userChatsRef.child(String.valueOf(userId)).child(chatId);
        
        if (incrementUnread) {
            ref.child("unreadCount").get().addOnSuccessListener(dataSnapshot -> {
                int currentCount = dataSnapshot.exists() ? dataSnapshot.getValue(Integer.class) : 0;
                updates.put("unreadCount", currentCount + 1);
                ref.updateChildren(updates);
            });
        } else {
            ref.updateChildren(updates);
        }
    }

    /**
     * Get user's chat list
     */
    public void getUserChats(long userId, UserChatsCallback callback) {
        userChatsRef.child(String.valueOf(userId))
            .orderByChild("lastMessageTime")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<UserChat> chats = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        UserChat chat = child.getValue(UserChat.class);
                        if (chat != null) {
                            // Set chatId from the key of the snapshot
                            chat.setChatId(child.getKey());
                            chats.add(chat);
                        }
                    }
                    // Reverse to show newest first
                    java.util.Collections.reverse(chats);
                    callback.onSuccess(chats);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
    }

    /**
     * Get messages for a chat
     */
    public void getMessages(String chatId, MessagesCallback callback) {
        messagesRef.child(chatId)
            .orderByChild("timestamp")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Message> messages = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Message message = child.getValue(Message.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    callback.onSuccess(messages);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
    }

    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(String chatId, long userId) {
        // Validate chatId
        if (chatId == null || chatId.isEmpty()) {
            return;
        }
        
        // Reset unread count
        userChatsRef.child(String.valueOf(userId)).child(chatId)
            .child("unreadCount").setValue(0);

        // Mark all messages as read
        messagesRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Message msg = child.getValue(Message.class);
                    if (msg != null && msg.getSenderId() != userId && !msg.isRead()) {
                        child.getRef().child("read").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Callbacks
    public interface ChatCallback {
        void onSuccess(String chatId);
        void onError(String error);
    }

    public interface MessageCallback {
        void onSuccess(Message message);
        void onError(String error);
    }

    public interface UserChatsCallback {
        void onSuccess(List<UserChat> chats);
        void onError(String error);
    }

    public interface MessagesCallback {
        void onSuccess(List<Message> messages);
        void onError(String error);
    }
}
