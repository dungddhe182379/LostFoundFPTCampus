# 🔐 FIX TOKEN EXPIRY & NETWORK HANDLING

## ✅ Các fix đã thực hiện:

### 1. **Auto Logout khi Token hết hạn (401 Unauthorized)**

**ApiClient.java - AuthInterceptor:**
```java
Response response = chain.proceed(newRequest);

// Check if token is invalid/expired (401 Unauthorized)
if (response.code() == 401) {
    handleUnauthorized();
}

private void handleUnauthorized() {
    // Clear all user data
    prefs.edit().clear().apply();
    
    // Navigate to login screen
    Intent intent = new Intent(appContext, LoginActivity.class);
    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
    appContext.startActivity(intent);
    
    // Show notification
    Toast.makeText("Phiên đăng nhập đã hết hạn...");
}
```

**Kết quả:**
- ✅ Khi API trả về 401 → App tự động clear data
- ✅ Chuyển về LoginActivity
- ✅ Toast thông báo "Phiên đăng nhập đã hết hạn"

---

### 2. **Network Detection - NetworkUtil.java**

**Tạo utility class mới:**
```java
public class NetworkUtil {
    // Check if device has internet connection
    public static boolean isNetworkAvailable(Context context)
    
    // Check if device has WiFi connection
    public static boolean isWifiConnected(Context context)
    
    // Get network status message
    public static String getNetworkStatusMessage(Context context)
}
```

**Kết quả:**
- ✅ Detect WiFi, Mobile Data, hoặc không có mạng
- ✅ Compatible với Android M+ và older versions

---

### 3. **MainActivity - Chỉ sync khi có mạng**

**Trước:**
```java
private void checkAndSyncOfflineItems() {
    syncService.hasUnsyncedItems((hasUnsynced, count) -> {
        // Show snackbar luôn
    });
}
```

**Sau:**
```java
private void checkAndSyncOfflineItems() {
    // Check network first
    if (!NetworkUtil.isNetworkAvailable(this)) {
        return; // Don't show sync prompt if no network
    }
    
    syncService.hasUnsyncedItems(...);
}

private void syncOfflineItems() {
    // Check network again before sync
    if (!NetworkUtil.isNetworkAvailable(this)) {
        Snackbar.make("Không có kết nối mạng...");
        return;
    }
    
    // Proceed with sync
}
```

**Kết quả:**
- ✅ Không hiển thị Snackbar "Đồng bộ ngay" nếu offline
- ✅ Click sync → Check lại network trước khi thực hiện
- ✅ Thông báo rõ ràng nếu không có mạng

---

### 4. **AddItemActivity - Hiển thị rõ trạng thái**

**Trước:**
```java
private void createItem(...) {
    // Save to local
    // Then sync to server (luôn)
}
```

**Sau:**
```java
private void createItem(...) {
    // Save to local first
    database.insert(item);
    
    // Check network before sync
    if (NetworkUtil.isNetworkAvailable(this)) {
        syncToServer(item);
    } else {
        Toast.makeText("✓ Đã lưu offline. Sẽ tự động đồng bộ khi có mạng.");
        finish();
    }
}
```

**Kết quả:**
- ✅ Nếu offline → Toast rõ ràng "Đã lưu offline"
- ✅ Nếu online → Sync ngay lập tức
- ✅ Không cố gắng sync khi biết không có mạng

---

## 🧪 Test Cases:

### Test 1: Token hết hạn
```
1. Login vào app
2. Đợi token hết hạn (hoặc thay đổi secret key trên server)
3. Thực hiện bất kỳ API call nào (vào List Items)
4. ✓ App tự động logout
5. ✓ Quay về LoginActivity
6. ✓ Toast: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
```

### Test 2: Offline mode - Add item
```
1. Tắt WiFi/Data
2. Vào AddItemActivity
3. Nhập thông tin và click "Đăng bài"
4. ✓ Toast: "✓ Đã lưu offline. Sẽ tự động đồng bộ khi có mạng."
5. ✓ Item lưu trong local DB với synced=false
6. ✓ App không cố gắng call API
```

### Test 3: Online mode - Auto sync
```
1. Tắt WiFi
2. Add 2 items offline
3. Bật WiFi trở lại
4. Quay về MainActivity
5. ✓ Snackbar hiển thị: "Có 2 bài đăng chưa đồng bộ"
6. ✓ Button "Đồng bộ ngay"
7. Click button
8. ✓ Sync thành công
9. ✓ Items có ID từ server
```

### Test 4: Click sync khi offline
```
1. Add item offline
2. Quay về MainActivity (vẫn offline)
3. ✓ KHÔNG hiển thị Snackbar (vì không có mạng)
4. Bật WiFi
5. ✓ Snackbar xuất hiện
6. Tắt WiFi lại
7. Click "Đồng bộ ngay"
8. ✓ Snackbar: "Không có kết nối mạng. Vui lòng kiểm tra lại."
```

---

## 📝 Files đã sửa:

1. **ApiClient.java**
   - Thêm `handleUnauthorized()` cho 401 response
   - Auto clear data và navigate to Login

2. **NetworkUtil.java** (NEW)
   - Check network availability
   - Support Android M+ và older versions

3. **MainActivity.java**
   - Check network trước khi show sync prompt
   - Check network trước khi thực hiện sync

4. **AddItemActivity.java**
   - Check network trước khi sync
   - Toast rõ ràng cho offline mode

5. **AndroidManifest.xml**
   - Permission: `ACCESS_NETWORK_STATE` (đã có sẵn ✓)

---

## 🎯 Kết quả cuối cùng:

### ✅ Token Management:
- Auto logout khi 401 Unauthorized
- Clear all user data
- Navigate to LoginActivity
- Toast thông báo rõ ràng

### ✅ Network Handling:
- Detect network status chính xác
- Không sync khi offline
- Thông báo rõ ràng cho user
- Auto sync khi có mạng trở lại

### ✅ User Experience:
- Không có "loading vô tận" khi offline
- Toast messages rõ ràng và thân thiện
- Sync prompt chỉ hiển thị khi có mạng
- Tự động handle edge cases

---

## 🚀 Build & Test:

```bash
cd C:\Users\doduy\AndroidStudioProjects\LostFoundFPTCampus
.\gradlew.bat clean assembleDebug
```

**Test scenarios:**
1. ✅ Add item offline → Lưu local
2. ✅ Go online → Snackbar prompt
3. ✅ Click sync → Upload thành công
4. ✅ Token hết hạn → Auto logout
5. ✅ Click sync offline → Thông báo không có mạng
