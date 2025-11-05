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
        private final LinearLayout sentLayout;
        private final LinearLayout receivedLayout;
        private final TextView sentMessage;
        private final TextView sentTime;
        private final TextView receivedMessage;
        private final TextView receivedTime;
        private final TextView senderName;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            sentLayout = itemView.findViewById(R.id.sentLayout);
            receivedLayout = itemView.findViewById(R.id.receivedLayout);
            sentMessage = itemView.findViewById(R.id.sentMessage);
            sentTime = itemView.findViewById(R.id.sentTime);
            receivedMessage = itemView.findViewById(R.id.receivedMessage);
            receivedTime = itemView.findViewById(R.id.receivedTime);
            senderName = itemView.findViewById(R.id.senderName);
        }

        public void bind(Message message, long currentUserId) {
            boolean isSentByMe = message.getSenderId() == currentUserId;

            if (isSentByMe) {
                // Show sent layout, hide received
                sentLayout.setVisibility(View.VISIBLE);
                receivedLayout.setVisibility(View.GONE);
                
                sentMessage.setText(message.getMessage());
                sentTime.setText(formatTime(message.getTimestamp()));
            } else {
                // Show received layout, hide sent
                sentLayout.setVisibility(View.GONE);
                receivedLayout.setVisibility(View.VISIBLE);
                
                receivedMessage.setText(message.getMessage());
                receivedTime.setText(formatTime(message.getTimestamp()));
                senderName.setText(message.getSenderName());
            }
        }

        private String formatTime(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
