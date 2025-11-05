package com.fptcampus.lostfoundfptcampus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.ChatActivity;
import com.fptcampus.lostfoundfptcampus.controller.adapter.ChatListAdapter;
import com.fptcampus.lostfoundfptcampus.model.UserChat;
import com.fptcampus.lostfoundfptcampus.util.FirebaseChatManager;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;

import java.util.List;

/**
 * Chat List Fragment - Display list of user's chats
 */
public class ChatListFragment extends Fragment {
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    
    private ChatListAdapter adapter;
    private SharedPreferencesManager prefsManager;
    private FirebaseChatManager chatManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRecyclerView();
        
        prefsManager = new SharedPreferencesManager(requireContext());
        chatManager = FirebaseChatManager.getInstance();
        
        loadChats();
        
        swipeRefresh.setOnRefreshListener(this::loadChats);
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        adapter = new ChatListAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnChatClickListener(chat -> {
            // Open chat activity
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("chatId", chat.getChatId());
            intent.putExtra("itemId", chat.getItemId());
            intent.putExtra("otherUserId", chat.getOtherUserId());
            intent.putExtra("otherUserName", chat.getOtherUserName());
            intent.putExtra("isAnonymous", chat.isAnonymous());
            startActivity(intent);
        });
    }

    private void loadChats() {
        swipeRefresh.setRefreshing(true);
        long userId = prefsManager.getUserId();

        chatManager.getUserChats(userId, new FirebaseChatManager.UserChatsCallback() {
            @Override
            public void onSuccess(List<UserChat> chats) {
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        
                        if (chats.isEmpty()) {
                            emptyState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.setChats(chats);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChats();
    }
}
