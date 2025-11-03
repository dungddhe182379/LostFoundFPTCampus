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
import com.fptcampus.lostfoundfptcampus.model.dto.ConfirmHandoverRequest;
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

        } else {

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

                Toast.makeText(this, "Đã cấp quyền camera", Toast.LENGTH_SHORT).show();
                
                // Start scanner nếu đang ở tab scanner
                if (tabLayout.getSelectedTabPosition() == 0) {
                    switchToScannerView();
                }
            } else {
                hasCameraPermission = false;

                
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
            // Get selected text from AutoCompleteTextView
            String selectedTitle = actvItemSelector.getText().toString();
            
            // Find item by title in filteredItems
            selectedItem = null;
            for (LostItem item : filteredItems) {
                if (item.getTitle().equals(selectedTitle)) {
                    selectedItem = item;
                    break;
                }
            }
            
            if (selectedItem != null) {

                onItemSelected();
            } else {

            }
        });

        btnGenerateQr.setOnClickListener(this::onBtnGenerateQrClick);
        btnShareQr.setOnClickListener(this::onBtnShareQrClick);

        // Setup scanner callback
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null) {

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

            
            // Filter: chỉ lấy items của user này VÀ status = "found"
            myItems = new ArrayList<>();
            for (LostItem item : allItems) {
                if (item.getUserId() == currentUserId && 
                    "found".equalsIgnoreCase(item.getStatus())) {
                    myItems.add(item);
                }
            }
            


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
        


        try {
            // Parse QR content: {"itemId":123,"title":"Lost iPhone","token":"TOKEN_xxx"}
            org.json.JSONObject json = new org.json.JSONObject(content);
            long itemId = json.getLong("itemId");
            String qrToken = json.getString("token");
            String itemTitle = json.optString("title", "Unknown Item");
            
            // Lấy thông tin người quét (receiver)
            long receiverId = prefsManager.getUserId();
            

            
            // Lấy thông tin chi tiết item và hiển thị dialog xác nhận
            showItemDetailAndConfirm(itemId, qrToken, itemTitle, receiverId);
            
        } catch (Exception e) {

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
        
        long itemCreatorId = giverId;
        long scannerId = receiverId;
        
        // KIỂM TRA: Không cho phép người tạo item quét QR của chính mình
        if (itemCreatorId == scannerId) {

            showErrorDialog("Không thể xác nhận", 
                "Bạn không thể xác nhận bàn giao đồ vật của chính mình");
            barcodeScanner.resume();
            return;
        }
        
        // Hiển thị progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Đang xác nhận...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Xác định logic dựa trên status của item
        String itemStatus = item.getStatus();
        Long lostUserId, foundUserId, returnedUserId;
        
        if ("found".equals(itemStatus)) {
            // Item được tạo bởi người TÌM THẤY -> người quét là người MẤT
            foundUserId = itemCreatorId;      // Người tạo item (người tìm thấy)
            lostUserId = scannerId;           // Người quét (người mất đồ)
            returnedUserId = scannerId;       // Người quét (người nhận lại)

        } else if ("lost".equals(itemStatus)) {
            // Item được tạo bởi người MẤT -> người quét là người TÌM THẤY
            lostUserId = itemCreatorId;       // Người tạo item (người mất)
            foundUserId = scannerId;          // Người quét (người tìm thấy và trả)
            returnedUserId = itemCreatorId;   // Người tạo item (người nhận lại)

        } else {
            progressDialog.dismiss();

            showErrorDialog("Trạng thái không hợp lệ", "Đồ vật phải ở trạng thái 'lost' hoặc 'found'");
            barcodeScanner.resume();
            return;
        }
        
        // Create UpdateItemRequest with 3 user role fields + status
        com.fptcampus.lostfoundfptcampus.model.dto.UpdateItemRequest request = 
            new com.fptcampus.lostfoundfptcampus.model.dto.UpdateItemRequest.Builder()
                .setStatus("returned")
                .setLostUserId(lostUserId)
                .setFoundUserId(foundUserId)
                .setReturnedUserId(returnedUserId)
                .build();
        
        // Call updateItem API (PUT)
        ApiClient.getItemApi().updateItem(token, itemId, request).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                progressDialog.dismiss();

                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Success - item updated
                    LostItem updatedItem = response.body().getData();





                    
                    // Update karma for BOTH lostUser and foundUser (+10 each)
                    updateKarmaForBothUsers(updatedItem);
                    
                } else if (response.isSuccessful() && response.body() != null) {
                    // API returned error
                    String errorMsg = response.body().getError();

                    
                    // Hiển thị error message thân thiện hơn
                    if (errorMsg != null && errorMsg.contains("Invalid or expired")) {
                        showErrorDialog("QR Code không hợp lệ", 
                            "Mã QR đã hết hạn hoặc đã được sử dụng.\n\nVui lòng tạo mã QR mới.");
                    } else if (errorMsg != null && errorMsg.contains("already marked as returned")) {
                        showErrorDialog("Đã xác nhận trước đó", 
                            "Vật phẩm này đã được xác nhận trả lại rồi.");
                    } else {
                        showErrorDialog("Không thể xác nhận", errorMsg);
                    }
                    
                } else {
                    // Network error or null response

                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();

                            
                            // Parse error từ JSON nếu có
                            try {
                                org.json.JSONObject errorJson = new org.json.JSONObject(errorBody);
                                String errorMessage = errorJson.optString("error", "Không thể xác nhận giao dịch");
                                showErrorDialog("Lỗi", errorMessage);
                            } catch (Exception e) {
                                showErrorDialog("Lỗi", "Không thể xác nhận giao dịch");
                            }
                        } catch (Exception e) {
                            showErrorDialog("Lỗi", "Không thể xác nhận giao dịch");
                        }
                    } else {
                        showErrorDialog("Lỗi", "Không thể xác nhận giao dịch");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                progressDialog.dismiss();

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

            Toast.makeText(this, "Vui lòng cấp quyền camera để quét QR", Toast.LENGTH_SHORT).show();
            requestCameraPermission();
            return;
        }
        

        barcodeScanner.setVisibility(View.VISIBLE);
        generatorLayout.setVisibility(View.GONE);
        tvInstructions.setText("Hướng camera vào mã QR để quét");
        
        try {
            barcodeScanner.resume();

        } catch (Exception e) {

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

        
        // CHỈ resume scanner khi có permission VÀ scanner visible
        if (hasCameraPermission && barcodeScanner != null && barcodeScanner.getVisibility() == View.VISIBLE) {
            try {
                barcodeScanner.resume();

            } catch (Exception e) {

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

    /**
     * Update karma for BOTH lostUser and foundUser (+10 each)
     */
    private void updateKarmaForBothUsers(final LostItem item) {
        String token = "Bearer " + prefsManager.getToken();
        long currentUserId = prefsManager.getUserId();
        
        Long lostUserId = item.getLostUserId();
        Long foundUserId = item.getFoundUserId();
        
        if (lostUserId == null || foundUserId == null) {

            showSuccessDialog("Xác nhận thành công!", 
                "Đã hoàn tất giao dịch trả đồ.\n\n" +
                "📦 " + item.getTitle() + "\n" +
                "✅ Trạng thái: " + item.getStatus());
            return;
        }
        

        
        final int[] completedCalls = {0};
        final int[] updatedKarma = {0};
        
        // Update karma for lostUser
        ApiClient.getUserApi().getUserById(token, lostUserId).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User lostUser = response.body().getData();
                    int newKarma = lostUser.getKarma() + 10;
                    lostUser.setKarma(newKarma);
                    updateUserKarma(lostUser, currentUserId, updatedKarma);

                }
                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {

                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }
        });
        
        // Update karma for foundUser
        ApiClient.getUserApi().getUserById(token, foundUserId).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User foundUser = response.body().getData();
                    int newKarma = foundUser.getKarma() + 10;
                    foundUser.setKarma(newKarma);
                    updateUserKarma(foundUser, currentUserId, updatedKarma);

                }
                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {

                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }
        });
    }
    
    private void updateUserKarma(com.fptcampus.lostfoundfptcampus.model.User user, long currentUserId, int[] updatedKarma) {
        String token = "Bearer " + prefsManager.getToken();
        
        ApiClient.getUserApi().updateProfile(token, user).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User updated = response.body().getData();
                    if (updated.getId() == currentUserId) {
                        prefsManager.saveUserKarma(updated.getKarma());
                        updatedKarma[0] = updated.getKarma();

                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {

            }
        });
    }
    
    private void checkBothUpdatesComplete(int completedCalls, LostItem item, int currentUserKarma) {
        if (completedCalls >= 2) {
            runOnUiThread(() -> {
                if (currentUserKarma > 0) {
                    showSuccessDialogWithKarma(item, currentUserKarma);
                } else {
                    showSuccessDialog("Xác nhận thành công!", 
                        "Đã hoàn tất giao dịch trả đồ.\n\n" +
                        "📦 " + item.getTitle() + "\n" +
                        "✅ Trạng thái: " + item.getStatus());
                }
            });
        }
    }

    private void showSuccessDialogWithKarma(LostItem item, int newKarma) {
        String message = "🎉 Đã hoàn tất giao dịch trả đồ!\n\n";
        message += "📦 " + item.getTitle() + "\n";
        message += "✅ Trạng thái: " + item.getStatus() + "\n\n";
        message += "⭐ Karma của bạn: " + newKarma + " điểm\n";
        message += "(+10 điểm từ bàn giao thành công)";
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("✅ Xác nhận thành công!");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            barcodeScanner.resume();
        });
        builder.setCancelable(false);
        builder.show();
    }
}
