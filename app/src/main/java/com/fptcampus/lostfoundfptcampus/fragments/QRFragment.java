package com.fptcampus.lostfoundfptcampus.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.ServerTimeSync;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRFragment extends Fragment {
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private BarcodeView barcodeView;
    private TextView tvScanStatus;
    private MaterialButton btnToggleFlash;
    private MaterialButton btnScanAgain;

    private SharedPreferencesManager prefsManager;
    private boolean isFlashOn = false;
    private boolean isScanning = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindingView(view);
        bindingAction();

        prefsManager = new SharedPreferencesManager(requireContext());

        checkCameraPermission();
    }

    private void bindingView(View view) {
        barcodeView = view.findViewById(R.id.barcodeView);
        tvScanStatus = view.findViewById(R.id.tvScanStatus);
        btnToggleFlash = view.findViewById(R.id.btnToggleFlash);
        btnScanAgain = view.findViewById(R.id.btnScanAgain);
    }

    private void bindingAction() {
        btnToggleFlash.setOnClickListener(v -> toggleFlash());
        btnScanAgain.setOnClickListener(v -> resumeScanning());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Cần quyền camera để quét mã QR", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startScanning() {
        if (barcodeView != null) {
            isScanning = true;
            barcodeView.decodeContinuous(barcodeCallback);
            barcodeView.resume(); // Start camera preview
            updateScanStatus("Đang quét mã QR...", R.color.text_secondary);
        }
    }

    private void resumeScanning() {
        if (!isScanning && barcodeView != null) {
            isScanning = true;
            barcodeView.decodeContinuous(barcodeCallback);
            barcodeView.resume(); // Restart camera preview
            updateScanStatus("Đang quét mã QR...", R.color.text_secondary);
        }
    }

    private void pauseScanning() {
        isScanning = false;
        barcodeView.pause();
    }

    private final BarcodeCallback barcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result != null && result.getText() != null && isScanning) {
                pauseScanning();
                handleQRCode(result.getText());
            }
        }
    };

    private void handleQRCode(String qrCode) {
        if (!isAdded() || getActivity() == null) return;

        updateScanStatus("✅ Quét thành công!", R.color.success);

        try {
            // Parse QR content: {"itemId":123,"title":"Lost iPhone","token":"TOKEN_xxx"}
            org.json.JSONObject json = new org.json.JSONObject(qrCode);
            long itemId = json.getLong("itemId");
            String qrToken = json.getString("token");
            String itemTitle = json.optString("title", "Unknown Item");
            
            android.util.Log.d("QRFragment", "Processing QR: itemId=" + itemId + ", title=" + itemTitle);
            
            // Show confirmation dialog
            showHandoverConfirmation(itemId, qrToken, itemTitle);
            
        } catch (Exception e) {
            android.util.Log.e("QRFragment", "Error parsing QR content", e);
            updateScanStatus("❌ Mã QR không hợp lệ", R.color.error);
            Toast.makeText(requireContext(), "Mã QR không đúng định dạng", Toast.LENGTH_SHORT).show();
            resumeScanning();
        }
    }

    private String parseItemUuid(String qrCode) {
        // DEPRECATED - Use JSON parsing instead
        return null;
    }

    private void showHandoverConfirmation(long itemId, String qrToken, String itemTitle) {
        if (!isAdded() || getActivity() == null) return;

        // Get item details from API first
        String token = "Bearer " + prefsManager.getToken();
        ApiClient.getItemApi().getItemById(token, itemId).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                if (!isAdded() || getActivity() == null) return;
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Sync server time from API response
                    ServerTimeSync.updateServerTime(response.body().getTimestamp());
                    
                    LostItem item = response.body().getData();
                    
                    // Build message
                    StringBuilder message = new StringBuilder();
                    message.append("Bạn có chắc muốn xác nhận bàn giao đồ vật này?\n\n");
                    message.append("📦 Tên: ").append(item.getTitle()).append("\n");
                    message.append("📝 Mô tả: ").append(item.getDescription() != null ? item.getDescription() : "Không có").append("\n");
                    message.append("🏷️ Danh mục: ").append(item.getCategory()).append("\n");
                    message.append("📍 Trạng thái: ").append(item.getStatus());
                    
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("📦 Xác nhận bàn giao")
                            .setMessage(message.toString())
                            .setPositiveButton("Xác nhận", (dialog, which) -> {
                                confirmHandover(itemId, qrToken, item);
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> {
                                resumeScanning();
                            })
                            .setCancelable(false)
                            .show();
                    
                } else {
                    Toast.makeText(requireContext(), "Không tìm thấy thông tin vật phẩm", Toast.LENGTH_SHORT).show();
                    resumeScanning();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resumeScanning();
            }
        });
    }

    private void confirmHandover(long itemId, String qrToken, LostItem item) {
        if (!isAdded() || getActivity() == null) return;

        updateScanStatus("⏳ Đang xử lý...", R.color.text_secondary);

        String token = "Bearer " + prefsManager.getToken();
        long itemCreatorId = item.getUserId();
        long scannerId = prefsManager.getUserId();
        
        // KIỂM TRA: Không cho phép người tạo item quét QR của chính mình
        if (itemCreatorId == scannerId) {
            android.util.Log.w("QRFragment", "❌ Cannot scan own item: userId=" + scannerId);
            updateScanStatus("❌ Không thể quét mã QR của chính bạn!", R.color.error);
            Toast.makeText(requireContext(), 
                "Bạn không thể xác nhận bàn giao đồ vật của chính mình", 
                Toast.LENGTH_LONG).show();
            resumeScanning();
            return;
        }
        
        android.util.Log.d("QRFragment", "✅ Validation passed - confirming handover with token: " + qrToken);
        
        // Create ConfirmHandoverRequest with qrToken
        com.fptcampus.lostfoundfptcampus.model.dto.ConfirmHandoverRequest request = 
            new com.fptcampus.lostfoundfptcampus.model.dto.ConfirmHandoverRequest(qrToken);
        
        // Call confirmHandover API (POST) - Backend sẽ tự động:
        // 1. Set 3 user role fields (lostUserId, foundUserId, returnedUserId)
        // 2. Update status = "returned"
        // 3. Award +10 karma cho cả 2 người
        // 4. Create history record
        ApiClient.getItemApi().confirmHandover(token, itemId, request).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                if (!isAdded() || getActivity() == null) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Sync server time from API response
                    ServerTimeSync.updateServerTime(response.body().getTimestamp());
                    
                    // Success - handover confirmed
                    LostItem updatedItem = response.body().getData();
                    android.util.Log.d("QRFragment", "✅ Handover confirmed successfully!");
                    android.util.Log.d("QRFragment", "Item ID: " + updatedItem.getId());
                    android.util.Log.d("QRFragment", "Item status: " + updatedItem.getStatus());
                    android.util.Log.d("QRFragment", "userId (creator): " + updatedItem.getUserId());
                    android.util.Log.d("QRFragment", "lostUserId: " + updatedItem.getLostUserId());
                    android.util.Log.d("QRFragment", "foundUserId: " + updatedItem.getFoundUserId());
                    android.util.Log.d("QRFragment", "returnedUserId: " + updatedItem.getReturnedUserId());
                    
                    // CHECK: Nếu backend không set đúng các field, app sẽ tự fix
                    fixItemFieldsIfNeeded(updatedItem, scannerId, itemCreatorId, item);
                    
                    updateScanStatus("✅ Bàn giao thành công!", R.color.success);
                    
                    // Update karma for both users
                    updateKarmaForBothUsers(updatedItem);
                    
                } else {
                    // Error
                    android.util.Log.e("QRFragment", "Failed to confirm handover");
                    String errorMessage = "Không thể xác nhận bàn giao";
                    
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("QRFragment", "Error body: " + errorBody);
                            
                            // Parse error message from response
                            if (errorBody.contains("expired")) {
                                errorMessage = "Mã QR đã hết hạn (quá 24 giờ)";
                            } else if (errorBody.contains("already used")) {
                                errorMessage = "Mã QR đã được sử dụng rồi";
                            } else if (errorBody.contains("already returned")) {
                                errorMessage = "Đồ vật đã được trả rồi";
                            } else if (errorBody.contains("permission")) {
                                errorMessage = "Bạn không có quyền quét mã QR này";
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    updateScanStatus("❌ Bàn giao thất bại", R.color.error);
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    resumeScanning();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;

                android.util.Log.e("QRFragment", "Network error: " + t.getMessage(), t);
                updateScanStatus("❌ Lỗi kết nối", R.color.error);
                Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resumeScanning();
            }
        });
    }

    private void showSuccessDialog(LostItem item) {
        if (!isAdded() || getActivity() == null) return;

        String message = "Đồ vật đã được bàn giao thành công!";
        if (item != null) {
            message += "\n\nTên: " + item.getTitle();
            message += "\nTrạng thái: " + item.getStatus();
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("✅ Thành công")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    resumeScanning();
                })
                .show();
    }

    private void toggleFlash() {
        try {
            if (isFlashOn) {
                barcodeView.setTorch(false);
                btnToggleFlash.setText("💡 Đèn Flash");
                isFlashOn = false;
            } else {
                barcodeView.setTorch(true);
                btnToggleFlash.setText("🔦 Tắt Flash");
                isFlashOn = true;
            }
        } catch (Exception e) {
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không thể bật/tắt đèn flash", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateScanStatus(String message, int colorRes) {
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                tvScanStatus.setText(message);
                tvScanStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
            });
        }
    }

    /**
     * Refresh current user's profile to get updated karma from backend
     * Backend automatically awards +10 karma to both users in confirmHandover API
     */
    private void refreshUserProfile() {
        if (!isAdded() || getActivity() == null) return;

        String token = "Bearer " + prefsManager.getToken();
        long currentUserId = prefsManager.getUserId();
        
        android.util.Log.d("QRFragment", "Refreshing user profile to get updated karma");
        
        ApiClient.getUserApi().getProfile(token).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Sync server time from API response
                    ServerTimeSync.updateServerTime(response.body().getTimestamp());
                    
                    com.fptcampus.lostfoundfptcampus.model.User user = response.body().getData();
                    
                    // Update SharedPreferences with new karma
                    prefsManager.saveUserKarma(user.getKarma());
                    
                    android.util.Log.d("QRFragment", "✅ User profile refreshed - New karma: " + user.getKarma());
                } else {
                    android.util.Log.e("QRFragment", "Failed to refresh user profile");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {
                android.util.Log.e("QRFragment", "Error refreshing profile: " + t.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null) {
            barcodeView.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    /**
     * Update karma for BOTH foundUser (giver) and returnedUser (receiver) (+10 each)
     * Backend doesn't auto-update karma, so we need to do it manually
     * 
     * This method:
     * 1. Gets foundUser (giver) from API and updates karma +10
     * 2. Gets returnedUser (receiver) from API and updates karma +10
     * 3. Refreshes current user's profile if they are one of them
     */
    /**
     * Fix item fields if backend didn't set them correctly
     * LOGIC CHUẨN:
     * - Item "lost" (người mất tạo QR) → scanner nhặt được → scanner trả lại creator
     *   → lostUserId=creator, foundUserId=scanner, returnedUserId=creator
     * - Item "found" (người nhặt tạo QR) → scanner là chủ → creator trả lại scanner  
     *   → foundUserId=creator, lostUserId=(scanner nếu chưa có), returnedUserId=scanner
     */
    private void fixItemFieldsIfNeeded(LostItem updatedItem, long scannerId, long creatorId, LostItem originalItem) {
        boolean needsFix = false;
        String originalStatus = originalItem.getStatus() != null ? originalItem.getStatus().toLowerCase() : "";
        
        android.util.Log.d("QRFragment", "=== FIX ITEM FIELDS ===");
        android.util.Log.d("QRFragment", "Original status: " + originalStatus);
        android.util.Log.d("QRFragment", "Creator ID: " + creatorId + ", Scanner ID: " + scannerId);
        
        // CASE 1: Item ban đầu là LOST (người mất đồ tạo QR)
        if ("lost".equals(originalStatus)) {
            android.util.Log.d("QRFragment", "Case: LOST item - creator mất đồ, scanner tìm thấy và trả lại");
            
            // lostUserId = creator (người tạo = người mất)
            if (updatedItem.getLostUserId() == null || updatedItem.getLostUserId() != creatorId) {
                updatedItem.setLostUserId(creatorId);
                needsFix = true;
                android.util.Log.d("QRFragment", "Fix: Set lostUserId = " + creatorId);
            }
            
            // foundUserId = scanner (người quét = người tìm thấy)
            if (updatedItem.getFoundUserId() == null || updatedItem.getFoundUserId() != scannerId) {
                updatedItem.setFoundUserId(scannerId);
                needsFix = true;
                android.util.Log.d("QRFragment", "Fix: Set foundUserId = " + scannerId);
            }
            
            // returnedUserId = creator (người mất nhận lại)
            if (updatedItem.getReturnedUserId() == null || updatedItem.getReturnedUserId() != creatorId) {
                updatedItem.setReturnedUserId(creatorId);
                needsFix = true;
                android.util.Log.d("QRFragment", "Fix: Set returnedUserId = " + creatorId);
            }
        }
        // CASE 2: Item ban đầu là FOUND (người nhặt được tạo QR)
        else if ("found".equals(originalStatus)) {
            android.util.Log.d("QRFragment", "Case: FOUND item - creator nhặt được, scanner là chủ nhận lại");
            
            // foundUserId = creator (người tạo = người nhặt)
            if (updatedItem.getFoundUserId() == null || updatedItem.getFoundUserId() != creatorId) {
                updatedItem.setFoundUserId(creatorId);
                needsFix = true;
                android.util.Log.d("QRFragment", "Fix: Set foundUserId = " + creatorId);
            }
            
            // lostUserId = scanner (người quét = người mất ban đầu - nếu chưa có)
            if (updatedItem.getLostUserId() == null) {
                updatedItem.setLostUserId(scannerId);
                needsFix = true;
                android.util.Log.d("QRFragment", "Fix: Set lostUserId = " + scannerId);
            }
            
            // returnedUserId = scanner (người quét = người nhận lại)
            if (updatedItem.getReturnedUserId() == null || updatedItem.getReturnedUserId() != scannerId) {
                updatedItem.setReturnedUserId(scannerId);
                needsFix = true;
                android.util.Log.d("QRFragment", "Fix: Set returnedUserId = " + scannerId);
            }
        }
        
        if (needsFix) {
            android.util.Log.w("QRFragment", "⚠️ Backend didn't set fields correctly - fixing via PUT API");
            updateItemFields(updatedItem);
        } else {
            android.util.Log.d("QRFragment", "✅ All fields are correct, no fix needed");
        }
    }
    
    /**
     * Update item fields via PUT API
     */
    private void updateItemFields(LostItem item) {
        String token = "Bearer " + prefsManager.getToken();
        
        // Create UpdateItemRequest with all fields from item
        com.fptcampus.lostfoundfptcampus.model.dto.UpdateItemRequest request = 
            new com.fptcampus.lostfoundfptcampus.model.dto.UpdateItemRequest.Builder()
                .setTitle(item.getTitle())
                .setDescription(item.getDescription())
                .setCategory(item.getCategory())
                .setStatus(item.getStatus())
                .setLatitude(item.getLatitude())
                .setLongitude(item.getLongitude())
                .setImageUrl(item.getImageUrl())
                .setLostUserId(item.getLostUserId())
                .setFoundUserId(item.getFoundUserId())
                .setReturnedUserId(item.getReturnedUserId())
                .build();
        
        android.util.Log.d("QRFragment", "Updating item " + item.getId() + " with: lostUserId=" + 
                          item.getLostUserId() + ", foundUserId=" + item.getFoundUserId() + 
                          ", returnedUserId=" + item.getReturnedUserId());
        
        ApiClient.getItemApi().updateItem(token, item.getId(), request).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    android.util.Log.d("QRFragment", "✅ Item fields updated successfully");
                    LostItem fixed = response.body().getData();
                    android.util.Log.d("QRFragment", "Fixed - lostUserId: " + fixed.getLostUserId() + 
                                                     ", foundUserId: " + fixed.getFoundUserId() + 
                                                     ", returnedUserId: " + fixed.getReturnedUserId());
                } else {
                    android.util.Log.e("QRFragment", "❌ Failed to update item fields");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                android.util.Log.e("QRFragment", "❌ Error updating item fields: " + t.getMessage());
            }
        });
    }
    
    private void updateKarmaForBothUsers(final LostItem item) {
        if (!isAdded() || getActivity() == null) return;

        String token = "Bearer " + prefsManager.getToken();
        long currentUserId = prefsManager.getUserId();
        
        // Lấy cả 3 trường để xác định ai là ai
        Long lostUserId = item.getLostUserId();         // Người mất đồ
        Long foundUserId = item.getFoundUserId();       // Người tìm thấy/trả đồ
        Long returnedUserId = item.getReturnedUserId(); // Người nhận lại đồ
        
        android.util.Log.d("QRFragment", "=== KARMA UPDATE (USE ALL 3 FIELDS) ===");
        android.util.Log.d("QRFragment", "Item ID: " + item.getId());
        android.util.Log.d("QRFragment", "lostUserId: " + lostUserId);
        android.util.Log.d("QRFragment", "foundUserId: " + foundUserId);
        android.util.Log.d("QRFragment", "returnedUserId: " + returnedUserId);
        android.util.Log.d("QRFragment", "currentUserId: " + currentUserId);
        
        // Check nếu thiếu field nào
        if (lostUserId == null || foundUserId == null || returnedUserId == null) {
            android.util.Log.e("QRFragment", "❌ ERROR: Missing user IDs after fix!");
            android.util.Log.e("QRFragment", "lostUserId=" + lostUserId + ", foundUserId=" + foundUserId + ", returnedUserId=" + returnedUserId);
            showSuccessDialog(item);
            return;
        }
        
        // XÁC ĐỊNH 2 NGƯỜI CẦN UPDATE KARMA:
        // - Người 1: foundUserId (người tìm thấy/trả đồ) → +10 karma
        // - Người 2: returnedUserId (người nhận lại đồ) → +10 karma
        // NOTE: foundUserId và returnedUserId có thể trùng nhau trong một số case
        //       nhưng logic đúng là phải update cả 2 (nếu trùng thì update 1 lần)
        
        java.util.Set<Long> userIdsToUpdate = new java.util.HashSet<>();
        userIdsToUpdate.add(foundUserId);
        userIdsToUpdate.add(returnedUserId);
        
        android.util.Log.d("QRFragment", "✅ Will update karma for " + userIdsToUpdate.size() + " user(s): " + userIdsToUpdate);
        
        // Counter to track API calls
        final int totalUsers = userIdsToUpdate.size();
        final int[] completedCalls = {0};
        final int[] updatedKarma = {0};
        
        // Update karma cho từng user trong set
        for (Long userId : userIdsToUpdate) {
            final long finalUserId = userId;
            
            ApiClient.getUserApi().getUserById(token, userId).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
                @Override
                public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                       Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        com.fptcampus.lostfoundfptcampus.model.User user = response.body().getData();
                        int oldKarma = user.getKarma();
                        int newKarma = oldKarma + 10;
                        
                        // Update via PUT API
                        user.setKarma(newKarma);
                        updateUserKarma(user, currentUserId, updatedKarma);
                        
                        String role = "";
                        if (finalUserId == foundUserId) role += "Found";
                        if (finalUserId == returnedUserId) role += (role.isEmpty() ? "" : "+") + "Returned";
                        if (finalUserId == lostUserId) role += (role.isEmpty() ? "" : "+") + "Lost";
                        
                        android.util.Log.d("QRFragment", "✅ User " + finalUserId + " (" + role + ") karma: " + oldKarma + " → " + newKarma);
                    }
                    
                    completedCalls[0]++;
                    checkBothUpdatesComplete(completedCalls[0], totalUsers, item, updatedKarma[0]);
                }

                @Override
                public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {
                    android.util.Log.e("QRFragment", "Error getting user " + finalUserId + ": " + t.getMessage());
                    completedCalls[0]++;
                    checkBothUpdatesComplete(completedCalls[0], totalUsers, item, updatedKarma[0]);
                }
            });
        }
    }
    
    /**
     * Update user karma via PUT API
     */
    private void updateUserKarma(com.fptcampus.lostfoundfptcampus.model.User user, long currentUserId, int[] updatedKarma) {
        String token = "Bearer " + prefsManager.getToken();
        
        ApiClient.getUserApi().updateProfile(token, user).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User updated = response.body().getData();
                    
                    // If this is current user, update SharedPreferences
                    if (updated.getId() == currentUserId) {
                        prefsManager.saveUserKarma(updated.getKarma());
                        updatedKarma[0] = updated.getKarma();
                        android.util.Log.d("QRFragment", "✅ Current user karma saved: " + updated.getKarma());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {
                android.util.Log.e("QRFragment", "Error updating user karma: " + t.getMessage());
            }
        });
    }
    
    /**
     * Check if all karma updates are complete, then show dialog
     */
    private void checkBothUpdatesComplete(int completedCalls, int totalUsers, LostItem item, int currentUserKarma) {
        if (completedCalls >= totalUsers) {
            // All API calls completed
            android.util.Log.d("QRFragment", "✅ All " + totalUsers + " karma updates completed");
            if (currentUserKarma > 0) {
                showSuccessDialogWithKarma(item, currentUserKarma);
            } else {
                showSuccessDialog(item);
            }
        }
    }

    /**
     * Show success dialog with karma information
     */
    private void showSuccessDialogWithKarma(LostItem item, int newKarma) {
        if (!isAdded() || getActivity() == null) return;

        String message = "🎉 Đồ vật đã được bàn giao thành công!\n\n";
        if (item != null) {
            message += "📦 Tên: " + item.getTitle() + "\n";
            message += "📊 Trạng thái: " + item.getStatus() + "\n\n";
        }
        message += "⭐ Karma của bạn: " + newKarma + " điểm\n";
        message += "(+10 điểm từ bàn giao thành công)";

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("✅ Thành công")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    resumeScanning();
                })
                .show();
    }
}
