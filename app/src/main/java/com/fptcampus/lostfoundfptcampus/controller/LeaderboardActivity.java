package com.fptcampus.lostfoundfptcampus.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.adapter.LeaderboardAdapter;
import com.fptcampus.lostfoundfptcampus.model.User;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ErrorDialogHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        executorService = Executors.newSingleThreadExecutor();
        userList = new ArrayList<>();

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
        showLoading(true);

        executorService.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<User> users = db.userDao().getTopKarmaUsers(50); // Get top 50

            runOnUiThread(() -> {
                showLoading(false);
                
                List<User> finalUsers = users;
                if (finalUsers != null && !finalUsers.isEmpty()) {
                    displayLeaderboard(finalUsers);
                } else {
                    // Generate sample data for demo
                    List<User> sampleUsers = generateSampleData();
                    displayLeaderboard(sampleUsers);
                }
            });
        });

        // TODO: In production, sync with API
        // Call API to get real-time leaderboard
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
