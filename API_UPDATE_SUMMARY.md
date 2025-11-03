# 🔄 CÁC THAY ĐỔI API - USER ROLES TRACKING

**Ngày cập nhật:** November 3, 2025  
**Phiên bản API:** 1.2  
**Phiên bản Database:** 2 → 3

---

## 📋 TÓM TẮT THAY ĐỔI

API đã thêm **3 trường mới** vào bảng `items` để tracking người mất, người tìm thấy, và người nhận:

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `lostUserId` | Long | ID người mất đồ (chủ sở hữu) |
| `foundUserId` | Long | ID người tìm thấy đồ |
| `returnedUserId` | Long | ID người nhận lại đồ (sau khi bàn giao) |

---

## ✅ CÁC FILE ĐÃ CẬP NHẬT

### 1. **LostItem.java** ✅
- ✅ Thêm 3 fields mới với `@ColumnInfo`, `@Expose`, `@SerializedName`
- ✅ Thêm getters/setters cho 3 fields
- ✅ Type: `Long` (nullable) để hỗ trợ NULL values

**Code:**
```java
@ColumnInfo(name = "lost_user_id")
@Expose
@SerializedName("lostUserId")
private Long lostUserId;

@ColumnInfo(name = "found_user_id")
@Expose
@SerializedName("foundUserId")
private Long foundUserId;

@ColumnInfo(name = "returned_user_id")
@Expose
@SerializedName("returnedUserId")
private Long returnedUserId;
```

### 2. **AppDatabase.java** ✅
- ✅ Tăng version: `2` → `3`
- ✅ Thêm `MIGRATION_2_3` với ALTER TABLE statements
- ✅ Thêm `.addMigrations(MIGRATION_2_3)` vào database builder

**Migration SQL:**
```sql
ALTER TABLE items ADD COLUMN lost_user_id INTEGER;
ALTER TABLE items ADD COLUMN found_user_id INTEGER;
ALTER TABLE items ADD COLUMN returned_user_id INTEGER;
```

### 3. **ItemApi.java** ✅
- ✅ **KHÔNG CẦN THAY ĐỔI**
- ✅ Gson tự động parse 3 fields mới từ API response
- ✅ Tất cả endpoints đều trả về LostItem với fields mới

### 4. **Fragments/Activities** ✅
- ✅ **KHÔNG CẦN THAY ĐỔI**
- ✅ Tất cả code hiện tại tự động nhận 3 fields mới
- ✅ Có thể truy cập qua: `item.getLostUserId()`, `item.getFoundUserId()`, `item.getReturnedUserId()`

---

## 📊 LUỒNG DỮ LIỆU

### Kịch bản 1: Tạo đồ mất
```
User A mất iPhone
↓
POST /api/lostfound/items
Body: { title: "iPhone 15", status: "lost", ... }
↓
API Response:
{
  userId: 5,
  lostUserId: 5,        ← API tự động set
  foundUserId: null,
  returnedUserId: null,
  status: "lost"
}
↓
Room Database lưu với 3 fields mới
```

### Kịch bản 2: Tìm thấy đồ
```
User B tìm thấy iPhone
↓
POST /api/lostfound/items
Body: { title: "iPhone 15", status: "found", ... }
↓
API Response:
{
  userId: 10,
  lostUserId: null,
  foundUserId: 10,      ← API tự động set
  returnedUserId: null,
  status: "found"
}
```

### Kịch bản 3: Bàn giao đồ (QR Code)
```
User A scan QR để nhận lại iPhone
↓
POST /api/lostfound/items/38/confirm-handover
Header: Authorization: Bearer {user_a_token}
Body: { qrToken: "TOKEN_1730678400000" }
↓
API Response:
{
  userId: 10,
  lostUserId: 5,
  foundUserId: 10,
  returnedUserId: 5,    ← API set = người scan QR
  status: "returned"
}
```

---

## 🎯 CÁCH SỬ DỤNG TRONG CODE

### Truy cập các fields mới:

```java
// Trong Fragment/Activity
LostItem item = ...; // Lấy từ API hoặc Room

// Kiểm tra ai là người mất đồ
Long lostUserId = item.getLostUserId();
if (lostUserId != null) {
    // Có thể load User info từ UserApi
    loadUserInfo(lostUserId);
}

// Kiểm tra ai tìm thấy đồ
Long foundUserId = item.getFoundUserId();
if (foundUserId != null && item.getStatus().equals("found")) {
    tvFoundBy.setText("Tìm thấy bởi: User ID " + foundUserId);
}

// Kiểm tra ai đã nhận lại đồ
Long returnedUserId = item.getReturnedUserId();
if (returnedUserId != null && item.getStatus().equals("returned")) {
    tvReturnedTo.setText("Đã trả cho: User ID " + returnedUserId);
}
```

### Ví dụ hiển thị trong UI:

```java
// DetailItemFragment.java
private void displayUserRoles() {
    if (currentItem.getLostUserId() != null) {
        // TODO: Load user name from API và hiển thị
        // tvLostBy.setText("Người mất: " + userName);
    }
    
    if (currentItem.getFoundUserId() != null) {
        // TODO: Load user name from API và hiển thị
        // tvFoundBy.setText("Người tìm thấy: " + userName);
    }
    
    if (currentItem.getReturnedUserId() != null) {
        // TODO: Load user name from API và hiển thị
        // tvReturnedTo.setText("Đã trả cho: " + userName);
    }
}
```

---

## 🧪 TESTING

### Test Migration:
1. ✅ Build project - **No errors**
2. ⏳ Install app trên device
3. ⏳ Kiểm tra database đã migrate thành công
4. ⏳ Sync items từ API
5. ⏳ Verify 3 fields mới được populate đúng

### Test Commands:
```bash
# Build project
./gradlew assembleDebug

# Check database version
adb shell "run-as com.fptcampus.lostfoundfptcampus sqlite3 /data/data/com.fptcampus.lostfoundfptcampus/databases/lostfound_fptcampus.db 'PRAGMA user_version;'"

# Check new columns
adb shell "run-as com.fptcampus.lostfoundfptcampus sqlite3 /data/data/com.fptcampus.lostfoundfptcampus/databases/lostfound_fptcampus.db 'PRAGMA table_info(items);'"
```

---

## 📝 NOTES

### ✅ KHÔNG CẦN THAY ĐỔI:
- ItemApi.java (Gson auto-parse)
- DetailItemFragment.java
- ItemsFragment.java
- MyItemsFragment.java
- MapFragment.java
- QrScanActivity.java
- AddItemActivity.java

### 📌 CẦN LÀM SAU (OPTIONAL):
- [ ] Hiển thị user roles trong DetailItemFragment UI
- [ ] Load User info từ UserApi dựa vào userId
- [ ] Thêm filters theo lostUserId/foundUserId trong ItemsFragment
- [ ] Statistics về số item đã tìm thấy/trả lại

---

## 🔍 API ENDPOINTS SUPPORT

Tất cả endpoints đã support 3 fields mới:

✅ `GET /api/lostfound/items` - List all items  
✅ `GET /api/lostfound/items/{id}` - Get item by ID  
✅ `GET /api/lostfound/items/status/{status}` - Filter by status  
✅ `POST /api/lostfound/items` - Create item (auto-populate lostUserId/foundUserId)  
✅ `PUT /api/lostfound/items/{id}` - Update item  
✅ `POST /api/lostfound/items/{id}/confirm-handover` - Set returnedUserId

---

## 📞 SUPPORT

**Migration Issues?**
- Check `DATABASE_MIGRATION_GUIDE.md` for detailed guide
- Check `DB-migration-v2-to-v3.sql` for SQL script
- Use `.fallbackToDestructiveMigration()` if needed (dev only)

**API Issues?**
- Check `LOSTFOUND_API_DOCUMENTATION.md` for API docs
- Verify JWT token is valid
- Check Gson parsing logs

---

## ✅ CHECKLIST

- [x] Update LostItem model
- [x] Add database migration
- [x] Update AppDatabase version
- [x] No API changes needed
- [x] No UI changes needed (optional)
- [x] Create migration documentation
- [x] Build successful - No errors
- [ ] Test on device
- [ ] Verify API sync

---

**Status:** ✅ **HOÀN THÀNH CẬP NHẬT CODE**  
**Next:** Build và test trên thiết bị để verify migration

---

**Generated:** November 3, 2025  
**By:** GitHub Copilot AI Assistant
