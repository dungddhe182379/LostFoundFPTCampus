package com.fptcampus.lostfoundfptcampus.controller.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.Message;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for chat messages
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messages;
    private long currentUserId;

    public MessageAdapter(long currentUserId) {
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message, currentUserId);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout messageContainer;
        private final MaterialCardView cardMessage;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final TextView tvSenderName;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            cardMessage = itemView.findViewById(R.id.cardMessage);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }

        public void bind(Message message, long currentUserId) {
            boolean isSentByMe = message.getSenderId() == currentUserId;

            // Message text
            tvMessage.setText(message.getMessage());

            // Time
            tvTime.setText(formatTime(message.getTimestamp()));

            // Sender name (only show for received messages)
            if (!isSentByMe) {
                tvSenderName.setVisibility(View.VISIBLE);
                tvSenderName.setText(message.getSenderName());
            } else {
                tvSenderName.setVisibility(View.GONE);
            }

            // Layout alignment
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) cardMessage.getLayoutParams();
            if (isSentByMe) {
                // Sent message - align right, blue background
                params.gravity = Gravity.END;
                cardMessage.setCardBackgroundColor(itemView.getContext().getColor(R.color.primary));
                tvMessage.setTextColor(itemView.getContext().getColor(android.R.color.white));
                tvTime.setTextColor(itemView.getContext().getColor(android.R.color.white));
            } else {
                // Received message - align left, gray background
                params.gravity = Gravity.START;
                cardMessage.setCardBackgroundColor(itemView.getContext().getColor(R.color.surface_variant));
                tvMessage.setTextColor(itemView.getContext().getColor(R.color.text_primary));
                tvTime.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                tvSenderName.setTextColor(itemView.getContext().getColor(R.color.primary));
            }
            cardMessage.setLayoutParams(params);
        }

        private String formatTime(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
