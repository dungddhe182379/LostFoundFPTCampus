package com.fptcampus.lostfoundfptcampus.controller;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.adapter.MessageAdapter;
import com.fptcampus.lostfoundfptcampus.model.Message;
import com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;

import java.util.List;

/**
 * Chat Activity - 1-on-1 chat conversation
 */
public class ChatActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private EditText messageInput;
    private ImageButton sendButton;

    private MessageAdapter adapter;
    private LinearLayoutManager layoutManager;
    
    private String chatId;
    private long itemId;
    private long otherUserId;
    private String otherUserName;
    private boolean isAnonymous;
    
    private SharedPreferencesManager prefsManager;
    private FirebaseChatManager chatManager;
    private long currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getIntentData();
        bindViews();
        setupToolbar();
        setupRecyclerView();

        prefsManager = new SharedPreferencesManager(this);
        chatManager = FirebaseChatManager.getInstance();
        currentUserId = prefsManager.getUserId();

        loadMessages();
        markMessagesAsRead();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void getIntentData() {
        chatId = getIntent().getStringExtra("chatId");
        itemId = getIntent().getLongExtra("itemId", 0);
        otherUserId = getIntent().getLongExtra("otherUserId", 0);
        otherUserName = getIntent().getStringExtra("otherUserName");
        isAnonymous = getIntent().getBooleanExtra("isAnonymous", false);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(isAnonymous ? "Người dùng ẩn danh" : otherUserName);
            getSupportActionBar().setSubtitle("Vật phẩm #" + itemId);
        }
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Show latest messages at bottom
        
        adapter = new MessageAdapter(currentUserId);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void loadMessages() {
        chatManager.getMessages(chatId, new FirebaseChatManager.MessagesCallback() {
            @Override
            public void onMessagesChanged(List<Message> messages) {
                runOnUiThread(() -> {
                    adapter.setMessages(messages);
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> 
                    Toast.makeText(ChatActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        String senderName = isAnonymous ? "Ẩn danh" : prefsManager.getUserName();
        if (senderName == null) {
            senderName = "User #" + currentUserId;
        }
        
        chatManager.sendMessage(chatId, currentUserId, senderName, text, new FirebaseChatManager.MessageCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    messageInput.setText("");
                    scrollToBottom();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> 
                    Toast.makeText(ChatActivity.this, "Gửi thất bại: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }

    private void markMessagesAsRead() {
        chatManager.markMessagesAsRead(chatId, currentUserId);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Mark messages as read when leaving
        markMessagesAsRead();
    }
}
