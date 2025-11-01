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
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
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
            // TODO: Navigate to detail activity
            Toast.makeText(this, "Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
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
        Call<ApiResponse<List<LostItem>>> call;
        
        if ("all".equals(currentStatus)) {
            call = ApiClient.getItemApi().getAllItems("Bearer " + prefsManager.getToken());
        } else {
            call = ApiClient.getItemApi().getItemsByStatus("Bearer " + prefsManager.getToken(), currentStatus);
        }

        call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, Response<ApiResponse<List<LostItem>>> response) {
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<LostItem>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<LostItem> items = apiResponse.getData();
                        adapter.setItems(items);
                        
                        // Save to local database
                        executorService.execute(() -> {
                            for (LostItem item : items) {
                                database.lostItemDao().insert(item);
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(ListItemActivity.this, "Lỗi tải dữ liệu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
