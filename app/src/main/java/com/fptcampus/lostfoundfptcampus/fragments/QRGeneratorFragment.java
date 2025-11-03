package com.fptcampus.lostfoundfptcampus.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.adapter.QrItemSelectorAdapter;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.navigation.NavigationHost;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.ServerTimeSync;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRGeneratorFragment extends Fragment {
    
    // UI Components
    private ImageButton btnBack;
    private MaterialCardView cardItemSelection, cardSelectedItem, cardQrCode;
    private TabLayout tabLayoutFilter;
    private TextInputEditText etSearch;
    private RecyclerView rvItems;
    private TextView tvEmptyState;
    private ImageView ivSelectedItemImage, ivQrCode;
    private TextView tvSelectedItemTitle, tvSelectedItemCategory, tvSelectedItemStatus;
    private TextView tvQrInfo, tvQrExpiry;
    private MaterialButton btnGenerateQr, btnShareQr;
    private ProgressBar progressBar;
    
    // Data
    private SharedPreferencesManager prefsManager;
    private NavigationHost navigationHost;
    private QrItemSelectorAdapter adapter;
    private List<LostItem> allItems = new ArrayList<>();
    private LostItem selectedItem;
    private LostItem preselectedItem; // Item passed from DetailItemFragment
    private Bitmap currentQrBitmap;
    private String currentQrToken;
    
    // Constants
    private static final String ARG_ITEM_ID = "item_id";
    private static final int QR_CODE_SIZE = 800;
    
    public static QRGeneratorFragment newInstance() {
        return new QRGeneratorFragment();
    }
    
    public static QRGeneratorFragment newInstance(long itemId) {
        QRGeneratorFragment fragment = new QRGeneratorFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ITEM_ID, itemId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_generator, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        initData();
        setupListeners();
        
        // Check if item was preselected
        if (getArguments() != null && getArguments().containsKey(ARG_ITEM_ID)) {
            long itemId = getArguments().getLong(ARG_ITEM_ID);
            loadPreselectedItem(itemId);
        } else {
            // Load user's items for selection
            loadUserItems();
        }
    }
    
    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        cardItemSelection = view.findViewById(R.id.cardItemSelection);
        cardSelectedItem = view.findViewById(R.id.cardSelectedItem);
        cardQrCode = view.findViewById(R.id.cardQrCode);
        tabLayoutFilter = view.findViewById(R.id.tabLayoutFilter);
        etSearch = view.findViewById(R.id.etSearch);
        rvItems = view.findViewById(R.id.rvItems);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        ivSelectedItemImage = view.findViewById(R.id.ivSelectedItemImage);
        ivQrCode = view.findViewById(R.id.ivQrCode);
        tvSelectedItemTitle = view.findViewById(R.id.tvSelectedItemTitle);
        tvSelectedItemCategory = view.findViewById(R.id.tvSelectedItemCategory);
        tvSelectedItemStatus = view.findViewById(R.id.tvSelectedItemStatus);
        tvQrInfo = view.findViewById(R.id.tvQrInfo);
        tvQrExpiry = view.findViewById(R.id.tvQrExpiry);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);
        btnShareQr = view.findViewById(R.id.btnShareQr);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Setup RecyclerView
        rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
    }
    
    private void initData() {
        prefsManager = new SharedPreferencesManager(requireContext());
        
        if (getActivity() instanceof NavigationHost) {
            navigationHost = (NavigationHost) getActivity();
        }
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
        
        // Tab filter
        tabLayoutFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String status = tab.getPosition() == 0 ? "found" : "lost";
                filterItemsByStatus(status);
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // Search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (adapter != null) {
                    adapter.filter(s.toString());
                    updateEmptyState();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        btnGenerateQr.setOnClickListener(v -> generateQrCode());
        btnShareQr.setOnClickListener(v -> shareQrCode());
    }
    
    private void loadUserItems() {
        showLoading(true);
        
        String token = "Bearer " + prefsManager.getToken();
        long userId = prefsManager.getUserId();
        
        ApiClient.getItemApi().getAllItems(token).enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, 
                                   Response<ApiResponse<List<LostItem>>> response) {
                showLoading(false);
                
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Sync server time from API response
                    ServerTimeSync.updateServerTime(response.body().getTimestamp());
                    
                    List<LostItem> items = response.body().getData();
                    
                    // Filter items created by current user with status "lost" or "found"
                    allItems.clear();
                    for (LostItem item : items) {
                        if (item.getUserId() == userId && 
                           ("lost".equals(item.getStatus()) || "found".equals(item.getStatus()))) {
                            allItems.add(item);
                        }
                    }
                    
                    android.util.Log.d("QRGenerator", "Loaded " + allItems.size() + " items for user " + userId);
                    
                    setupAdapter();
                    filterItemsByStatus("found"); // Default to "found" tab
                    
                } else {
                    Toast.makeText(requireContext(), "Không thể tải danh sách đồ vật", 
                            Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                showLoading(false);
                if (isAdded()) {
                    android.util.Log.e("QRGenerator", "Error loading items: " + t.getMessage());
                    Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void loadPreselectedItem(long itemId) {
        showLoading(true);
        
        String token = "Bearer " + prefsManager.getToken();
        
        ApiClient.getItemApi().getItemById(token, itemId).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, 
                                   Response<ApiResponse<LostItem>> response) {
                showLoading(false);
                
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Sync server time from API response
                    ServerTimeSync.updateServerTime(response.body().getTimestamp());
                    
                    preselectedItem = response.body().getData();
                    selectedItem = preselectedItem;
                    
                    android.util.Log.d("QRGenerator", "Loaded preselected item: " + selectedItem.getTitle());
                    
                    // Hide item selection card, show selected item
                    cardItemSelection.setVisibility(View.GONE);
                    showSelectedItem();
                    
                    // Auto-generate QR code
                    generateQrCode();
                    
                } else {
                    Toast.makeText(requireContext(), "Không thể tải thông tin đồ vật", 
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                showLoading(false);
                if (isAdded()) {
                    android.util.Log.e("QRGenerator", "Error loading item: " + t.getMessage());
                    Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void setupAdapter() {
        adapter = new QrItemSelectorAdapter(allItems, item -> {
            selectedItem = item;
            showSelectedItem();
        });
        
        rvItems.setAdapter(adapter);
        updateEmptyState();
    }
    
    private void filterItemsByStatus(String status) {
        if (adapter != null) {
            adapter.filterByStatus(status);
            updateEmptyState();
        }
    }
    
    private void updateEmptyState() {
        if (adapter != null && adapter.getItemCount() == 0) {
            rvItems.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvItems.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }
    
    private void showSelectedItem() {
        if (selectedItem == null) return;
        
        cardSelectedItem.setVisibility(View.VISIBLE);
        
        tvSelectedItemTitle.setText(selectedItem.getTitle());
        tvSelectedItemCategory.setText(selectedItem.getCategory());
        
        // Set status
        String status = selectedItem.getStatus();
        tvSelectedItemStatus.setText(getStatusText(status));
        tvSelectedItemStatus.setBackgroundResource(getStatusBackground(status));
        
        // Load image
        if (selectedItem.getImageUrl() != null && !selectedItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(selectedItem.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(ivSelectedItemImage);
        } else {
            ivSelectedItemImage.setImageResource(R.drawable.ic_image_placeholder);
        }
        
        btnGenerateQr.setEnabled(true);
    }
    
    private void generateQrCode() {
        if (selectedItem == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn đồ vật", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Generate token using server time to avoid timezone issues
            // Format: TOKEN_{serverTimestamp}
            long serverTime = ServerTimeSync.getServerTime();
            currentQrToken = "TOKEN_" + serverTime;
            
            // Log time info for debugging
            android.util.Log.d("QRGenerator", "Server time offset: " + 
                ServerTimeSync.getServerTimeOffsetHours() + " hours");
            android.util.Log.d("QRGenerator", "Generated token: " + currentQrToken);
            
            // Create QR content (JSON format)
            String qrContent = String.format(
                "{\"itemId\":%d,\"title\":\"%s\",\"token\":\"%s\"}",
                selectedItem.getId(),
                selectedItem.getTitle().replace("\"", "\\\""),
                currentQrToken
            );
            
            android.util.Log.d("QRGenerator", "Generating QR with content: " + qrContent);
            
            // Generate QR code bitmap
            currentQrBitmap = createQrBitmap(qrContent, QR_CODE_SIZE, QR_CODE_SIZE);
            
            // Display QR code
            ivQrCode.setImageBitmap(currentQrBitmap);
            cardQrCode.setVisibility(View.VISIBLE);
            btnShareQr.setVisibility(View.VISIBLE);
            
            Toast.makeText(requireContext(), "Đã tạo mã QR thành công!", 
                    Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            android.util.Log.e("QRGenerator", "Error generating QR code", e);
            Toast.makeText(requireContext(), "Lỗi tạo mã QR: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private Bitmap createQrBitmap(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        
        return bitmap;
    }
    
    private void shareQrCode() {
        if (currentQrBitmap == null) {
            Toast.makeText(requireContext(), "Chưa có mã QR để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Save QR to cache and share
            String fileName = "qr_" + selectedItem.getId() + "_" + System.currentTimeMillis() + ".png";
            java.io.File cachePath = new java.io.File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            
            java.io.File file = new java.io.File(cachePath, fileName);
            java.io.FileOutputStream stream = new java.io.FileOutputStream(file);
            currentQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            
            android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );
            
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, 
                    "Mã QR bàn giao: " + selectedItem.getTitle());
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ mã QR"));
            
        } catch (Exception e) {
            android.util.Log.e("QRGenerator", "Error sharing QR code", e);
            Toast.makeText(requireContext(), "Lỗi chia sẻ: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnGenerateQr.setEnabled(!show);
    }
    
    private String getStatusText(String status) {
        switch (status.toLowerCase()) {
            case "lost": return "Mất đồ";
            case "found": return "Tìm thấy";
            case "returned": return "Đã trả";
            default: return status;
        }
    }
    
    private int getStatusBackground(String status) {
        switch (status.toLowerCase()) {
            case "lost": return R.drawable.bg_status_lost;
            case "found": return R.drawable.bg_status_found;
            case "returned": return R.drawable.bg_status_returned;
            default: return R.drawable.bg_status_found;
        }
    }
}
