# 🔄 HƯỚNG DẪN TEST OFFLINE-ONLINE SYNC

## Vấn đề đã fix:
1. ✅ **ListItemActivity không load được dữ liệu từ DB**
   - Fixed: Load all items từ API, filter locally theo tab
   - Save vào DB với `synced=true` để phân biệt với offline items

2. ✅ **Offline items không đẩy lên server**
   - Created: `SyncService.java` để tự động đồng bộ
   - Auto sync khi app khởi động hoặc resume

3. ✅ **Không xóa offline items sau khi sync thành công**
   - Implemented: Delete old local item với temporary ID
   - Insert server item với real ID từ API

---

## 📋 Test Case 1: Load Items từ Database

### Bước 1: Import fake data vào MySQL
```sql
-- Chạy file này trong MySQL Workbench
mysql -u root -p lostfound_fptcampus < DB-fake-data-extended.sql
```

### Bước 2: Build và chạy app
```bash
cd c:\Users\doduy\AndroidStudioProjects\LostFoundFPTCampus
.\gradlew.bat clean assembleDebug
```

### Bước 3: Login vào app
- Email: `an.nv@fpt.edu.vn`
- Password: `123456`

### Bước 4: Vào "Danh sách đồ thất lạc"
- ✅ Phải hiển thị **30+ items** từ database
- ✅ Tab "Tất cả": Hiển thị tất cả items
- ✅ Tab "Đã mất": Chỉ hiển thị status = "lost"
- ✅ Tab "Tìm được": Chỉ hiển thị status = "found"
- ✅ Swipe to refresh: Load lại dữ liệu từ API

**Expected Result:**
```
✓ Danh sách hiển thị đầy đủ items
✓ Ảnh thumbnail load từ Unsplash
✓ Filter theo tab hoạt động đúng
✓ Không có lỗi "No data" hoặc empty list
```

---

## 📋 Test Case 2: Offline Mode - Thêm item mới

### Bước 1: Tắt mạng (Airplane mode)
- Bật chế độ máy bay trên điện thoại

### Bước 2: Thêm item mới
1. Click nút FAB "+" ở ListItemActivity
2. Nhập thông tin:
   - Tiêu đề: "Ví da offline test"
   - Mô tả: "Test offline mode"
   - Loại: "wallet"
   - Trạng thái: "lost"
3. Click "Get Location" (optional)
4. Click "Đăng bài"

**Expected Result:**
```
✓ Toast hiển thị: "Đã lưu offline: UnknownHostException" hoặc tương tự
✓ App không crash
✓ Item được lưu vào local database với synced=false
✓ Quay lại ListItemActivity, item mới hiển thị trong danh sách
```

### Bước 3: Kiểm tra database local
```kotlin
// Item có các thuộc tính:
- id: Local temporary ID (auto-generated)
- uuid: UUID string
- synced: false (quan trọng!)
- title: "Ví da offline test"
```

---

## 📋 Test Case 3: Auto Sync khi Online

### Bước 1: Bật lại mạng
- Tắt chế độ máy bay

### Bước 2: Quay lại MainActivity (press back)
- Ứng dụng sẽ tự động check unsynced items

**Expected Result:**
```
✓ Snackbar hiển thị: "Có 1 bài đăng chưa đồng bộ"
✓ Button "Đồng bộ ngay" xuất hiện
```

### Bước 3: Click "Đồng bộ ngay"

**Expected Result:**
```
✓ Toast hiển thị: "Đang đồng bộ..."
✓ Toast hiển thị: "Đã đồng bộ: Ví da offline test"
✓ Snackbar hiển thị: "Đã đồng bộ thành công 1 bài đăng!"
```

### Bước 4: Kiểm tra database

**Local Database:**
```
✓ Item cũ (temporary ID) đã bị xóa
✓ Item mới (server ID) được insert với synced=true
✓ Không còn unsynced items
```

**Server Database (MySQL):**
```sql
SELECT * FROM items WHERE title LIKE '%offline test%';
-- ✓ Item mới xuất hiện với ID từ server
-- ✓ user_id, title, description, category đúng
-- ✓ created_at = thời gian sync
```

---

## 📋 Test Case 4: Multiple Offline Items

### Bước 1: Tắt mạng
- Airplane mode ON

### Bước 2: Thêm 3 items khác nhau
1. Item 1: "Laptop offline"
2. Item 2: "Phone offline"
3. Item 3: "Keys offline"

### Bước 3: Bật mạng và quay về MainActivity

**Expected Result:**
```
✓ Snackbar: "Có 3 bài đăng chưa đồng bộ"
✓ Click "Đồng bộ ngay"
✓ Toast hiển thị progress cho từng item
✓ Final Snackbar: "Đã đồng bộ thành công 3 bài đăng!"
```

### Bước 4: Verify trong ListItemActivity
```
✓ Tất cả 3 items hiển thị
✓ Có ID từ server (không còn temporary ID)
✓ Reload list không bị duplicate
```

---

## 📋 Test Case 5: Sync với API Error

### Bước 1: Thêm item offline
- Tắt mạng → Add item → "Test API error"

### Bước 2: Bật mạng nhưng sai token
```java
// Temporarily modify SharedPreferencesManager
// Return invalid token to simulate 401 error
```

### Bước 3: Click "Đồng bộ ngay"

**Expected Result:**
```
✓ Snackbar: "Đồng bộ: 0 thành công, 1 thất bại"
✓ Item vẫn ở trạng thái unsynced
✓ Có thể retry sau khi fix token
```

---

## 🐛 Debug Tips

### 1. Check Logcat
```
Filter: "SyncService"
Look for:
- "Found X unsynced items"
- "Successfully synced: [item_title]"
- "Failed to sync item: [item_title] - [error]"
```

### 2. Check Local Database
```java
// Run in Android Studio Database Inspector
SELECT * FROM items WHERE synced = 0;  -- Unsynced items
SELECT * FROM items WHERE synced = 1;  -- Synced items
```

### 3. Check API Response
```bash
# Test API manually
curl -X GET "https://vietsuky.com/api/lostfound/items" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## ✅ Success Criteria

### ✓ ListItemActivity
- [ ] Load all items từ database khi offline
- [ ] Load all items từ API khi online
- [ ] Filter theo tab hoạt động đúng
- [ ] Swipe to refresh hoạt động
- [ ] Không có empty state khi có data

### ✓ Offline Mode
- [ ] Add item offline lưu vào local DB
- [ ] Item có flag synced=false
- [ ] App không crash khi no network
- [ ] Toast thông báo "Đã lưu offline"

### ✓ Sync Service
- [ ] Auto check unsynced items on resume
- [ ] Snackbar hiển thị số lượng unsynced
- [ ] Sync thành công replace local với server item
- [ ] Delete local temporary ID item
- [ ] Toast progress cho từng item
- [ ] Final snackbar với success/fail count

### ✓ Data Integrity
- [ ] Không duplicate items sau sync
- [ ] Server ID replace local ID
- [ ] All fields giữ nguyên (title, description, etc.)
- [ ] No unsynced items sau successful sync

---

## 📝 Known Issues & Limitations

1. **Image Upload**: Chưa implement upload ảnh
   - Workaround: Để imageUrl = null hoặc placeholder

2. **Conflict Resolution**: Nếu item đã tồn tại trên server
   - Current: API trả về error
   - TODO: Implement conflict resolution strategy

3. **Network Change Listener**: Chưa auto sync khi network available
   - Current: Manual sync hoặc on app resume
   - TODO: Implement BroadcastReceiver cho ConnectivityManager

4. **Partial Sync Failure**: Nếu 1 trong nhiều items fail
   - Current: Hiển thị count thành công/thất bại
   - Unsynced items sẽ retry ở lần sync tiếp theo

---

## 🚀 Next Steps

1. **Implement Image Upload**
   - Use Multipart request
   - Compress image before upload
   - Store local path for offline items

2. **Background Sync Worker**
   - Use WorkManager
   - Periodic sync every 15 minutes
   - Retry with exponential backoff

3. **Real-time Updates**
   - WebSocket for live item updates
   - Push notifications for matched items

4. **Better Error Handling**
   - Show ErrorDialog instead of Toast
   - Detailed error messages
   - Retry button for failed syncs
