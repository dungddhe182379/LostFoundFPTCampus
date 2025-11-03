# LOST & FOUND FPT CAMPUS - API DOCUMENTATION

**Base URL:** `https://vietsuky.com/api/lostfound` (hoặc `http://localhost:8080/Vietsuky2/api/lostfound`)

**Authentication:** JWT Token (Bearer Token trong header Authorization)

---

## 📱 AUTHENTICATION APIs

### 1. Register New User
- **URL:** `POST /api/lostfound/auth/register`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "name": "Nguyen Van A",
  "email": "nguyenvana@fpt.edu.vn",
  "password": "password123",
  "phone": "0123456789"
}
```
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Nguyen Van A",
      "email": "nguyenvana@fpt.edu.vn",
      "phone": "0123456789",
      "avatarUrl": null,
      "karma": 0,
      "createdAt": "2025-11-01T10:30:00"
    }
  },
  "timestamp": 1730454600000
}
```

### 2. Login User
- **URL:** `POST /api/lostfound/auth/login`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "email": "nguyenvana@fpt.edu.vn",
  "password": "password123"
}
```
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": { ... }
  },
  "timestamp": 1730454600000
}
```
- **Response Error (401):**
```json
{
  "success": false,
  "error": "Invalid email or password",
  "timestamp": 1730454600000
}
```

---

## 👤 USER APIs

### 3. Get Current User Profile
- **URL:** `GET /api/lostfound/user/profile`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "uuid": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Nguyen Van A",
    "email": "nguyenvana@fpt.edu.vn",
    "phone": "0123456789",
    "avatarUrl": "https://example.com/avatar.jpg",
    "karma": 15,
    "createdAt": "2025-11-01T10:30:00"
  },
  "timestamp": 1730454600000
}
```

### 4. Update User Profile
- **URL:** `PUT /api/lostfound/user/profile`
- **Headers:** 
  - `Authorization: Bearer {token}`
  - `Content-Type: application/json`
- **Body:**
```json
{
  "name": "Nguyen Van A Updated",
  "phone": "0987654321",
  "avatarUrl": "https://example.com/new-avatar.jpg"
}
```
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": { ... },
  "timestamp": 1730454600000
}
```

### 5. Get User By ID
- **URL:** `GET /api/lostfound/user/{userId}`
- **Headers:** `Authorization: Bearer {token}`
- **Example:** `GET /api/lostfound/user/5`
- **Response Success (200):** Same as profile response

### 6. Get All Users 🆕
- **URL:** `GET /api/lostfound/user`
- **Headers:** `Authorization: Bearer {token}`
- **Description:** Get list of all registered users (sorted by creation date, newest first)
- **Response Success (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "uuid": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Nguyen Van A",
      "email": "nguyenvana@fpt.edu.vn",
      "phone": "0123456789",
      "avatarUrl": "https://example.com/avatar1.jpg",
      "karma": 100,
      "createdAt": "2025-11-01T10:30:00"
    },
    {
      "id": 2,
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Tran Thi B",
      "email": "tranthib@fpt.edu.vn",
      "phone": "0987654321",
      "avatarUrl": null,
      "karma": 50,
      "createdAt": "2025-11-01T11:00:00"
    }
  ],
  "timestamp": 1730454600000
}
```
- **Notes:**
  - Password hash is **NOT** included in response (security)
  - Requires authentication (JWT token)
  - Returns all users in database
  - Useful for admin panels, leaderboards, user selection

---

## 📦 ITEM APIs (Lost & Found Items)

### 7. Get All Items
- **URL:** `GET /api/lostfound/items`
- **Headers:** `Authorization: Bearer {token}`
- **Query Parameters (Optional):**
  - `userId` - Filter by user ID
  - `category` - Filter by category
- **Example:** 
  - `GET /api/lostfound/items` - Get all items
  - `GET /api/lostfound/items?userId=5` - Get items by user
  - `GET /api/lostfound/items?category=electronics` - Get items by category
- **Response Success (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "uuid": "item-uuid-123",
      "userId": 5,
      "lostUserId": 5,
      "foundUserId": null,
      "returnedUserId": null,
      "title": "Lost iPhone 15",
      "description": "Lost my iPhone 15 Pro Max near library",
      "category": "electronics",
      "status": "lost",
      "latitude": 21.0285,
      "longitude": 105.8542,
      "imageUrl": "https://example.com/item.jpg",
      "createdAt": "2025-11-01T10:30:00",
      "updatedAt": "2025-11-01T10:30:00"
    }
  ],
  "timestamp": 1730454600000
}
```
- **User Role Fields (NEW):**
  - `lostUserId` - ID of user who lost the item (owner)
  - `foundUserId` - ID of user who found the item
  - `returnedUserId` - ID of user who received the item back after handover

### 8. Get Item By ID
- **URL:** `GET /api/lostfound/items/{itemId}`
- **Headers:** `Authorization: Bearer {token}`
- **Example:** `GET /api/lostfound/items/1`
- **Response Success (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "uuid": "item-uuid-123",
    "userId": 5,
    "lostUserId": 5,
    "foundUserId": null,
    "returnedUserId": null,
    "title": "Lost iPhone 15",
    "description": "Lost my iPhone 15 Pro Max near library",
    "category": "electronics",
    "status": "lost",
    "latitude": 21.0285,
    "longitude": 105.8542,
    "imageUrl": "https://example.com/item.jpg",
    "createdAt": "2025-11-01T10:30:00",
    "updatedAt": "2025-11-01T10:30:00"
  },
  "timestamp": 1730454600000
}
```

### 9. Get Items By Status
- **URL:** `GET /api/lostfound/items/status/{status}`
- **Headers:** `Authorization: Bearer {token}`
- **Status Values:** `lost`, `found`, `returned`
- **Example:** 
  - `GET /api/lostfound/items/status/lost` - Get all lost items
  - `GET /api/lostfound/items/status/found` - Get all found items
  - `GET /api/lostfound/items/status/returned` - Get all returned items
- **Response Success (200):** Array of items

### 10. Search Items
- **URL:** `GET /api/lostfound/items/search?q={keyword}`
- **Headers:** `Authorization: Bearer {token}`
- **Query Parameters:**
  - `q` - Search keyword (searches in title and description)
- **Example:** `GET /api/lostfound/items/search?q=iPhone`
- **Response Success (200):** Array of matching items

### 11. Create New Item
- **URL:** `POST /api/lostfound/items`
- **Headers:** 
  - `Authorization: Bearer {token}`
  - `Content-Type: application/json`
- **Body:**
```json
{
  "title": "Lost iPhone 15",
  "description": "Lost my iPhone 15 Pro Max near library",
  "category": "electronics",
  "status": "lost",
  "latitude": 21.0285,
  "longitude": 105.8542,
  "imageUrl": "https://example.com/item.jpg"
}
```
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Item created successfully",
  "data": {
    "id": 1,
    "uuid": "item-uuid-123",
    "userId": 5,
    "lostUserId": 5,
    "foundUserId": null,
    "returnedUserId": null,
    "title": "Lost iPhone 15",
    "status": "lost",
    ...
  },
  "timestamp": 1730454600000
}
```
- **Auto-Set User Roles:**
  - If `status = "lost"` → `lostUserId` is set to current user
  - If `status = "found"` → `foundUserId` is set to current user
  - `returnedUserId` is set later via confirm-handover endpoint

### 12. Update Item
- **URL:** `PUT /api/lostfound/items/{itemId}`
- **Headers:** 
  - `Authorization: Bearer {token}`
  - `Content-Type: application/json`
- **Body:** (Only include fields to update)
```json
{
  "title": "Updated title",
  "description": "Updated description",
  "status": "found",
  "imageUrl": "https://example.com/new-image.jpg"
}
```
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Item updated successfully",
  "data": { ... },
  "timestamp": 1730454600000
}
```

### 13. Delete Item
- **URL:** `DELETE /api/lostfound/items/{itemId}`
- **Headers:** `Authorization: Bearer {token}`
- **Example:** `DELETE /api/lostfound/items/1`
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Item deleted successfully",
  "data": null,
  "timestamp": 1730454600000
}
```

### 14. Confirm Handover (QR Code) 🆕
- **URL:** `POST /api/lostfound/items/{itemId}/confirm-handover`
- **Headers:** 
  - `Authorization: Bearer {token}`
  - `Content-Type: application/json`
- **Description:** Confirm item handover using QR code token. Updates item status, sets user role fields, and awards karma to both giver and receiver.
- **Body:**
```json
{
  "qrToken": "TOKEN_1730678400000"
}
```
- **Token Format:** `TOKEN_{timestamp_in_milliseconds}`
- **Token Rules:**
  - Must start with `TOKEN_`
  - Valid for 24 hours from generation
  - Can only be used once
  - Timestamp must not be in the future
- **Validation:**
  - ❌ Scanner cannot be the item creator (userId)
  - ✅ Scanner must be different from item owner
- **Example:** `POST /api/lostfound/items/38/confirm-handover`
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Handover confirmed successfully",
  "data": {
    "id": 38,
    "uuid": "item-uuid-123",
    "userId": 10,
    "lostUserId": 5,
    "foundUserId": 10,
    "returnedUserId": 5,
    "title": "iPhone 15 Pro Max",
    "description": "Màu xanh, còn mới 99%",
    "category": "electronics",
    "status": "returned",
    "latitude": 21.0285,
    "longitude": 105.8542,
    "imageUrl": "https://example.com/item.jpg",
    "createdAt": "2025-11-01T10:30:00",
    "updatedAt": "2025-11-03T15:45:30"
  },
  "timestamp": 1730678730000
}
```
- **After Handover:**
  - ✅ `lostUserId` is set based on original lost item owner
  - ✅ `foundUserId` is set based on who found the item (giver)
  - ✅ `returnedUserId` is set to the receiver (person who scanned QR)
  - ✅ `status` changes to `returned`
  - ✅ History record is created with giver and receiver
  - ✅ **Karma +10** awarded to both giver and receiver
- **Response Error (400) - Invalid/Expired Token:**
```json
{
  "success": false,
  "error": "Invalid or expired QR token: Token expired (older than 24 hours)",
  "timestamp": 1730678730000
}
```
- **Response Error (400) - Token Already Used:**
```json
{
  "success": false,
  "error": "Invalid or expired QR token: Token already used",
  "timestamp": 1730678730000
}
```
- **Response Error (404) - Item Not Found:**
```json
{
  "success": false,
  "error": "Item not found with ID: 99999",
  "timestamp": 1730678730000
}
```
- **Response Error (409) - Item Already Returned:**
```json
{
  "success": false,
  "error": "Item already marked as returned",
  "timestamp": 1730678730000
}
```
- **Notes:**
  - ✅ **Ownership check** - Scanner cannot be the item creator (checked both client & server side)
  - ✅ Automatically changes item status from `found` → `returned`
  - ✅ Sets all 3 user role fields: `lostUserId`, `foundUserId`, `returnedUserId`
  - ✅ Creates history record with:
    - `giverId` = `foundUserId` (person who found the item)
    - `receiverId` = `returnedUserId` (person receiving it back)
  - ✅ **Karma Rewards:**
    - Giver (foundUserId): +10 karma for returning item
    - Receiver (returnedUserId): +10 karma for successful recovery
  - QR token should be generated by frontend/Android app using current timestamp
  - Example token generation: `"TOKEN_" + System.currentTimeMillis()`

---

## 🔔 NOTIFICATION APIs

### 15. Get All Notifications
- **URL:** `GET /api/lostfound/notifications`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 5,
      "title": "Item Found",
      "body": "Someone found your lost item!",
      "isRead": false,
      "createdAt": "2025-11-01T10:30:00"
    }
  ],
  "timestamp": 1730454600000
}
```

### 16. Get Unread Notifications
- **URL:** `GET /api/lostfound/notifications/unread`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):** Array of unread notifications

### 17. Get Unread Count
- **URL:** `GET /api/lostfound/notifications/count`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):**
```json
{
  "success": true,
  "data": {
    "count": 5
  },
  "timestamp": 1730454600000
}
```

### 18. Create Notification
- **URL:** `POST /api/lostfound/notifications`
- **Headers:** 
  - `Authorization: Bearer {token}`
  - `Content-Type: application/json`
- **Body:**
```json
{
  "title": "New Message",
  "body": "You have a new message about your item"
}
```
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Notification created",
  "data": { ... },
  "timestamp": 1730454600000
}
```

### 19. Mark Notification As Read
- **URL:** `PUT /api/lostfound/notifications/{notificationId}/read`
- **Headers:** `Authorization: Bearer {token}`
- **Example:** `PUT /api/lostfound/notifications/1/read`
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Notification marked as read",
  "data": null,
  "timestamp": 1730454600000
}
```

### 20. Mark All Notifications As Read
- **URL:** `PUT /api/lostfound/notifications/read-all`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):**
```json
{
  "success": true,
  "message": "All notifications marked as read",
  "data": null,
  "timestamp": 1730454600000
}
```

### 21. Delete Notification
- **URL:** `DELETE /api/lostfound/notifications/{notificationId}`
- **Headers:** `Authorization: Bearer {token}`
- **Example:** `DELETE /api/lostfound/notifications/1`
- **Response Success (200):**
```json
{
  "success": true,
  "message": "Notification deleted",
  "data": null,
  "timestamp": 1730454600000
}
```

---

## 🔐 AUTHENTICATION FLOW

### How to use JWT Token:

1. **Register or Login** to get JWT token
2. **Store the token** in your Android app (SharedPreferences)
3. **Include token in every API request** as header:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```
4. **Token expires in 7 days** - need to login again after expiration

### Example Android Code (Retrofit):

```java
// Add Authorization header to all requests
@Headers("Authorization: Bearer " + token)
@GET("api/lostfound/items")
Call<ApiResponse<List<Item>>> getAllItems();
```

Or use Interceptor:

```java
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(chain -> {
        Request request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer " + token)
            .build();
        return chain.proceed(request);
    })
    .build();
```

---

## ❌ ERROR RESPONSES

All error responses follow this format:

```json
{
  "success": false,
  "error": "Error message here",
  "timestamp": 1730454600000
}
```

**Common HTTP Status Codes:**
- `200` - Success
- `400` - Bad Request (validation error)
- `401` - Unauthorized (invalid or missing token)
- `403` - Forbidden (no permission)
- `404` - Not Found
- `409` - Conflict (e.g., email already registered)
- `500` - Internal Server Error

---

## 📋 ITEM STATUS VALUES

- `lost` - Item is lost (default)
  - `lostUserId` is set to user who reported it lost
- `found` - Item has been found
  - `foundUserId` is set to user who found it
- `returned` - Item has been returned to owner
  - `returnedUserId` is set to user who received it back

## 👥 USER ROLE FIELDS (Updated Schema)

**New Fields in Items Table:**

| Field | Type | Description | When Set |
|-------|------|-------------|----------|
| `lostUserId` | Long | User who lost the item (owner) | When item created with `status="lost"` |
| `foundUserId` | Long | User who found the item | When item created with `status="found"` |
| `returnedUserId` | Long | User who received the item back | When handover confirmed via QR code |

**Example Workflow:**

1. **User A loses iPhone** → Creates item: `lostUserId=A`, `status=lost`
2. **User B finds iPhone** → Updates/Creates: `foundUserId=B`, `status=found`
3. **User A scans QR** → Confirms handover: `returnedUserId=A`, `status=returned`

**History Record Created:**
- `giverId` = User B (found_user_id)
- `receiverId` = User A (returned_user_id)
- Tracks the complete handover transaction

---

## 📝 NOTES

1. **All timestamps** are in ISO 8601 format: `yyyy-MM-dd'T'HH:mm:ss`
2. **Latitude/Longitude** are decimal values (e.g., 21.0285, 105.8542)
3. **Categories** are strings (e.g., "electronics", "documents", "keys", "wallet")
4. **Image URLs** should be full URLs to uploaded images
5. **User can only update/delete their own items**
6. **Token must be included in all requests except register/login**

---

## 🚀 QUICK START FOR ANDROID

### 1. Register User
```
POST https://vietsuky.com/api/lostfound/auth/register
Body: { "name": "Test", "email": "test@fpt.edu.vn", "password": "123456" }
```

### 2. Get Token from Response
```json
{ "data": { "token": "eyJhbG..." } }
```

### 3. Use Token for Other Requests
```
GET https://vietsuky.com/api/lostfound/items
Header: Authorization: Bearer eyJhbG...
```

---

## 📱 QR CODE HANDOVER WORKFLOW

### Scenario: Giver (người tìm thấy) trả đồ cho Receiver (người nhận)

**Step 1: Giver creates item**
```
POST /api/lostfound/items
Body: {
  "title": "Found iPhone 15",
  "status": "found",
  ...
}
```

**Step 2: Giver generates QR code**
```kotlin
// Android/Frontend code
val timestamp = System.currentTimeMillis()
val qrToken = "TOKEN_$timestamp"
val qrData = """
{
  "itemId": 38,
  "qrToken": "$qrToken"
}
"""
// Generate QR code from qrData string
```

**Step 3: Receiver scans QR code**
- Receiver opens app and scans QR code
- App extracts `itemId` and `qrToken` from QR data

**Step 4: Receiver confirms handover**
```
POST /api/lostfound/items/38/confirm-handover
Header: Authorization: Bearer {receiver_jwt_token}
Body: {
  "qrToken": "TOKEN_1730678400000"
}
```

**Step 5: Backend validates and updates**
- ✅ Token format valid
- ✅ Token not expired (< 24h)
- ✅ Token not used before
- ✅ Item exists and not already returned
- ✅ Scanner is not the item creator (ownership check)
- ✅ Update item status: `found` → `returned`
- ✅ Set all 3 user role fields: `lostUserId`, `foundUserId`, `returnedUserId`
- ✅ Create history record (giver + receiver + token)
- ✅ Award +10 karma to giver (foundUserId)
- ✅ Award +10 karma to receiver (returnedUserId)

**Step 6: Success response**
```json
{
  "success": true,
  "message": "Handover confirmed successfully",
  "data": { "id": 38, "status": "returned", ... }
}
```

### Android Example Code:

```kotlin
// Generate QR Token
fun generateQRToken(): String {
    val timestamp = System.currentTimeMillis()
    return "TOKEN_$timestamp"
}

// Create QR Data
data class QRData(
    val itemId: Long,
    val qrToken: String
)

val qrData = QRData(itemId = 38, qrToken = generateQRToken())
val qrJson = Gson().toJson(qrData)
// Generate QR code bitmap from qrJson

// Confirm Handover API Call
data class ConfirmHandoverRequest(val qrToken: String)

suspend fun confirmHandover(itemId: Long, qrToken: String): ApiResponse<Item> {
    return apiService.confirmHandover(
        itemId = itemId,
        request = ConfirmHandoverRequest(qrToken)
    )
}
```

---

## 📞 SUPPORT

If you encounter any issues with the API, please check:
1. Token is valid and not expired
2. Content-Type header is set correctly for POST/PUT requests
3. Request body is valid JSON
4. All required fields are provided

**Server URL:** https://vietsuky.com/api/lostfound (production)
**Database:** lostfound_fptcampus

---

## 📊 API SUMMARY

**Total Endpoints:** 21 APIs
- **Authentication:** 2 APIs
- **User Management:** 4 APIs
- **Item Management:** 8 APIs (including Confirm Handover 🆕)
- **Notifications:** 7 APIs

**Latest Update:** November 3, 2025 - Added Confirm Handover API (QR Code)

---

**Generated:** November 3, 2025
**Version:** 1.2
**Author:** API Documentation Generator
