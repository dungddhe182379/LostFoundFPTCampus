package com.fptcampus.lostfoundfptcampus.fragments;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.dto.CreateItemRequest;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportItemFragment extends Fragment {
    
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    private ChipGroup chipGroupReportType;
    private Chip chipLost, chipFound;
    private AutoCompleteTextView spinnerCategory;
    private TextInputEditText etTitle, etDescription, etLocation, etDate, etContactInfo;
    private ImageView ivItemImage;
    private LinearLayout ivAddImage;
    private MaterialButton btnSubmit, btnClear;
    
    private SharedPreferencesManager prefsManager;
    private String selectedImageBase64 = null;
    private String reportType = "lost"; // "lost" or "found"
    private Calendar selectedDate;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report_item, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefsManager = new SharedPreferencesManager(requireContext());
        selectedDate = Calendar.getInstance();
        
        bindingView(view);
        setupCategorySpinner();
        bindingAction();
    }
    
    private void bindingView(View view) {
        chipGroupReportType = view.findViewById(R.id.chipGroupReportType);
        chipLost = view.findViewById(R.id.chipLost);
        chipFound = view.findViewById(R.id.chipFound);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etLocation = view.findViewById(R.id.etLocation);
        etDate = view.findViewById(R.id.etDate);
        etContactInfo = view.findViewById(R.id.etContactInfo);
        ivItemImage = view.findViewById(R.id.ivItemImage);
        ivAddImage = view.findViewById(R.id.ivAddImage);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnClear = view.findViewById(R.id.btnClear);
        
        // Set default date
        updateDateField();
    }
    
    private void setupCategorySpinner() {
        String[] categories = {
            "Điện thoại", "Laptop", "Ví", "Chìa khóa", "Thẻ sinh viên",
            "Sách/Tài liệu", "Tai nghe", "Túi xách", "Đồng hồ", "Trang sức", "Khác"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        );
        spinnerCategory.setAdapter(adapter);
    }
    
    private void bindingAction() {
        // Report type selection
        chipGroupReportType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int selectedId = checkedIds.get(0);
                if (selectedId == R.id.chipLost) {
                    reportType = "lost";
                } else if (selectedId == R.id.chipFound) {
                    reportType = "found";
                }
                updateUIForReportType();
            }
        });
        
        // Date picker
        etDate.setOnClickListener(v -> showDatePicker());
        
        // Image picker
        ivAddImage.setOnClickListener(v -> checkPermissionAndPickImage());
        ivItemImage.setOnClickListener(v -> checkPermissionAndPickImage());
        
        // Submit button
        btnSubmit.setOnClickListener(v -> submitReport());
        
        // Clear button
        btnClear.setOnClickListener(v -> clearForm());
    }
    
    private void updateUIForReportType() {
        if ("lost".equals(reportType)) {
            etDate.setHint("Ngày mất đồ");
            etLocation.setHint("Vị trí mất đồ");
            etDescription.setHint("Mô tả đồ vật bị mất (màu sắc, đặc điểm...)");
            btnSubmit.setText("Báo Mất Đồ");
        } else {
            etDate.setHint("Ngày nhặt được");
            etLocation.setHint("Vị trí nhặt được");
            etDescription.setHint("Mô tả đồ vật nhặt được (màu sắc, đặc điểm...)");
            btnSubmit.setText("Báo Nhặt Được Đồ");
        }
    }
    
    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(Calendar.YEAR, year);
                selectedDate.set(Calendar.MONTH, month);
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateField();
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(selectedDate.getTime()));
    }
    
    private void checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openImagePicker();
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    
                    // Resize if too large
                    Bitmap resized = resizeBitmap(bitmap, 800, 800);
                    
                    // Convert to Base64
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    resized.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    selectedImageBase64 = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
                    
                    // Display image
                    ivItemImage.setImageBitmap(resized);
                    ivItemImage.setVisibility(View.VISIBLE);
                    ivAddImage.setVisibility(View.GONE);
                    
                } catch (IOException e) {
                    Toast.makeText(requireContext(), "Không thể tải ảnh", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;
        
        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }
    
    private void submitReport() {
        // Validate inputs
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String category = spinnerCategory.getText().toString().trim();
        String contactInfo = etContactInfo.getText() != null ? etContactInfo.getText().toString().trim() : "";
        
        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tên đồ vật");
            etTitle.requestFocus();
            return;
        }
        
        if (category.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (description.isEmpty()) {
            etDescription.setError("Vui lòng nhập mô tả");
            etDescription.requestFocus();
            return;
        }
        
        if (location.isEmpty()) {
            etLocation.setError("Vui lòng nhập vị trí");
            etLocation.requestFocus();
            return;
        }
        
        // Create item request object
        CreateItemRequest request = new CreateItemRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setCategory(category);
        request.setStatus(reportType); // "lost" or "found"
        
        // Parse location to lat/lng (simplified - using placeholder for now)
        // TODO: Implement proper geocoding or location picker
        request.setLatitude(null);
        request.setLongitude(null);
        
        // Image
        if (selectedImageBase64 != null) {
            request.setImageUrl(selectedImageBase64);
        }
        
        // Show loading
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");
        
        // Submit to API
        String token = prefsManager.getToken();
        ApiClient.getItemApi().createItem("Bearer " + token, request)
            .enqueue(new Callback<ApiResponse<LostItem>>() {
                @Override
                public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(reportType.equals("lost") ? "Báo Mất Đồ" : "Báo Nhặt Được Đồ");
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(requireContext(), "Đã gửi báo cáo thành công!", Toast.LENGTH_SHORT).show();
                        clearForm();
                    } else {
                        Toast.makeText(requireContext(), "Không thể gửi báo cáo", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText(reportType.equals("lost") ? "Báo Mất Đồ" : "Báo Nhặt Được Đồ");
                    Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void clearForm() {
        etTitle.setText("");
        etDescription.setText("");
        etLocation.setText("");
        etContactInfo.setText("");
        spinnerCategory.setText("");
        
        selectedDate = Calendar.getInstance();
        updateDateField();
        
        selectedImageBase64 = null;
        ivItemImage.setVisibility(View.GONE);
        ivAddImage.setVisibility(View.VISIBLE);
        
        chipLost.setChecked(true);
        reportType = "lost";
        updateUIForReportType();
    }
}
