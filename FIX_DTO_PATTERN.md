# 🔧 FIX LỖI SYNC - DTO PATTERN

## ❌ Vấn đề cũ:
```java
// Gửi toàn bộ LostItem object lên API
Call<ApiResponse<LostItem>> call = ApiClient.getItemApi().createItem(token, item);

// JSON gửi đi (SAI):
{
  "id": 123,              // ❌ Server không nhận
  "uuid": "...",
  "userId": 9,
  "title": "Test",
  "synced": false,        // ❌ Server không nhận
  "createdAt": "...",     // ❌ Server tự generate
  "updatedAt": "..."      // ❌ Server tự generate
}
```

## ✅ Giải pháp mới:
```java
// Tạo DTO riêng chỉ gửi fields cần thiết
CreateItemRequest request = new CreateItemRequest(
    item.getUuid(),
    item.getUserId(),
    item.getTitle(),
    item.getDescription(),
    item.getCategory(),
    item.getStatus(),
    item.getLatitude(),
    item.getLongitude(),
    item.getImageUrl()
);

Call<ApiResponse<LostItem>> call = ApiClient.getItemApi().createItem(token, request);

// JSON gửi đi (ĐÚNG):
{
  "uuid": "...",
  "userId": 9,
  "title": "Test",
  "description": "...",
  "category": "wallet",
  "status": "lost",
  "latitude": 10.762622,
  "longitude": 106.682223,
  "imageUrl": null
}
```

## 📝 Files đã sửa:

1. **CreateItemRequest.java** (NEW)
   - DTO chỉ chứa fields cần gửi lên API
   - Không có: id, createdAt, updatedAt, synced

2. **ItemApi.java**
   - Đổi `@Body LostItem` → `@Body CreateItemRequest`

3. **AddItemActivity.java**
   - Tạo CreateItemRequest từ LostItem trước khi sync

4. **SyncService.java**
   - Tạo CreateItemRequest trong syncSingleItem()

5. **ListItemActivity.java**
   - Tạo CreateItemRequest trong syncUnsyncedItems()

6. **LostItem.java**
   - `@Expose(serialize = false, deserialize = true)` cho id, createdAt, updatedAt
   - Không serialize khi gửi, nhưng deserialize khi nhận

## 🧪 Test Case:

### 1. Offline mode test:
```
1. Tắt wifi
2. Add item: "Test offline DTO"
3. ✓ Item lưu local với synced=false
```

### 2. Sync test:
```
1. Bật wifi
2. Vào MainActivity
3. ✓ Snackbar: "Có 1 bài đăng chưa đồng bộ"
4. Click "Đồng bộ ngay"
5. ✓ Check Logcat cho request body
6. ✓ Toast: "Đã đồng bộ: Test offline DTO"
7. ✓ Item có ID từ server
```

### 3. Verify trong MySQL:
```sql
SELECT * FROM items WHERE title LIKE '%Test offline%';
-- ✓ Item xuất hiện với server ID
-- ✓ createdAt và updatedAt do server generate
```

## 🔍 Debug với Logcat:

```
# Filter: "OkHttp"
# Look for POST request:

POST /api/lostfound/items
Content-Type: application/json
Authorization: Bearer ...

{
  "uuid": "...",
  "userId": 9,
  "title": "Test offline DTO",
  ...
}

# ✓ Không có: id, synced, createdAt, updatedAt
# ✓ Response 200 OK với server item
```

## 🎯 Expected Result:

- ✅ Không còn lỗi 400 Bad Request
- ✅ Item sync thành công lên server
- ✅ Local item được thay thế bằng server item
- ✅ Không duplicate items
- ✅ ID từ server replace local ID
