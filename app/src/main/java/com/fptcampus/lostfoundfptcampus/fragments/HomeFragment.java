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
import com.fptcampus.lostfoundfptcampus.controller.AddItemActivity;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.navigation.NavigationHost;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.card.MaterialCardView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {
    
    private NavigationHost navigationHost;
    private SharedPreferencesManager prefsManager;
    private AppDatabase database;
    private ExecutorService executorService;
    
    private TextView tvWelcome, tvKarma;
    private TextView tvMyLostCount, tvMyFoundCount, tvReturnedCount;
    private MaterialCardView cardReportLost, cardReportFound, cardViewMap;

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
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        prefsManager = new SharedPreferencesManager(requireContext());
        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        
        bindViews(view);
        setupClickListeners();
        loadUserInfo();
        loadStatistics();
    }

    private void bindViews(View view) {
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvKarma = view.findViewById(R.id.tvKarma);
        tvMyLostCount = view.findViewById(R.id.tvMyLostCount);
        tvMyFoundCount = view.findViewById(R.id.tvMyFoundCount);
        tvReturnedCount = view.findViewById(R.id.tvReturnedCount);
        cardReportLost = view.findViewById(R.id.cardReportLost);
        cardReportFound = view.findViewById(R.id.cardReportFound);
        cardViewMap = view.findViewById(R.id.cardViewMap);
    }

    private void setupClickListeners() {
        cardReportLost.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddItemActivity.class);
            intent.putExtra("STATUS", "lost");
            startActivity(intent);
        });

        cardReportFound.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddItemActivity.class);
            intent.putExtra("STATUS", "found");
            startActivity(intent);
        });

        cardViewMap.setOnClickListener(v -> {
            // Navigate to MapFragment
            if (navigationHost != null) {
                MapFragment mapFragment = new MapFragment();
                navigationHost.navigateTo(mapFragment, true);
            }
        });
    }

    private void loadUserInfo() {
        String userName = prefsManager.getUserName();
        int karma = prefsManager.getUserKarma();

        tvWelcome.setText("Xin chào, " + userName + "!");
        tvKarma.setText("⭐ Karma: " + karma + " điểm");
    }

    private void loadStatistics() {
        long userId = prefsManager.getUserId();
        String token = "Bearer " + prefsManager.getToken();
        
        // Load ALL items from API (không filter theo userId ở client)
        com.fptcampus.lostfoundfptcampus.model.api.ItemApi itemApi = 
            com.fptcampus.lostfoundfptcampus.util.ApiClient.getItemApi();
        
        // Get ALL items và filter theo 3 role fields
        retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call = 
            itemApi.getAllItems(token);
        
        call.enqueue(new retrofit2.Callback<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>>() {
            @Override
            public void onResponse(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call,
                retrofit2.Response<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> response) {
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem> allItems = response.body().getData();
                    
                    if (allItems != null) {
                        int myLostCount = 0;    // Items tôi BỊ MẤT (lostUserId)
                        int myFoundCount = 0;   // Items tôi TÌM THẤY (foundUserId)
                        int returnedCount = 0;  // Items tôi ĐÃ NHẬN LẠI (returnedUserId)
                        
                        for (com.fptcampus.lostfoundfptcampus.model.LostItem item : allItems) {
                            // Đếm items mà user là người MẤT đồ
                            if (item.getLostUserId() != null && item.getLostUserId() == userId) {
                                if ("lost".equalsIgnoreCase(item.getStatus())) {
                                    myLostCount++;
                                }
                            }
                            
                            // Đếm items mà user là người TÌM THẤY
                            if (item.getFoundUserId() != null && item.getFoundUserId() == userId) {
                                if ("found".equalsIgnoreCase(item.getStatus())) {
                                    myFoundCount++;
                                }
                            }
                            
                            // Đếm items mà user là người NHẬN LẠI (đã hoàn tất)
                            if (item.getReturnedUserId() != null && item.getReturnedUserId() == userId) {
                                if ("returned".equalsIgnoreCase(item.getStatus())) {
                                    returnedCount++;
                                }
                            }
                        }
                        
                        final int finalLostCount = myLostCount;
                        final int finalFoundCount = myFoundCount;
                        final int finalReturnedCount = returnedCount;
                        
                        android.util.Log.d("HomeFragment", "Statistics - Lost: " + finalLostCount + ", Found: " + finalFoundCount + ", Returned: " + finalReturnedCount);
                        
                        // Check if fragment is still attached before updating UI
                        if (isAdded() && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                tvMyLostCount.setText(String.valueOf(finalLostCount));
                                tvMyFoundCount.setText(String.valueOf(finalFoundCount));
                                tvReturnedCount.setText(String.valueOf(finalReturnedCount));
                            });
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call,
                Throwable t) {
                android.util.Log.e("HomeFragment", "Failed to load statistics", t);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
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
