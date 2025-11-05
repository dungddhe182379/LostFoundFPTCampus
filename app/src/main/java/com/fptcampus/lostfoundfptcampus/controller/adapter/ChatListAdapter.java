package com.fptcampus.lostfoundfptcampus.controller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.UserChat;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for chat list
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private List<UserChat> chats;
    private OnChatClickListener listener;

    public ChatListAdapter() {
        this.chats = new ArrayList<>();
    }

    public void setChats(List<UserChat> chats) {
        this.chats = chats;
        notifyDataSetChanged();
    }

    public void setOnChatClickListener(OnChatClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        UserChat chat = chats.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardChat;
        private final TextView tvUserName;
        private final TextView tvLastMessage;
        private final TextView tvTime;
        private final TextView tvUnreadBadge;
        private final TextView tvItemId;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardChat = itemView.findViewById(R.id.cardChat);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
            tvItemId = itemView.findViewById(R.id.tvItemId);
        }

        public void bind(UserChat chat) {
            // User name (hoặc ẩn danh)
            if (chat.isAnonymous()) {
                tvUserName.setText("👤 " + chat.getOtherUserName());
            } else {
                tvUserName.setText(chat.getOtherUserName());
            }

            // Last message
            tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "Bắt đầu cuộc trò chuyện");

            // Time
            tvTime.setText(formatTime(chat.getLastMessageTime()));

            // Item ID badge
            tvItemId.setText("Đồ vật #" + chat.getItemId());

            // Unread badge
            if (chat.getUnreadCount() > 0) {
                tvUnreadBadge.setVisibility(View.VISIBLE);
                tvUnreadBadge.setText(String.valueOf(Math.min(chat.getUnreadCount(), 99)));
            } else {
                tvUnreadBadge.setVisibility(View.GONE);
            }

            // Click listener
            cardChat.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });
        }

        private String formatTime(long timestamp) {
            if (timestamp == 0) return "";
            
            Date date = new Date(timestamp);
            Date now = new Date();
            
            long diff = now.getTime() - timestamp;
            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) {
                return "Vừa xong";
            } else if (minutes < 60) {
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

    public interface OnChatClickListener {
        void onChatClick(UserChat chat);
    }
}
