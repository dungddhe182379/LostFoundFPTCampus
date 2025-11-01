package com.fptcampus.lostfoundfptcampus.controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
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
    private MaterialButton btnViewDetail, btnShowRoute;

    private IMapController mapController;
    private SharedPreferencesManager prefsManager;
    private ExecutorService executorService;
    private LostItem selectedItem;
    private Polyline routeLine;
    private GeoPoint currentMarkerPosition; // Vị trí marker được chọn
    private Marker selectedMarker;

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
        
        // Create "Chỉ đường" button programmatically
        btnShowRoute = new MaterialButton(this);
        btnShowRoute.setText("Chỉ đường");
        btnShowRoute.setIcon(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_directions));
        btnShowRoute.setOnClickListener(this::onBtnShowRouteClick);
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

        // Add listener để đóng InfoWindow khi click vùng trống
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                // Đóng info khi scroll map
                hideItemInfo();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });

        // Add touch listener để bắt click vào vùng trống (không phải marker)
        mapView.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private static final float CLICK_THRESHOLD = 10;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        
                        // Kiểm tra xem có phải là click (không phải drag)
                        if (Math.abs(endX - startX) < CLICK_THRESHOLD && 
                            Math.abs(endY - startY) < CLICK_THRESHOLD) {
                            // Click vào map (không phải marker) -> đóng info
                            hideItemInfo();
                        }
                        break;
                }
                return false; // Return false để map vẫn nhận event
            }
        });

        // Add marker for FPT location (vị trí hiện tại/xuất phát)
        addFptMarker();

        // Check if launched with specific location
        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", FPT_LAT);
        double longitude = intent.getDoubleExtra("longitude", FPT_LNG);
        String title = intent.getStringExtra("title");

        // If specific location provided (từ Detail screen)
        if (title != null && latitude != FPT_LAT && longitude != FPT_LNG) {
            // Zoom GẦN vào vị trí item
            mapController.setZoom(18.0);
            GeoPoint itemPoint = new GeoPoint(latitude, longitude);
            mapController.setCenter(itemPoint);
            
            // Tạo LostItem tạm từ Intent data
            LostItem tempItem = new LostItem();
            tempItem.setTitle(title);
            tempItem.setLatitude(latitude);
            tempItem.setLongitude(longitude);
            tempItem.setStatus(intent.getStringExtra("status") != null ? intent.getStringExtra("status") : "lost");
            tempItem.setCategory(intent.getStringExtra("category") != null ? intent.getStringExtra("category") : "Khác");
            tempItem.setDescription(intent.getStringExtra("description"));
            tempItem.setImageUrl(intent.getStringExtra("imageUrl"));
            tempItem.setId(intent.getLongExtra("itemId", 0));
            
            // Add marker
            addMarker(tempItem);
            
            // Auto-show info card như khi click marker
            showItemInfo(tempItem);
        } else {
            // Zoom bình thường cho view tổng quan
            mapController.setZoom(18.0);
            GeoPoint startPoint = new GeoPoint(FPT_LAT, FPT_LNG);
            mapController.setCenter(startPoint);
        }
    }

    private void addFptMarker() {
        // Thêm marker màu xanh dương cho vị trí FPT (điểm xuất phát)
        GeoPoint fptPoint = new GeoPoint(FPT_LAT, FPT_LNG);
        Marker fptMarker = new Marker(mapView);
        fptMarker.setPosition(fptPoint);
        fptMarker.setTitle("📍 Vị trí hiện tại (FPT)");
        fptMarker.setSnippet("Điểm xuất phát");
        fptMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // Tạo drawable màu xanh dương cho vị trí hiện tại
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (icon != null) {
            icon.setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN);
            fptMarker.setIcon(icon);
        }
        
        mapView.getOverlays().add(fptMarker);
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
        // Clear existing markers (trừ FPT marker và route)
        List<org.osmdroid.views.overlay.Overlay> toKeep = new ArrayList<>();
        for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
            // Keep FPT marker (has "Vị trí hiện tại" in title)
            if (overlay instanceof Marker) {
                Marker m = (Marker) overlay;
                if (m.getTitle() != null && m.getTitle().contains("Vị trí hiện tại")) {
                    toKeep.add(overlay);
                }
            }
            // Keep route line
            if (overlay instanceof Polyline) {
                toKeep.add(overlay);
            }
        }
        
        mapView.getOverlays().clear();
        mapView.getOverlays().addAll(toKeep);

        // Add FPT marker if not exists
        boolean hasFptMarker = false;
        for (org.osmdroid.views.overlay.Overlay overlay : toKeep) {
            if (overlay instanceof Marker) {
                Marker m = (Marker) overlay;
                if (m.getTitle() != null && m.getTitle().contains("Vị trí hiện tại")) {
                    hasFptMarker = true;
                    break;
                }
            }
        }
        if (!hasFptMarker) {
            addFptMarker();
        }

        int markerCount = 0;
        for (LostItem item : items) {
            if (item.getLatitude() != null && item.getLongitude() != null) {
                addMarker(item); // Truyền toàn bộ item object
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

    private void addMarker(LostItem item) {
        GeoPoint point = new GeoPoint(item.getLatitude(), item.getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(item.getTitle());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // Lưu item object vào marker để dùng sau
        marker.setRelatedObject(item);

        // Set marker color based on status - Tạo icon màu tùy chỉnh
        int color;
        String statusLabel;
        String status = item.getStatus();
        if (status == null) status = "";
        
        switch (status.toLowerCase()) {
            case "lost":
                color = Color.RED; // 🔴 Đỏ: đồ thất lạc
                statusLabel = "Thất lạc";
                break;
            case "found":
                color = Color.GREEN; // 🟢 Xanh lá: đã tìm thấy
                statusLabel = "Đã tìm thấy";
                break;
            case "returned":
                color = Color.rgb(255, 193, 7); // 🟡 Vàng: đã trả
                statusLabel = "Đã trả";
                break;
            default:
                color = Color.GRAY; // ⚪ Xám: không xác định
                statusLabel = "Không rõ";
        }
        
        // Tạo marker icon với màu sắc tùy chỉnh
        android.graphics.drawable.Drawable defaultMarker = getResources().getDrawable(org.osmdroid.library.R.drawable.marker_default);
        if (defaultMarker != null) {
            defaultMarker = defaultMarker.mutate(); // Để không ảnh hưởng markers khác
            defaultMarker.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            marker.setIcon(defaultMarker);
        }
        
        // Set snippet (description) hiển thị dưới title
        marker.setSnippet(statusLabel);
        
        // Tạo và gắn custom InfoWindow
        CustomMarkerInfoWindow infoWindow = new CustomMarkerInfoWindow(R.layout.marker_info_window, mapView);
        marker.setInfoWindow(infoWindow);
        
        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            // If clicking the same marker again -> toggle off
            if (selectedMarker != null && selectedMarker.equals(clickedMarker)) {
                hideItemInfoWithAnimation();
                selectedMarker = null;
                return true;
            }

            // If clicking different marker -> close old, show new
            if (selectedMarker != null && !selectedMarker.equals(clickedMarker)) {
                // Close old marker's InfoWindow
                selectedMarker.closeInfoWindow();
                
                // Fade out old card description
                cardItemInfo.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        cardItemInfo.setVisibility(View.GONE);
                        
                        // Then show new marker's content
                        selectedMarker = clickedMarker;
                        showNewMarkerContent(clickedMarker);
                    })
                    .start();
            } else {
                // First time clicking any marker
                selectedMarker = clickedMarker;
                showNewMarkerContent(clickedMarker);
            }
            
            return true;
        });

        mapView.getOverlays().add(marker);
    }
    
    private void onMarkerClick(Marker marker) {
        // Get item object from marker
        Object relatedObj = marker.getRelatedObject();
        if (!(relatedObj instanceof LostItem)) {
            // Nếu không phải item marker (có thể là FPT marker)
            return;
        }
        
        LostItem item = (LostItem) relatedObj;
        
        // Hiển thị thông tin item
        showItemInfo(item);
        
        // KHÔNG center map để tránh animation đóng InfoWindow
        // mapController.animateTo(marker.getPosition());
    }
    
    private void showNewMarkerContent(Marker marker) {
        // Close all InfoWindows first
        org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(mapView);
        
        // Get item from marker
        Object relatedObj = marker.getRelatedObject();
        if (!(relatedObj instanceof LostItem)) {
            return;
        }
        
        LostItem item = (LostItem) relatedObj;
        
        // Show InfoWindow above marker first
        marker.showInfoWindow();
        
        // Then fade in card description below with animation
        showItemInfoWithAnimation(item);
    }
    
    private void showItemInfoWithAnimation(LostItem item) {
        selectedItem = item; // Lưu để dùng cho nút "Xem chi tiết"
        
        // Save marker position for routing
        if (item.getLatitude() != null && item.getLongitude() != null) {
            currentMarkerPosition = new GeoPoint(item.getLatitude(), item.getLongitude());
        }
        
        // Hiển thị thông tin item
        tvItemTitle.setText(item.getTitle());
        
        // Hiển thị category và status
        String statusText = "";
        if (item.getStatus() != null) {
            switch (item.getStatus().toLowerCase()) {
                case "lost":
                    statusText = "🔴 Thất lạc";
                    break;
                case "found":
                    statusText = "🟢 Đã tìm thấy";
                    break;
                case "returned":
                    statusText = "🟡 Đã trả";
                    break;
                default:
                    statusText = "⚪ Không rõ";
            }
        }
        String category = item.getCategory() != null ? item.getCategory() : "Khác";
        tvItemCategory.setText(category + " • " + statusText);
        
        // Load ảnh từ imageUrl (nếu có)
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            // Sử dụng Glide để load ảnh thực
            com.bumptech.glide.Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery) // Hiển thị khi đang load
                .error(android.R.drawable.ic_menu_report_image) // Hiển thị khi lỗi
                .centerCrop()
                .into(ivItemPreview);
        } else {
            // Hiển thị placeholder nếu không có ảnh
            ivItemPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        // Add route button to card if not already added
        android.view.ViewGroup parent = (android.view.ViewGroup) btnViewDetail.getParent();
        if (parent != null && btnShowRoute.getParent() == null) {
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 0, 0);
            parent.addView(btnShowRoute, 0, params);
        }
        
        // Fade in animation
        cardItemInfo.setAlpha(0f);
        cardItemInfo.setVisibility(View.VISIBLE);
        cardItemInfo.animate()
            .alpha(1f)
            .setDuration(250)
            .start();
    }
    
    private void showItemInfo(LostItem item) {
        selectedItem = item; // Lưu để dùng cho nút "Xem chi tiết"
        
        // Save marker position for routing
        if (item.getLatitude() != null && item.getLongitude() != null) {
            currentMarkerPosition = new GeoPoint(item.getLatitude(), item.getLongitude());
        }
        
        // Hiển thị thông tin item
        tvItemTitle.setText(item.getTitle());
        
        // Hiển thị category và status
        String statusText = "";
        if (item.getStatus() != null) {
            switch (item.getStatus().toLowerCase()) {
                case "lost":
                    statusText = "🔴 Thất lạc";
                    break;
                case "found":
                    statusText = "🟢 Đã tìm thấy";
                    break;
                case "returned":
                    statusText = "🟡 Đã trả";
                    break;
                default:
                    statusText = "⚪ Không rõ";
            }
        }
        String category = item.getCategory() != null ? item.getCategory() : "Khác";
        tvItemCategory.setText(category + " • " + statusText);
        
        // Load ảnh từ imageUrl (nếu có)
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            // Sử dụng Glide để load ảnh thực
            com.bumptech.glide.Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery) // Hiển thị khi đang load
                .error(android.R.drawable.ic_menu_report_image) // Hiển thị khi lỗi
                .centerCrop()
                .into(ivItemPreview);
        } else {
            // Hiển thị placeholder nếu không có ảnh
            ivItemPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        // Add route button to card if not already added
        android.view.ViewGroup parent = (android.view.ViewGroup) btnViewDetail.getParent();
        if (parent != null && btnShowRoute.getParent() == null) {
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 0, 0);
            parent.addView(btnShowRoute, 0, params);
        }
        
        cardItemInfo.setVisibility(View.VISIBLE);
    }

    private void hideItemInfoWithAnimation() {
        // Force close selected marker's InfoWindow
        if (selectedMarker != null && selectedMarker.getInfoWindow() instanceof CustomMarkerInfoWindow) {
            CustomMarkerInfoWindow infoWindow = (CustomMarkerInfoWindow) selectedMarker.getInfoWindow();
            infoWindow.forceClose();
        }
        
        // Close all InfoWindows on map (fallback)
        org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(mapView);
        
        // Fade out animation for card
        cardItemInfo.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction(() -> {
                cardItemInfo.setVisibility(View.GONE);
                cardItemInfo.setAlpha(1f); // Reset alpha for next show
            })
            .start();
    }
    
    private void hideItemInfo() {
        // Force close selected marker's InfoWindow
        if (selectedMarker != null && selectedMarker.getInfoWindow() instanceof CustomMarkerInfoWindow) {
            CustomMarkerInfoWindow infoWindow = (CustomMarkerInfoWindow) selectedMarker.getInfoWindow();
            infoWindow.forceClose();
        }
        
        // Close all InfoWindows on map (fallback)
        org.osmdroid.views.overlay.infowindow.InfoWindow.closeAllInfoWindowsOn(mapView);
        
        // Hide bottom info card and clear selection
        runOnUiThread(() -> {
            try {
                cardItemInfo.setVisibility(View.GONE);
                // Remove route button from parent if present
                if (btnShowRoute != null && btnShowRoute.getParent() != null) {
                    ((android.view.ViewGroup) btnShowRoute.getParent()).removeView(btnShowRoute);
                }
            } catch (Exception ignored) {}
        });
        selectedItem = null;
        currentMarkerPosition = null;
        selectedMarker = null;
    }

    private void onToolbarBackClick(View view) {
        finish();
    }

    private void onFabMyLocationClick(View view) {
        // Center on FPT Campus với zoom cao
        GeoPoint fptPoint = new GeoPoint(FPT_LAT, FPT_LNG);
        mapController.animateTo(fptPoint);
        mapController.setZoom(21.0); // Zoom rất gần
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

    private void onBtnShowRouteClick(View view) {
        if (currentMarkerPosition == null) {
            Toast.makeText(this, "Vui lòng chọn một item trên bản đồ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Draw route from FPT to selected marker
        GeoPoint fptPoint = new GeoPoint(FPT_LAT, FPT_LNG);
        drawRoute(fptPoint, currentMarkerPosition);
    }

    private void drawRoute(GeoPoint start, GeoPoint end) {
        // Remove old route if exists
        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
        }

        Toast.makeText(this, "Đang tìm đường đi...", Toast.LENGTH_SHORT).show();

        // Use OSRM API to get walking route
        executorService.execute(() -> {
            try {
                // OSRM API format: http://router.project-osrm.org/route/v1/driving/lon1,lat1;lon2,lat2?overview=full&geometries=polyline
                String url = String.format(
                    "https://router.project-osrm.org/route/v1/foot/%f,%f;%f,%f?overview=full&geometries=polyline",
                    start.getLongitude(), start.getLatitude(),
                    end.getLongitude(), end.getLatitude()
                );

                // Make HTTP request
                java.net.URL osrmUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) osrmUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // Read response
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream())
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    String jsonResponse = response.toString();
                    List<GeoPoint> routePoints = parseOSRMResponse(jsonResponse);

                    if (routePoints != null && routePoints.size() > 0) {
                        // Draw route on UI thread
                        runOnUiThread(() -> {
                            routeLine = new Polyline();
                            routeLine.setPoints(routePoints);
                            routeLine.setColor(Color.BLUE);
                            routeLine.setWidth(8f);
                            
                            mapView.getOverlays().add(routeLine);
                            mapView.invalidate();

                            // Get distance from OSRM response (in meters)
                            double distance = extractDistanceFromResponse(jsonResponse);
                            String distanceText = distance < 1000 
                                ? String.format("%.0f m", distance)
                                : String.format("%.2f km", distance / 1000);

                            double duration = extractDurationFromResponse(jsonResponse); // in seconds
                            String durationText = duration < 60
                                ? String.format("%.0f giây", duration)
                                : String.format("%.0f phút", duration / 60);

                            Toast.makeText(this, 
                                "Khoảng cách: " + distanceText + "\nThời gian đi bộ: " + durationText, 
                                Toast.LENGTH_LONG).show();

                            // KHÔNG zoom/move - giữ nguyên vị trí hiện tại, chỉ vẽ route
                        });
                    } else {
                        // Fallback to straight line if parsing fails
                        runOnUiThread(() -> drawStraightLineRoute(start, end));
                    }
                } else {
                    // Fallback to straight line if API fails
                    runOnUiThread(() -> drawStraightLineRoute(start, end));
                }
                
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback to straight line on error
                runOnUiThread(() -> drawStraightLineRoute(start, end));
            }
        });
    }

    private List<GeoPoint> parseOSRMResponse(String jsonResponse) {
        try {
            // Simple JSON parsing without external library
            // Extract geometry from: "routes":[{"geometry":"encoded_polyline",...}]
            int geometryStart = jsonResponse.indexOf("\"geometry\":\"") + 12;
            int geometryEnd = jsonResponse.indexOf("\"", geometryStart);
            String encodedPolyline = jsonResponse.substring(geometryStart, geometryEnd);
            
            // Decode polyline
            return decodePolyline(encodedPolyline);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private double extractDistanceFromResponse(String jsonResponse) {
        try {
            // Extract distance: "distance":1234.5
            int distStart = jsonResponse.indexOf("\"distance\":") + 11;
            int distEnd = jsonResponse.indexOf(",", distStart);
            if (distEnd == -1) distEnd = jsonResponse.indexOf("}", distStart);
            String distStr = jsonResponse.substring(distStart, distEnd);
            return Double.parseDouble(distStr);
        } catch (Exception e) {
            return 0;
        }
    }

    private double extractDurationFromResponse(String jsonResponse) {
        try {
            // Extract duration: "duration":123.4
            int durStart = jsonResponse.indexOf("\"duration\":") + 11;
            int durEnd = jsonResponse.indexOf(",", durStart);
            if (durEnd == -1) durEnd = jsonResponse.indexOf("}", durStart);
            String durStr = jsonResponse.substring(durStart, durEnd);
            return Double.parseDouble(durStr);
        } catch (Exception e) {
            return 0;
        }
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        // Google Polyline encoding algorithm
        List<GeoPoint> points = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            points.add(new GeoPoint((double) lat / 1e5, (double) lng / 1e5));
        }

        return points;
    }

    private void drawStraightLineRoute(GeoPoint start, GeoPoint end) {
        // Fallback: draw straight line
        routeLine = new Polyline();
        List<GeoPoint> points = new ArrayList<>();
        points.add(start);
        points.add(end);
        routeLine.setPoints(points);
        routeLine.setColor(Color.RED);
        routeLine.setWidth(8f);
        
        mapView.getOverlays().add(routeLine);
        mapView.invalidate();

        double distance = start.distanceToAsDouble(end);
        String distanceText = distance < 1000 
            ? String.format("%.0f m", distance)
            : String.format("%.2f km", distance / 1000);

        Toast.makeText(this, 
            "Không tìm được đường đi.\nKhoảng cách thẳng: " + distanceText, 
            Toast.LENGTH_LONG).show();

        // KHÔNG zoom/move - giữ nguyên vị trí hiện tại
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

    // Custom InfoWindow for marker
    private class CustomMarkerInfoWindow extends org.osmdroid.views.overlay.infowindow.InfoWindow {
        private ImageView ivMarkerImage;
        private TextView tvMarkerTitle;
        private TextView tvMarkerStatus;
        private boolean preventAutoClose = true; // Flag để ngăn auto-close

        public CustomMarkerInfoWindow(int layoutResId, MapView mapView) {
            super(layoutResId, mapView);
            
            ivMarkerImage = mView.findViewById(R.id.ivMarkerImage);
            tvMarkerTitle = mView.findViewById(R.id.tvMarkerTitle);
            tvMarkerStatus = mView.findViewById(R.id.tvMarkerStatus);
        }

        @Override
        public void onOpen(Object item) {
            preventAutoClose = true; // Reset flag
            
            Marker marker = (Marker) item;
            Object relatedObj = marker.getRelatedObject();
            
            if (relatedObj instanceof LostItem) {
                LostItem lostItem = (LostItem) relatedObj;
                
                // Load image
                if (lostItem.getImageUrl() != null && !lostItem.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(MapActivity.this)
                        .load(lostItem.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(ivMarkerImage);
                } else {
                    ivMarkerImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
                
                // Set title
                tvMarkerTitle.setText(lostItem.getTitle());
                
                // Set status
                String status = lostItem.getStatus();
                String statusLabel;
                switch (status != null ? status.toLowerCase() : "") {
                    case "lost":
                        statusLabel = "🔴 Thất lạc";
                        break;
                    case "found":
                        statusLabel = "🟢 Đã tìm thấy";
                        break;
                    case "returned":
                        statusLabel = "🟡 Đã trả";
                        break;
                    default:
                        statusLabel = "⚪ Không rõ";
                }
                tvMarkerStatus.setText(statusLabel);
            }
            
            // Giữ InfoWindow mở - không tự động đóng
            mView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onClose() {
            // CHỈ đóng khi được gọi EXPLICITLY từ hideItemInfo()
            if (!preventAutoClose) {
                mView.setVisibility(View.GONE);
            }
            // Nếu preventAutoClose=true thì KHÔNG đóng (ignore auto-close từ OSMDroid)
        }
        
        // Method để force close khi cần
        public void forceClose() {
            preventAutoClose = false;
            mView.setVisibility(View.GONE);
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
