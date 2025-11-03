package com.fptcampus.lostfoundfptcampus.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.adapter.ItemAdapter;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.navigation.NavigationHost;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * My Items Fragment - Display user's lost and found items
 */
public class MyItemsFragment extends Fragment {
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;

    private ItemAdapter adapter;
    private SharedPreferencesManager prefsManager;
    private AppDatabase database;
    private ExecutorService executorService;

    private String currentFilter = "all"; // all, lost, found, returned

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new SharedPreferencesManager(requireContext());
        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();

        bindingView(view);
        bindingAction();
        setupRecyclerView();
        setupTabs();
        loadMyItems();
    }

    private void bindingView(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        swipeRefresh.setOnRefreshListener(this::loadMyItems);
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter();
        adapter.setOnItemClickListener(item -> {
            // Navigate to detail fragment
            if (getActivity() instanceof NavigationHost) {
                DetailItemFragment detailFragment = DetailItemFragment.newInstance(item);
                ((NavigationHost) getActivity()).navigateTo(detailFragment, true);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Tất cả"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã mất"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã nhặt"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã trả"));
        tabLayout.addTab(tabLayout.newTab().setText("Đã nhận"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "lost";
                        break;
                    case 2:
                        currentFilter = "found";
                        break;
                    case 3:
                        currentFilter = "given_back";  // Đã trả (tôi nhặt và trả)
                        break;
                    case 4:
                        currentFilter = "received_back";  // Đã nhận (tôi mất và nhận lại)
                        break;
                }
                loadMyItems();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadMyItems() {
        swipeRefresh.setRefreshing(true);
        long currentUserId = prefsManager.getUserId();
        String token = "Bearer " + prefsManager.getToken();
        
        // Call API to get ALL items
        com.fptcampus.lostfoundfptcampus.model.api.ItemApi itemApi = 
            com.fptcampus.lostfoundfptcampus.util.ApiClient.getItemApi();
        
        retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>>> call = 
            itemApi.getAllItems(token);
        
        call.enqueue(new retrofit2.Callback<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>>>() {
            @Override
            public void onResponse(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>>> call,
                retrofit2.Response<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>>> response) {
                
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<LostItem> allItems = response.body().getData();
                    List<LostItem> filteredItems = new ArrayList<>();
                    
                    if (allItems != null) {
                        // Filter theo user roles và status - LOGIC CHUẨN
                        for (LostItem item : allItems) {
                            boolean shouldAdd = false;
                            String status = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
                            
                            switch (currentFilter) {
                                case "all":
                                    // Tất cả items liên quan (lostUserId hoặc foundUserId = tôi)
                                    shouldAdd = (item.getLostUserId() != null && item.getLostUserId() == currentUserId) ||
                                                (item.getFoundUserId() != null && item.getFoundUserId() == currentUserId);
                                    break;
                                    
                                case "lost":
                                    // Đã mất = lostUserId=tôi VÀ status="lost"
                                    shouldAdd = "lost".equals(status) && 
                                                item.getLostUserId() != null && 
                                                item.getLostUserId() == currentUserId;
                                    break;
                                    
                                case "found":
                                    // Đã nhặt = foundUserId=tôi VÀ status="found"
                                    shouldAdd = "found".equals(status) && 
                                                item.getFoundUserId() != null && 
                                                item.getFoundUserId() == currentUserId;
                                    break;
                                    
                                case "given_back":
                                    // Đã trả = foundUserId=tôi VÀ status="returned" (tôi nhặt và đã trả cho chủ)
                                    shouldAdd = "returned".equals(status) && 
                                                item.getFoundUserId() != null && 
                                                item.getFoundUserId() == currentUserId;
                                    break;
                                    
                                case "received_back":
                                    // Đã nhận = lostUserId=tôi VÀ returnedUserId=tôi VÀ status="returned" (tôi mất và đã nhận lại)
                                    shouldAdd = "returned".equals(status) && 
                                                item.getLostUserId() != null && item.getLostUserId() == currentUserId &&
                                                item.getReturnedUserId() != null && item.getReturnedUserId() == currentUserId;
                                    break;
                            }
                            
                            if (shouldAdd) {
                                filteredItems.add(item);
                            }
                        }
                    }
                    
                    android.util.Log.d("MyItemsFragment", "Found " + filteredItems.size() + " items for user " + currentUserId + " with filter: " + currentFilter);
                    
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            if (filteredItems.isEmpty()) {
                                tvEmptyState.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                                updateEmptyStateMessage();
                            } else {
                                tvEmptyState.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                                adapter.setItems(filteredItems);
                            }
                        });
                    }
                } else {
                    android.util.Log.e("MyItemsFragment", "Failed to load items from API");
                    loadMyItemsFromDatabase(); // Fallback to database
                }
            }
            
            @Override
            public void onFailure(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>>> call,
                Throwable t) {
                swipeRefresh.setRefreshing(false);
                android.util.Log.e("MyItemsFragment", "API call failed, loading from database", t);
                loadMyItemsFromDatabase(); // Fallback to database
            }
        });
    }
    
    private void loadMyItemsFromDatabase() {
        long currentUserId = prefsManager.getUserId();
        
        executorService.execute(() -> {
            // Fallback: Lấy từ database local với logic tương tự
            List<LostItem> allItems = database.lostItemDao().getAllItems();
            List<LostItem> filteredItems = new ArrayList<>();
            
            for (LostItem item : allItems) {
                boolean shouldAdd = false;
                String status = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
                
                switch (currentFilter) {
                    case "all":
                        shouldAdd = (item.getLostUserId() != null && item.getLostUserId() == currentUserId) ||
                                    (item.getFoundUserId() != null && item.getFoundUserId() == currentUserId);
                        break;
                    case "lost":
                        shouldAdd = "lost".equals(status) && 
                                    item.getLostUserId() != null && item.getLostUserId() == currentUserId;
                        break;
                    case "found":
                        shouldAdd = "found".equals(status) && 
                                    item.getFoundUserId() != null && item.getFoundUserId() == currentUserId;
                        break;
                    case "given_back":
                        shouldAdd = "returned".equals(status) && 
                                    item.getFoundUserId() != null && item.getFoundUserId() == currentUserId;
                        break;
                    case "received_back":
                        shouldAdd = "returned".equals(status) && 
                                    item.getLostUserId() != null && item.getLostUserId() == currentUserId &&
                                    item.getReturnedUserId() != null && item.getReturnedUserId() == currentUserId;
                        break;
                }
                
                if (shouldAdd) {
                    filteredItems.add(item);
                }
            }

            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (filteredItems.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        updateEmptyStateMessage();
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.setItems(filteredItems);
                    }
                });
            }
        });
    }

    private void updateEmptyStateMessage() {
        String message;
        switch (currentFilter) {
            case "lost":
                message = "📭\n\nBạn chưa báo mất đồ vật nào";
                break;
            case "found":
                message = "📭\n\nBạn chưa báo nhặt được đồ vật nào";
                break;
            case "given_back":
                message = "🎁\n\nBạn chưa trả đồ vật nào cho chủ";
                break;
            case "received_back":
                message = "✅\n\nBạn chưa nhận lại đồ vật nào";
                break;
            default:
                message = "📭\n\nBạn chưa có đồ vật nào\n\nHãy bắt đầu bằng cách báo mất hoặc báo nhặt được đồ!";
                break;
        }
        tvEmptyState.setText(message);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
