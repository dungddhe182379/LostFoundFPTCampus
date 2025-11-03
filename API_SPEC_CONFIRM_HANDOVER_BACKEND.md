# API ENDPOINT SPECIFICATION: CONFIRM HANDOVER

**Date:** November 3, 2025  
**Purpose:** New endpoint để xử lý QR code handover với token validation thay vì ownership check  
**For:** Backend Team Implementation

---

## 🎯 BUSINESS REQUIREMENT

### Problem:
- User A (giver) tạo QR code với item của mình (status = "found")
- User B (receiver) quét QR code để nhận đồ
- User B **KHÔNG phải owner** của item → PUT /items/{id} returns 403 Permission Denied
- Cần endpoint mới cho phép receiver confirm handover qua QR token

### Solution:
Tạo endpoint POST `/api/lostfound/items/{itemId}/confirm-handover` để:
1. **Validate QR token** thay vì check ownership
2. **Update item status** to "returned"
3. **Auto-create history record** (không cần gọi API riêng)
4. (Optional Phase 2) Send notification, update karma

---

## 📋 API SPECIFICATION

### Endpoint:
```
POST /api/lostfound/items/{itemId}/confirm-handover
```

### Authentication:
```
Header: Authorization: Bearer {jwt_token}
```

### Path Parameter:
- `itemId` (long) - ID của item cần confirm

### Request Body:
```json
{
  "qrToken": "TOKEN_1730678400000"
}
```

**Token Format:** `TOKEN_{timestamp}` (e.g., TOKEN_1730678400000)

### Success Response (200 OK):
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

### Error Responses:

#### 400 Bad Request - Invalid Token:
```json
{
  "success": false,
  "error": "Invalid or expired QR token",
  "timestamp": 1730678730000
}
```

**Reasons:**
- Token format sai (không bắt đầu bằng "TOKEN_")
- Token đã hết hạn (> 24 giờ)
- Token đã được sử dụng rồi

#### 401 Unauthorized:
```json
{
  "success": false,
  "error": "Unauthorized - Invalid JWT token",
  "timestamp": 1730678730000
}
```

#### 404 Not Found:
```json
{
  "success": false,
  "error": "Item not found",
  "timestamp": 1730678730000
}
```

#### 409 Conflict - Already Returned:
```json
{
  "success": false,
  "error": "Item already marked as returned",
  "timestamp": 1730678730000
}
```

---

## 💻 BACKEND IMPLEMENTATION (Java)

### 1. Controller Method:

```java
@RestController
@RequestMapping("/api/lostfound/items")
public class ItemController {
    
    @Autowired
    private ItemService itemService;
    
    @PostMapping("/{itemId}/confirm-handover")
    public ResponseEntity<ApiResponse<?>> confirmHandover(
            @PathVariable Long itemId,
            @RequestBody ConfirmHandoverRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // 1. Extract user from JWT token
            String token = authHeader.replace("Bearer ", "");
            Long receiverId = jwtService.getUserIdFromToken(token);
            
            // 2. Validate and confirm handover
            LostItem updatedItem = itemService.confirmHandover(itemId, request.getQrToken(), receiverId);
            
            // 3. Return success response
            return ResponseEntity.ok(ApiResponse.success(
                "Handover confirmed successfully", 
                updatedItem
            ));
            
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
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                ApiResponse.error("Internal server error")
            );
        }
    }
}
```

### 2. Request DTO:

```java
public class ConfirmHandoverRequest {
    private String qrToken;
    
    // Constructors
    public ConfirmHandoverRequest() {}
    
    public ConfirmHandoverRequest(String qrToken) {
        this.qrToken = qrToken;
    }
    
    // Getters/Setters
    public String getQrToken() {
        return qrToken;
    }
    
    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}
```

### 3. Service Implementation:

```java
@Service
public class ItemService {
    
    @Autowired
    private ItemRepository itemRepository;
    
    @Autowired
    private HistoryRepository historyRepository;
    
    @Transactional
    public LostItem confirmHandover(Long itemId, String qrToken, Long receiverId) 
            throws InvalidTokenException, ItemNotFoundException, ItemAlreadyReturnedException {
        
        // 1. Validate QR token format
        if (!isValidTokenFormat(qrToken)) {
            throw new InvalidTokenException("Invalid token format");
        }
        
        // 2. Check token expiration (24 hours)
        if (isTokenExpired(qrToken)) {
            throw new InvalidTokenException("Token expired");
        }
        
        // 3. Get item from database
        LostItem item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + itemId));
        
        // 4. Check if already returned
        if ("returned".equals(item.getStatus())) {
            throw new ItemAlreadyReturnedException("Item already returned");
        }
        
        // 5. Check if token already used (check history table)
        boolean tokenUsed = historyRepository.existsByQrToken(qrToken);
        if (tokenUsed) {
            throw new InvalidTokenException("Token already used");
        }
        
        // 6. Update item status
        item.setStatus("returned");
        item.setUpdatedAt(LocalDateTime.now());
        LostItem updatedItem = itemRepository.save(item);
        
        // 7. Create history record
        History history = new History();
        history.setItemId(itemId);
        history.setGiverId(item.getUserId()); // Owner của item
        history.setReceiverId(receiverId); // User đang quét QR
        history.setQrToken(qrToken);
        history.setAction("handover_confirmed");
        history.setCreatedAt(LocalDateTime.now());
        historyRepository.save(history);
        
        // 8. (Optional Phase 2) Send notification
        // notificationService.sendHandoverNotification(item.getUserId(), receiverId, itemId);
        
        // 9. (Optional Phase 2) Update karma
        // karmaService.incrementKarma(item.getUserId(), 10); // Giver +10
        // karmaService.incrementKarma(receiverId, 5); // Receiver +5
        
        return updatedItem;
    }
    
    private boolean isValidTokenFormat(String token) {
        return token != null && token.startsWith("TOKEN_") && token.length() > 6;
    }
    
    private boolean isTokenExpired(String token) {
        try {
            String timestampStr = token.substring(6); // Remove "TOKEN_"
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            long twentyFourHours = 24 * 60 * 60 * 1000;
            return (now - timestamp) > twentyFourHours;
        } catch (Exception e) {
            return true; // Invalid format = expired
        }
    }
}
```

### 4. Custom Exceptions:

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

### 5. Repository Methods:

```java
public interface HistoryRepository extends JpaRepository<History, Long> {
    boolean existsByQrToken(String qrToken);
}
```

---

## 🗄️ DATABASE CHANGES

### Option 1: Store token in History table (Recommended for Phase 1)

**Add column to `histories` table:**
```sql
ALTER TABLE histories 
ADD COLUMN qr_token VARCHAR(50) DEFAULT NULL,
ADD INDEX idx_qr_token (qr_token);
```

**Pros:**
- Simple implementation
- No new table needed
- Token automatically stored with history

**Cons:**
- Can only check used tokens, not expired tokens

### Option 2: Dedicated QR Tokens table (Phase 2)

```sql
CREATE TABLE qr_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(50) UNIQUE NOT NULL,
    item_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    used_by_user_id BIGINT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_token (token),
    INDEX idx_item_id (item_id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    FOREIGN KEY (used_by_user_id) REFERENCES users(id)
);
```

---

## 🔒 SECURITY CONSIDERATIONS

### 1. Token Validation:
- ✅ Format check: `TOKEN_{timestamp}`
- ✅ Expiration: 24 hours from creation
- ✅ One-time use: Check history/qr_tokens table
- ⚠️ No encryption in Phase 1 (add in Phase 2 if needed)

### 2. Authorization:
- ✅ Receiver must have valid JWT token
- ✅ No ownership check on item
- ✅ Token itself is the authorization proof

### 3. Rate Limiting (Phase 2):
- Limit to 10 confirm attempts per user per minute
- Prevent brute force token guessing

### 4. Audit Trail:
- ✅ History record with giverId + receiverId
- ✅ QR token stored in history
- ✅ Timestamps for all operations

---

## 🧪 TESTING CHECKLIST

### Unit Tests:

```java
@Test
public void testConfirmHandover_Success() {
    // Given: Valid token, item exists, not returned
    // When: confirmHandover called
    // Then: Item status = "returned", history created
}

@Test
public void testConfirmHandover_InvalidTokenFormat() {
    // Given: Token = "INVALID_FORMAT"
    // When: confirmHandover called
    // Then: Throws InvalidTokenException
}

@Test
public void testConfirmHandover_ExpiredToken() {
    // Given: Token older than 24 hours
    // When: confirmHandover called
    // Then: Throws InvalidTokenException
}

@Test
public void testConfirmHandover_TokenAlreadyUsed() {
    // Given: Token exists in history table
    // When: confirmHandover called
    // Then: Throws InvalidTokenException
}

@Test
public void testConfirmHandover_ItemAlreadyReturned() {
    // Given: Item status = "returned"
    // When: confirmHandover called
    // Then: Throws ItemAlreadyReturnedException
}

@Test
public void testConfirmHandover_ItemNotFound() {
    // Given: ItemId doesn't exist
    // When: confirmHandover called
    // Then: Throws ItemNotFoundException
}
```

### Integration Tests:

```bash
# Test 1: Valid handover
curl -X POST https://vietsuky.com/api/lostfound/items/38/confirm-handover \
  -H "Authorization: Bearer {receiver_token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"TOKEN_1730678400000"}'

# Expected: 200 OK, item status = "returned"

# Test 2: Invalid token format
curl -X POST https://vietsuky.com/api/lostfound/items/38/confirm-handover \
  -H "Authorization: Bearer {receiver_token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"INVALID_123"}'

# Expected: 400 Bad Request

# Test 3: Token already used
curl -X POST https://vietsuky.com/api/lostfound/items/38/confirm-handover \
  -H "Authorization: Bearer {receiver_token}" \
  -H "Content-Type: application/json" \
  -d '{"qrToken":"TOKEN_1730678400000"}'

# Expected: 400 Bad Request (if called second time)

# Test 4: Item already returned
# (Call twice with same or different tokens)
# Expected: 409 Conflict
```

---

## 📱 ANDROID INTEGRATION

**Android code đã sẵn sàng:**

```java
// ConfirmHandoverRequest.java
public class ConfirmHandoverRequest {
    @Expose
    private String qrToken;
    
    public ConfirmHandoverRequest(String qrToken) {
        this.qrToken = qrToken;
    }
    // Getters/Setters
}

// ItemApi.java
@POST("api/lostfound/items/{itemId}/confirm-handover")
Call<ApiResponse<LostItem>> confirmHandover(
    @Header("Authorization") String token,
    @Path("itemId") long itemId,
    @Body ConfirmHandoverRequest request
);

// QrScanActivity.java
ConfirmHandoverRequest request = new ConfirmHandoverRequest(qrToken);
ApiClient.getItemApi().confirmHandover(token, itemId, request)
    .enqueue(new Callback<ApiResponse<LostItem>>() {
        @Override
        public void onResponse(...) {
            if (success) {
                showSuccessDialog("Xác nhận thành công!");
            } else {
                showErrorDialog("QR Code không hợp lệ");
            }
        }
    });
```

---

## 🚀 DEPLOYMENT PLAN

### Phase 1 (MVP - 1-2 days):
1. ✅ Implement basic endpoint
2. ✅ Token format + expiration validation
3. ✅ Update item status
4. ✅ Create history record
5. ✅ Store token in history table
6. ✅ Basic error handling
7. ✅ Unit tests
8. Deploy to staging
9. Android team testing
10. Deploy to production

### Phase 2 (Enhancements - 1 week):
1. Dedicated qr_tokens table
2. Notification system
3. Karma update
4. Rate limiting
5. Admin dashboard
6. Analytics/reporting

---

## ❓ Q&A FOR BACKEND TEAM

### Q: QR token được tạo ở đâu?
**A:** Android app tạo khi user click "Tạo QR". Format: `TOKEN_{System.currentTimeMillis()}`. Backend chỉ validate, không generate.

### Q: History record cần những field nào?
**A:** `itemId`, `giverId` (item owner), `receiverId` (current user), `qrToken`, `action="handover_confirmed"`, `createdAt`

### Q: Xử lý trường hợp user tự quét QR của chính mình?
**A:** Không cần block. Nếu user quét QR của item mình tạo, vẫn cho confirm (edge case hợp lệ).

### Q: Token có nên encrypt không?
**A:** Phase 1: Không cần (timestamp đủ random). Phase 2: Có thể thêm HMAC signature.

### Q: Item status transitions?
**A:** `lost` → user found it → `found` → QR confirm → `returned`. Không cho quay lại trạng thái cũ.

### Q: Notification gửi cho ai?
**A:** 
- Giver (item owner): "Đồ của bạn đã được trả lại cho {receiver_name}"
- Receiver: "Bạn đã nhận {item_title} từ {giver_name}"

### Q: Karma points?
**A:** Suggestion: Giver +10 (helpful person), Receiver +5 (honest finder). Tune sau dựa trên usage.

---

## 📊 MONITORING & METRICS

**Track these metrics:**
1. Total handovers confirmed
2. Invalid token attempts (security)
3. Already-returned conflicts (UX issue?)
4. Average time from QR creation to confirm
5. Peak usage times

**Logging:**
```java
log.info("Handover confirmed - Item: {}, Giver: {}, Receiver: {}, Token: {}", 
    itemId, giverId, receiverId, qrToken);
```

---

## 📞 CONTACT

**Android Team:** Ready for integration testing  
**Backend Team:** This spec is your implementation guide  
**Timeline:** MVP in 1-2 days, testing 1 day, production ASAP  
**Priority:** HIGH - Blocks QR handover feature

---

**Document Version:** 1.0  
**Created:** November 3, 2025  
**Status:** 📝 Spec Ready - Waiting Backend Implementation  
**Estimated Effort:** 1-2 days (Phase 1)
