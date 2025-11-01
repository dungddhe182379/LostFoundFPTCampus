# LOST & FOUND FPT CAMPUS - API DOCUMENTATION

**Base URL:** `http://vietsuky.com/api/lostfound` (hoặc `http://localhost:8080/Vietsuky2/api/lostfound`)

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

---

## 📦 ITEM APIs (Lost & Found Items)

### 6. Get All Items
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

### 7. Get Item By ID
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

### 8. Get Items By Status
- **URL:** `GET /api/lostfound/items/status/{status}`
- **Headers:** `Authorization: Bearer {token}`
- **Status Values:** `lost`, `found`, `returned`
- **Example:** 
  - `GET /api/lostfound/items/status/lost` - Get all lost items
  - `GET /api/lostfound/items/status/found` - Get all found items
  - `GET /api/lostfound/items/status/returned` - Get all returned items
- **Response Success (200):** Array of items

### 9. Search Items
- **URL:** `GET /api/lostfound/items/search?q={keyword}`
- **Headers:** `Authorization: Bearer {token}`
- **Query Parameters:**
  - `q` - Search keyword (searches in title and description)
- **Example:** `GET /api/lostfound/items/search?q=iPhone`
- **Response Success (200):** Array of matching items

### 10. Create New Item
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
  "data": { ... },
  "timestamp": 1730454600000
}
```

### 11. Update Item
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

### 12. Delete Item
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

---

## 🔔 NOTIFICATION APIs

### 13. Get All Notifications
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

### 14. Get Unread Notifications
- **URL:** `GET /api/lostfound/notifications/unread`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):** Array of unread notifications

### 15. Get Unread Count
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

### 16. Create Notification
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

### 17. Mark Notification As Read
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

### 18. Mark All Notifications As Read
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

### 19. Delete Notification
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
- `found` - Item has been found
- `returned` - Item has been returned to owner

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
POST http://vietsuky.com/api/lostfound/auth/register
Body: { "name": "Test", "email": "test@fpt.edu.vn", "password": "123456" }
```

### 2. Get Token from Response
```json
{ "data": { "token": "eyJhbG..." } }
```

### 3. Use Token for Other Requests
```
GET http://vietsuky.com/api/lostfound/items
Header: Authorization: Bearer eyJhbG...
```

---

## 📞 SUPPORT

If you encounter any issues with the API, please check:
1. Token is valid and not expired
2. Content-Type header is set correctly for POST/PUT requests
3. Request body is valid JSON
4. All required fields are provided

**Server URL:** http://vietsuky.com/Vietsuky2/ (production)
**Database:** lostfound_fptcampus

---

**Generated:** November 1, 2025
**Version:** 1.0
**Author:** API Documentation Generator
