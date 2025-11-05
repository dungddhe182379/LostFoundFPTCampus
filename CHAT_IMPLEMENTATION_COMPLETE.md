# Chat Feature Implementation - Complete Guide

## ✅ Đã hoàn thành

### 1. **Firebase Setup**
- ✅ Firebase Realtime Database dependency added
- ✅ Database structure designed: `chats/`, `messages/`, `userChats/`
- ✅ Firebase project: `lost-and-found-fpt-campus`

### 2. **Data Models**
- ✅ `Chat.java` - Chat conversation metadata
- ✅ `Message.java` - Individual messages
- ✅ `UserChat.java` - User's chat list entries

### 3. **Firebase Manager**
- ✅ `FirebaseChatManager.java` - Singleton manager với đầy đủ CRUD operations:
  - `generateChatId()` - Tạo unique chat ID
  - `createChat()` - Tạo chat mới với tùy chọn anonymous
  - `sendMessage()` - Gửi tin nhắn
  - `getUserChats()` - Lấy danh sách chat của user (real-time)
  - `getMessages()` - Lấy tin nhắn của chat (real-time)
  - `markMessagesAsRead()` - Đánh dấu đã đọc

### 4. **RecyclerView Adapters**
- ✅ `ChatListAdapter.java` - Hiển thị danh sách chat
- ✅ `MessageAdapter.java` - Hiển thị tin nhắn (sent/received styling)

### 5. **UI Components**
- ✅ `ChatListFragment.java` - Fragment hiển thị danh sách chat
- ✅ `ChatActivity.java` - Activity chat 1-1

### 6. **Layout Files**
- ✅ `item_chat.xml` - Chat list item layout
- ✅ `item_message.xml` - Message bubble layout (sent & received)
- ✅ `fragment_chat_list.xml` - Chat list screen
- ✅ `activity_chat.xml` - Chat conversation screen

### 7. **Drawable Resources**
- ✅ `circle_badge.xml` - Unread count badge
- ✅ `ic_chat.xml` - Chat icon
- ✅ `ic_send.xml` - Send button icon

### 8. **Navigation Integration**
- ✅ Added "Tin nhắn" to `bottom_navigation_menu.xml` (7 items total)
- ✅ Updated `MainActivity.java` to handle chat navigation
- ✅ Added `ChatActivity` to `AndroidManifest.xml`

---

## 🎨 Features

### ✨ Implemented Features
1. **Real-time Chat**
   - Firebase Realtime Database listeners
   - Instant message delivery
   - Auto-scroll to latest message

2. **Anonymous Chat Support**
   - `isAnonymous` flag in UserChat model
   - Display "Ẩn danh" instead of real name
   - Privacy protection for users

3. **Material Design 3**
   - Consistent styling with app theme
   - Sent messages: Blue (colorPrimary), right-aligned
   - Received messages: Gray (colorSurfaceVariant), left-aligned
   - Smooth animations and transitions

4. **Unread Count**
   - Red badge showing unread count (max 99)
   - Auto-reset when entering chat
   - Real-time updates

5. **Time Formatting**
   - Relative time (e.g., "2 phút trước", "5 giờ trước")
   - Fallback to date for older messages
   - HH:mm format in chat bubbles

6. **Empty States**
   - Friendly empty state in ChatListFragment
   - Guidance text for users

---

## 📋 Cách sử dụng trong code

### 1. **Tạo chat mới (với dialog anonymous)**

```java
// Trong ItemDetailActivity hoặc nơi cần tạo chat
long itemId = item.getId();
long currentUserId = prefsManager.getUserId();
long otherUserId = item.getLostUserId(); // hoặc foundUserId tùy context

// Show dialog chọn anonymous
new MaterialAlertDialogBuilder(this)
    .setTitle("Chọn chế độ trò chuyện")
    .setMessage("Bạn muốn hiển thị tên của mình hay trò chuyện ẩn danh?")
    .setPositiveButton("Hiển thị tên", (dialog, which) -> {
        createChatAndOpen(itemId, currentUserId, otherUserId, false);
    })
    .setNegativeButton("Ẩn danh", (dialog, which) -> {
        createChatAndOpen(itemId, currentUserId, otherUserId, true);
    })
    .show();
```

### 2. **Helper method để tạo chat**

```java
private void createChatAndOpen(long itemId, long userId1, long userId2, boolean isAnonymous) {
    FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
    
    chatManager.createChat(itemId, userId1, userId2, isAnonymous, 
        new FirebaseChatManager.ChatCallback() {
            @Override
            public void onSuccess(String chatId) {
                // Mở ChatActivity
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("chatId", chatId);
                intent.putExtra("itemId", itemId);
                intent.putExtra("otherUserId", userId2);
                intent.putExtra("otherUserName", getOtherUserName(userId2));
                intent.putExtra("isAnonymous", isAnonymous);
                startActivity(intent);
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(context, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
}
```

### 3. **Lấy danh sách chat của user**

```java
// Đã được implement trong ChatListFragment
FirebaseChatManager.getInstance().getUserChats(userId, new UserChatsCallback() {
    @Override
    public void onSuccess(List<UserChat> chats) {
        // Update UI
        adapter.setChats(chats);
    }
    
    @Override
    public void onError(String error) {
        // Handle error
    }
});
```

### 4. **Gửi tin nhắn**

```java
// Đã được implement trong ChatActivity
chatManager.sendMessage(chatId, senderId, senderName, messageText, 
    new MessageCallback() {
        @Override
        public void onSuccess() {
            // Clear input
            messageInput.setText("");
        }
        
        @Override
        public void onError(String error) {
            // Show error
        }
    });
```

---

## 🔥 Firebase Database Structure

```json
{
  "chats": {
    "123_456_789": {
      "chatId": "123_456_789",
      "itemId": 123,
      "lostUserId": 456,
      "foundUserId": 789,
      "createdAt": 1234567890000,
      "lastMessage": "Xin chào...",
      "lastMessageTime": 1234567890000,
      "participants": {
        "456": true,
        "789": true
      }
    }
  },
  
  "messages": {
    "123_456_789": {
      "msg_1": {
        "messageId": "msg_1",
        "senderId": 456,
        "senderName": "Nguyễn Văn A",
        "message": "Xin chào",
        "timestamp": 1234567890000,
        "read": true,
        "type": "text"
      }
    }
  },
  
  "userChats": {
    "456": {
      "123_456_789": {
        "chatId": "123_456_789",
        "itemId": 123,
        "otherUserId": 789,
        "otherUserName": "Trần Thị B",
        "lastMessage": "Xin chào...",
        "lastMessageTime": 1234567890000,
        "unreadCount": 0,
        "isAnonymous": false
      }
    }
  }
}
```

---

## 🧪 Testing Steps

1. **Build and run app**
   ```bash
   ./gradlew clean assembleDebug
   ```

2. **Test chat list**
   - Navigate to "Tin nhắn" tab
   - Should show empty state initially
   - Pull to refresh should work

3. **Test create chat**
   - Go to an item detail (when ItemDetailActivity exists)
   - Click "Liên hệ" button
   - Choose anonymous or normal mode
   - Chat should be created and opened

4. **Test sending messages**
   - Type message in input field
   - Click send button
   - Message should appear instantly
   - Check other user's chat list for unread count

5. **Test real-time updates**
   - Open same chat on 2 devices
   - Send message from one device
   - Should appear instantly on other device

6. **Test unread count**
   - Send messages without opening chat
   - Check chat list for unread badge
   - Open chat and verify badge disappears

---

## 🎯 Integration với ItemDetailActivity

Khi bạn tạo `ItemDetailActivity`, thêm button "Liên hệ":

### Layout (activity_item_detail.xml)
```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/contactButton"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Liên hệ"
    android:icon="@drawable/ic_chat"
    app:iconGravity="start" />
```

### Activity Code
```java
public class ItemDetailActivity extends AppCompatActivity {
    
    private MaterialButton contactButton;
    private Item item;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        
        contactButton = findViewById(R.id.contactButton);
        
        // Load item data...
        
        contactButton.setOnClickListener(v -> showAnonymousDialog());
    }
    
    private void showAnonymousDialog() {
        long currentUserId = new SharedPreferencesManager(this).getUserId();
        
        // Determine other user based on item status
        long otherUserId = item.getStatus().equals("lost") 
            ? item.getLostUserId() 
            : item.getFoundUserId();
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Chọn chế độ trò chuyện")
            .setMessage("Bạn muốn hiển thị tên của mình hay trò chuyện ẩn danh?")
            .setPositiveButton("Hiển thị tên", (dialog, which) -> {
                createChatAndOpen(item.getId(), currentUserId, otherUserId, false);
            })
            .setNegativeButton("Ẩn danh", (dialog, which) -> {
                createChatAndOpen(item.getId(), currentUserId, otherUserId, true);
            })
            .setNeutralButton("Hủy", null)
            .show();
    }
    
    private void createChatAndOpen(long itemId, long userId1, long userId2, boolean isAnonymous) {
        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
        
        chatManager.createChat(itemId, userId1, userId2, isAnonymous, 
            new FirebaseChatManager.ChatCallback() {
                @Override
                public void onSuccess(String chatId) {
                    Intent intent = new Intent(ItemDetailActivity.this, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("itemId", itemId);
                    intent.putExtra("otherUserId", userId2);
                    intent.putExtra("otherUserName", getOtherUserName(userId2));
                    intent.putExtra("isAnonymous", isAnonymous);
                    startActivity(intent);
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(ItemDetailActivity.this, 
                        "Lỗi tạo chat: " + error, 
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private String getOtherUserName(long userId) {
        // Get from item or API
        return item.getStatus().equals("lost") 
            ? item.getLostUserName() 
            : item.getFoundUserName();
    }
}
```

---

## 📱 Navigation Structure

```
MainActivity (Bottom Navigation - 7 items)
├── Xếp hạng (LeaderboardFragment)
├── Đồ vật (ItemsFragment)
├── Trang chủ (HomeFragment)
├── Quét QR (QRFragment)
├── Tin nhắn (ChatListFragment) ← NEW
├── Bản đồ (MapFragment)
└── Cá nhân (ProfileFragment)

ChatListFragment
└── Click chat → ChatActivity
    └── Send/receive messages in real-time

ItemDetailActivity (when created)
└── Click "Liên hệ" → Anonymous Dialog
    └── Choose mode → ChatActivity
```

---

## 🚀 Next Steps

1. ✅ **All chat components created**
2. ✅ **Navigation integrated**
3. ⏳ **Create ItemDetailActivity** (khi cần)
4. ⏳ **Add "Liên hệ" button** in ItemDetailActivity
5. ⏳ **Test end-to-end flow**
6. ⏳ **Add notification support** (optional)
7. ⏳ **Add image sharing** (optional future feature)

---

## 🎉 Summary

**Tính năng Chat đã được implement hoàn chỉnh:**
- ✅ Firebase Realtime Database
- ✅ Anonymous/Normal chat modes
- ✅ Real-time messaging
- ✅ Unread count badges
- ✅ Material Design 3 styling
- ✅ Empty states
- ✅ Time formatting
- ✅ Integration-ready

**Chỉ cần:**
1. Build và test
2. Tạo ItemDetailActivity (khi cần)
3. Thêm button "Liên hệ" theo code mẫu ở trên

**Firebase Console:**
- Project: `lost-and-found-fpt-campus`
- Database: Realtime Database
- Rules: Cần set cho production (hiện tại development mode)

**Production Checklist:**
- [ ] Update Firebase Database Rules for security
- [ ] Add input validation
- [ ] Add offline support
- [ ] Add message encryption (if needed)
- [ ] Add report/block user feature
- [ ] Add notification for new messages
