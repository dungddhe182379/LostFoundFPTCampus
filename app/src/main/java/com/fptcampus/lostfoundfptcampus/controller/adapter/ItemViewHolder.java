package com.fptcampus.lostfoundfptcampus.controller.adapter;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ItemViewHolder extends RecyclerView.ViewHolder {
    private ImageView ivItemImage;
    private TextView tvItemTitle;
    private TextView tvItemDescription;
    private TextView tvItemStatus;
    private TextView tvItemDate;
    private LostItem currentItem;
    private ItemAdapter.OnItemClickListener listener;

    public ItemViewHolder(@NonNull View itemView, ItemAdapter.OnItemClickListener listener) {
        super(itemView);
        this.listener = listener;
        bindingView();
        bindingAction();
    }

    private void bindingView() {
        ivItemImage = itemView.findViewById(R.id.ivItemImage);
        tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
        tvItemDescription = itemView.findViewById(R.id.tvItemDescription);
        tvItemStatus = itemView.findViewById(R.id.tvItemStatus);
        tvItemDate = itemView.findViewById(R.id.tvItemDate);
    }

    private void bindingAction() {
        itemView.setOnClickListener(this::onItemViewClick);
    }

    private void onItemViewClick(View view) {
        if (currentItem != null && listener != null) {
            listener.onItemClick(currentItem);
        }
    }

    public void bind(LostItem item, long currentUserId) {
        this.currentItem = item;
        
        tvItemTitle.setText(item.getTitle());
        tvItemDescription.setText(item.getDescription());
        
        // Set status with color
        String status = item.getStatus();
        if (status != null) {
            switch (status.toLowerCase()) {
                case "lost":
                    tvItemStatus.setText("Đã mất");
                    tvItemStatus.setBackgroundColor(Color.parseColor("#F44336")); // Red
                    break;
                case "found":
                    tvItemStatus.setText("Đã nhặt");
                    tvItemStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                    break;
                case "returned":
                    // PHÂN BIỆT: Đã trả vs Đã nhận
                    // - Đã trả: foundUserId = currentUser (tôi nhặt và trả cho người khác)
                    // - Đã nhận: returnedUserId = currentUser (tôi nhận lại đồ của mình)
                    boolean isGivenBack = (item.getFoundUserId() != null && item.getFoundUserId() == currentUserId);
                    boolean isReceivedBack = (item.getReturnedUserId() != null && item.getReturnedUserId() == currentUserId);
                    
                    if (isReceivedBack && !isGivenBack) {
                        // Tôi là người nhận lại
                        tvItemStatus.setText("Đã nhận");
                        tvItemStatus.setBackgroundColor(Color.parseColor("#9C27B0")); // Purple
                    } else {
                        // Tôi là người trả (hoặc cả 2)
                        tvItemStatus.setText("Đã trả");
                        tvItemStatus.setBackgroundColor(Color.parseColor("#2196F3")); // Blue
                    }
                    break;
                default:
                    tvItemStatus.setText(status);
                    tvItemStatus.setBackgroundColor(Color.GRAY);
            }
        }
        
        // Format date
        if (item.getCreatedAt() != null) {
            tvItemDate.setText(getTimeAgo(item.getCreatedAt()));
        }
        
        // TODO: Load image using Glide or Picasso
        // For now, just set a placeholder
        ivItemImage.setImageResource(R.drawable.ic_launcher_foreground);
    }

    private String getTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(date);
        }
    }
}
