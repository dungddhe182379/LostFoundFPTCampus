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
        long giverId = item.getUserId();
        long receiverId = prefsManager.getUserId();
        
        android.util.Log.d("QRFragment", "Confirming handover: itemId=" + itemId + ", giverId=" + giverId + ", receiverId=" + receiverId);
        
        // Create request body - ConfirmHandoverRequest only has qrToken field
        com.fptcampus.lostfoundfptcampus.model.dto.ConfirmHandoverRequest request = 
            new com.fptcampus.lostfoundfptcampus.model.dto.ConfirmHandoverRequest(qrToken);
        
        // Call confirmHandover API
        ApiClient.getItemApi().confirmHandover(token, itemId, request).enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                if (!isAdded() || getActivity() == null) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Success
                    LostItem updatedItem = response.body().getData();
                    android.util.Log.d("QRFragment", "✅ Handover confirmed successfully");
                    android.util.Log.d("QRFragment", "Item status: " + updatedItem.getStatus());
                    
                    updateScanStatus("✅ Bàn giao thành công!", R.color.success);
                    showSuccessDialog(updatedItem);
                    
                } else {
                    // Error
                    android.util.Log.e("QRFragment", "Failed to confirm handover");
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            android.util.Log.e("QRFragment", "Error body: " + errorBody);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    updateScanStatus("❌ Bàn giao thất bại", R.color.error);
                    Toast.makeText(requireContext(), "Không thể xác nhận bàn giao", Toast.LENGTH_SHORT).show();
                    resumeScanning();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;

                android.util.Log.e("QRFragment", "Network error: " + t.getMessage(), t);
                updateScanStatus("❌ Lỗi kết nối", R.color.error);
                Toast.makeText(requireContext(), "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
}
