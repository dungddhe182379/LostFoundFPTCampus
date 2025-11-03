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
                        currentFilter = "returned";
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

        executorService.execute(() -> {
            // Lấy TẤT CẢ items từ database
            List<LostItem> allItems = database.lostItemDao().getAllItems();
            List<LostItem> filteredItems = new java.util.ArrayList<>();
            
            // Filter theo 3 role fields (lostUserId, foundUserId, returnedUserId)
            for (LostItem item : allItems) {
                boolean isRelated = false;
                boolean matchesStatus = true;
                
                // Check xem user có liên quan đến item này không
                if (item.getLostUserId() != null && item.getLostUserId() == currentUserId) {
                    isRelated = true;
                }
                if (item.getFoundUserId() != null && item.getFoundUserId() == currentUserId) {
                    isRelated = true;
                }
                if (item.getReturnedUserId() != null && item.getReturnedUserId() == currentUserId) {
                    isRelated = true;
                }
                
                // Nếu không phải "all", kiểm tra status filter
                if (!"all".equals(currentFilter)) {
                    matchesStatus = currentFilter.equalsIgnoreCase(item.getStatus());
                }
                
                // Thêm vào list nếu có liên quan VÀ match status
                if (isRelated && matchesStatus) {
                    filteredItems.add(item);
                }
            }

            requireActivity().runOnUiThread(() -> {
                swipeRefresh.setRefreshing(false);
                
                android.util.Log.d("MyItemsFragment", "Found " + filteredItems.size() + " items for user " + currentUserId + " with filter: " + currentFilter);
                
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
            case "returned":
                message = "📭\n\nChưa có đồ vật nào được trả";
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
