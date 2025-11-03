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
            Toast.makeText(this, "Không có mạng - Hiển thị dữ liệu offline", Toast.LENGTH_SHORT).show();
            showLoading(false);
            loadFromCache(); // Fallback to cache khi không có mạng
            return;
        }

        // Show loading và call API TRƯỚC (để luôn lấy data mới nhất)
        showLoading(true);
        
        // 🆕 Call API để lấy TẤT CẢ users mới nhất
        Call<ApiResponse<List<User>>> call = ApiClient.getUserApi()
                .getAllUsers("Bearer " + prefsManager.getToken());

        call.enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<User>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        List<User> users = apiResponse.getData();


                        
                        // Log top 10 users

                        for (int i = 0; i < Math.min(10, users.size()); i++) {
                            User u = users.get(i);

                        }
                        
                        // Cache tất cả users vào database (async)
                        executorService.execute(() -> {
                            // Clear old data first
                            database.userDao().deleteAll();

                            
                            // Insert new data
                            for (User user : users) {
                                database.userDao().insert(user);
                            }

                        });
                        
                        // Xử lý và hiển thị NGAY
                        processAndDisplayUsers(users);
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LeaderboardActivity.this, 
                                "Lỗi: " + (apiResponse.getError() != null ? apiResponse.getError() : "Không có dữ liệu"), 
                                Toast.LENGTH_SHORT).show();
                            // Fallback to cache khi API error
                            loadFromCache();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(LeaderboardActivity.this, 
                            "Lỗi server: " + response.code(), 
                            Toast.LENGTH_SHORT).show();
                        // Fallback to cache khi server error
                        loadFromCache();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {

                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LeaderboardActivity.this, 
                        "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    // Fallback to cache khi network error
                    loadFromCache();
                });
            }
        });
    }
    
    private void processAndDisplayUsers(List<User> users) {


        
        // Sort by karma (highest first)
        java.util.Collections.sort(users, (u1, u2) -> {
            return Integer.compare(u2.getKarma(), u1.getKarma());
        });
        
        // Log top 5 after sorting

        for (int i = 0; i < Math.min(5, users.size()); i++) {
            User u = users.get(i);

        }
        
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
        // Load from local database (cached from API)
        // 🆕 NEW: Database bây giờ chứa TẤT CẢ users từ API GET /api/lostfound/user
        // Không còn giới hạn chỉ users có items nữa
        executorService.execute(() -> {
            List<User> users = database.userDao().getTopKarmaUsers(100); // Get top 100
            


            
            // Debug: Print all users với karma
            if (users != null && users.size() > 0) {

                for (int i = 0; i < Math.min(10, users.size()); i++) {
                    User u = users.get(i);

                }
            } else {

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
                

                
                // Check if current user already in list
                boolean userExists = false;
                for (User u : users) {
                    if (u.getId() == currentUser.getId()) {
                        userExists = true;
                        // Update karma if changed
                        u.setKarma(currentUser.getKarma());

                        break;
                    }
                }
                
                if (!userExists && currentUser.getId() > 0) {
                    users.add(currentUser);

                }
            }
            
            // Sort by karma (highest first)
            java.util.Collections.sort(users, (u1, u2) -> {
                return Integer.compare(u2.getKarma(), u1.getKarma());
            });
            


            List<User> finalUsers = users;
            runOnUiThread(() -> {
                showLoading(false);
                
                if (finalUsers != null && !finalUsers.isEmpty()) {
                    displayLeaderboard(finalUsers);
                } else {
                    // Generate sample data for demo

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
