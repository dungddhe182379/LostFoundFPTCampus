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
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private LostItem selectedItem;
    private Bitmap currentQrBitmap;
    private boolean hasCameraPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        executorService = Executors.newSingleThreadExecutor();

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
            selectedItem = myItems.get(position);
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
            myItems = db.lostItemDao().getAllItems();

            runOnUiThread(() -> {
                setupItemSelector();
            });
        });
    }

    private void setupItemSelector() {
        if (myItems == null || myItems.isEmpty()) {
            ErrorDialogHelper.showError(this, "Thông báo",
                    "Bạn chưa có đồ thất lạc nào. Vui lòng đăng đồ trước khi tạo mã QR.");
            return;
        }

        List<String> itemTitles = new ArrayList<>();
        for (LostItem item : myItems) {
            itemTitles.add(item.getTitle());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, itemTitles);
        actvItemSelector.setAdapter(adapter);
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

            ErrorDialogHelper.showSuccess(this, "Thành công",
                    "Đã tạo mã QR cho: " + selectedItem.getTitle(), null);

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

        ErrorDialogHelper.showSuccess(this, "Quét thành công",
                "Nội dung: " + content, () -> {
                    barcodeScanner.resume();
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
