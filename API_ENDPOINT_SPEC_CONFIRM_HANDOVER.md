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

### 6. Get All Users 🆕
- **URL:** `GET /api/lostfound/user`
- **Headers:** `Authorization: Bearer {token}`
- **Description:** Get list of all registered users (sorted by creation date, newest first)
- **Last Test:** November 3, 2025 - ✅ **66 users** on production
- **Response Success (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 118,
      "uuid": "fa622609-f4d6-4478-bc03-55e84df24d1c",
      "name": "Do Duy Dung 2",
      "email": "dung2@fpt.edu.vn",
      "phone": "0123456765",
      "karma": 0,
      "createdAt": "2025-11-03T16:24:12"
    },
    {
      "id": 117,
      "uuid": "550e8400-e29b-41d4-a716-446655440001",
      "name": "Trinh Van Tuan",
      "email": "tuantvhe180495@fpt.edu.vn",
      "phone": "0987654321",
      "karma": 0,
      "createdAt": "2025-11-03T12:50:32"
    },
    {
      "id": 11,
      "uuid": "a13f2cb5-b747-11f0-9256-fa163e438cc0",
      "name": "Nguyễn Văn An",
      "email": "an.nv@fpt.edu.vn",
      "phone": "0901234567",
      "karma": 530,
      "createdAt": "2025-08-04T10:30:00"
    }
  ],
  "timestamp": 1762165057257
}
```
- **Statistics (Nov 3, 2025):**
  - **Total Users:** 66
  - **Total Karma:** 7,336
  - **Average Karma:** 111.15
  - **Highest Karma:** 530 (Nguyễn Văn An)
- **Notes:**
  - ✅ Password hash is **NOT** included in response (security verified)
  - ✅ Requires authentication (JWT token)
  - ✅ Returns all users in database, sorted by `createdAt DESC`
  - ✅ Production tested: 66 users retrieved successfully
  - **Use cases:** Admin panels, leaderboards, user search, analytics

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
  "data": { ... },
  "timestamp": 1730454600000
}
```

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

---

## 🔔 NOTIFICATION APIs

### 14. Get All Notifications
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

### 15. Get Unread Notifications
- **URL:** `GET /api/lostfound/notifications/unread`
- **Headers:** `Authorization: Bearer {token}`
- **Response Success (200):** Array of unread notifications

### 16. Get Unread Count
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

### 17. Create Notification
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

### 18. Mark Notification As Read
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

### 19. Mark All Notifications As Read
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

### 20. Delete Notification
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

## � SYSTEM APIs

### 21. Get API Specifications 🆕
- **URL:** `GET /api/lostfound/spec`
- **Headers:** None required (public endpoint)
- **Description:** Get complete API specifications, endpoints list, and system information
- **Response Success (200):**
```json
{
  "success": true,
  "data": {
    "name": "Lost & Found FPT Campus API",
    "version": "1.1.0",
    "description": "RESTful API for Lost & Found item management system",
    "baseUrl": "/api/lostfound",
    "environment": "production",
    "authentication": {
      "type": "JWT",
      "header": "Authorization",
      "format": "Bearer {token}",
      "tokenExpiry": "7 days"
    },
    "endpoints": [
      {
        "method": "POST",
        "path": "/auth/register",
        "description": "Register new user",
        "requiresAuth": false,
        "category": "Authentication"
      },
      {
        "method": "GET",
        "path": "/user",
        "description": "Get all users",
        "requiresAuth": true,
        "category": "Users"
      }
      // ... more endpoints
    ],
    "statistics": {
      "totalEndpoints": 21,
      "authEndpoints": 2,
      "userEndpoints": 4,
      "itemEndpoints": 7,
      "notificationEndpoints": 7,
      "systemEndpoints": 1
    },
    "itemCategories": [
      "electronics", "documents", "wallet", "keys", 
      "clothes", "books", "accessories", "others"
    ],
    "itemStatuses": ["lost", "found", "returned"],
    "responseFormat": {
      "success": "boolean",
      "message": "string (optional)",
      "data": "object or array",
      "timestamp": "long (epoch milliseconds)"
    },
    "errorCodes": {
      "200": "Success",
      "400": "Bad Request",
      "401": "Unauthorized",
      "404": "Not Found",
      "500": "Internal Server Error"
    },
    "server": {
      "host": "vietsuky.com",
      "protocol": "HTTPS",
      "port": 443,
      "timezone": "Asia/Ho_Chi_Minh"
    }
  },
  "timestamp": 1730454600000
}
```
- **Use Cases:**
  - Auto-generate API documentation
  - Display available endpoints in admin panel
  - API discovery for client applications
  - Version checking and compatibility
  - Client-side validation of request format

---

## �🔐 AUTHENTICATION FLOW

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

**Server URL:** https://vietsuky.com/api/lostfound (production)
**Database:** lostfound_fptcampus

---

## 📊 API SUMMARY

**Total Endpoints:** 21 APIs
- **Authentication:** 2 APIs
- **User Management:** 4 APIs
- **Item Management:** 7 APIs
- **Notifications:** 7 APIs
- **System:** 1 API (API Spec 🆕)

**Latest Updates:**
- **November 3, 2025** - ✅ Get All Users API tested on production: **66 users**, avg karma 111.15
- November 2, 2025 - Added Get All Users endpoint (API #6)
- November 3, 2025 - Added API Specifications endpoint (API #21)
- November 2, 2025 - Added Get All Users API

---

**Generated:** November 3, 2025
**Version:** 1.2
**Author:** API Documentation Generator
