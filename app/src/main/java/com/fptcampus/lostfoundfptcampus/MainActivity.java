package com.fptcampus.lostfoundfptcampus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fptcampus.lostfoundfptcampus.controller.LoginActivity;
import com.fptcampus.lostfoundfptcampus.navigation.NavigationHost;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.fptcampus.lostfoundfptcampus.util.SyncService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity implements NavigationHost {
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;

    private SharedPreferencesManager prefsManager;
    private SyncService syncService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ApiClient with context
        ApiClient.initialize(this);

        prefsManager = new SharedPreferencesManager(this);

        // Check if logged in
        if (!prefsManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        bindingView();
        setupBottomNavigation();
        setupBackPressedHandler();
        
        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadHomeFragment();
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }
        
        // Initialize sync service
        syncService = new SyncService(this);
        checkAndSyncOfflineItems();
    }
    
    private void setupBackPressedHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_leaderboard) {
                loadLeaderboardFragment();
                return true;
            } else if (itemId == R.id.navigation_items) {
                navigateToItemsList();
                return true;
            } else if (itemId == R.id.navigation_home) {
                loadHomeFragment();
                return true;
            } else if (itemId == R.id.navigation_qr) {
                loadQRFragment();
                return true;
            } else if (itemId == R.id.navigation_chat) {
                loadChatListFragment();
                return true;
            } else if (itemId == R.id.navigation_map) {
                navigateToMap();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                loadProfileFragment();
                return true;
            }
            
            return false;
        });
    }
    
    private void loadHomeFragment() {
        Fragment homeFragment = new com.fptcampus.lostfoundfptcampus.fragments.HomeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, homeFragment)
                .commit();
    }

    private void loadLeaderboardFragment() {
        Fragment leaderboardFragment = new com.fptcampus.lostfoundfptcampus.fragments.LeaderboardFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, leaderboardFragment)
                .commit();
    }
    
    private void loadReportFragment() {
        Fragment reportFragment = new com.fptcampus.lostfoundfptcampus.fragments.ReportItemFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, reportFragment)
                .commit();
    }

    private void loadProfileFragment() {
        Fragment profileFragment = new com.fptcampus.lostfoundfptcampus.fragments.ProfileFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, profileFragment)
                .commit();
    }

    private void loadItemsFragment() {
        Fragment itemsFragment = new com.fptcampus.lostfoundfptcampus.fragments.ItemsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, itemsFragment)
                .commit();
    }

    private void navigateToItemsList() {
        // Use fragment instead of activity
        loadItemsFragment();
    }

    private void loadQRFragment() {
        Fragment qrFragment = new com.fptcampus.lostfoundfptcampus.fragments.QRFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, qrFragment)
                .commit();
    }

    private void loadChatListFragment() {
        Fragment chatListFragment = new com.fptcampus.lostfoundfptcampus.fragments.ChatListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, chatListFragment)
                .commit();
    }

    private void loadMapFragment() {
        Fragment mapFragment = new com.fptcampus.lostfoundfptcampus.fragments.MapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, mapFragment)
                .commit();
    }

    private void navigateToMap() {
        // Use fragment instead of activity
        loadMapFragment();
    }

    private void navigateToQR() {
        // Use fragment instead of activity
        loadQRFragment();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // NavigationHost implementation
    @Override
    public void navigateTo(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void navigateToTab(int position) {
        if (position >= 0 && position < bottomNavigation.getMenu().size()) {
            bottomNavigation.setSelectedItemId(
                    bottomNavigation.getMenu().getItem(position).getItemId()
            );
        }
    }

    @Override
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void logout() {
        prefsManager.clearAll();
        ApiClient.reset();
        navigateToLogin();
    }

    @Override
    public int getCurrentUserId() {
        return (int) prefsManager.getUserId();
    }

    @Override
    public String getCurrentUsername() {
        return prefsManager.getUserName();
    }

    private void checkAndSyncOfflineItems() {
        // Check network first
        if (!com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(this)) {
            return; // Don't show sync prompt if no network
        }
        
        syncService.hasUnsyncedItems((hasUnsynced, count) -> {
            if (hasUnsynced) {
                runOnUiThread(() -> {
                    Snackbar snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        "Có " + count + " bài đăng chưa đồng bộ",
                        Snackbar.LENGTH_LONG
                    );
                    snackbar.setAction("Đồng bộ ngay", v -> syncOfflineItems());
                    snackbar.show();
                });
            }
        });
    }
    
    private void syncOfflineItems() {
        View rootView = findViewById(android.R.id.content);
        
        // Check network again before sync
        if (!com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(this)) {
            Snackbar.make(rootView, "Không có kết nối mạng. Vui lòng kiểm tra lại.", 
                Snackbar.LENGTH_LONG).show();
            return;
        }
        
        Snackbar.make(rootView, "Đang đồng bộ...", Snackbar.LENGTH_SHORT).show();
        
        syncService.syncUnsyncedItems(new SyncService.SyncCallback() {
            @Override
            public void onSyncComplete(int successCount, int failCount) {
                runOnUiThread(() -> {
                    String message;
                    if (successCount > 0 && failCount == 0) {
                        message = "Đã đồng bộ thành công " + successCount + " bài đăng!";
                    } else if (successCount > 0 && failCount > 0) {
                        message = "Đồng bộ: " + successCount + " thành công, " + failCount + " thất bại";
                    } else {
                        message = "Đồng bộ thất bại. Vui lòng thử lại sau.";
                    }
                    
                    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
                });
            }

            @Override
            public void onSyncProgress(String itemTitle) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Đã đồng bộ: " + itemTitle, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndSyncOfflineItems();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (syncService != null) {
            syncService.shutdown();
        }
    }
}