package com.fptcampus.lostfoundfptcampus.controller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.History;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.model.dto.UpdateItemRequest;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * QR Scan Activity - Scan and Generate QR codes
 * Following MVC pattern from lostfound_project_summary.md
 */
public class QrScanActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private DecoratedBarcodeView barcodeScanner;
    private ScrollView generatorLayout;
    private AutoCompleteTextView actvItemSelector;
    private ImageView ivQrCode;
    private TextView tvSelectedItemInfo, tvInstructions;
    private MaterialButton btnGenerateQr, btnShareQr;

    private ExecutorService executorService;
    private List<LostItem> myItems;
    private List<LostItem> filteredItems;
    private LostItem selectedItem;
    private Bitmap currentQrBitmap;
    private boolean hasCameraPermission = false;
    private SharedPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        executorService = Executors.newSingleThreadExecutor();
        prefsManager = new SharedPreferencesManager(this);

        bindingView();
        bindingAction();
        loadMyItems();
        
        // Check camera permission TRƯỚC KHI start scanner
        checkCameraPermission();

        // Check if launched in generate mode
        String mode = getIntent().getStringExtra("mode");
        if ("generate".equals(mode)) {
            tabLayout.selectTab(tabLayout.getTabAt(1));
            switchToGeneratorView();
        } else {
            // Nếu ở scanner mode, cần có permission
            if (hasCameraPermission) {
                switchToScannerView();
            }
        }
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true;
            android.util.Log.d("QrScanActivity", "✅ Camera permission already granted");
        } else {
            android.util.Log.d("QrScanActivity", "⚠️ Camera permission not granted, requesting...");
            requestCameraPermission();
        }
    }
    
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            // Show explanation why we need camera
            ErrorDialogHelper.showError(this, "Cần quyền Camera",
                    "Ứng dụng cần quyền truy cập camera để quét mã QR. Vui lòng cấp quyền.",
                    () -> {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                CAMERA_PERMISSION_REQUEST);
                    });
        } else {
            // Request permission directly
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasCameraPermission = true;
                android.util.Log.d("QrScanActivity", "✅ Camera permission granted");
                Toast.makeText(this, "Đã cấp quyền camera", Toast.LENGTH_SHORT).show();
                
                // Start scanner nếu đang ở tab scanner
                if (tabLayout.getSelectedTabPosition() == 0) {
                    switchToScannerView();
                }
            } else {
                hasCameraPermission = false;
                android.util.Log.e("QrScanActivity", "❌ Camera permission denied");
                
                // Switch to generator tab
                tabLayout.selectTab(tabLayout.getTabAt(1));
                switchToGeneratorView();
                
                ErrorDialogHelper.showError(this, "Quyền bị từ chối",
                        "Không thể quét QR code mà không có quyền camera. Vui lòng cấp quyền trong Cài đặt.");
            }
        }
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        barcodeScanner = findViewById(R.id.barcodeScanner);
        generatorLayout = findViewById(R.id.generatorLayout);
        actvItemSelector = findViewById(R.id.actvItemSelector);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvSelectedItemInfo = findViewById(R.id.tvSelectedItemInfo);
        tvInstructions = findViewById(R.id.tvInstructions);
        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        btnShareQr = findViewById(R.id.btnShareQr);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(this::onToolbarBackClick);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    switchToScannerView();
                } else {
                    switchToGeneratorView();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        actvItemSelector.setOnItemClickListener((parent, view, position, id) -> {
            // Get item from FILTERED list, not myItems
            selectedItem = filteredItems.get(position);
            onItemSelected();
        });

        btnGenerateQr.setOnClickListener(this::onBtnGenerateQrClick);
        btnShareQr.setOnClickListener(this::onBtnShareQrClick);

        // Setup scanner callback
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null) {
                    android.util.Log.d("QrScanActivity", "✅ QR Code scanned: " + result.getText());
                    onQrScanned(result.getText());
                }
            }
        });
        
        // Initialize scanner settings
        barcodeScanner.getBarcodeView().setDecoderFactory(new com.journeyapps.barcodescanner.DefaultDecoderFactory());
        barcodeScanner.initializeFromIntent(getIntent());
    }

    private void loadMyItems() {
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<LostItem> allItems = db.lostItemDao().getAllItems();
            
            // Lấy userId hiện tại
            long currentUserId = prefsManager.getUserId();
            android.util.Log.d("QrScanActivity", "Current userId: " + currentUserId);
            
            // Filter: chỉ lấy items của user này VÀ status = "found"
            myItems = new ArrayList<>();
            for (LostItem item : allItems) {
                if (item.getUserId() == currentUserId && 
                    "found".equalsIgnoreCase(item.getStatus())) {
                    myItems.add(item);
                }
            }
            
            android.util.Log.d("QrScanActivity", "Loaded " + myItems.size() + " found items from " + allItems.size() + " total items");

            runOnUiThread(() -> {
                if (myItems == null || myItems.isEmpty()) {
                    ErrorDialogHelper.showError(this, 
                        "Không có vật phẩm", 
                        "Bạn chưa có vật phẩm nào với trạng thái 'Đã tìm thấy'.");
                } else {
                    filteredItems = new ArrayList<>(myItems);
                    setupItemSelector();
                }
            });
        });
    }

    private void setupItemSelector() {
        if (myItems == null || myItems.isEmpty()) {
            return;
        }

        // Setup adapter with initial data
        updateItemAdapter();
        
        // Add realtime search listener
        actvItemSelector.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterItems(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void filterItems(String query) {
        filteredItems.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // No search query - show all items
            filteredItems.addAll(myItems);
        } else {
            // Filter by title or description
            String lowerQuery = query.toLowerCase().trim();
            for (LostItem item : myItems) {
                boolean matchTitle = item.getTitle() != null && 
                                    item.getTitle().toLowerCase().contains(lowerQuery);
                boolean matchDesc = item.getDescription() != null && 
                                   item.getDescription().toLowerCase().contains(lowerQuery);
                boolean matchCategory = item.getCategory() != null && 
                                       item.getCategory().toLowerCase().contains(lowerQuery);
                
                if (matchTitle || matchDesc || matchCategory) {
                    filteredItems.add(item);
                }
            }
        }
        
        android.util.Log.d("QrScanActivity", "Filtered to " + filteredItems.size() + " items from query: " + query);
        updateItemAdapter();
    }
    
    private void updateItemAdapter() {
        List<String> itemTitles = new ArrayList<>();
        for (LostItem item : filteredItems) {
            itemTitles.add(item.getTitle());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, itemTitles);
        actvItemSelector.setAdapter(adapter);
        
        // Show dropdown if there are results
        if (!itemTitles.isEmpty()) {
            actvItemSelector.showDropDown();
        }
    }

    private void onItemSelected() {
        if (selectedItem != null) {
            tvSelectedItemInfo.setText(String.format("%s - %s",
                    selectedItem.getTitle(),
                    selectedItem.getCategory()));
            btnGenerateQr.setEnabled(true);
        }
    }

    private void onBtnGenerateQrClick(View view) {
        if (selectedItem == null) {
            ErrorDialogHelper.showError(this, "Lỗi", "Vui lòng chọn đồ thất lạc");
            return;
        }

        try {
            // Generate QR code content (JSON format)
            String qrContent = String.format("{\"itemId\":%d,\"title\":\"%s\",\"token\":\"%s\"}",
                    selectedItem.getId(),
                    selectedItem.getTitle(),
                    generateToken());

            currentQrBitmap = generateQrCode(qrContent, 800, 800);
            ivQrCode.setImageBitmap(currentQrBitmap);
            btnShareQr.setEnabled(true);

            // Không hiển thị popup - silent success
            android.util.Log.d("QrScanActivity", "✅ QR Code generated for item: " + selectedItem.getTitle());

        } catch (Exception e) {
            ErrorDialogHelper.showError(this, "Lỗi", "Không thể tạo mã QR: " + e.getMessage());
        }
    }

    private Bitmap generateQrCode(String content, int width, int height) throws Exception {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }

    private String generateToken() {
        return "TOKEN_" + System.currentTimeMillis();
    }

    private void onBtnShareQrClick(View view) {
        ErrorDialogHelper.showError(this, "Chức năng đang phát triển",
                "Chức năng chia sẻ QR code sẽ được cập nhật trong phiên bản tiếp theo");
    }

    private void onQrScanned(String content) {
        barcodeScanner.pause();
        
        android.util.Log.d("QrScanActivity", "QR Content: " + content);

        try {
            // Parse QR content: {"itemId":123,"title":"Lost iPhone","token":"TOKEN_xxx"}
            org.json.JSONObject json = new org.json.JSONObject(content);
            long itemId = json.getLong("itemId");
            String qrToken = json.getString("token");
            String itemTitle = json.optString("title", "Unknown Item");
            
            // Lấy thông tin người quét (receiver)
            long receiverId = prefsManager.getUserId();
            
            android.util.Log.d("QrScanActivity", "Processing QR: itemId=" + itemId + ", receiverId=" + receiverId);
            
            // Lấy thông tin chi tiết item và hiển thị dialog xác nhận
            showItemDetailAndConfirm(itemId, qrToken, itemTitle, receiverId);
            
        } catch (Exception e) {
            android.util.Log.e("QrScanActivity", "Error parsing QR content", e);
            Toast.makeText(this, "Mã QR không hợp lệ", Toast.LENGTH_SHORT).show();
            barcodeScanner.resume();
        }
    }
    
    private void showItemDetailAndConfirm(long itemId, String qrToken, String itemTitle, long receiverId) {
        String token = "Bearer " + prefsManager.getToken();
        
        // Lấy thông tin chi tiết item
        ApiClient.getItemApi().getItemById(token, itemId).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LostItem item = response.body().getData();
                    
                    runOnUiThread(() -> {
                        // Tạo dialog hiển thị thông tin item
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(QrScanActivity.this);
                        builder.setTitle("Xác nhận trả đồ");
                        
                        // Tạo nội dung hiển thị
                        StringBuilder message = new StringBuilder();
                        message.append("📦 Tên: ").append(item.getTitle()).append("\n\n");
                        message.append("📝 Mô tả: ").append(item.getDescription() != null ? item.getDescription() : "Không có").append("\n\n");
                        message.append("🏷️ Danh mục: ").append(item.getCategory()).append("\n\n");
                        message.append("📍 Trạng thái: ").append(item.getStatus()).append("\n\n");
                        message.append("Bạn có xác nhận đã nhận lại đồ này?");
                        
                        builder.setMessage(message.toString());
                        
                        // Nút xác nhận
                        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
                            dialog.dismiss();
                            // Thực hiện update item và tạo history
                            long giverId = item.getUserId();
                            confirmHandoverAndUpdate(itemId, qrToken, giverId, receiverId, item);
                        });
                        
                        // Nút hủy
                        builder.setNegativeButton("Hủy", (dialog, which) -> {
                            dialog.dismiss();
                            barcodeScanner.resume();
                        });
                        
                        builder.setCancelable(false);
                        builder.show();
                    });
                    
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(QrScanActivity.this, "Không tìm thấy thông tin vật phẩm", Toast.LENGTH_SHORT).show();
                        barcodeScanner.resume();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(QrScanActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    barcodeScanner.resume();
                });
            }
        });
    }
    
    private void confirmHandoverAndUpdate(long itemId, String qrToken, long giverId, long receiverId, LostItem item) {
        String token = "Bearer " + prefsManager.getToken();
        
        // Hiển thị progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang cập nhật...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Tạo request chỉ với status field
        UpdateItemRequest updateRequest = new UpdateItemRequest();
        updateRequest.setStatus("returned");
        
        android.util.Log.d("QrScanActivity", "Confirming handover for item " + itemId);
        
        // Bước 1: Update item
        ApiClient.getItemApi().updateItem(token, itemId, updateRequest).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                android.util.Log.d("QrScanActivity", "Update response code: " + response.code());
                
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isSuccess()) {
                        android.util.Log.d("QrScanActivity", "✅ Item updated to 'returned' status");
                        
                        // Bước 2: Tạo history
                        History history = new History();
                        history.setItemId(itemId);
                        history.setGiverId(giverId);
                        history.setReceiverId(receiverId);
                        history.setQrToken(qrToken);
                        history.setConfirmedAt(new Date());
                        
                        ApiClient.getHistoryApi().createHistory(token, history).enqueue(new Callback<ApiResponse<History>>() {
                            @Override
                            public void onResponse(Call<ApiResponse<History>> call, Response<ApiResponse<History>> response) {
                                progressDialog.dismiss();
                                
                                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                    android.util.Log.d("QrScanActivity", "✅ History created successfully");
                                    showSuccessDialog("Xác nhận thành công!", "Đã cập nhật trạng thái vật phẩm và ghi nhận giao dịch.");
                                } else {
                                    android.util.Log.e("QrScanActivity", "Failed to create history");
                                    showErrorDialog("Cảnh báo", "Đã cập nhật vật phẩm nhưng không thể ghi lịch sử giao dịch.");
                                }
                            }

                            @Override
                            public void onFailure(Call<ApiResponse<History>> call, Throwable t) {
                                progressDialog.dismiss();
                                android.util.Log.e("QrScanActivity", "History API error", t);
                                showErrorDialog("Cảnh báo", "Đã cập nhật vật phẩm nhưng lỗi khi ghi lịch sử: " + t.getMessage());
                            }
                        });
                        
                    } else {
                        progressDialog.dismiss();
                        String errorMsg = response.body().getError();
                        android.util.Log.e("QrScanActivity", "Update failed - Error: " + errorMsg);
                        showErrorDialog("Không thể cập nhật", errorMsg);
                    }
                } else {
                    progressDialog.dismiss();
                    android.util.Log.e("QrScanActivity", "Failed to update item - Response unsuccessful or null");
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("QrScanActivity", "Error body: " + errorBody);
                            showErrorDialog("Lỗi cập nhật", errorBody);
                        } catch (Exception e) {
                            showErrorDialog("Lỗi cập nhật", "Không thể cập nhật trạng thái vật phẩm");
                        }
                    } else {
                        showErrorDialog("Lỗi cập nhật", "Không thể cập nhật trạng thái vật phẩm");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                progressDialog.dismiss();
                android.util.Log.e("QrScanActivity", "Update item API error", t);
                showErrorDialog("Lỗi kết nối", t.getMessage());
            }
        });
    }
    
    private void showSuccessDialog(String title, String message) {
        runOnUiThread(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("✅ " + title);
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                barcodeScanner.resume();
            });
            builder.setCancelable(false);
            builder.show();
        });
    }
    
    private void showErrorDialog(String title, String message) {
        runOnUiThread(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("❌ " + title);
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                barcodeScanner.resume();
            });
            builder.setCancelable(false);
            builder.show();
        });
    }
    


    private void switchToScannerView() {
        // Check permission trước khi start scanner
        if (!hasCameraPermission) {
            android.util.Log.w("QrScanActivity", "⚠️ Cannot start scanner - no camera permission");
            Toast.makeText(this, "Vui lòng cấp quyền camera để quét QR", Toast.LENGTH_SHORT).show();
            requestCameraPermission();
            return;
        }
        
        android.util.Log.d("QrScanActivity", "Starting camera scanner...");
        barcodeScanner.setVisibility(View.VISIBLE);
        generatorLayout.setVisibility(View.GONE);
        tvInstructions.setText("Hướng camera vào mã QR để quét");
        
        try {
            barcodeScanner.resume();
            android.util.Log.d("QrScanActivity", "✅ Camera scanner resumed");
        } catch (Exception e) {
            android.util.Log.e("QrScanActivity", "❌ Error starting scanner", e);
            Toast.makeText(this, "Lỗi khởi động camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void switchToGeneratorView() {
        barcodeScanner.setVisibility(View.GONE);
        generatorLayout.setVisibility(View.VISIBLE);
        tvInstructions.setText("Chọn đồ thất lạc để tạo mã QR");
        barcodeScanner.pause();
    }

    private void onToolbarBackClick(View view) {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("QrScanActivity", "onResume - hasCameraPermission: " + hasCameraPermission);
        
        // CHỈ resume scanner khi có permission VÀ scanner visible
        if (hasCameraPermission && barcodeScanner != null && barcodeScanner.getVisibility() == View.VISIBLE) {
            try {
                barcodeScanner.resume();
                android.util.Log.d("QrScanActivity", "✅ Scanner resumed in onResume");
            } catch (Exception e) {
                android.util.Log.e("QrScanActivity", "❌ Error resuming scanner", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
