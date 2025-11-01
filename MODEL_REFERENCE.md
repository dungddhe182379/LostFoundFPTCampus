# 📦 MODEL REFERENCE

## Room Entities (Local Database)

### 1. User.java
```java
@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String uuid;
    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String avatarUrl;
    private int karma;
    private Date createdAt;
    private Date updatedAt;
}
```

### 2. LostItem.java
```java
@Entity(tableName = "items")
public class LostItem {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String uuid;
    private long userId;
    private String title;
    private String description;
    private String category;
    private String status; // "lost", "found", "returned"
    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private Date createdAt;
    private Date updatedAt;
    private boolean synced; // Offline sync flag
}
```

### 3. Photo.java
```java
@Entity(tableName = "photos")
public class Photo {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long itemId;
    private String url;
    private boolean isPrimary;
}
```

### 4. Notification.java
```java
@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long userId;
    private String title;
    private String body;
    private boolean isRead;
    private Date createdAt;
}
```

### 5. KarmaLog.java
```java
@Entity(tableName = "karma_logs")
public class KarmaLog {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long userId;
    private int changeValue;
    private String reason;
    private Date createdAt;
}
```

### 6. History.java
```java
@Entity(tableName = "histories")
public class History {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long itemId;
    private Long giverId;
    private Long receiverId;
    private String qrToken;
    private Date confirmedAt;
}
```

---

## API Models

### ApiResponse<T>
```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private long timestamp;
}
```

### LoginRequest
```java
public class LoginRequest {
    private String email;
    private String password;
}
```

### RegisterRequest
```java
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
}
```

### LoginResponse
```java
public class LoginResponse {
    private String token;
    private User user;
}
```

### NotificationCountResponse
```java
public class NotificationCountResponse {
    private int count;
}
```

---

## DAO Interfaces

### UserDao
```java
@Insert
long insert(User user);

@Query("SELECT * FROM users WHERE email = :email")
User getUserByEmail(String email);

@Query("SELECT * FROM users ORDER BY karma DESC LIMIT :limit")
List<User> getTopKarmaUsers(int limit);
```

### LostItemDao
```java
@Insert
long insert(LostItem item);

@Query("SELECT * FROM items ORDER BY created_at DESC")
List<LostItem> getAllItems();

@Query("SELECT * FROM items WHERE status = :status")
List<LostItem> getItemsByStatus(String status);

@Query("SELECT * FROM items WHERE synced = 0")
List<LostItem> getUnsyncedItems();
```

### NotificationDao
```java
@Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = 0")
List<Notification> getUnreadNotificationsByUserId(long userId);

@Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
int getUnreadCount(long userId);
```

---

## API Interfaces (Retrofit)

### AuthApi
```java
@POST("api/lostfound/auth/register")
Call<ApiResponse<LoginResponse>> register(@Body RegisterRequest request);

@POST("api/lostfound/auth/login")
Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
```

### ItemApi
```java
@GET("api/lostfound/items")
Call<ApiResponse<List<LostItem>>> getAllItems(@Header("Authorization") String token);

@GET("api/lostfound/items/status/{status}")
Call<ApiResponse<List<LostItem>>> getItemsByStatus(
    @Header("Authorization") String token,
    @Path("status") String status
);

@POST("api/lostfound/items")
Call<ApiResponse<LostItem>> createItem(
    @Header("Authorization") String token,
    @Body LostItem item
);
```

### UserApi
```java
@GET("api/lostfound/user/profile")
Call<ApiResponse<User>> getProfile(@Header("Authorization") String token);

@PUT("api/lostfound/user/profile")
Call<ApiResponse<User>> updateProfile(
    @Header("Authorization") String token,
    @Body User user
);
```

### NotificationApi
```java
@GET("api/lostfound/notifications")
Call<ApiResponse<List<Notification>>> getAllNotifications(
    @Header("Authorization") String token
);

@GET("api/lostfound/notifications/count")
Call<ApiResponse<NotificationCountResponse>> getUnreadCount(
    @Header("Authorization") String token
);
```

---

## Utility Classes

### SharedPreferencesManager
```java
void saveToken(String token)
String getToken()
void saveUserId(long userId)
long getUserId()
void saveUserName(String name)
String getUserName()
void saveUserKarma(int karma)
int getUserKarma()
boolean isLoggedIn()
void clearAll()
```

### PermissionHelper
```java
boolean hasLocationPermission(Context context)
void requestLocationPermission(Activity activity)
boolean hasCameraPermission(Context context)
void requestCameraPermission(Activity activity)
boolean hasStoragePermission(Context context)
void requestStoragePermission(Activity activity)
```

### ApiClient
```java
static void initialize(Context context)
static Retrofit getClient()
static AuthApi getAuthApi()
static UserApi getUserApi()
static ItemApi getItemApi()
static NotificationApi getNotificationApi()
static void reset()
```

---

## Category Values

```java
String[] categories = {
    "electronics",  // Đồ điện tử
    "wallet",       // Ví
    "card",         // Thẻ
    "keys",         // Chìa khóa
    "documents",    // Giấy tờ
    "clothes",      // Quần áo
    "bag",          // Túi xách
    "phone",        // Điện thoại
    "other"         // Khác
};
```

## Status Values

```java
String[] statuses = {
    "lost",      // Đã mất
    "found",     // Đã tìm thấy
    "returned"   // Đã trả
};
```

---

## Date Format

```java
// Gson date format
"yyyy-MM-dd'T'HH:mm:ss"

// Display format
SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
```

---

## Constants

```java
// SharedPreferences
public static final String PREF_NAME = "LostFoundPrefs";
public static final String KEY_JWT_TOKEN = "jwt_token";
public static final String KEY_USER_ID = "user_id";

// Permission Request Codes
public static final int REQUEST_LOCATION_PERMISSION = 100;
public static final int REQUEST_CAMERA_PERMISSION = 101;
public static final int REQUEST_STORAGE_PERMISSION = 102;

// Database
public static final String DATABASE_NAME = "lostfound_fptcampus.db";
```

---

## Common Patterns

### Execute background task
```java
ExecutorService executorService = Executors.newSingleThreadExecutor();
executorService.execute(() -> {
    // Background work
    runOnUiThread(() -> {
        // Update UI
    });
});
```

### Retrofit API call
```java
Call<ApiResponse<T>> call = ApiClient.getApi().method();
call.enqueue(new Callback<ApiResponse<T>>() {
    @Override
    public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<T> apiResponse = response.body();
            if (apiResponse.isSuccess()) {
                T data = apiResponse.getData();
                // Handle success
            }
        }
    }
    
    @Override
    public void onFailure(Call<ApiResponse<T>> call, Throwable t) {
        // Handle error
    }
});
```

---

**Last Updated:** November 1, 2025
