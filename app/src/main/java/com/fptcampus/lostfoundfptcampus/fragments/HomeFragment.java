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
    private TextView tvMyLostCount, tvMyFoundCount, tvGivenBackCount, tvReceivedBackCount;
    private MaterialCardView cardReport, cardViewMap, cardQRScan;

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
        tvGivenBackCount = view.findViewById(R.id.tvGivenBackCount);  // Đã trả (tôi nhặt và trả)
        tvReceivedBackCount = view.findViewById(R.id.tvReceivedBackCount);  // Đã nhận (tôi mất và nhận lại)
        cardReport = view.findViewById(R.id.cardReport);
        cardViewMap = view.findViewById(R.id.cardViewMap);
        cardQRScan = view.findViewById(R.id.cardQRScan);
    }

    private void setupClickListeners() {
        cardReport.setOnClickListener(v -> {
            // Navigate to ReportItemFragment
            if (navigationHost != null) {
                ReportItemFragment reportFragment = new ReportItemFragment();
                navigationHost.navigateTo(reportFragment, true);
            }
        });

        cardViewMap.setOnClickListener(v -> {
            // Navigate to MapFragment
            if (navigationHost != null) {
                MapFragment mapFragment = new MapFragment();
                navigationHost.navigateTo(mapFragment, true);
            }
        });

        cardQRScan.setOnClickListener(v -> {
            // Navigate to QRFragment
            if (navigationHost != null) {
                QRFragment qrFragment = new QRFragment();
                navigationHost.navigateTo(qrFragment, true);
            }
        });
    }

    private void loadUserInfo() {
        // Load from API first, fallback to SharedPreferences
        String token = "Bearer " + prefsManager.getToken();
        long userId = prefsManager.getUserId();
        
        // WORKAROUND: Use getUserById instead of getProfile because backend returns wrong user
        com.fptcampus.lostfoundfptcampus.util.ApiClient.getUserApi().getUserById(token, userId)
            .enqueue(new retrofit2.Callback<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>>() {
                @Override
                public void onResponse(
                    retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call,
                    retrofit2.Response<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> response) {
                    
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        com.fptcampus.lostfoundfptcampus.model.User user = response.body().getData();
                        
                        // Update SharedPreferences with latest data from API
                        prefsManager.saveUserName(user.getName());
                        prefsManager.saveUserEmail(user.getEmail());
                        prefsManager.saveUserKarma(user.getKarma());
                        
                        if (isAdded() && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                tvWelcome.setText("Xin chào, " + user.getName() + "!");
                                tvKarma.setText("⭐ Karma: " + user.getKarma() + " điểm");
                            });
                        }
                        

                    } else {
                        // Fallback to SharedPreferences if API fails
                        loadUserInfoFromPrefs();
                    }
                }
                
                @Override
                public void onFailure(
                    retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<com.fptcampus.lostfoundfptcampus.model.User>> call,
                    Throwable t) {

                    // Fallback to SharedPreferences
                    loadUserInfoFromPrefs();
                }
            });
    }
    
    private void loadUserInfoFromPrefs() {
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
                        int myLostCount = 0;        // Items tôi BỊ MẤT và chưa tìm thấy
                        int myFoundCount = 0;       // Items tôi NHẶT ĐƯỢC và chưa trả
                        int givenBackCount = 0;     // Items tôi NHẶT và ĐÃ TRẢ cho chủ
                        int receivedBackCount = 0;  // Items tôi MẤT và ĐÃ NHẬN LẠI
                        
                        for (com.fptcampus.lostfoundfptcampus.model.LostItem item : allItems) {
                            String status = item.getStatus() != null ? item.getStatus().toLowerCase() : "";
                            
                            // LOGIC CHUẨN - PHÂN LOẠI RÕ RÀNG:
                            
                            // 1. Đã mất = lostUserId=tôi VÀ status="lost" (chưa ai nhặt)
                            if ("lost".equals(status) && item.getLostUserId() != null && item.getLostUserId() == userId) {
                                myLostCount++;
                            }
                            
                            // 2. Đã nhặt = foundUserId=tôi VÀ status="found" (nhặt rồi nhưng chưa trả)
                            else if ("found".equals(status) && item.getFoundUserId() != null && item.getFoundUserId() == userId) {
                                myFoundCount++;
                            }
                            
                            // 3. Đã trả = foundUserId=tôi VÀ status="returned" (tôi nhặt và đã trả cho người khác)
                            else if ("returned".equals(status) && item.getFoundUserId() != null && item.getFoundUserId() == userId) {
                                givenBackCount++;
                            }
                            
                            // 4. Đã nhận = lostUserId=tôi VÀ returnedUserId=tôi VÀ status="returned" (tôi mất và đã nhận lại)
                            else if ("returned".equals(status) && 
                                     item.getLostUserId() != null && item.getLostUserId() == userId &&
                                     item.getReturnedUserId() != null && item.getReturnedUserId() == userId) {
                                receivedBackCount++;
                            }
                        }
                        
                        final int finalLostCount = myLostCount;
                        final int finalFoundCount = myFoundCount;
                        final int finalGivenBackCount = givenBackCount;
                        final int finalReceivedBackCount = receivedBackCount;
                        

                        
                        // Check if fragment is still attached before updating UI
                        if (isAdded() && getActivity() != null) {
                            requireActivity().runOnUiThread(() -> {
                                tvMyLostCount.setText(String.valueOf(finalLostCount));
                                tvMyFoundCount.setText(String.valueOf(finalFoundCount));
                                tvGivenBackCount.setText(String.valueOf(finalGivenBackCount));
                                tvReceivedBackCount.setText(String.valueOf(finalReceivedBackCount));
                            });
                        }
                    }
                }
            }
            
            @Override
            public void onFailure(
                retrofit2.Call<com.fptcampus.lostfoundfptcampus.model.api.ApiResponse<java.util.List<com.fptcampus.lostfoundfptcampus.model.LostItem>>> call,
                Throwable t) {

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
