# QR Scan Logic Update - Cập nhật Logic Quét QR

## Tổng quan

Cập nhật logic quét QR để:
1. **Cập nhật đúng 3 trường user role** dựa trên loại item (lost/found)
2. **Sử dụng API PUT** thay vì confirm-handover
3. **Cập nhật karma cho CẢ 2 người** (+10 điểm cho lostUser và +10 cho foundUser)

---

## Logic Mới

### 1. Khi quét QR của người TÌM THẤY (item status = "found")

**Nghĩa là:**
- Người tạo item: Người tìm thấy đồ (foundUser)
- Người quét QR: Người mất đồ (lostUser)

**Cập nhật:**
```
foundUserId   = item.userId  (người tạo item - người tìm thấy)
lostUserId    = scannerId    (người quét - người mất đồ)
returnedUserId = scannerId    (người quét - người nhận lại)
status        = "returned"
```

### 2. Khi quét QR của người MẤT ĐỒ (item status = "lost")

**Nghĩa là:**
- Người tạo item: Người mất đồ (lostUser)
- Người quét QR: Người tìm thấy và trả (foundUser)

**Cập nhật:**
```
lostUserId     = item.userId  (người tạo item - người mất)
foundUserId    = scannerId    (người quét - người tìm thấy và trả)
returnedUserId = item.userId  (người tạo item - người nhận lại)
status         = "returned"
```

---

## Karma Update Flow

### Quy trình cập nhật karma:

1. **Cập nhật item** qua API `PUT /api/lostfound/items/{itemId}` với:
   - `status = "returned"`
   - 3 trường `lostUserId`, `foundUserId`, `returnedUserId`

2. **Lấy thông tin lostUser** từ API `GET /api/lostfound/user/{lostUserId}`

3. **Cập nhật karma lostUser** (+10 điểm):
   - `lostUser.karma = currentKarma + 10`
   - Gọi API `PUT /api/lostfound/user/profile` với body là lostUser đã cập nhật

4. **Lấy thông tin foundUser** từ API `GET /api/lostfound/user/{foundUserId}`

5. **Cập nhật karma foundUser** (+10 điểm):
   - `foundUser.karma = currentKarma + 10`
   - Gọi API `PUT /api/lostfound/user/profile` với body là foundUser đã cập nhật

6. **Cập nhật SharedPreferences** nếu current user là một trong hai người:
   - `prefsManager.saveUserKarma(newKarma)`

7. **Hiển thị dialog thành công** với karma đã cập nhật

---

## Files Changed

### 1. UpdateItemRequest.java
**Thêm 3 trường mới:**
```java
@Expose
private Long lostUserId;

@Expose
private Long foundUserId;

@Expose
private Long returnedUserId;
```

**Builder methods:**
```java
.setLostUserId(Long lostUserId)
.setFoundUserId(Long foundUserId)
.setReturnedUserId(Long returnedUserId)
```

### 2. QRFragment.java
**Method `confirmHandover()` - Logic mới:**
- Phân biệt 2 case dựa trên `item.getStatus()`
- Xác định đúng `lostUserId`, `foundUserId`, `returnedUserId`
- Sử dụng `updateItem()` API thay vì `confirmHandover()`

**Method `updateKarmaForBothUsers()` - Mới:**
- Gọi `getUserById()` cho cả lostUser và foundUser
- Cập nhật karma +10 cho mỗi người
- Gọi `updateProfile()` để lưu karma mới vào database
- Track số lượng API calls hoàn thành

**Method `updateUserKarma()` - Mới:**
- Gọi API PUT để cập nhật user profile
- Nếu là current user, lưu vào SharedPreferences

**Method `checkBothUpdatesComplete()` - Mới:**
- Đợi cả 2 API calls hoàn thành
- Hiển thị dialog với karma đã cập nhật

### 3. QrScanActivity.java
**Tương tự QRFragment:**
- Cập nhật `confirmHandoverAndUpdate()` với logic mới
- Thêm `updateKarmaForBothUsers()`
- Thêm `updateUserKarma()`
- Thêm `checkBothUpdatesComplete()`
- Thêm `showSuccessDialogWithKarma()`

### 4. ProfileFragment.java
**Method `onResume()` - Mới:**
```java
@Override
public void onResume() {
    super.onResume();
    loadUserProfile(); // Reload karma khi quay lại fragment
}
```

---

## API Endpoints Sử dụng

### 1. Update Item
```
PUT /api/lostfound/items/{itemId}
Authorization: Bearer {token}
Body: UpdateItemRequest {
  status: "returned",
  lostUserId: Long,
  foundUserId: Long,
  returnedUserId: Long
}
Response: ApiResponse<LostItem>
```

### 2. Get User By ID
```
GET /api/lostfound/user/{userId}
Authorization: Bearer {token}
Response: ApiResponse<User>
```

### 3. Update User Profile
```
PUT /api/lostfound/user/profile
Authorization: Bearer {token}
Body: User (với karma đã cập nhật)
Response: ApiResponse<User>
```

---

## Testing Checklist

### Test Case 1: Quét QR của FOUND item
- [ ] User A tạo item với status="found" (người tìm thấy)
- [ ] User B quét QR của User A
- [ ] Kiểm tra item sau khi cập nhật:
  - `foundUserId` = User A
  - `lostUserId` = User B
  - `returnedUserId` = User B
  - `status` = "returned"
- [ ] Kiểm tra karma:
  - User A karma +10
  - User B karma +10
- [ ] User B thấy karma mới trong dialog
- [ ] ProfileFragment của User B hiển thị karma mới

### Test Case 2: Quét QR của LOST item
- [ ] User A tạo item với status="lost" (người mất)
- [ ] User B quét QR của User A
- [ ] Kiểm tra item sau khi cập nhật:
  - `lostUserId` = User A
  - `foundUserId` = User B
  - `returnedUserId` = User A
  - `status` = "returned"
- [ ] Kiểm tra karma:
  - User A karma +10
  - User B karma +10
- [ ] User B thấy karma mới trong dialog
- [ ] ProfileFragment của cả 2 user hiển thị karma mới

### Test Case 3: Self-scan prevention
- [ ] User A tạo item
- [ ] User A quét QR của chính mình
- [ ] Hiển thị error: "Không thể quét mã QR của chính bạn"
- [ ] Không cập nhật database
- [ ] Camera resume để quét tiếp

### Test Case 4: Network errors
- [ ] Tắt internet
- [ ] Quét QR
- [ ] Xem error handling
- [ ] Bật lại internet
- [ ] Thử lại

---

## Notes

1. **Không dùng confirm-handover API nữa** - Endpoint cũ không cho phép custom 3 trường user role

2. **Karma được cập nhật qua 2 API calls riêng biệt** - Mỗi user (lostUser và foundUser) được query và update độc lập

3. **SharedPreferences chỉ update cho current user** - User khác sẽ thấy karma mới khi họ login lại hoặc refresh profile

4. **Race condition safe** - Sử dụng counter để đợi cả 2 API calls hoàn thành trước khi hiển thị dialog

5. **Validation ở client-side** - Kiểm tra item status phải là "lost" hoặc "found" trước khi cho phép quét

---

## Potential Issues & Solutions

### Issue 1: User API không hỗ trợ update karma trực tiếp
**Solution:** Sử dụng `updateProfile()` API với User object đầy đủ, chỉ thay đổi field karma

### Issue 2: Backend validation
**Nếu backend validate karma không được update từ client:**
- Cần thay đổi backend để cho phép admin/system update karma
- Hoặc tạo API endpoint riêng: `POST /api/lostfound/user/{userId}/add-karma`

### Issue 3: Concurrent karma updates
**Nếu 2 người cùng quét 2 QR khác nhau cùng lúc:**
- Backend cần implement optimistic locking hoặc atomic increment
- Client không cần xử lý vì backend đã handle

---

## Migration Guide

### Nếu đã có data cũ với confirm-handover:
1. Các item "returned" cũ không có đầy đủ 3 trường user role
2. Cần migration script để fill missing fields
3. Hoặc chấp nhận data cũ không đầy đủ (chỉ áp dụng logic mới cho QR scan sau này)

### Backward compatibility:
- API cũ (`POST /items/{id}/confirm-handover`) vẫn hoạt động
- Logic mới sử dụng API khác (`PUT /items/{id}`)
- Không ảnh hưởng data/code cũ

---

**Last Updated:** 2025-11-04  
**Version:** 2.0  
**Author:** AI Assistant
