# 🚀 QUICK START GUIDE

## Bước 1: Mở Project

```bash
1. Mở Android Studio
2. File → Open
3. Chọn thư mục: LostFoundFPTCampus
4. Wait for Gradle Sync
```

## Bước 2: Chạy App

```bash
1. Kết nối thiết bị Android hoặc khởi động Emulator
2. Click nút "Run" (hoặc Shift + F10)
3. App sẽ mở LoginActivity
```

## Bước 3: Test App

### Đăng ký tài khoản mới

```
Họ tên: Nguyễn Văn Test
Email: test@fpt.edu.vn
Mật khẩu: 123456
Số điện thoại: 0123456789
```

### Hoặc dùng account có sẵn (nếu đã có trong database)

```
Email: an@fpt.edu.vn
Password: 123456
```

## Bước 4: Test các chức năng

1. ✅ **Đăng nhập** → Sẽ lưu token và navigate đến MainActivity
2. ✅ **Xem danh sách** → Click card "Danh sách đồ"
3. ✅ **Filter theo tab** → "Tất cả", "Đã mất", "Đã tìm"
4. ✅ **Thêm đồ mới** → Click FAB button (nút + màu xanh)
5. ✅ **Lấy vị trí GPS** → Click "Lấy vị trí hiện tại"
6. ✅ **Đăng xuất** → Click "Đăng xuất" ở MainActivity

---

## 🔧 Cấu hình API URL

Nếu muốn test với local server:

**File:** `app/src/main/java/com/fptcampus/lostfoundfptcampus/util/ApiClient.java`

```java
// Sửa dòng này:
private static final String BASE_URL = "http://10.0.2.2:8080/Vietsuky2/";
// 10.0.2.2 = localhost trên Android Emulator
```

---

## 📱 Test trên thiết bị thật

Nếu test trên điện thoại thật và server chạy local:

```java
// Dùng IP máy tính trong mạng LAN
private static final String BASE_URL = "http://192.168.1.100:8080/Vietsuky2/";
```

Kiểm tra IP máy:
```bash
# Windows
ipconfig

# Mac/Linux
ifconfig
```

---

## ✅ Checklist

- [x] Room Database được tạo tự động khi app chạy
- [x] Token được lưu trong SharedPreferences
- [x] Offline sync: data lưu local trước, sau đó sync lên server
- [x] JWT auto-inject vào mọi API request
- [x] Permission GPS được request khi cần

---

## 🐛 Nếu gặp lỗi

### Lỗi: "Unable to resolve dependency"

```bash
File → Invalidate Caches / Restart
```

### Lỗi: "Cleartext Traffic Not Permitted"

```
→ Đã fix trong network_security_config.xml
→ Đảm bảo android:usesCleartextTraffic="true" trong manifest
```

### Lỗi: Location Permission Denied

```
→ Settings → Apps → LostFoundFPTCampus → Permissions
→ Enable Location
```

---

## 📊 Kiểm tra Database

Sử dụng Android Studio Database Inspector:

```
View → Tool Windows → App Inspection
→ Chọn tab "Database Inspector"
→ Chọn app đang chạy
→ Xem tables: users, items, photos, etc.
```

---

## 🎯 Flow test đầy đủ

1. **Đăng ký** tài khoản mới → Email @fpt.edu.vn
2. **Đăng nhập** → Nhận JWT token
3. **Main Screen** → Xem thông tin user + karma
4. **Danh sách** → Xem items (load từ local + API)
5. **Thêm item** → Nhập title, description, category, lấy GPS
6. **Submit** → Lưu local → Sync lên server
7. **Refresh** → Swipe down để reload
8. **Đăng xuất** → Clear token, back to login

---

**Thời gian setup:** ~2 phút  
**Build time:** ~30 giây (lần đầu)  
**App size:** ~15 MB

---

Happy Coding! 🚀
