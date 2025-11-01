package com.fptcampus.lostfoundfptcampus.controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Map Activity - Display items on OSMDroid map
 * Following MVC pattern from lostfound_project_summary.md
 */
public class MapActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final double FPT_LAT = 21.0135; // FPT hola
    private static final double FPT_LNG = 105.5266;

    private MaterialToolbar toolbar;
    private MapView mapView;
    private FloatingActionButton fabMyLocation, fabFilter;
    private MaterialCardView cardItemInfo;
    private ImageView ivItemPreview;
    private TextView tvItemTitle, tvItemCategory;
    private MaterialButton btnViewDetail;

    private IMapController mapController;
    private SharedPreferencesManager prefsManager;
    private ExecutorService executorService;
    private LostItem selectedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        
        setContentView(R.layout.activity_map);

        prefsManager = new SharedPreferencesManager(this);
        executorService = Executors.newSingleThreadExecutor();

        bindingView();
        bindingAction();
        initializeMap();
        checkLocationPermission();
        loadItems();
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        mapView = findViewById(R.id.mapView);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        fabFilter = findViewById(R.id.fabFilter);
        cardItemInfo = findViewById(R.id.cardItemInfo);
        ivItemPreview = findViewById(R.id.ivItemPreview);
        tvItemTitle = findViewById(R.id.tvItemTitle);
        tvItemCategory = findViewById(R.id.tvItemCategory);
        btnViewDetail = findViewById(R.id.btnViewDetail);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(this::onToolbarBackClick);
        fabMyLocation.setOnClickListener(this::onFabMyLocationClick);
        fabFilter.setOnClickListener(this::onFabFilterClick);
        btnViewDetail.setOnClickListener(this::onBtnViewDetailClick);
    }

    private void initializeMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Check if launched with specific location
        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", FPT_LAT);
        double longitude = intent.getDoubleExtra("longitude", FPT_LNG);
        String title = intent.getStringExtra("title");

        GeoPoint startPoint = new GeoPoint(latitude, longitude);
        mapController.setCenter(startPoint);

        // If specific location provided, add marker
        if (title != null) {
            addMarker(latitude, longitude, title, "lost");
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void loadItems() {
        // Load from local first
        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<LostItem> localItems = db.lostItemDao().getAllItems();

            runOnUiThread(() -> {
                displayItemsOnMap(localItems);
            });
        });

        // Then sync from API
        String token = prefsManager.getToken();
        if (token != null && !token.isEmpty()) {
            // Check network before API call
            if (!com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(this)) {
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(this, 
                        "Không có mạng - Hiển thị dữ liệu offline", 
                        android.widget.Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            Call<ApiResponse<List<LostItem>>> call = ApiClient.getItemApi()
                    .getAllItems("Bearer " + token);

            call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<LostItem>>> call,
                                       Response<ApiResponse<List<LostItem>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<LostItem>> apiResponse = response.body();
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            displayItemsOnMap(apiResponse.getData());
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                    // Silent fail - already showing local data
                }
            });
        }
    }

    private void displayItemsOnMap(List<LostItem> items) {
        // Clear existing markers
        mapView.getOverlays().clear();

        int markerCount = 0;
        for (LostItem item : items) {
            if (item.getLatitude() != null && item.getLongitude() != null) {
                addMarker(item.getLatitude(), item.getLongitude(),
                        item.getTitle(), item.getStatus());
                markerCount++;
            }
        }

        mapView.invalidate();

        final int finalCount = markerCount;
        runOnUiThread(() -> {
            if (finalCount > 0) {
                // Success - markers added
            } else {
                ErrorDialogHelper.showError(this, "Thông báo",
                        "Chưa có đồ thất lạc nào có thông tin vị trí");
            }
        });
    }

    private void addMarker(double latitude, double longitude, String title, String status) {
        GeoPoint point = new GeoPoint(latitude, longitude);
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Set marker icon based on status
        // Note: OSMDroid uses default marker, you can customize with drawable
        
        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            onMarkerClick(clickedMarker);
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    private void onMarkerClick(Marker marker) {
        // Show item info card
        tvItemTitle.setText(marker.getTitle());
        tvItemCategory.setText("Xem chi tiết để biết thêm");
        cardItemInfo.setVisibility(View.VISIBLE);

        // Center map on marker
        mapController.animateTo(marker.getPosition());
    }

    private void onToolbarBackClick(View view) {
        finish();
    }

    private void onFabMyLocationClick(View view) {
        // Center on FPT Campus
        GeoPoint fptPoint = new GeoPoint(FPT_LAT, FPT_LNG);
        mapController.animateTo(fptPoint);
        mapController.setZoom(16.0);
    }

    private void onFabFilterClick(View view) {
        ErrorDialogHelper.showError(this, "Chức năng đang phát triển",
                "Chức năng lọc đồ thất lạc sẽ được cập nhật trong phiên bản tiếp theo");
    }

    private void onBtnViewDetailClick(View view) {
        if (selectedItem != null) {
            Intent intent = new Intent(this, DetailItemActivity.class);
            intent.putExtra("itemId", selectedItem.getId());
            intent.putExtra("title", selectedItem.getTitle());
            intent.putExtra("description", selectedItem.getDescription());
            intent.putExtra("category", selectedItem.getCategory());
            intent.putExtra("status", selectedItem.getStatus());
            intent.putExtra("latitude", selectedItem.getLatitude());
            intent.putExtra("longitude", selectedItem.getLongitude());
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
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
