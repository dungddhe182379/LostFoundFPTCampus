# LOST & FOUND API - QUICK REFERENCE

## 🌐 Base URL
```
Production: http://vietsuky.com/Vietsuky2/api/lostfound
Local:      http://localhost:8080/Vietsuky2/api/lostfound
```

---

## 📱 AUTHENTICATION (No Token Required)

```
POST   /api/lostfound/auth/register      # Register new user
POST   /api/lostfound/auth/login         # Login user
```

**Body Example (Register):**
```json
{ "name": "User Name", "email": "user@fpt.edu.vn", "password": "123456", "phone": "0123456789" }
```

**Body Example (Login):**
```json
{ "email": "user@fpt.edu.vn", "password": "123456" }
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUz...",
    "user": { "id": 1, "name": "...", "email": "..." }
  }
}
```

---

## 👤 USER PROFILE (Requires Token)

```
GET    /api/lostfound/user/profile       # Get current user
PUT    /api/lostfound/user/profile       # Update profile
GET    /api/lostfound/user/{id}          # Get user by ID
```

---

## 📦 ITEMS (Requires Token)

```
GET    /api/lostfound/items                    # Get all items
GET    /api/lostfound/items?userId={id}        # Get items by user
GET    /api/lostfound/items?category={cat}     # Get items by category
GET    /api/lostfound/items/{id}               # Get item by ID
GET    /api/lostfound/items/status/{status}    # Get by status (lost/found/returned)
GET    /api/lostfound/items/search?q={keyword} # Search items
POST   /api/lostfound/items                    # Create new item
PUT    /api/lostfound/items/{id}               # Update item
DELETE /api/lostfound/items/{id}               # Delete item
```

**Body Example (Create Item):**
```json
{
  "title": "Lost iPhone 15",
  "description": "Lost near library",
  "category": "electronics",
  "status": "lost",
  "latitude": 21.0285,
  "longitude": 105.8542,
  "imageUrl": "https://example.com/image.jpg"
}
```

---

## 🔔 NOTIFICATIONS (Requires Token)

```
GET    /api/lostfound/notifications                # Get all notifications
GET    /api/lostfound/notifications/unread         # Get unread only
GET    /api/lostfound/notifications/count          # Get unread count
POST   /api/lostfound/notifications                # Create notification
PUT    /api/lostfound/notifications/{id}/read      # Mark as read
PUT    /api/lostfound/notifications/read-all       # Mark all as read
DELETE /api/lostfound/notifications/{id}           # Delete notification
```

---

## 🔑 AUTHENTICATION HEADER

Add this header to all requests (except register/login):
```
Authorization: Bearer {your_jwt_token}
```

**Example:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ✅ SUCCESS RESPONSE FORMAT

```json
{
  "success": true,
  "message": "Optional message",
  "data": { /* your data */ },
  "timestamp": 1730454600000
}
```

---

## ❌ ERROR RESPONSE FORMAT

```json
{
  "success": false,
  "error": "Error message",
  "timestamp": 1730454600000
}
```

---

## 📊 HTTP STATUS CODES

- `200` OK - Success
- `400` Bad Request - Invalid data
- `401` Unauthorized - Invalid/missing token
- `403` Forbidden - No permission
- `404` Not Found
- `409` Conflict - Duplicate data
- `500` Internal Server Error

---

## 🚀 QUICK TEST WITH CURL

**1. Register:**
```bash
curl -X POST http://vietsuky.com/Vietsuky2/api/lostfound/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@fpt.edu.vn","password":"123456"}'
```

**2. Login:**
```bash
curl -X POST http://vietsuky.com/Vietsuky2/api/lostfound/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@fpt.edu.vn","password":"123456"}'
```

**3. Get Items (use token from login):**
```bash
curl -X GET http://vietsuky.com/Vietsuky2/api/lostfound/items \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**4. Create Item:**
```bash
curl -X POST http://vietsuky.com/Vietsuky2/api/lostfound/items \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"title":"Lost Phone","description":"Lost iPhone","category":"electronics","status":"lost"}'
```

---

## 📱 ANDROID RETROFIT EXAMPLE

```java
public interface LostFoundApi {
    @POST("api/lostfound/auth/login")
    Call<ApiResponse<LoginData>> login(@Body LoginRequest request);
    
    @GET("api/lostfound/items")
    Call<ApiResponse<List<Item>>> getItems(
        @Header("Authorization") String token
    );
    
    @POST("api/lostfound/items")
    Call<ApiResponse<Item>> createItem(
        @Header("Authorization") String token,
        @Body Item item
    );
}

// Usage
String token = "Bearer " + yourSavedToken;
api.getItems(token).enqueue(new Callback<>() { ... });
```

---

## 🗂️ DATABASE INFO

- **Database Name:** `lostfound_fptcampus`
- **Tables:** users, items, photos, histories, karma_logs, notifications, roles, user_roles
- **Character Set:** utf8mb4

---

## ⚠️ IMPORTANT NOTES

1. **Token expires in 7 days** - save it securely
2. **All POST/PUT requests** need `Content-Type: application/json` header
3. **User can only edit/delete their own items**
4. **Status values:** `lost`, `found`, `returned`
5. **Timestamps** in ISO format: `2025-11-01T10:30:00`

---

**Last Updated:** November 1, 2025
