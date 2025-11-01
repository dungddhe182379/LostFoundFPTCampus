package com.fptcampus.lostfoundfptcampus.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Detail Item Activity - Display item details
 * Following MVC pattern from lostfound_project_summary.md
 */
public class DetailItemActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private ImageView ivItemImage;
    private TextView tvStatus, tvTitle, tvCategory, tvDescription;
    private TextView tvLocation, tvUserName, tvCreatedAt;
    private MaterialButton btnViewOnMap, btnGenerateQr, btnContact;

    private LostItem currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_item);

        bindingView();
        bindingAction();
        loadItemData();
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        ivItemImage = findViewById(R.id.ivItemImage);
        tvStatus = findViewById(R.id.tvStatus);
        tvTitle = findViewById(R.id.tvTitle);
        tvCategory = findViewById(R.id.tvCategory);
        tvDescription = findViewById(R.id.tvDescription);
        tvLocation = findViewById(R.id.tvLocation);
        tvUserName = findViewById(R.id.tvUserName);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        btnContact = findViewById(R.id.btnContact);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(this::onToolbarBackClick);
        btnViewOnMap.setOnClickListener(this::onBtnViewOnMapClick);
        btnGenerateQr.setOnClickListener(this::onBtnGenerateQrClick);
        btnContact.setOnClickListener(this::onBtnContactClick);
    }

    private void loadItemData() {
        Intent intent = getIntent();
        long itemId = intent.getLongExtra("itemId", -1);
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        String category = intent.getStringExtra("category");
        String status = intent.getStringExtra("status");
        double latitude = intent.getDoubleExtra("latitude", 0);
        double longitude = intent.getDoubleExtra("longitude", 0);
        long createdAt = intent.getLongExtra("createdAt", 0);

        if (itemId == -1) {
            ErrorDialogHelper.showError(this, "Lỗi", "Không tìm thấy thông tin đồ thất lạc", this::finish);
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

    private void onToolbarBackClick(View view) {
        finish();
    }

    private void onBtnViewOnMapClick(View view) {
        if (currentItem != null && currentItem.getLatitude() != null && currentItem.getLongitude() != null) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("latitude", currentItem.getLatitude());
            intent.putExtra("longitude", currentItem.getLongitude());
            intent.putExtra("title", currentItem.getTitle());
            startActivity(intent);
        } else {
            ErrorDialogHelper.showError(this, "Lỗi", "Đồ thất lạc này không có thông tin vị trí");
        }
    }

    private void onBtnGenerateQrClick(View view) {
        if (currentItem != null) {
            Intent intent = new Intent(this, QrScanActivity.class);
            intent.putExtra("mode", "generate");
            intent.putExtra("itemId", currentItem.getId());
            intent.putExtra("itemTitle", currentItem.getTitle());
            startActivity(intent);
        }
    }

    private void onBtnContactClick(View view) {
        ErrorDialogHelper.showError(this, "Chức năng đang phát triển", 
                "Chức năng liên hệ sẽ được cập nhật trong phiên bản tiếp theo");
    }
}
