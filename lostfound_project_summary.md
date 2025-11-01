# 🪙 Lost&Found FPT Campus+ — Ngữ cảnh dự án (chuẩn MVC Android, cập nhật bảo mật)

## 🌟 Mục tiêu
Ứng dụng Android giúp sinh viên đăng và tìm đồ thất lạc, xác nhận trả đồ qua QR, xem bản đồ vị trí, và tích điểm Karma để khuyến khích hành động tốt trong cộng đồng sinh viên.

---

## 🧹 Mô hình MVC (chuẩn Android Java)

| Thành phần | Vai trò | Ví dụ trong dự án |
|-----------------|-----------|-----------------------------------|
| **Model** | Chứa dữ liệu và logic xử lý (Entity, DAO, API, Repository) | LostItem.java, User.java, LostItemDao.java, LostItemApi.java |
| **View** | Giao diện hiển thị (XML) | activity_add_item.xml, activity_list_item.xml, activity_map.xml |
| **Controller** | Activity/Fragment nhận input, gọi Model, cập nhật View | AddItemActivity.java, ListItemActivity.java, MapActivity.java |

---

## 🧠 Luồng hoạt động mẫu

**Người dùng đăng đồ thất lạc:**
```
(View) activity_add_item.xml
     ↓  [User click "Đăng"]
(Controller) AddItemActivity.java
     ↓  [Lấy input từ EditText, ImageView, GPS]
(Model) LostItem.java + LostItemDao + LostItemApi
     ↓  [Lưu local + gửi Retrofit POST /api/items]
(Controller) Cập nhật UI / Thông báo thành công
```

---

## 🧱 Chức năng chính theo MVC

| Nhóm | Đánh giá | Gợi ý thêm |
|--------|-------------|-------------|
| Đăng nhập / Đăng ký | Có xác thực email hoặc domain (@fpt.edu.vn) | Quên mật khẩu / Refresh token / Role (user, admin) |
| Đăng đồ thất lạc | OK, cốt lõi | Sửa / Xóa bài đăng |
| Danh sách đồ | OK | Lọc nâng cao (loại, vị trí, thời gian) |
| Bản đồ vị trí | OK | Cluster marker / "Gần tôi" |
| Quét QR xác nhận trả đồ | Xuất sắc | Lưu lịch sử trao đồ |
| Karma & BXH | OK | Huy hiệu / Cấp bậc (badge system) |
| Thông báo gần vị trí | Dùng FCM | ✅ |
| Offline-first | Rất tốt | Đồng bộ khi có mạng |

---

## ⚙️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|----------------|------------|
| Giao diện | XML Layout, RecyclerView, ConstraintLayout |
| Controller | Activity / Fragment |
| Database cục bộ | Room (Entity + DAO + Database) |
| Gọi API | Retrofit 2 (JWT Token) |
| Server | RESTful API (Tomcat + Hibernate, tương thích Node.js / Spring Boot / Flask) |
| Map | OSMDroid (OpenStreetMap – miễn phí) |
| QR | ZXing (QR scanner/generator) |
| Thông báo | Firebase Cloud Messaging (FCM, tuỳ chọn) |
| Offline Sync | Room + Retrofit |

---

## 📂 Cấu trúc thư mục Android (MVC)
```
app/
 ├─ model/
 │   ├─ LostItem.java
 │   ├─ User.java
 │   ├─ Karma.java
 │   ├─ LostItemDao.java
 │   ├─ AppDatabase.java
 │   ├─ LostItemApi.java
 │   ├─ AuthApi.java
 │   └─ ApiClient.java
 ├─ view/
 │   ├─ activity_login.xml
 │   ├─ activity_add_item.xml
 │   ├─ activity_list_item.xml
 │   ├─ activity_map.xml
 │   ├─ activity_qr_scan.xml
 │   └─ activity_leaderboard.xml
 ├─ controller/
 │   ├─ LoginActivity.java
 │   ├─ AddItemActivity.java
 │   ├─ ListItemActivity.java
 │   ├─ MapActivity.java
 │   ├─ QrScanActivity.java
 │   └─ LeaderboardActivity.java
```

---

## 🧹 Tóm tắt kiến trúc tổng thể

**Project:** Lost&Found FPT Campus+  
**Architecture:** MVC (Model = Data/Logic, View = XML, Controller = Activity)  
**Platform:** Android Java (API 26)  
**Local:** Room Database (Entity, DAO, Database)  
**Network:** Retrofit 2 + JWT Auth  
**Server:** RESTful API (Tomcat + Hibernate, tương thích Node.js / Spring Boot)

### **Main Features**
- Đăng ký / Đăng nhập (JWT)
- Đăng đồ thất lạc (ảnh + mô tả + vị trí)
- Danh sách đồ (RecyclerView)
- Bản đồ vị trí (OSMDroid)
- Quét QR xác nhận trao đồ (ZXing)
- Điểm Karma + Bảng xếp hạng
- Offline cache (Room)
- Thông báo (FCM – tuỳ chọn)

---

## 🔐 Bảo mật & Chống fake server

### **Phía Server:**
- Xác thực JWT + BCrypt (hash mật khẩu).
- Role-based access control (user, helper, admin).
- Secret JWT lưu trong environment variable.
- Có thể nâng cấp RS256 cho chữ ký bất đối xứng.

### **Phía Android App:**
- Bắt buộc HTTPS (TLS) cho mọi API (`https://api.vietsuky.com`).
- Certificate Pinning (OkHttp / Network Security Config) để xác minh server thật.
- Optional App Attestation (Google Play Integrity / SafetyNet) để xác minh app chính chủ.
- Token ngắn hạn + Refresh token rotation.
- Tuyệt đối không bypass SSL validation khi debug.

### **Mục tiêu:**
> Đảm bảo app chỉ giao tiếp với server thật, chống MITM / fake server, giữ an toàn cho người dùng.

---

## 🔖 Thư viện sử dụng (Gradle Kotlin DSL)

```kotlin
// --- ROOM DATABASE ---
val room_version = "2.6.1" // hoặc 2.5.2 nếu muốn ổn định hơn cho API 26
implementation("androidx.room:room-runtime:$room_version")
annotationProcessor("androidx.room:room-compiler:$room_version")
implementation("androidx.room:room-ktx:$room_version")

// --- RETROFIT + GSON ---
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

// --- JWT TOKEN (chỉ decode trên Android) ---
implementation("com.auth0.android:jwtdecode:2.0.2")

// --- OSMDroid (bản đồ open source) ---
implementation("org.osmdroid:osmdroid-android:6.1.16")
implementation("org.osmdroid:osmbonuspack:6.9.0") // hỗ trợ tìm đường, route, marker mở rộng

// --- ZXing (QR code scanner/generator) ---
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
implementation("com.google.zxing:core:3.5.2")

// --- ANDROID CORE UI ---
implementation(libs.appcompat)
implementation(libs.material)
implementation(libs.activity)
implementation(libs.constraintlayout)

// --- TEST ---
testImplementation(libs.junit)
androidTestImplementation(libs.ext.junit)
androidTestImplementation(libs.espresso.core)


🧩 Bổ sung ngữ cảnh dự án
🔄 Quản lý upload và tác vụ nền

Nếu chỉ upload khi người dùng đang mở màn hình, sử dụng ExecutorService là đủ.

Nếu cần retry tự động, upload khi mạng trở lại, hoặc upload nền hàng loạt, nên dùng WorkManager.

Nếu upload nhiều ảnh song song, dùng:

Executors.newFixedThreadPool(n);


để tận dụng đa luồng mà vẫn kiểm soát số lượng tiến trình.

⚙️ Quy ước binding View & Action

Các bước bindingView() và bindingAction() nên tách riêng, gọi ngoài hàm onCreate() để code gọn gàng, dễ đọc.

Khi gán sự kiện, nên dùng method reference (this::method) thay vì tạo new View.OnClickListener().

Ví dụ mẫu:

private void bindingView() {
    buttonStart = findViewById(R.id.buttonStart);
    buttonStop = findViewById(R.id.buttonStop);
}

private void bindingAction() {
    buttonStart.setOnClickListener(this::onBtnStartService);
    buttonStop.setOnClickListener(this::onBtnStopService);
}

private void onBtnStartService(View view) {
    startService(new Intent(this, MyMusicService.class));
}

private void onBtnStopService(View view) {
    stopService(new Intent(this, MyMusicService.class));
}

🧱 Cấu trúc RecyclerView chuẩn (tách riêng Adapter & ViewHolder)

Adapter: quản lý danh sách và liên kết dữ liệu.

ViewHolder: binding view, xử lý sự kiện click.

ProductAdapter.java

public class ProductAdapter extends RecyclerView.Adapter<ProductViewHolder> {
    private final List<Product> productList;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(productList.get(position));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}


ProductViewHolder.java

public class ProductViewHolder extends RecyclerView.ViewHolder {
    private ImageView itemImage;
    private TextView itemName;
    private Product currentProduct;

    public ProductViewHolder(@NonNull View itemView) {
        super(itemView);
        bindingView();
        bindingAction();
    }

    private void bindingView() {
        itemImage = itemView.findViewById(R.id.item_image);
        itemName = itemView.findViewById(R.id.item_name);
    }

    private void bindingAction() {
        itemView.setOnClickListener(this::onItemViewClick);
    }

    private void onItemViewClick(View view) {
        if (currentProduct != null) {
            Context context = view.getContext();
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("productName", currentProduct.getName());
            intent.putExtra("productImage", currentProduct.getImage());
            context.startActivity(intent);
        }
    }

    public void bind(Product product) {
        this.currentProduct = product;
        itemName.setText(product.getName());
        itemImage.setImageResource(product.getImage());
    }
}


Tóm lại:

App dùng ExecutorService cho tác vụ tạm thời, WorkManager cho tác vụ nền bền vững.

Code nên chia rõ bindingView() / bindingAction() và sử dụng method reference.

RecyclerView cần tách Adapter và ViewHolder thành hai file để dễ mở rộng và bảo trì.
```