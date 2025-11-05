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
import com.fptcampus.lostfoundfptcampus.util.NetworkUtil;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * My Items Fragment - Display user's lost and found items
 */
public class MyItemsFragment extends Fragment {
    private MaterialToolbar toolbar;
    private com.google.android.material.textfield.TextInputEditText searchEditText;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private TextView tvEmptyState;

    private ItemAdapter adapter;
    private SharedPreferencesManager prefsManager;
    private AppDatabase database;
    private ExecutorService executorService;

    private String currentFilter = "all"; // all, lost, found, returned
    private String searchQuery = ""; // Search query
    private List<LostItem> allLoadedItems = new ArrayList<>(); // Cache all items

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
        searchEditText = view.findViewById(R.id.searchEditText);
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        swipeRefresh.setOnRefreshListener(this::loadMyItems);
        
        // Search functionality
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                filterAndDisplayItems();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter();
        adapter.setCurrentUserId(prefsManager.getUserId()); // Set current user ID để phân biệt "Đã trả" vs "Đã nhận"
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
                filterAndDisplayItems();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadMyItems() {
        // Check network FIRST (giống LeaderboardActivity)
        if (!NetworkUtil.isNetworkAvailable(requireContext())) {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(requireContext(), "Không có kết nối mạng. Vui lòng kiểm tra và thử lại.", Toast.LENGTH_LONG).show();
            
            // Hiển thị empty state
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Không có kết nối mạng");
                    recyclerView.setVisibility(View.GONE);
                });
            }
            return;
        }
        
        swipeRefresh.setRefreshing(true);
        long currentUserId = prefsManager.getUserId();
        String token = "Bearer " + prefsManager.getToken();
        
        android.util.Log.d("MyItems", "Loading items for user " + currentUserId + " with filter: " + currentFilter);
        
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
                
                if (response.isSuccessful() && response.body() != null) {
                    com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // Success! Store all items and filter them
                        allLoadedItems = apiResponse.getData() != null ? apiResponse.getData() : new ArrayList<>();
                        
                        android.util.Log.d("MyItems", "Received " + allLoadedItems.size() + " items from API");
                        
                        // Filter and display
                        if (isAdded() && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> filterAndDisplayItems());
                        }
                    } else {
                        // API returned error
                        String errorMsg = apiResponse.getError() != null ? apiResponse.getError() : "Không thể tải danh sách";
                        android.util.Log.e("MyItems", "API Error: " + errorMsg);
                        
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "❌ " + errorMsg, Toast.LENGTH_LONG).show();
                            tvEmptyState.setVisibility(View.VISIBLE);
                            tvEmptyState.setText(errorMsg);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                } else {
                    // HTTP error
                    String errorMsg = "Lỗi server: " + response.code();
                    if (response.message() != null) {
                        errorMsg += " - " + response.message();
                    }
                    android.util.Log.e("MyItems", "HTTP Error: " + response.code() + " - " + response.message());
                    
                    if (isAdded()) {
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Lỗi tải dữ liệu");
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            }
            
            @Override
            public void onFailure(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<LostItem>>> call,
                Throwable t) {
                swipeRefresh.setRefreshing(false);
                
                String errorMsg = "Lỗi kết nối: " + t.getMessage();
                android.util.Log.e("MyItems", "Network Error", t);
                
                if (isAdded()) {
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                    tvEmptyState.setVisibility(View.VISIBLE);
                    tvEmptyState.setText("Lỗi kết nối mạng");
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });
    }
    
    /**
     * Filter items by tab filter AND search query
     */
    private void filterAndDisplayItems() {
        long currentUserId = prefsManager.getUserId();
        List<LostItem> filteredItems = new ArrayList<>();
        
        for (LostItem item : allLoadedItems) {
            // First: Filter by tab (user role & status)
            boolean matchesTab = false;
            String status = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
            
            switch (currentFilter) {
                case "all":
                    matchesTab = (item.getLostUserId() != null && item.getLostUserId() == currentUserId) ||
                                 (item.getFoundUserId() != null && item.getFoundUserId() == currentUserId);
                    break;
                case "lost":
                    matchesTab = "lost".equals(status) && 
                                 item.getLostUserId() != null && 
                                 item.getLostUserId() == currentUserId;
                    break;
                case "found":
                    matchesTab = "found".equals(status) && 
                                 item.getFoundUserId() != null && 
                                 item.getFoundUserId() == currentUserId;
                    break;
                case "given_back":
                    matchesTab = "returned".equals(status) && 
                                 item.getFoundUserId() != null && 
                                 item.getFoundUserId() == currentUserId;
                    break;
                case "received_back":
                    matchesTab = "returned".equals(status) && 
                                 item.getLostUserId() != null && item.getLostUserId() == currentUserId &&
                                 item.getReturnedUserId() != null && item.getReturnedUserId() == currentUserId;
                    break;
            }
            
            if (!matchesTab) continue;
            
            // Second: Filter by search query (title, description, or ID)
            if (!searchQuery.isEmpty()) {
                String query = searchQuery.toLowerCase();
                String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                String itemId = String.valueOf(item.getId());
                
                boolean matchesSearch = title.contains(query) || 
                                        desc.contains(query) || 
                                        itemId.contains(query) ||
                                        ("#" + itemId).contains(query);
                
                if (!matchesSearch) continue;
            }
            
            filteredItems.add(item);
        }
        
        android.util.Log.d("MyItems", "Filtered to " + filteredItems.size() + " items (filter: " + currentFilter + ", search: '" + searchQuery + "')");
        
        // Update UI
        if (filteredItems.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            updateEmptyStateMessage();
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setItems(filteredItems);
        }
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
