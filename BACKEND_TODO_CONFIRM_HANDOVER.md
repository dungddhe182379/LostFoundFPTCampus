# 🚀 BACKEND API TODO - CONFIRM HANDOVER ENDPOINT

**Priority:** HIGH  
**Estimated Time:** 1-2 days  
**Deadline:** ASAP (blocking Android feature)

---

## 📝 YÊU CẦU

Tạo endpoint mới để xử lý QR code handover:

```
POST /api/lostfound/items/{itemId}/confirm-handover
```

---

## 🎯 MỤC ĐÍCH

Cho phép **người nhận** (receiver) quét QR code và confirm nhận đồ từ **người tìm thấy** (giver), không cần phải là owner của item.

**Vấn đề hiện tại:** PUT /items/{itemId} check ownership → 403 Forbidden khi receiver cập nhật  
**Giải pháp:** Validate QR token thay vì check ownership

---

## 📋 SPEC CHI TIẾT

### 1. REQUEST

**Method:** POST  
**Path:** `/api/lostfound/items/{itemId}/confirm-handover`  
**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameter:**
- `itemId` (long) - ID của item

**Request Body:**
```json
{
  "qrToken": "TOKEN_1730678400000"
}
```

**Token Format:** `TOKEN_{timestamp_in_milliseconds}`

---

### 2. SUCCESS RESPONSE (200 OK)

```json
{
  "success": true,
  "message": "Handover confirmed successfully",
  "data": {
    "id": 38,
    "uuid": "item-uuid-123",
    "userId": 10,
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

---

### 3. ERROR RESPONSES

#### 400 Bad Request - Invalid/Expired Token
```json
{
  "success": false,
  "error": "Invalid or expired QR token",
  "timestamp": 1730678730000
}
```

**Khi nào trả 400:**
- Token format sai (không bắt đầu bằng "TOKEN_")
- Token đã hết hạn (> 24 giờ)
- Token đã được sử dụng rồi

#### 401 Unauthorized
```json
{
  "success": false,
  "error": "Unauthorized - Invalid JWT token",
  "timestamp": 1730678730000
}
```

#### 404 Not Found
```json
{
  "success": false,
  "error": "Item not found",
  "timestamp": 1730678730000
}
```

#### 409 Conflict - Already Returned
```json
{
  "success": false,
  "error": "Item already marked as returned",
  "timestamp": 1730678730000
}
```

---

## 🔧 BACKEND CẦN LÀM GÌ

### Step 1: Thêm DTO (Request)

```java
public class ConfirmHandoverRequest {
    private String qrToken;
    
    // Constructors
    public ConfirmHandoverRequest() {}
    
    public ConfirmHandoverRequest(String qrToken) {
        this.qrToken = qrToken;
    }
    
    // Getters/Setters
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}
```

---

### Step 2: Thêm Controller Method

```java
@PostMapping("/{itemId}/confirm-handover")
public ResponseEntity<ApiResponse<?>> confirmHandover(
        @PathVariable Long itemId,
        @RequestBody ConfirmHandoverRequest request,
        @RequestHeader("Authorization") String authHeader) {
    
    try {
        // Extract receiverId từ JWT token
        String token = authHeader.replace("Bearer ", "");
        Long receiverId = jwtService.getUserIdFromToken(token);
        
        // Validate & confirm handover
        LostItem updatedItem = itemService.confirmHandover(
            itemId, 
            request.getQrToken(), 
            receiverId
        );
        
        return ResponseEntity.ok(
            ApiResponse.success("Handover confirmed successfully", updatedItem)
        );
        
    } catch (InvalidTokenException e) {
        return ResponseEntity.badRequest().body(
            ApiResponse.error("Invalid or expired QR token")
        );
    } catch (ItemNotFoundException e) {
        return ResponseEntity.status(404).body(
            ApiResponse.error("Item not found")
        );
    } catch (ItemAlreadyReturnedException e) {
        return ResponseEntity.status(409).body(
            ApiResponse.error("Item already marked as returned")
        );
    }
}
```

---

### Step 3: Thêm Service Logic

```java
@Transactional
public LostItem confirmHandover(Long itemId, String qrToken, Long receiverId) 
        throws InvalidTokenException, ItemNotFoundException, ItemAlreadyReturnedException {
    
    // 1. VALIDATE TOKEN FORMAT
    if (!qrToken.startsWith("TOKEN_") || qrToken.length() <= 6) {
        throw new InvalidTokenException("Invalid token format");
    }
    
    // 2. CHECK TOKEN EXPIRATION (24 hours)
    try {
        long timestamp = Long.parseLong(qrToken.substring(6));
        long now = System.currentTimeMillis();
        if ((now - timestamp) > 86400000) { // 24 hours = 86400000 ms
            throw new InvalidTokenException("Token expired");
        }
    } catch (NumberFormatException e) {
        throw new InvalidTokenException("Invalid token format");
    }
    
    // 3. GET ITEM
    LostItem item = itemRepository.findById(itemId)
        .orElseThrow(() -> new ItemNotFoundException("Item not found"));
    
    // 4. CHECK IF ALREADY RETURNED
    if ("returned".equals(item.getStatus())) {
        throw new ItemAlreadyReturnedException("Item already returned");
    }
    
    // 5. CHECK IF TOKEN ALREADY USED
    boolean tokenUsed = historyRepository.existsByQrToken(qrToken);
    if (tokenUsed) {
        throw new InvalidTokenException("Token already used");
    }
    
    // 6. UPDATE ITEM STATUS
    item.setStatus("returned");
    item.setUpdatedAt(LocalDateTime.now());
    LostItem updatedItem = itemRepository.save(item);
    
    // 7. CREATE HISTORY RECORD
    History history = new History();
    history.setItemId(itemId);
    history.setGiverId(item.getUserId()); // Item owner
    history.setReceiverId(receiverId);    // Current user
    history.setQrToken(qrToken);
    history.setAction("handover_confirmed");
    history.setCreatedAt(LocalDateTime.now());
    historyRepository.save(history);
    
    return updatedItem;
}
```

---

### Step 4: Database Update

**Thêm column vào bảng `histories`:**

```sql
ALTER TABLE histories 
ADD COLUMN qr_token VARCHAR(50) DEFAULT NULL,
ADD INDEX idx_qr_token (qr_token);
```

---

### Step 5: Repository Method

```java
public interface HistoryRepository extends JpaRepository<History, Long> {
    boolean existsByQrToken(String qrToken);
}
```

---

### Step 6: Custom Exceptions

```java
public class InvalidTokenException extends Exception {
    public InvalidTokenException(String message) {
        super(message);
    }
}

public class ItemNotFoundException extends Exception {
    public ItemNotFoundException(String message) {
        super(message);
    }
}

public class ItemAlreadyReturnedException extends Exception {
    public ItemAlreadyReturnedException(String message) {
        super(message);
    }
}
```

---

## ✅ CHECKLIST

- [ ] Tạo `ConfirmHandoverRequest.java`
- [ ] Thêm method `confirmHandover()` vào `ItemController`
- [ ] Thêm method `confirmHandover()` vào `ItemService`
- [ ] Tạo 3 custom exceptions
- [ ] Thêm method `existsByQrToken()` vào `HistoryRepository`
- [ ] Run SQL: `ALTER TABLE histories ADD COLUMN qr_token VARCHAR(50)`
- [ ] Unit tests (ít nhất 6 test cases)
- [ ] Deploy to staging
- [ ] Test với Android team
- [ ] Deploy to production

---

## 🧪 TEST CASES

### Test 1: Valid Token
```bash
curl -X POST https://vietsuky.com/api/lostfound/items/38/confirm-handover \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"TOKEN_1730678400000"}'
```
**Expected:** 200 OK, item.status = "returned"

### Test 2: Invalid Format
```bash
curl -X POST https://vietsuky.com/api/lostfound/items/38/confirm-handover \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"INVALID_123"}'
```
**Expected:** 400 Bad Request

### Test 3: Expired Token (>24h)
```bash
curl -X POST https://vietsuky.com/api/lostfound/items/38/confirm-handover \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"TOKEN_1700000000000"}'
```
**Expected:** 400 Bad Request

### Test 4: Token Already Used
Gọi API 2 lần với cùng token  
**Expected:** Lần 2 trả 400 Bad Request

### Test 5: Item Already Returned
Item có status = "returned", gọi API  
**Expected:** 409 Conflict

### Test 6: Item Not Found
```bash
curl -X POST https://vietsuky.com/api/lostfound/items/99999/confirm-handover \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"TOKEN_1730678400000"}'
```
**Expected:** 404 Not Found

---

## 📞 LIÊN HỆ

**Questions?** Contact Android team  
**Blocking:** QR handover feature không work nếu không có endpoint này  
**Testing:** Android team sẵn sàng test ngay khi staging ready

---

## 📚 REFERENCE

Chi tiết implementation đầy đủ xem file: `API_SPEC_CONFIRM_HANDOVER_BACKEND.md`

---

**Created:** November 3, 2025  
**Status:** ⏳ Pending Implementation  
**Priority:** 🔥 HIGH
