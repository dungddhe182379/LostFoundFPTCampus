# 🪙 Lost & Found FPT Campus - Android App

## 📋 Tổng quan

Ứng dụng Android giúp sinh viên FPT Campus đăng và tìm đồ thất lạc, xác nhận trả đồ qua QR, xem bản đồ vị trí, và tích điểm Karma.

### ✨ Tính năng đã implement

- ✅ **Đăng nhập / Đăng ký** với JWT Authentication
- ✅ **Room Database** cho offline-first
- ✅ **Đăng đồ thất lạc** với vị trí GPS
- ✅ **Danh sách đồ** với filter theo trạng thái
- ✅ **RecyclerView** với pattern chuẩn (Adapter + ViewHolder)
- ✅ **Retrofit + OkHttp** với JWT interceptor
- ✅ **SharedPreferences** quản lý session
- ✅ **Network Security Config** cho HTTPS
- ✅ **Offline sync** với Room Database

### 🚧 Tính năng đang phát triển

- ⏳ Bản đồ với OSMDroid
- ⏳ QR Scanner với ZXing
- ⏳ Bảng xếp hạng Karma
- ⏳ Upload ảnh thực tế
- ⏳ Thông báo push (FCM)

---

## 🏗️ Kiến trúc MVC

```
app/
├─ model/                          # MODEL LAYER
│   ├─ User.java                   # Entity với Room annotations
│   ├─ LostItem.java
│   ├─ Photo.java
│   ├─ History.java
│   ├─ KarmaLog.java
│   ├─ Notification.java
│   ├─ Converters.java             # TypeConverter cho Date
│   ├─ dao/                        # Data Access Objects
│   │   ├─ UserDao.java
│   │   ├─ LostItemDao.java
│   │   ├─ PhotoDao.java
│   │   ├─ HistoryDao.java
│   │   ├─ KarmaLogDao.java
│   │   └─ NotificationDao.java
│   ├─ database/
│   │   └─ AppDatabase.java        # Room Database singleton
│   └─ api/                        # API Models & Interfaces
│       ├─ ApiResponse.java
│       ├─ LoginRequest.java
│       ├─ LoginResponse.java
│       ├─ RegisterRequest.java
│       ├─ AuthApi.java            # Retrofit interface
│       ├─ UserApi.java
│       ├─ ItemApi.java
│       └─ NotificationApi.java
│
├─ view/                           # VIEW LAYER (XML)
│   ├─ activity_login.xml
│   ├─ activity_register.xml
│   ├─ activity_main.xml
│   ├─ activity_list_item.xml
│   ├─ activity_add_item.xml
│   └─ item_lost_item.xml          # RecyclerView item
│
├─ controller/                     # CONTROLLER LAYER
│   ├─ LoginActivity.java
│   ├─ RegisterActivity.java
│   ├─ ListItemActivity.java
│   ├─ AddItemActivity.java
│   └─ adapter/
│       ├─ ItemAdapter.java        # RecyclerView Adapter
│       └─ ItemViewHolder.java     # ViewHolder pattern
│
└─ util/                           # UTILITIES
    ├─ ApiClient.java              # Retrofit + JWT Interceptor
    ├─ SharedPreferencesManager.java
    └─ PermissionHelper.java
```

---

## 🔧 Cấu hình

### 1. Dependencies (build.gradle.kts)

```kotlin
// Room Database
implementation("androidx.room:room-runtime:2.6.1")
annotationProcessor("androidx.room:room-compiler:2.6.1")

// Retrofit + Gson
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// JWT Decode
implementation("com.auth0.android:jwtdecode:2.0.2")

// OSMDroid (Maps)
implementation("org.osmdroid:osmdroid-android:6.1.16")

// ZXing (QR)
implementation("com.journeyapps:zxing-android-embedded:4.3.0")

// Google Play Services
implementation("com.google.android.gms:play-services-location:21.0.1")
```

### 2. API Configuration

File: `util/ApiClient.java`

```java
// Production URL
private static final String BASE_URL = "http://vietsuky.com/Vietsuky2/";

// Local testing (Android Emulator)
// private static final String BASE_URL = "http://10.0.2.2:8080/Vietsuky2/";
```

### 3. Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

---

## 🚀 Cách chạy

### 1. Clone & Open Project

```bash
# Mở project trong Android Studio
# File → Open → chọn thư mục LostFoundFPTCampus
```

### 2. Sync Gradle

```
File → Sync Project with Gradle Files
```

### 3. Run App

```
- Kết nối thiết bị Android hoặc khởi động Emulator
- Click "Run" (Shift + F10)
```

### 4. Test Account

```
Email: test@fpt.edu.vn
Password: 123456
```

Hoặc đăng ký tài khoản mới với email `@fpt.edu.vn`

---

## 📱 Luồng hoạt động

### 1. Đăng nhập

```
LoginActivity
    ↓ [User nhập email/password]
    ↓ Retrofit.post("/api/lostfound/auth/login")
    ↓ [Nhận JWT token + user info]
    ↓ SharedPreferences.saveToken()
    ↓ Room.userDao().insert(user)
    ↓ Navigate to MainActivity
```

### 2. Đăng đồ thất lạc

```
AddItemActivity
    ↓ [User nhập title, description, category]
    ↓ [Lấy GPS location]
    ↓ Room.lostItemDao().insert() → Save local first
    ↓ Retrofit.post("/api/lostfound/items") → Sync to server
    ↓ Room.markAsSynced() if success
    ↓ Navigate back to ListItemActivity
```

### 3. Xem danh sách

```
ListItemActivity
    ↓ Room.lostItemDao().getAllItems() → Load from local
    ↓ RecyclerView.setAdapter(ItemAdapter)
    ↓ Retrofit.get("/api/lostfound/items") → Fetch from API
    ↓ Room.insert() → Cache to local
    ↓ RecyclerView.notifyDataSetChanged()
```

---

## 🗂️ Database Schema (Room)

### Users Table
```sql
id, uuid, name, email, password_hash, phone, avatar_url, karma, created_at, updated_at
```

### Items Table
```sql
id, uuid, user_id, title, description, category, status, latitude, longitude, 
image_url, created_at, updated_at, synced
```

### Other Tables
- `photos` - Ảnh của items
- `histories` - Lịch sử trao đồ qua QR
- `karma_logs` - Lịch sử tích điểm
- `notifications` - Thông báo

---

## 🔐 Bảo mật

### JWT Authentication
- Token được lưu trong `SharedPreferences`
- Auto-inject vào mọi API request qua `OkHttp Interceptor`
- Token expires trong 7 ngày

### Network Security
- HTTPS required cho production (`vietsuky.com`)
- Certificate pinning (có thể enable trong `network_security_config.xml`)
- Cleartext traffic chỉ cho localhost khi dev

### Offline Security
- Password không bao giờ lưu local
- Chỉ lưu JWT token và user info cơ bản
- Room Database không mã hóa (có thể thêm SQLCipher)

---

## 📊 API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/lostfound/auth/register` | Đăng ký |
| POST | `/api/lostfound/auth/login` | Đăng nhập |
| GET | `/api/lostfound/items` | Lấy danh sách items |
| GET | `/api/lostfound/items/status/{status}` | Filter theo status |
| POST | `/api/lostfound/items` | Tạo item mới |
| PUT | `/api/lostfound/items/{id}` | Cập nhật item |
| DELETE | `/api/lostfound/items/{id}` | Xóa item |

Chi tiết: `LOSTFOUND_API_DOCUMENTATION.md`

---

## 🐛 Troubleshooting

### 1. Build Error: Room Schema Export

```kotlin
// Thêm vào build.gradle.kts
javaCompileOptions {
    annotationProcessorOptions {
        arguments["room.schemaLocation"] = "$projectDir/schemas"
    }
}
```

### 2. Network Error: Cleartext Traffic

```xml
<!-- AndroidManifest.xml -->
android:usesCleartextTraffic="true"
android:networkSecurityConfig="@xml/network_security_config"
```

### 3. Location Permission Denied

```java
// Runtime permission check
if (!PermissionHelper.hasLocationPermission(this)) {
    PermissionHelper.requestLocationPermission(this);
}
```

---

## 📝 TODO - Next Steps

- [ ] Implement MapActivity với OSMDroid
- [ ] Implement QrScanActivity với ZXing
- [ ] Implement LeaderboardActivity
- [ ] Upload ảnh thực tế (Multipart)
- [ ] Detail Item Activity
- [ ] Edit/Delete Item
- [ ] Search functionality
- [ ] Push Notifications (FCM)
- [ ] Image loading (Glide/Picasso)
- [ ] Pull to refresh
- [ ] Pagination

---

## 📚 Tài liệu tham khảo

- [Android Room Database](https://developer.android.com/training/data-storage/room)
- [Retrofit 2](https://square.github.io/retrofit/)
- [JWT Authentication](https://jwt.io/)
- [OSMDroid](https://github.com/osmdroid/osmdroid)
- [ZXing](https://github.com/journeyapps/zxing-android-embedded)

---

## 👨‍💻 Developer

**Project:** Lost & Found FPT Campus  
**Platform:** Android (Java)  
**Min SDK:** 26 (Android 8.0)  
**Target SDK:** 36  
**Architecture:** MVC (Model-View-Controller)  
**Database:** Room (SQLite) + MySQL (Server)  
**Network:** Retrofit 2 + OkHttp 3  
**Authentication:** JWT Bearer Token

---

## 📄 License

Dự án học tập - FPT University

---

**Last Updated:** November 1, 2025
