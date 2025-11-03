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
                    android.util.Log.d("QRFragment", "Item status: " + updatedItem.getStatus());
                    android.util.Log.d("QRFragment", "lostUserId: " + updatedItem.getLostUserId());
                    android.util.Log.d("QRFragment", "foundUserId: " + updatedItem.getFoundUserId());
                    android.util.Log.d("QRFragment", "returnedUserId: " + updatedItem.getReturnedUserId());
                    
                    updateScanStatus("✅ Bàn giao thành công!", R.color.success);
                    
                    // Backend doesn't auto-update karma, so we need to do it manually
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
    private void updateKarmaForBothUsers(final LostItem item) {
        if (!isAdded() || getActivity() == null) return;

        String token = "Bearer " + prefsManager.getToken();
        long currentUserId = prefsManager.getUserId();
        
        Long foundUserId = item.getFoundUserId();      // Giver (người tìm thấy)
        Long returnedUserId = item.getReturnedUserId(); // Receiver (người nhận lại)
        
        if (foundUserId == null || returnedUserId == null) {
            android.util.Log.e("QRFragment", "foundUserId or returnedUserId is null");
            showSuccessDialog(item);
            return;
        }
        
        android.util.Log.d("QRFragment", "Updating karma for foundUserId=" + foundUserId + " (giver) and returnedUserId=" + returnedUserId + " (receiver)");
        
        // Counter to track both API calls
        final int[] completedCalls = {0};
        final int[] updatedKarma = {0};
        
        // Update karma for foundUser (giver)
        ApiClient.getUserApi().getUserById(token, foundUserId).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User giverUser = response.body().getData();
                    int newKarma = giverUser.getKarma() + 10;
                    
                    // Update via PUT API
                    giverUser.setKarma(newKarma);
                    updateUserKarma(giverUser, currentUserId, updatedKarma);
                    
                    android.util.Log.d("QRFragment", "✅ Giver (foundUser) karma updated: " + newKarma);
                }
                
                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {
                android.util.Log.e("QRFragment", "Error getting giver user: " + t.getMessage());
                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }
        });
        
        // Update karma for returnedUser (receiver)
        ApiClient.getUserApi().getUserById(token, returnedUserId).enqueue(new Callback<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, 
                                   Response<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User receiverUser = response.body().getData();
                    int newKarma = receiverUser.getKarma() + 10;
                    
                    // Update via PUT API
                    receiverUser.setKarma(newKarma);
                    updateUserKarma(receiverUser, currentUserId, updatedKarma);
                    
                    android.util.Log.d("QRFragment", "✅ Receiver (returnedUser) karma updated: " + newKarma);
                }
                
                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }

            @Override
            public void onFailure(Call<ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call, Throwable t) {
                android.util.Log.e("QRFragment", "Error getting receiver user: " + t.getMessage());
                completedCalls[0]++;
                checkBothUpdatesComplete(completedCalls[0], item, updatedKarma[0]);
            }
        });
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
     * Check if both karma updates are complete, then show dialog
     */
    private void checkBothUpdatesComplete(int completedCalls, LostItem item, int currentUserKarma) {
        if (completedCalls >= 2) {
            // Both API calls completed
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
