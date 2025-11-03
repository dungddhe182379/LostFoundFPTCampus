package com.fptcampus.lostfoundfptcampus.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class QrItemSelectorAdapter extends RecyclerView.Adapter<QrItemSelectorAdapter.ViewHolder> {
    
    private List<LostItem> items;
    private List<LostItem> filteredItems;
    private LostItem selectedItem;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LostItem item);
    }

    public QrItemSelectorAdapter(List<LostItem> items, OnItemClickListener listener) {
        this.items = items;
        this.filteredItems = new ArrayList<>(items);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_qr_selector, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LostItem item = filteredItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    public void filter(String query) {
        filteredItems.clear();
        
        if (query == null || query.trim().isEmpty()) {
            filteredItems.addAll(items);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (LostItem item : items) {
                boolean matchTitle = item.getTitle() != null && 
                        item.getTitle().toLowerCase().contains(lowerQuery);
                boolean matchDesc = item.getDescription() != null && 
                        item.getDescription().toLowerCase().contains(lowerQuery);
                boolean matchCategory = item.getCategory() != null && 
                        item.getCategory().toLowerCase().contains(lowerQuery);
                
                if (matchTitle || matchDesc || matchCategory) {
                    filteredItems.add(item);
                }
            }
        }
        
        notifyDataSetChanged();
    }

    public void filterByStatus(String status) {
        filteredItems.clear();
        
        for (LostItem item : items) {
            if (status == null || status.isEmpty() || status.equalsIgnoreCase(item.getStatus())) {
                filteredItems.add(item);
            }
        }
        
        notifyDataSetChanged();
    }

    public void updateItems(List<LostItem> newItems) {
        this.items = newItems;
        this.filteredItems = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    public LostItem getSelectedItem() {
        return selectedItem;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardItem;
        ImageView ivItemImage;
        TextView tvItemTitle;
        TextView tvItemCategory;
        TextView tvItemStatus;
        ImageView ivCheckmark;

        ViewHolder(View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.cardItem);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemStatus = itemView.findViewById(R.id.tvItemStatus);
            ivCheckmark = itemView.findViewById(R.id.ivCheckmark);
        }

        void bind(LostItem item) {
            tvItemTitle.setText(item.getTitle());
            tvItemCategory.setText(item.getCategory());
            
            // Set status with color
            String status = item.getStatus();
            tvItemStatus.setText(getStatusText(status));
            tvItemStatus.setBackgroundResource(getStatusBackground(status));
            
            // Load image
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivItemImage);
            } else {
                ivItemImage.setImageResource(R.drawable.ic_image_placeholder);
            }
            
            // Show checkmark if selected
            boolean isSelected = selectedItem != null && selectedItem.getId() == item.getId();
            ivCheckmark.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // Highlight selected card
            if (isSelected) {
                cardItem.setStrokeColor(itemView.getContext().getColor(R.color.primary));
                cardItem.setStrokeWidth(4);
            } else {
                cardItem.setStrokeColor(itemView.getContext().getColor(R.color.border_light));
                cardItem.setStrokeWidth(1);
            }
            
            // Click listener
            cardItem.setOnClickListener(v -> {
                selectedItem = item;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
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
}
