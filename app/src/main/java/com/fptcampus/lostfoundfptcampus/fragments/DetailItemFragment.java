package com.fptcampus.lostfoundfptcampus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.QrScanActivity;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Detail Item Fragment - Display item details
 */
public class DetailItemFragment extends Fragment {
    private MaterialToolbar toolbar;
    private ImageView ivItemImage;
    private TextView tvStatus, tvTitle, tvCategory, tvDescription;
    private TextView tvLocation, tvUserName, tvCreatedAt;
    private MaterialButton btnViewOnMap, btnGenerateQr, btnContact;

    private LostItem currentItem;
    private SharedPreferencesManager prefsManager;
    private com.fptcampus.lostfoundfptcampus.navigation.NavigationHost navigationHost;

    public static DetailItemFragment newInstance(LostItem item) {
        DetailItemFragment fragment = new DetailItemFragment();
        Bundle args = new Bundle();
        args.putLong("itemId", item.getId());
        args.putString("title", item.getTitle());
        args.putString("description", item.getDescription());
        args.putString("category", item.getCategory());
        args.putString("status", item.getStatus());
        args.putDouble("latitude", item.getLatitude() != null ? item.getLatitude() : 0.0);
        args.putDouble("longitude", item.getLongitude() != null ? item.getLongitude() : 0.0);
        args.putLong("userId", item.getUserId());
        args.putLong("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().getTime() : 0);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefsManager = new SharedPreferencesManager(requireContext());
        
        // Initialize navigationHost
        if (getActivity() instanceof com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) {
            navigationHost = (com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) getActivity();
        }
        
        bindingView(view);
        bindingAction();
        loadItemData();
    }

    private void bindingView(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        ivItemImage = view.findViewById(R.id.ivItemImage);
        tvStatus = view.findViewById(R.id.tvStatus);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvCategory = view.findViewById(R.id.tvCategory);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt);
        btnViewOnMap = view.findViewById(R.id.btnViewOnMap);
        btnGenerateQr = view.findViewById(R.id.btnGenerateQr);
        btnContact = view.findViewById(R.id.btnContact);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnViewOnMap.setOnClickListener(this::onBtnViewOnMapClick);
        btnGenerateQr.setOnClickListener(this::onBtnGenerateQrClick);
        btnContact.setOnClickListener(this::onBtnContactClick);
    }

    private void loadItemData() {
        Bundle args = getArguments();
        if (args == null) {
            ErrorDialogHelper.showError(requireContext(), "Lỗi", "Không tìm thấy thông tin đồ thất lạc", 
                () -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
            return;
        }

        long itemId = args.getLong("itemId", -1);
        String title = args.getString("title");
        String description = args.getString("description");
        String category = args.getString("category");
        String status = args.getString("status");
        double latitude = args.getDouble("latitude", 0);
        double longitude = args.getDouble("longitude", 0);
        long createdAt = args.getLong("createdAt", 0);
        long itemUserId = args.getLong("userId", -1);

        if (itemId == -1) {
            ErrorDialogHelper.showError(requireContext(), "Lỗi", "Không tìm thấy thông tin đồ thất lạc", 
                () -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
            return;
        }

        // Create temporary item object
        currentItem = new LostItem();
        currentItem.setId(itemId);
        currentItem.setTitle(title);
        currentItem.setDescription(description);
        currentItem.setCategory(category);
        currentItem.setStatus(status);
        currentItem.setLatitude(latitude);
        currentItem.setLongitude(longitude);
        currentItem.setUserId(itemUserId);

        // Display data
        tvTitle.setText(title);
        tvDescription.setText(description != null && !description.isEmpty() ? description : "Không có mô tả");
        tvCategory.setText(getCategoryName(category));
        tvLocation.setText(String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", latitude, longitude));

        // Status badge
        switch (status != null ? status : "lost") {
            case "found":
                tvStatus.setText("ĐÃ TÌM THẤY");
                tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "returned":
                tvStatus.setText("ĐÃ TRẢ");
                tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;
            default:
                tvStatus.setText("ĐÃ MẤT");
                tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
        }

        // Kiểm tra quyền sở hữu - chỉ hiện nút Generate QR nếu:
        // 1. User là người tạo item (lost hoặc found)
        // 2. Item chưa được trả (status != "returned")
        long currentUserId = prefsManager.getUserId();
        boolean isOwner = (itemUserId != -1 && currentUserId != -1 && itemUserId == currentUserId);
        boolean isReturned = "returned".equals(status);
        
        if (!isOwner || isReturned) {
            btnGenerateQr.setVisibility(View.GONE);
        } else {
            btnGenerateQr.setVisibility(View.VISIBLE);
        }

        // Format date
        if (createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvCreatedAt.setText(sdf.format(createdAt));
        }

        // TODO: Load user name from API
        tvUserName.setText("Sinh viên FPT");
    }

    private String getCategoryName(String category) {
        if (category == null) return "Khác";
        
        switch (category) {
            case "electronics": return "Điện tử";
            case "wallet": return "Ví";
            case "card": return "Thẻ";
            case "keys": return "Chìa khóa";
            case "documents": return "Giấy tờ";
            case "clothes": return "Quần áo";
            case "bag": return "Túi xách";
            case "phone": return "Điện thoại";
            default: return "Khác";
        }
    }

    private void onBtnViewOnMapClick(View view) {
        if (currentItem != null && currentItem.getLatitude() != null && currentItem.getLongitude() != null) {
            // Navigate to MapFragment with item highlight
            if (getActivity() instanceof com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) {
                MapFragment mapFragment = MapFragment.newInstanceWithItem(currentItem.getId());
                ((com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) getActivity()).navigateTo(mapFragment, true);
            }
        } else {
            ErrorDialogHelper.showError(requireContext(), "Lỗi", "Đồ thất lạc này không có thông tin vị trí");
        }
    }

    private void onBtnGenerateQrClick(View view) {
        if (currentItem != null && navigationHost != null) {
            // Navigate to QRGeneratorFragment with preselected item
            QRGeneratorFragment qrFragment = QRGeneratorFragment.newInstance(currentItem.getId());
            navigationHost.navigateTo(qrFragment, true);
        }
    }

    private void onBtnContactClick(View view) {
        ErrorDialogHelper.showError(requireContext(), "Chức năng đang phát triển", 
                "Chức năng liên hệ sẽ được cập nhật trong phiên bản tiếp theo");
    }
}
