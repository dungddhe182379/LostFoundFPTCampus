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
import com.fptcampus.lostfoundfptcampus.controller.AddItemActivity;
import com.fptcampus.lostfoundfptcampus.controller.adapter.ItemAdapter;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemsFragment extends Fragment {
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabAdd;
    private LinearLayout emptyState;

    private ItemAdapter adapter;
    private AppDatabase database;
    private ExecutorService executorService;
    private SharedPreferencesManager prefsManager;
    private com.fptcampus.lostfoundfptcampus.navigation.NavigationHost navigationHost;
    
    private String currentStatus = "all";

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) {
            navigationHost = (com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindingView(view);
        bindingAction();
        
        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        prefsManager = new SharedPreferencesManager(requireContext());

        setupRecyclerView();
        setupSwipeRefresh();
        loadItemsFromApi();
    }

    private void bindingView(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyState = view.findViewById(R.id.emptyState);
    }

    private void bindingAction() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddItemActivity.class);
            startActivity(intent);
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                onTabChanged(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
            getResources().getColor(R.color.primary, null),
            getResources().getColor(R.color.secondary, null),
            getResources().getColor(R.color.accent, null)
        );
        swipeRefresh.setOnRefreshListener(this::loadItemsFromApi);
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter();
        adapter.setOnItemClickListener(item -> {
            // Navigate to DetailItemFragment
            if (navigationHost != null) {
                DetailItemFragment detailFragment = DetailItemFragment.newInstance(item);
                navigationHost.navigateTo(detailFragment, true);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void onTabChanged(int position) {
        switch (position) {
            case 0:
                currentStatus = "all";
                break;
            case 1:
                currentStatus = "lost";
                break;
            case 2:
                currentStatus = "found";
                break;
            case 3:
                currentStatus = "returned";
                break;
        }
        loadItemsFromApi();
    }

    private void loadItemsFromApi() {
        // Check network first
        if (!com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(requireContext())) {
            swipeRefresh.setRefreshing(false);
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            }
            loadItemsFromLocal();
            return;
        }
        
        String token = "Bearer " + prefsManager.getToken();
        
        // Load all items from API
        Call<ApiResponse<List<LostItem>>> call = ApiClient.getItemApi().getAllItems(token);

        call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, Response<ApiResponse<List<LostItem>>> response) {
                if (!isAdded() || getActivity() == null) return;
                
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<LostItem>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<LostItem> allItems = apiResponse.getData();
                        
                        // Filter based on current status
                        List<LostItem> filteredItems;
                        if ("all".equals(currentStatus)) {
                            filteredItems = allItems;
                        } else {
                            filteredItems = new ArrayList<>();
                            for (LostItem item : allItems) {
                                if (currentStatus.equalsIgnoreCase(item.getStatus())) {
                                    filteredItems.add(item);
                                }
                            }
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            adapter.setItems(filteredItems);
                            updateEmptyState(filteredItems.isEmpty());
                        });
                        
                        // Save to local database for offline access
                        executorService.execute(() -> {
                            for (LostItem item : allItems) {
                                item.setSynced(true);
                                database.lostItemDao().insert(item);
                            }
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "Lỗi: " + (apiResponse.getError() != null ? apiResponse.getError() : "Không có dữ liệu"), 
                                Toast.LENGTH_SHORT).show();
                            loadItemsFromLocal();
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "Lỗi tải dữ liệu (Code: " + response.code() + ")", 
                            Toast.LENGTH_SHORT).show();
                        loadItemsFromLocal();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                
                swipeRefresh.setRefreshing(false);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), 
                        "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    loadItemsFromLocal();
                });
            }
        });
    }

    private void loadItemsFromLocal() {
        executorService.execute(() -> {
            List<LostItem> items;
            if ("all".equals(currentStatus)) {
                items = database.lostItemDao().getAllItems();
            } else {
                items = database.lostItemDao().getItemsByStatus(currentStatus);
            }
            
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setItems(items);
                    updateEmptyState(items.isEmpty());
                });
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItemsFromApi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
