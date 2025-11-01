package com.fptcampus.lostfoundfptcampus.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.adapter.LeaderboardAdapter;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.User;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
import com.fptcampus.lostfoundfptcampus.util.NetworkUtil;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Leaderboard Activity - Display karma ranking
 * Following MVC pattern from lostfound_project_summary.md
 */
public class LeaderboardActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvRank1Name, tvRank1Karma;
    private TextView tvRank2Name, tvRank2Karma;
    private TextView tvRank3Name, tvRank3Karma;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private LeaderboardAdapter adapter;
    private ExecutorService executorService;
    private List<User> userList;
    private SharedPreferencesManager prefsManager;
    private AppDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        executorService = Executors.newSingleThreadExecutor();
        userList = new ArrayList<>();
        prefsManager = new SharedPreferencesManager(this);
        database = AppDatabase.getInstance(this);

        bindingView();
        bindingAction();
        setupRecyclerView();
        loadLeaderboard();
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        tvRank1Name = findViewById(R.id.tvRank1Name);
        tvRank1Karma = findViewById(R.id.tvRank1Karma);
        tvRank2Name = findViewById(R.id.tvRank2Name);
        tvRank2Karma = findViewById(R.id.tvRank2Karma);
        tvRank3Name = findViewById(R.id.tvRank3Name);
        tvRank3Karma = findViewById(R.id.tvRank3Karma);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void bindingAction() {
        toolbar.setNavigationOnClickListener(this::onToolbarBackClick);
        swipeRefresh.setOnRefreshListener(this::onSwipeRefresh);
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadLeaderboard() {
        // Check network first
        if (!NetworkUtil.isNetworkAvailable(this)) {
            showLoading(false);
            ErrorDialogHelper.showError(
                this,
                "Không có kết nối mạng",
                "Vui lòng kiểm tra kết nối internet và thử lại."
            );
            // Try to load from cache
            loadFromCache();
            return;
        }

        showLoading(true);
        
        // Step 1: Load all items to get unique user IDs
        Call<ApiResponse<List<LostItem>>> call = ApiClient.getItemApi()
                .getAllItems("Bearer " + prefsManager.getToken());

        call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, Response<ApiResponse<List<LostItem>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<LostItem>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<LostItem> items = apiResponse.getData();
                        android.util.Log.d("LeaderboardActivity", "Loaded " + items.size() + " items from API");
                        
                        // Step 2: Extract unique user IDs
                        Set<Long> userIds = new HashSet<>();
                        for (LostItem item : items) {
                            if (item.getUserId() > 0) {
                                userIds.add(item.getUserId());
                            }
                        }
                        
                        android.util.Log.d("LeaderboardActivity", "Found " + userIds.size() + " unique users");
                        
                        // Step 3: Fetch each user's details
                        fetchUsersDetails(new ArrayList<>(userIds));
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LeaderboardActivity.this, 
                                "Lỗi: " + (apiResponse.getError() != null ? apiResponse.getError() : "Không có dữ liệu"), 
                                Toast.LENGTH_SHORT).show();
                            loadFromCache();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(LeaderboardActivity.this, 
                            "Lỗi server: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                        loadFromCache();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LeaderboardActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    loadFromCache();
                });
            }
        });
    }
    
    private void fetchUsersDetails(List<Long> userIds) {
        if (userIds.isEmpty()) {
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, "Không tìm thấy người dùng nào", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        List<User> fetchedUsers = new ArrayList<>();
        final int[] completedRequests = {0};
        final int totalRequests = userIds.size();
        
        for (Long userId : userIds) {
            Call<ApiResponse<User>> call = ApiClient.getUserApi()
                    .getUserById("Bearer " + prefsManager.getToken(), userId);
            
            call.enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                    synchronized (fetchedUsers) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<User> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                User user = apiResponse.getData();
                                fetchedUsers.add(user);
                                android.util.Log.d("LeaderboardActivity", "Fetched user: " + user.getName() + " - Karma: " + user.getKarma());
                                
                                // Cache user to database
                                executorService.execute(() -> database.userDao().insert(user));
                            }
                        }
                        
                        completedRequests[0]++;
                        
                        // When all requests complete
                        if (completedRequests[0] == totalRequests) {
                            processAndDisplayUsers(fetchedUsers);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                    synchronized (fetchedUsers) {
                        completedRequests[0]++;
                        
                        if (completedRequests[0] == totalRequests) {
                            processAndDisplayUsers(fetchedUsers);
                        }
                    }
                }
            });
        }
    }
    
    private void processAndDisplayUsers(List<User> users) {
        android.util.Log.d("LeaderboardActivity", "Processing " + users.size() + " users");
        
        // Sort by karma (highest first)
        java.util.Collections.sort(users, (u1, u2) -> {
            return Integer.compare(u2.getKarma(), u1.getKarma());
        });
        
        runOnUiThread(() -> {
            showLoading(false);
            
            if (!users.isEmpty()) {
                displayLeaderboard(users);
            } else {
                Toast.makeText(this, "Không có dữ liệu xếp hạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadFromCache() {
        // Load from local database as fallback
        executorService.execute(() -> {
            List<User> users = database.userDao().getTopKarmaUsers(50); // Get top 50
            
            android.util.Log.d("LeaderboardActivity", "Loaded " + (users != null ? users.size() : 0) + " users from DB");
            
            // Debug: Print all users
            if (users != null) {
                for (User u : users) {
                    android.util.Log.d("LeaderboardActivity", "User: " + u.getName() + " - Karma: " + u.getKarma());
                }
            }

            // Ensure users list is mutable
            if (users == null) {
                users = new java.util.ArrayList<>();
            } else {
                users = new java.util.ArrayList<>(users); // Create mutable copy
            }

            // Add current user if not in list
            com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager prefsManager = 
                new com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager(this);
            
            if (prefsManager.isLoggedIn()) {
                User currentUser = new User();
                currentUser.setId(prefsManager.getUserId());
                currentUser.setName(prefsManager.getUserName());
                currentUser.setEmail(prefsManager.getUserEmail());
                currentUser.setKarma(prefsManager.getUserKarma());
                
                android.util.Log.d("LeaderboardActivity", "Current user: " + currentUser.getName() + " - Karma: " + currentUser.getKarma());
                
                // Check if current user already in list
                boolean userExists = false;
                for (User u : users) {
                    if (u.getId() == currentUser.getId()) {
                        userExists = true;
                        // Update karma if changed
                        u.setKarma(currentUser.getKarma());
                        android.util.Log.d("LeaderboardActivity", "Updated existing user karma");
                        break;
                    }
                }
                
                if (!userExists && currentUser.getId() > 0) {
                    users.add(currentUser);
                    android.util.Log.d("LeaderboardActivity", "Added current user to list");
                }
            }
            
            // Sort by karma (highest first)
            java.util.Collections.sort(users, (u1, u2) -> {
                return Integer.compare(u2.getKarma(), u1.getKarma());
            });
            
            android.util.Log.d("LeaderboardActivity", "Final sorted list size: " + users.size());

            List<User> finalUsers = users;
            runOnUiThread(() -> {
                showLoading(false);
                
                if (finalUsers != null && !finalUsers.isEmpty()) {
                    displayLeaderboard(finalUsers);
                } else {
                    // Generate sample data for demo
                    android.util.Log.d("LeaderboardActivity", "Using sample data");
                    List<User> sampleUsers = generateSampleData();
                    displayLeaderboard(sampleUsers);
                }
            });
        });
    }

    private void displayLeaderboard(List<User> users) {
        if (users == null || users.isEmpty()) {
            ErrorDialogHelper.showError(this, "Thông báo",
                    "Chưa có dữ liệu bảng xếp hạng");
            return;
        }

        // Display top 3
        if (users.size() >= 1) {
            User rank1 = users.get(0);
            tvRank1Name.setText(rank1.getName());
            tvRank1Karma.setText(rank1.getKarma() + " ⭐");
        }

        if (users.size() >= 2) {
            User rank2 = users.get(1);
            tvRank2Name.setText(rank2.getName());
            tvRank2Karma.setText(rank2.getKarma() + " ⭐");
        }

        if (users.size() >= 3) {
            User rank3 = users.get(2);
            tvRank3Name.setText(rank3.getName());
            tvRank3Karma.setText(rank3.getKarma() + " ⭐");
        }

        // Display full list
        userList.clear();
        userList.addAll(users);
        adapter.notifyDataSetChanged();
    }

    private List<User> generateSampleData() {
        List<User> sampleUsers = new ArrayList<>();

        String[] names = {
                "Nguyễn Văn An", "Trần Thị Bình", "Phạm Minh Cường",
                "Lê Hồng Dung", "Hoàng Thị Em", "Đặng Văn Phúc",
                "Võ Thị Giang", "Bùi Văn Hải", "Đinh Thị Hoa"
        };

        int[] karmaScores = {500, 350, 280, 220, 180, 150, 120, 100, 80};

        for (int i = 0; i < names.length; i++) {
            User user = new User();
            user.setId(i + 1);
            user.setName(names[i]);
            user.setEmail(names[i].toLowerCase().replace(" ", "") + "@fpt.edu.vn");
            user.setKarma(karmaScores[i]);
            sampleUsers.add(user);
        }

        return sampleUsers;
    }

    private void onToolbarBackClick(View view) {
        finish();
    }

    private void onSwipeRefresh() {
        loadLeaderboard();
        swipeRefresh.setRefreshing(false);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
