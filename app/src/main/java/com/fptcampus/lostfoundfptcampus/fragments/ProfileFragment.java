package com.fptcampus.lostfoundfptcampus.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.LeaderboardActivity;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.navigation.NavigationHost;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {
    
    private NavigationHost navigationHost;
    private SharedPreferencesManager prefsManager;
    private AppDatabase database;
    private ExecutorService executorService;
    
    private TextView tvAvatarInitial, tvUserName, tvUserEmail, tvKarmaScore, tvTotalItems;
    private MaterialCardView cardMyItems, cardLeaderboard;
    private MaterialButton btnLogout;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationHost) {
            navigationHost = (NavigationHost) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefsManager = new SharedPreferencesManager(requireContext());
        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        
        bindViews(view);
        setupClickListeners();
        loadUserProfile();
        loadStatistics();
    }

    private void bindViews(View view) {
        tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvKarmaScore = view.findViewById(R.id.tvKarmaScore);
        tvTotalItems = view.findViewById(R.id.tvTotalItems);
        cardMyItems = view.findViewById(R.id.cardMyItems);
        cardLeaderboard = view.findViewById(R.id.cardLeaderboard);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        cardMyItems.setOnClickListener(v -> {
            // Navigate to MyItemsFragment
            if (navigationHost != null) {
                MyItemsFragment myItemsFragment = new MyItemsFragment();
                navigationHost.navigateTo(myItemsFragment, true);
            }
        });

        cardLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LeaderboardActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            if (navigationHost != null) {
                navigationHost.logout();
            }
        });
    }

    private void loadUserProfile() {
        String token = "Bearer " + prefsManager.getToken();
        
        // Call API first to get fresh data
        com.fptcampus.lostfoundfptcampus.model.api.UserApi userApi = 
            com.fptcampus.lostfoundfptcampus.util.ApiClient.getUserApi();
        
        retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call = 
            userApi.getProfile(token);
        
        call.enqueue(new retrofit2.Callback<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
            @Override
            public void onResponse(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call,
                retrofit2.Response<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.fptcampus.lostfoundfptcampus.model.User user = response.body().getData();
                    
                    // Update SharedPreferences with fresh data
                    prefsManager.saveUserName(user.getName());
                    prefsManager.saveUserEmail(user.getEmail());
                    prefsManager.saveUserKarma(user.getKarma());
                    
                    // Update UI
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            updateUIWithUserData(user.getName(), user.getEmail(), user.getKarma());
                        });
                    }
                    

                } else {

                    loadUserProfileFromPrefs();
                }
            }
            
            @Override
            public void onFailure(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call,
                Throwable t) {

                loadUserProfileFromPrefs();
            }
        });
    }
    
    private void loadUserProfileFromPrefs() {
        String userName = prefsManager.getUserName();
        String userEmail = prefsManager.getUserEmail();
        int karma = prefsManager.getUserKarma();
        
        if (isAdded() && getActivity() != null) {
            requireActivity().runOnUiThread(() -> {
                updateUIWithUserData(userName, userEmail, karma);
            });
        }
    }
    
    private void updateUIWithUserData(String userName, String userEmail, int karma) {
        // Get first letter of name for avatar
        String initial = "U";
        if (userName != null && !userName.isEmpty()) {
            initial = userName.substring(0, 1).toUpperCase();
        }

        tvAvatarInitial.setText(initial);
        tvUserName.setText(userName);
        tvUserEmail.setText(userEmail);
        tvKarmaScore.setText(String.valueOf(karma));
    }

    private void loadStatistics() {
        long userId = prefsManager.getUserId();
        String token = "Bearer " + prefsManager.getToken();
        
        // Load ALL items from API
        com.fptcampus.lostfoundfptcampus.model.api.ItemApi itemApi = 
            com.fptcampus.lostfoundfptcampus.util.ApiClient.getItemApi();
        
        // Get ALL items và filter theo 3 role fields (lostUserId, foundUserId, returnedUserId)
        retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call = 
            itemApi.getAllItems(token);
        
        call.enqueue(new retrofit2.Callback<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>>() {
            @Override
            public void onResponse(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call,
                retrofit2.Response<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> response) {
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem> allItems = response.body().getData();
                    
                    // Đếm tất cả items liên quan đến user
                    // LOGIC CHUẨN: Chỉ đếm items mà user có vai trò chính (lostUserId hoặc foundUserId)
                    int totalItems = 0;
                    if (allItems != null) {
                        java.util.Set<Long> countedItemIds = new java.util.HashSet<>();
                        
                        for (com.fptcampus.lostfoundfptcampus.model.LostItem item : allItems) {
                            // Chỉ đếm items mà user là người MẤT hoặc người NHẶT
                            // Không đếm riêng returnedUserId vì nó đã được tính trong lostUserId/foundUserId
                            boolean isMyLostItem = (item.getLostUserId() != null && item.getLostUserId() == userId);
                            boolean isMyFoundItem = (item.getFoundUserId() != null && item.getFoundUserId() == userId);
                            
                            if ((isMyLostItem || isMyFoundItem) && !countedItemIds.contains(item.getId())) {
                                totalItems++;
                                countedItemIds.add(item.getId());
                            }
                        }
                    }
                    
                    final int finalTotalItems = totalItems;
                    

                    
                    // Check if fragment is still attached before updating UI
                    if (isAdded() && getActivity() != null) {
                        requireActivity().runOnUiThread(() -> {
                            tvTotalItems.setText(finalTotalItems + " đồ vật");
                        });
                    }
                }
            }
            
            @Override
            public void onFailure(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call,
                Throwable t) {

                // Check if fragment is still attached before updating UI
                if (isAdded() && getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        tvTotalItems.setText("0 đồ vật");
                    });
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        loadStatistics();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
