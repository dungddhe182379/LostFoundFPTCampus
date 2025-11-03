package com.fptcampus.lostfoundfptcampus.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.DetailItemActivity;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements LocationListener {
    private static final int LOCATION_PERMISSION_REQUEST = 200;
    private static final double FPT_UNIVERSITY_LAT = 21.0138;
    private static final double FPT_UNIVERSITY_LNG = 105.5252;

    private MapView mapView;
    private ProgressBar progressBar;
    private ChipGroup chipGroupFilter;
    private TextView tvLostCount, tvFoundCount, tvTotalCount;
    private FloatingActionButton fabMyLocation;
    private com.google.android.material.card.MaterialCardView cardItemInfo;
    private android.widget.ImageView ivItemPreview;
    private TextView tvItemTitle, tvItemCategory;
    private com.google.android.material.button.MaterialButton btnViewDetail, btnShowRoute;

    private SharedPreferencesManager prefsManager;
    private LocationManager locationManager;
    private MyLocationNewOverlay myLocationOverlay;
    private IMapController mapController;

    private String currentFilter = "all";
    private List<LostItem> allItems = new ArrayList<>();
    private LostItem selectedItem;
    private Marker selectedMarker;
    private GeoPoint currentMarkerPosition;
    private org.osmdroid.views.overlay.Polyline routeLine;
    private java.util.concurrent.ExecutorService executorService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize OSMDroid configuration - MUST be done before inflating layout
        Context context = requireContext();
        Configuration.getInstance().load(context, android.preference.PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindingView(view);
        bindingAction();

        prefsManager = new SharedPreferencesManager(requireContext());
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        executorService = java.util.concurrent.Executors.newSingleThreadExecutor();

        setupMap();
        checkLocationPermission();
        loadItemsFromApi();
    }

    private void bindingView(View view) {
        mapView = view.findViewById(R.id.mapView);
        progressBar = view.findViewById(R.id.progressBar);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        tvLostCount = view.findViewById(R.id.tvLostCount);
        tvFoundCount = view.findViewById(R.id.tvFoundCount);
        tvTotalCount = view.findViewById(R.id.tvTotalCount);
        fabMyLocation = view.findViewById(R.id.fabMyLocation);
        cardItemInfo = view.findViewById(R.id.cardItemInfo);
        ivItemPreview = view.findViewById(R.id.ivItemPreview);
        tvItemTitle = view.findViewById(R.id.tvItemTitle);
        tvItemCategory = view.findViewById(R.id.tvItemCategory);
        btnViewDetail = view.findViewById(R.id.btnViewDetail);
        btnShowRoute = view.findViewById(R.id.btnShowRoute);
    }

    private void bindingAction() {
        fabMyLocation.setOnClickListener(v -> moveToMyLocation());
        btnViewDetail.setOnClickListener(v -> onBtnViewDetailClick());
        btnShowRoute.setOnClickListener(v -> onBtnShowRouteClick());

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipAll) {
                    currentFilter = "all";
                } else if (checkedId == R.id.chipLost) {
                    currentFilter = "lost";
                } else if (checkedId == R.id.chipFound) {
                    currentFilter = "found";
                }
                updateMapMarkers();
            }
        });

        // Map touch listener to close info when clicking on empty area
        mapView.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            private static final float CLICK_THRESHOLD = 10;
            private long startTime;
            
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        startTime = System.currentTimeMillis();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                        float endX = event.getX();
                        float endY = event.getY();
                        long endTime = System.currentTimeMillis();
                        
                        boolean isClick = Math.abs(endX - startX) < CLICK_THRESHOLD && 
                                         Math.abs(endY - startY) < CLICK_THRESHOLD &&
                                         (endTime - startTime) < 200;
                        
                        if (isClick) {
                            v.postDelayed(() -> {
                                if (cardItemInfo.getVisibility() == View.VISIBLE) {
                                    hideItemInfoWithAnimation();
                                }
                            }, 100);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);

        mapController = mapView.getController();
        mapController.setZoom(16.0);

        // Set default location to FPT University
        GeoPoint startPoint = new GeoPoint(FPT_UNIVERSITY_LAT, FPT_UNIVERSITY_LNG);
        mapController.setCenter(startPoint);

        // Add my location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, this);
        }
    }

    private void moveToMyLocation() {
        if (myLocationOverlay.getMyLocation() != null) {
            mapController.animateTo(myLocationOverlay.getMyLocation());
            mapController.setZoom(18.0);
        } else {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadItemsFromApi() {
        if (!isAdded() || getActivity() == null) return;

        progressBar.setVisibility(View.VISIBLE);

        String token = "Bearer " + prefsManager.getToken();
        Call<ApiResponse<List<LostItem>>> call = ApiClient.getItemApi().getAllItems(token);

        call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, Response<ApiResponse<List<LostItem>>> response) {
                if (!isAdded() || getActivity() == null) return;

                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<LostItem>> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        allItems = apiResponse.getData();
                        updateMapMarkers();
                        updateStatistics();
                    } else {
                        Toast.makeText(requireContext(),
                                "Lỗi: " + (apiResponse.getError() != null ? apiResponse.getError() : "Không có dữ liệu"),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Lỗi tải dữ liệu: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;

                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Lỗi kết nối: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMapMarkers() {
        // Clear existing markers (except my location overlay)
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);

        // Filter items based on current filter
        List<LostItem> filteredItems = new ArrayList<>();
        for (LostItem item : allItems) {
            if ("all".equals(currentFilter) || currentFilter.equalsIgnoreCase(item.getStatus())) {
                if (item.getLatitude() != null && item.getLongitude() != null) {
                    filteredItems.add(item);
                }
            }
        }

        // Add markers for filtered items
        for (LostItem item : filteredItems) {
            addMarker(item);
        }

        mapView.invalidate();
    }

    private void addMarker(LostItem item) {
        GeoPoint point = new GeoPoint(item.getLatitude(), item.getLongitude());
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(item.getTitle());
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // Save item object to marker
        marker.setRelatedObject(item);

        // Set marker icon with color based on status
        int color;
        String status = item.getStatus();
        if (status == null) status = "";
        
        switch (status.toLowerCase()) {
            case "lost":
                color = android.graphics.Color.RED;
                break;
            case "found":
                color = android.graphics.Color.GREEN;
                break;
            case "returned":
                color = android.graphics.Color.rgb(255, 193, 7);
                break;
            default:
                color = android.graphics.Color.GRAY;
        }
        
        Drawable defaultMarker = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mapmode);
        if (defaultMarker != null) {
            defaultMarker = defaultMarker.mutate();
            defaultMarker.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            marker.setIcon(defaultMarker);
        }
        
        // Create and set custom InfoWindow
        CustomMarkerInfoWindow infoWindow = new CustomMarkerInfoWindow(R.layout.marker_info_window, mapView);
        marker.setInfoWindow(infoWindow);
        
        // Click listener
        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            // Toggle marker selection
            if (selectedMarker != null && selectedMarker.equals(clickedMarker)) {
                hideItemInfoWithAnimation();
                selectedMarker = null;
                return true;
            }

            // Switch to new marker
            if (selectedMarker != null && !selectedMarker.equals(clickedMarker)) {
                if (selectedMarker.getInfoWindow() instanceof CustomMarkerInfoWindow) {
                    ((CustomMarkerInfoWindow) selectedMarker.getInfoWindow()).forceClose();
                }
                
                cardItemInfo.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        cardItemInfo.setVisibility(View.GONE);
                        selectedMarker = clickedMarker;
                        showMarkerContent(clickedMarker, item);
                    })
                    .start();
            } else {
                selectedMarker = clickedMarker;
                showMarkerContent(clickedMarker, item);
            }
            
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    private Drawable getMarkerDrawable(int colorRes) {
        if (!isAdded() || getContext() == null) return null;
        
        // Use Android default map marker icon with color filter
        Drawable marker = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_mapmode);
        if (marker != null) {
            marker = marker.mutate(); // To not affect other markers
            int color = ContextCompat.getColor(requireContext(), colorRes);
            marker.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
        return marker;
    }

    private void showMarkerContent(Marker marker, LostItem item) {
        // Show InfoWindow
        marker.showInfoWindow();
        
        // Save position for routing
        currentMarkerPosition = marker.getPosition();
        
        // Setup and show card
        cardItemInfo.setAlpha(0f);
        cardItemInfo.setVisibility(View.VISIBLE);
        showItemInfo(item);
        
        cardItemInfo.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }
    
    private void showItemInfo(LostItem item) {
        selectedItem = item;
        
        // Save marker position
        if (item.getLatitude() != null && item.getLongitude() != null) {
            currentMarkerPosition = new GeoPoint(item.getLatitude(), item.getLongitude());
        }
        
        // Set title
        tvItemTitle.setText(item.getTitle());
        
        // Set category and status
        String statusText = "";
        if (item.getStatus() != null) {
            switch (item.getStatus().toLowerCase()) {
                case "lost":
                    statusText = "⛔ Thất lạc";
                    break;
                case "found":
                    statusText = "✅ Đã tìm thấy";
                    break;
                case "returned":
                    statusText = "📦 Đã trả";
                    break;
                default:
                    statusText = "⚙️ Không rõ";
            }
        }
        String category = item.getCategory() != null ? item.getCategory() : "Khác";
        tvItemCategory.setText(category + " • " + statusText);
        
        // Load image
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .centerCrop()
                .into(ivItemPreview);
        } else {
            ivItemPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        }
        
        cardItemInfo.setVisibility(View.VISIBLE);
    }
    
    private void hideItemInfoWithAnimation() {
        // Close InfoWindow
        if (selectedMarker != null && selectedMarker.getInfoWindow() instanceof CustomMarkerInfoWindow) {
            ((CustomMarkerInfoWindow) selectedMarker.getInfoWindow()).forceClose();
        }
        
        // Animate card out
        cardItemInfo.animate()
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(new android.view.animation.AccelerateInterpolator())
            .withEndAction(() -> {
                cardItemInfo.setVisibility(View.GONE);
                cardItemInfo.setAlpha(1f);
                selectedMarker = null;
                selectedItem = null;
                currentMarkerPosition = null;
            })
            .start();
    }
    
    private void onBtnViewDetailClick() {
        if (selectedItem != null) {
            Intent intent = new Intent(requireContext(), DetailItemActivity.class);
            intent.putExtra("itemId", selectedItem.getId());
            intent.putExtra("title", selectedItem.getTitle());
            intent.putExtra("description", selectedItem.getDescription());
            intent.putExtra("category", selectedItem.getCategory());
            intent.putExtra("status", selectedItem.getStatus());
            intent.putExtra("latitude", selectedItem.getLatitude() != null ? selectedItem.getLatitude() : 0.0);
            intent.putExtra("longitude", selectedItem.getLongitude() != null ? selectedItem.getLongitude() : 0.0);
            startActivity(intent);
        }
    }
    
    private void onBtnShowRouteClick() {
        if (currentMarkerPosition == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn một item trên bản đồ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Hide detail card and InfoWindow
        hideItemInfoWithAnimation();
        
        // Draw route from current location to marker
        GeoPoint myLocation = myLocationOverlay.getMyLocation();
        if (myLocation != null) {
            drawRoute(myLocation, currentMarkerPosition);
        } else {
            // Use FPT location as fallback
            GeoPoint fptPoint = new GeoPoint(FPT_UNIVERSITY_LAT, FPT_UNIVERSITY_LNG);
            drawRoute(fptPoint, currentMarkerPosition);
        }
    }
    
    // Custom InfoWindow class
    private class CustomMarkerInfoWindow extends org.osmdroid.views.overlay.infowindow.InfoWindow {
        private android.widget.ImageView ivMarkerImage;
        private TextView tvMarkerTitle;
        private TextView tvMarkerStatus;
        private boolean preventAutoClose = true;
        private boolean isOpened = false;

        public CustomMarkerInfoWindow(int layoutResId, MapView mapView) {
            super(layoutResId, mapView);
            
            ivMarkerImage = mView.findViewById(R.id.ivMarkerImage);
            tvMarkerTitle = mView.findViewById(R.id.tvMarkerTitle);
            tvMarkerStatus = mView.findViewById(R.id.tvMarkerStatus);
        }

        @Override
        public void onOpen(Object item) {
            preventAutoClose = true;
            isOpened = true;
            
            Marker marker = (Marker) item;
            Object relatedObj = marker.getRelatedObject();
            
            if (relatedObj instanceof LostItem) {
                LostItem lostItem = (LostItem) relatedObj;
                
                // Load image
                if (lostItem.getImageUrl() != null && !lostItem.getImageUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(MapFragment.this)
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
                        statusLabel = "⛔ Thất lạc";
                        break;
                    case "found":
                        statusLabel = "✅ Đã tìm thấy";
                        break;
                    case "returned":
                        statusLabel = "📦 Đã trả";
                        break;
                    default:
                        statusLabel = "⚙️ Không rõ";
                }
                tvMarkerStatus.setText(statusLabel);
            }
            
            mView.setVisibility(View.VISIBLE);
            
            mView.postDelayed(() -> {
                if (isOpened) {
                    mView.setVisibility(View.VISIBLE);
                    mView.bringToFront();
                    mView.invalidate();
                }
            }, 50);
        }

        @Override
        public void onClose() {
            if (!preventAutoClose) {
                isOpened = false;
                mView.setVisibility(View.GONE);
            } else {
                mView.setVisibility(View.VISIBLE);
            }
        }
        
        public void forceClose() {
            preventAutoClose = false;
            isOpened = false;
            mView.setVisibility(View.GONE);
            mView.postDelayed(() -> {
                preventAutoClose = true;
            }, 100);
        }
    }

    private void updateStatistics() {
        int lostCount = 0, foundCount = 0;

        for (LostItem item : allItems) {
            if ("lost".equalsIgnoreCase(item.getStatus())) {
                lostCount++;
            } else if ("found".equalsIgnoreCase(item.getStatus())) {
                foundCount++;
            }
        }

        final int finalLostCount = lostCount;
        final int finalFoundCount = foundCount;
        final int totalCount = allItems.size();

        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                tvLostCount.setText(String.valueOf(finalLostCount));
                tvFoundCount.setText(String.valueOf(finalFoundCount));
                tvTotalCount.setText(String.valueOf(totalCount));
            });
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // Location updated
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated in API 29, but required for older APIs
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Provider enabled
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Provider disabled
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    // Routing methods
    private void drawRoute(GeoPoint start, GeoPoint end) {
        executorService.execute(() -> {
            try {
                String urlString = "https://router.project-osrm.org/route/v1/foot/"
                        + start.getLongitude() + "," + start.getLatitude() + ";"
                        + end.getLongitude() + "," + end.getLatitude()
                        + "?overview=full&geometries=polyline";

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader in = new java.io.BufferedReader(
                            new java.io.InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    String jsonResponse = response.toString();
                    List<GeoPoint> routePoints = parseOSRMResponse(jsonResponse);

                    if (routePoints != null && !routePoints.isEmpty()) {
                        double distance = extractDistanceFromResponse(jsonResponse);
                        double duration = extractDurationFromResponse(jsonResponse);

                        requireActivity().runOnUiThread(() -> {
                            if (routeLine != null) {
                                mapView.getOverlays().remove(routeLine);
                            }

                            routeLine = new org.osmdroid.views.overlay.Polyline();
                            routeLine.setPoints(routePoints);
                            routeLine.setColor(android.graphics.Color.BLUE);
                            routeLine.setWidth(8f);
                            mapView.getOverlays().add(routeLine);
                            mapView.invalidate();

                            String distanceStr = distance >= 1000
                                    ? String.format("%.1f km", distance / 1000)
                                    : String.format("%.0f m", distance);
                            String durationStr = String.format("%.0f phút", duration / 60);

                            Toast.makeText(requireContext(),
                                    "Khoảng cách: " + distanceStr + " - Thời gian: " + durationStr,
                                    Toast.LENGTH_LONG).show();
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            drawStraightLineRoute(start, end);
                            Toast.makeText(requireContext(),
                                    "Không thể tìm đường, hiển thị đường thẳng",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        drawStraightLineRoute(start, end);
                        Toast.makeText(requireContext(),
                                "Lỗi kết nối, hiển thị đường thẳng",
                                Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    drawStraightLineRoute(start, end);
                    Toast.makeText(requireContext(),
                            "Lỗi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<GeoPoint> parseOSRMResponse(String jsonResponse) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            org.json.JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                org.json.JSONObject route = routes.getJSONObject(0);
                String geometry = route.getString("geometry");
                return decodePolyline(geometry);
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> poly = new java.util.ArrayList<>();
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

            GeoPoint p = new GeoPoint((lat / 1E5), (lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    private double extractDistanceFromResponse(String jsonResponse) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            org.json.JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                org.json.JSONObject route = routes.getJSONObject(0);
                return route.getDouble("distance");
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double extractDurationFromResponse(String jsonResponse) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            org.json.JSONArray routes = jsonObject.getJSONArray("routes");
            if (routes.length() > 0) {
                org.json.JSONObject route = routes.getJSONObject(0);
                return route.getDouble("duration");
            }
        } catch (org.json.JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void drawStraightLineRoute(GeoPoint start, GeoPoint end) {
        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
        }

        List<GeoPoint> points = new java.util.ArrayList<>();
        points.add(start);
        points.add(end);

        routeLine = new org.osmdroid.views.overlay.Polyline();
        routeLine.setPoints(points);
        routeLine.setColor(android.graphics.Color.RED);
        routeLine.setWidth(8f);
        mapView.getOverlays().add(routeLine);
        mapView.invalidate();

        double distance = start.distanceToAsDouble(end);
        String distanceStr = distance >= 1000
                ? String.format("%.1f km", distance / 1000)
                : String.format("%.0f m", distance);
        Toast.makeText(requireContext(),
                "Khoảng cách đường chim bay: " + distanceStr,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
