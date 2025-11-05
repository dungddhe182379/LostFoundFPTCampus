package com.fptcampus.lostfoundfptcampus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private MaterialButton btnViewOnMap, btnGenerateQr, btnContact, btnUpdate;

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
    
    // Simpler factory method when only itemId is known (will load from API)
    public static DetailItemFragment newInstanceById(long itemId) {
        DetailItemFragment fragment = new DetailItemFragment();
        Bundle args = new Bundle();
        args.putLong("itemId", itemId);
        args.putBoolean("loadFromApi", true);
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
        btnUpdate = view.findViewById(R.id.btnUpdate);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        btnViewOnMap.setOnClickListener(this::onBtnViewOnMapClick);
        btnGenerateQr.setOnClickListener(this::onBtnGenerateQrClick);
        btnContact.setOnClickListener(this::onBtnContactClick);
        btnUpdate.setOnClickListener(this::onBtnUpdateClick);
    }

    private void loadItemData() {
        Bundle args = getArguments();
        if (args == null) {
            ErrorDialogHelper.showError(requireContext(), "Lỗi", "Không tìm thấy thông tin đồ thất lạc", 
                () -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
            return;
        }

        long itemId = args.getLong("itemId", -1);
        
        // Check if we need to load from API
        if (args.getBoolean("loadFromApi", false)) {
            loadItemFromApi(itemId);
            return;
        }
        
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

        displayItemData();
    }
    
    private void loadItemFromApi(long itemId) {
        // TODO: Implement API call to get item details
        // For now, show a message
        Toast.makeText(requireContext(), "Đang tải thông tin vật phẩm #" + itemId + "...", Toast.LENGTH_SHORT).show();
        
        // Mock data for now - replace with actual API call
        currentItem = new LostItem();
        currentItem.setId(itemId);
        currentItem.setTitle("Đang tải...");
        currentItem.setDescription("Vui lòng đợi...");
        currentItem.setCategory("other");
        currentItem.setStatus("lost");
        currentItem.setLatitude(0.0);
        currentItem.setLongitude(0.0);
        currentItem.setUserId(0);
        
        displayItemData();
    }
    
    private void displayItemData() {
        if (currentItem == null) return;
        
        String title = currentItem.getTitle();
        String description = currentItem.getDescription();
        String category = currentItem.getCategory();
        String status = currentItem.getStatus();
        double latitude = currentItem.getLatitude() != null ? currentItem.getLatitude() : 0;
        double longitude = currentItem.getLongitude() != null ? currentItem.getLongitude() : 0;
        long itemUserId = currentItem.getUserId();
        
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

        // Kiểm tra quyền sở hữu
        long currentUserId = prefsManager.getUserId();
        boolean isOwner = (itemUserId != -1 && currentUserId != -1 && itemUserId == currentUserId);
        boolean isReturned = "returned".equals(status);
        
        // Chỉ hiện nút Generate QR nếu là owner và chưa trả
        if (!isOwner || isReturned) {
            btnGenerateQr.setVisibility(View.GONE);
        } else {
            btnGenerateQr.setVisibility(View.VISIBLE);
        }
        
        // Chỉ hiện nút Cập nhật nếu là owner và chưa trả
        if (!isOwner || isReturned) {
            btnUpdate.setVisibility(View.GONE);
        } else {
            btnUpdate.setVisibility(View.VISIBLE);
        }
        
        // Ẩn nút "Liên hệ" nếu user là chủ nhân hoặc item đã trả
        if (isOwner || isReturned) {
            btnContact.setVisibility(View.GONE);
        } else {
            btnContact.setVisibility(View.VISIBLE);
        }
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
        if (currentItem == null) {
            ErrorDialogHelper.showError(requireContext(), "Lỗi", "Không tìm thấy thông tin đồ vật");
            return;
        }
        
        long currentUserId = prefsManager.getUserId();
        long itemUserId = currentItem.getUserId();
        
        // Không thể liên hệ với chính mình
        if (currentUserId == itemUserId) {
            ErrorDialogHelper.showError(requireContext(), "Thông báo", "Bạn không thể liên hệ với chính mình");
            return;
        }
        
        // Check if chat already exists, if yes -> open directly, if no -> ask for anonymous option
        checkAndOpenChat();
    }
    
    private void checkAndOpenChat() {
        if (currentItem == null) return;
        
        long currentUserId = prefsManager.getUserId();
        long itemId = currentItem.getId();
        String status = currentItem.getStatus() != null ? currentItem.getStatus() : "lost";
        
        // Xác định lostUserId và foundUserId
        long lostUserId, foundUserId;
        
        if ("lost".equals(status)) {
            lostUserId = currentItem.getUserId();
            foundUserId = currentUserId;
        } else {
            lostUserId = currentUserId;
            foundUserId = currentItem.getUserId();
        }
        
        // Generate chatId to check if it exists
        com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager chatManager = 
            com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager.getInstance();
        
        String chatId = chatManager.generateChatId(itemId, lostUserId, foundUserId);
        
        // Check if chat exists in Firebase
        chatManager.checkChatExists(chatId, new com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager.ChatExistsCallback() {
            @Override
            public void onResult(boolean exists) {
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (exists) {
                            // Chat already exists -> Open directly
                            openChatActivity(chatId, itemId, currentItem.getUserId(), false);
                        } else {
                            // Chat doesn't exist -> Ask for anonymous option
                            showAnonymousDialog();
                        }
                    });
                }
            }
        });
    }
    
    private void showAnonymousDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Liên hệ")
            .setMessage("Bạn muốn liên hệ dưới hình thức nào?")
            .setPositiveButton("Hiển thị tên", (dialog, which) -> createChatAndOpen(false))
            .setNegativeButton("Ẩn danh", (dialog, which) -> createChatAndOpen(true))
            .setNeutralButton("Hủy", null)
            .show();
    }
    
    private void createChatAndOpen(boolean isAnonymous) {
        if (currentItem == null) return;
        
        long currentUserId = prefsManager.getUserId();
        long itemId = currentItem.getId();
        String status = currentItem.getStatus() != null ? currentItem.getStatus() : "lost";
        
        // Xác định lostUserId và foundUserId
        long lostUserId, foundUserId;
        String lostUserName = prefsManager.getUserName();
        String foundUserName = "Người dùng"; // TODO: Get from API or item data
        
        if ("lost".equals(status)) {
            // Item status = lost -> itemUserId là người mất, currentUser là người nhặt
            lostUserId = currentItem.getUserId();
            foundUserId = currentUserId;
        } else {
            // Item status = found -> itemUserId là người nhặt, currentUser là người mất
            lostUserId = currentUserId;
            foundUserId = currentItem.getUserId();
        }
        
        // Create or get chat
        com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager chatManager = 
            com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager.getInstance();
        
        chatManager.createChat(itemId, lostUserId, foundUserId, lostUserName, foundUserName, isAnonymous,
            new com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager.ChatCallback() {
                @Override
                public void onSuccess(String chatId) {
                    // Open chat activity
                    openChatActivity(chatId, itemId, currentItem.getUserId(), isAnonymous);
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Lỗi tạo chat: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    
    private void openChatActivity(String chatId, long itemId, long otherUserId, boolean isAnonymous) {
        Intent intent = new Intent(requireContext(), com.fptcampus.lostfoundfptcampus.controller.ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("itemId", itemId);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserName", isAnonymous ? "Người dùng ẩn danh" : "Người dùng");
        intent.putExtra("isAnonymous", isAnonymous);
        startActivity(intent);
    }
    
    private void onBtnUpdateClick(View view) {
        if (currentItem == null) {
            ErrorDialogHelper.showError(requireContext(), "Lỗi", "Không tìm thấy thông tin đồ vật");
            return;
        }
        
        // Navigate to ReportItemFragment in update mode
        if (navigationHost != null) {
            ReportItemFragment updateFragment = ReportItemFragment.newInstanceForUpdate(currentItem);
            navigationHost.navigateTo(updateFragment, true);
        }
    }
}
