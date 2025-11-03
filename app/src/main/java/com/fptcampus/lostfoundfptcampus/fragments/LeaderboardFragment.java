package com.fptcampus.lostfoundfptcampus.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.adapter.LeaderboardAdapter;
import com.fptcampus.lostfoundfptcampus.model.User;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaderboardFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView tvMyRank, tvMyKarma, tvMyName;

    private LeaderboardAdapter adapter;
    private SharedPreferencesManager prefsManager;
    private List<User> leaderboardList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefsManager = new SharedPreferencesManager(requireContext());

        bindingView(view);
        setupRecyclerView();
        setupSwipeRefresh();

        loadLeaderboard();
        loadMyStats();
    }

    private void bindingView(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        tvMyRank = view.findViewById(R.id.tvMyRank);
        tvMyKarma = view.findViewById(R.id.tvMyKarma);
        tvMyName = view.findViewById(R.id.tvMyName);
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.primary, null),
                getResources().getColor(R.color.secondary, null),
                getResources().getColor(R.color.accent, null)
        );
        swipeRefresh.setOnRefreshListener(() -> {
            loadLeaderboard();
            loadMyStats();
        });
    }

    private void loadLeaderboard() {
        String token = prefsManager.getToken();

        ApiClient.getUserApi().getLeaderboard("Bearer " + token)
                .enqueue(new Callback<ApiResponse<List<User>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            leaderboardList = response.body().getData();

                            if (leaderboardList != null && !leaderboardList.isEmpty()) {
                                // Sort by karma descending
                                leaderboardList.sort((u1, u2) -> Integer.compare(u2.getKarma(), u1.getKarma()));

                                adapter.setUsers(leaderboardList);
                                emptyState.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);

                                // Update my stats immediately after loading leaderboard
                                loadMyStats();
                            } else {
                                showEmptyState();
                            }
                        } else {
                            Toast.makeText(requireContext(), "Không thể tải bảng xếp hạng", Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
    }

    private void loadMyStats() {
        long userId = prefsManager.getUserId();
        String token = prefsManager.getToken();

        ApiClient.getUserApi().getUserById("Bearer " + token, userId)
                .enqueue(new Callback<ApiResponse<User>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            User user = response.body().getData();
                            if (user != null) {
                                updateMyStats(user);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                        // Ignore error for my stats
                    }
                });
    }

    private void updateMyStats(User user) {
        tvMyName.setText(user.getName());
        tvMyKarma.setText(String.valueOf(user.getKarma()));

        // Calculate rank
        int myRank = 0;
        for (int i = 0; i < leaderboardList.size(); i++) {
            if (leaderboardList.get(i).getId() == user.getId()) {
                myRank = i + 1;
                break;
            }
        }

        if (myRank > 0) {
            tvMyRank.setText("#" + myRank);
        } else {
            tvMyRank.setText("--");
        }
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLeaderboard();
        loadMyStats();
    }
}
