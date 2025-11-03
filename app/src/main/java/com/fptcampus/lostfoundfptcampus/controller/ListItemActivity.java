package com.fptcampus.lostfoundfptcampus.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.adapter.ItemAdapter;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.User;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListItemActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabAdd;

    private ItemAdapter adapter;
    private AppDatabase database;
    private ExecutorService executorService;
    private SharedPreferencesManager prefsManager;
    
    private String currentStatus = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_item);

        bindingView();
        bindingAction();
        
        database = AppDatabase.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        prefsManager = new SharedPreferencesManager(this);

        setupRecyclerView();
        loadItemsFromLocal();
        loadItemsFromApi();
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(v -> finish());
        fabAdd.setOnClickListener(this::onFabAddClick);
        swipeRefresh.setOnRefreshListener(this::onSwipeRefresh);
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

    private void setupRecyclerView() {
        adapter = new ItemAdapter();
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, DetailItemActivity.class);
            intent.putExtra("itemId", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("category", item.getCategory());
            intent.putExtra("status", item.getStatus());
            intent.putExtra("latitude", item.getLatitude() != null ? item.getLatitude() : 0.0);
            intent.putExtra("longitude", item.getLongitude() != null ? item.getLongitude() : 0.0);
            intent.putExtra("createdAt", item.getCreatedAt() != null ? item.getCreatedAt().getTime() : 0);
            intent.putExtra("userId", item.getUserId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
        }
        loadItemsFromLocal();
        loadItemsFromApi();
    }

    private void loadItemsFromLocal() {
        executorService.execute(() -> {
            List<LostItem> items;
            if ("all".equals(currentStatus)) {
                items = database.lostItemDao().getAllItems();
            } else {
                items = database.lostItemDao().getItemsByStatus(currentStatus);
            }
            
            runOnUiThread(() -> adapter.setItems(items));
        });
    }

    private void loadItemsFromApi() {
        // Check network first
        if (!com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(this)) {
            swipeRefresh.setRefreshing(false);
            com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper.showError(
                this,
                "Không có kết nối mạng",
                "Vui lòng kiểm tra kết nối internet và thử lại."
            );
            return;
        }
        
        // Always load all items from API, filter locally
        Call<ApiResponse<List<LostItem>>> call = ApiClient.getItemApi()
                .getAllItems("Bearer " + prefsManager.getToken());

        call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, Response<ApiResponse<List<LostItem>>> response) {
                swipeRefresh.setRefreshing(false);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<List<LostItem>> apiResponse = response.body();
                        
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            List<LostItem> allItems = apiResponse.getData();
                            
                            // Filter based on current status
                            List<LostItem> filteredItems;
                            if ("all".equals(currentStatus)) {
                                filteredItems = allItems;
                            } else {
                                filteredItems = new java.util.ArrayList<>();
                                for (LostItem item : allItems) {
                                    if (currentStatus.equals(item.getStatus())) {
                                        filteredItems.add(item);
                                    }
                                }
                            }
                            
                            adapter.setItems(filteredItems);
                            
                            // Save all items to local database for offline access
                            executorService.execute(() -> {
                                for (LostItem item : allItems) {
                                    item.setSynced(true); // Mark as synced since from server
                                    database.lostItemDao().insert(item);
                                }
                                
                                // Sync unsynced local items to server
                                syncUnsyncedItems();
                            });
                        } else {
                            // API error, show from cache
                            runOnUiThread(() -> {
                                Toast.makeText(ListItemActivity.this, 
                                    "Lỗi: " + (apiResponse.getError() != null ? apiResponse.getError() : "Không có dữ liệu"), 
                                    Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        // Response not successful, show cached data
                        runOnUiThread(() -> {
                            Toast.makeText(ListItemActivity.this, 
                                "Hiển thị dữ liệu offline (Lỗi: " + response.code() + ")", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    swipeRefresh.setRefreshing(false);
                    runOnUiThread(() -> {
                        Toast.makeText(ListItemActivity.this, 
                            "Lỗi xử lý dữ liệu: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                // Network error, show cached data
                runOnUiThread(() -> {
                    Toast.makeText(ListItemActivity.this, 
                        "Chế độ offline: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void syncUnsyncedItems() {
        // Run in background thread (already in executor)
        List<LostItem> unsyncedItems = database.lostItemDao().getUnsyncedItems();
        
        for (LostItem item : unsyncedItems) {
            // Create DTO - Server generates uuid and userId from token
            com.fptcampus.lostfoundfptcampus.model.dto.CreateItemRequest request = 
                new com.fptcampus.lostfoundfptcampus.model.dto.CreateItemRequest(
                    item.getTitle(),
                    item.getDescription(),
                    item.getCategory(),
                    item.getStatus(),
                    item.getLatitude(),
                    item.getLongitude(),
                    item.getImageUrl()
                );
            
            // Sync each unsynced item to server
            Call<ApiResponse<LostItem>> call = ApiClient.getItemApi().createItem(
                "Bearer " + prefsManager.getToken(),
                request
            );
            
            call.enqueue(new Callback<ApiResponse<LostItem>>() {
                @Override
                public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<LostItem> apiResponse = response.body();
                        
                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            // Server accepted item, update local
                            executorService.execute(() -> {
                                LostItem serverItem = apiResponse.getData();
                                
                                // Delete old local item
                                database.lostItemDao().delete(item);
                                
                                // Insert server item with correct ID
                                serverItem.setSynced(true);
                                database.lostItemDao().insert(serverItem);
                                
                                runOnUiThread(() -> {
                                    Toast.makeText(ListItemActivity.this, 
                                        "Đã đồng bộ: " + item.getTitle(), 
                                        Toast.LENGTH_SHORT).show();
                                    // Reload to show updated data
                                    loadItemsFromLocal();
                                });
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                    // Keep in unsynced state, will retry later
                }
            });
        }
    }

    private void onSwipeRefresh() {
        loadItemsFromApi();
    }

    private void onFabAddClick(View view) {
        Intent intent = new Intent(this, AddItemActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItemsFromLocal();
        loadItemsFromApi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
