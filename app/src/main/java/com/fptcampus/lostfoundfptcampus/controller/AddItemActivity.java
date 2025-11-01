package com.fptcampus.lostfoundfptcampus.controller;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.PermissionHelper;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddItemActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private ImageView ivItemImage;
    private MaterialButton btnSelectImage, btnGetLocation, btnSubmit;
    private TextInputEditText etTitle, etDescription;
    private AutoCompleteTextView actvCategory, actvStatus;
    private TextView tvLocation;
    private ProgressBar progressBar;

    private AppDatabase database;
    private ExecutorService executorService;
    private SharedPreferencesManager prefsManager;
    private FusedLocationProviderClient fusedLocationClient;

    private Double currentLatitude, currentLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        bindingView();
        bindingAction();

        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        prefsManager = new SharedPreferencesManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupDropdowns();
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        ivItemImage = findViewById(R.id.ivItemImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        btnSubmit = findViewById(R.id.btnSubmit);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        actvCategory = findViewById(R.id.actvCategory);
        actvStatus = findViewById(R.id.actvStatus);
        tvLocation = findViewById(R.id.tvLocation);
        progressBar = findViewById(R.id.progressBar);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(v -> finish());
        btnSelectImage.setOnClickListener(this::onBtnSelectImageClick);
        btnGetLocation.setOnClickListener(this::onBtnGetLocationClick);
        btnSubmit.setOnClickListener(this::onBtnSubmitClick);
    }

    private void setupDropdowns() {
        // Categories
        String[] categories = {"electronics", "wallet", "card", "keys", "documents", "clothes", "bag", "phone", "other"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(categoryAdapter);

        // Status
        String[] statuses = {"lost", "found"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuses);
        actvStatus.setAdapter(statusAdapter);
        actvStatus.setText("lost", false);
    }

    private void onBtnSelectImageClick(View view) {
        // TODO: Implement image picker
        Toast.makeText(this, "Chức năng chọn ảnh đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void onBtnGetLocationClick(View view) {
        if (!PermissionHelper.hasLocationPermission(this)) {
            PermissionHelper.requestLocationPermission(this);
            return;
        }

        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    tvLocation.setText(String.format("Vị trí: %.6f, %.6f", currentLatitude, currentLongitude));
                } else {
                    Toast.makeText(this, "Không thể lấy vị trí", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void onBtnSubmitClick(View view) {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String status = actvStatus.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Vui lòng nhập mô tả");
            etDescription.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn loại đồ vật", Toast.LENGTH_SHORT).show();
            return;
        }

        createItem(title, description, category, status);
    }

    private void createItem(String title, String description, String category, String status) {
        showLoading(true);

        LostItem item = new LostItem();
        item.setUuid(UUID.randomUUID().toString());
        item.setUserId(prefsManager.getUserId());
        item.setTitle(title);
        item.setDescription(description);
        item.setCategory(category);
        item.setStatus(status);
        item.setLatitude(currentLatitude);
        item.setLongitude(currentLongitude);
        item.setSynced(false);

        // Save to local first
        executorService.execute(() -> {
            long localId = database.lostItemDao().insert(item);
            item.setId(localId);

            // Check network before sync
            runOnUiThread(() -> {
                if (com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(this)) {
                    syncToServer(item);
                } else {
                    showLoading(false);
                    Toast.makeText(this, 
                        "✓ Đã lưu offline. Sẽ tự động đồng bộ khi có mạng.", 
                        Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        });
    }

    private void syncToServer(LostItem item) {
        // Create DTO - Server generates uuid and userId from token
        com.fptcampus.lostfoundfptcampus.model.dto.CreateItemRequest request = 
            new com.fptcampus.lostfoundfptcampus.model.dto.CreateItemRequest(
                item.getTitle(),
                item.getDescription(),
                item.getCategory(),
                item.getStatus(),
                item.getLatitude(),
                item.getLongitude(),
                item.getImageUrl()
            );
        
        Call<ApiResponse<LostItem>> call = ApiClient.getItemApi().createItem(
            "Bearer " + prefsManager.getToken(),
            request
        );

        call.enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LostItem> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        LostItem serverItem = apiResponse.getData();
                        
                        // Replace local item with server item
                        executorService.execute(() -> {
                            // Delete temporary local item
                            database.lostItemDao().delete(item);
                            
                            // Insert server item with real ID
                            serverItem.setSynced(true);
                            database.lostItemDao().insert(serverItem);
                        });

                        Toast.makeText(AddItemActivity.this, "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddItemActivity.this, "Lỗi: " + apiResponse.getError(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AddItemActivity.this, "Đã lưu offline, sẽ đồng bộ khi có mạng", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(AddItemActivity.this, "Đã lưu offline: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!show);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
