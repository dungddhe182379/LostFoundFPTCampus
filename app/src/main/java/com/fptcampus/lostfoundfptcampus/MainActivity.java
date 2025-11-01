package com.fptcampus.lostfoundfptcampus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fptcampus.lostfoundfptcampus.controller.AddItemActivity;
import com.fptcampus.lostfoundfptcampus.controller.ListItemActivity;
import com.fptcampus.lostfoundfptcampus.controller.LoginActivity;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.fptcampus.lostfoundfptcampus.util.SyncService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvWelcome, tvKarma;
    private MaterialCardView cardItems, cardMap, cardQR, cardLeaderboard;
    private MaterialButton btnLogout;

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
        bindingAction();
        loadUserInfo();
        
        // Initialize sync service
        syncService = new SyncService(this);
        checkAndSyncOfflineItems();
    }

    private void bindingView() {
        toolbar = findViewById(R.id.toolbar);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvKarma = findViewById(R.id.tvKarma);
        cardItems = findViewById(R.id.cardItems);
        cardMap = findViewById(R.id.cardMap);
        cardQR = findViewById(R.id.cardQR);
        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void bindingAction() {
        cardItems.setOnClickListener(this::onCardItemsClick);
        cardMap.setOnClickListener(this::onCardMapClick);
        cardQR.setOnClickListener(this::onCardQRClick);
        cardLeaderboard.setOnClickListener(this::onCardLeaderboardClick);
        btnLogout.setOnClickListener(this::onBtnLogoutClick);
    }

    private void loadUserInfo() {
        String userName = prefsManager.getUserName();
        int karma = prefsManager.getUserKarma();

        tvWelcome.setText("Xin chào, " + userName + "!");
        tvKarma.setText("Karma: " + karma + " điểm");
    }

    private void onCardItemsClick(View view) {
        Intent intent = new Intent(this, ListItemActivity.class);
        startActivity(intent);
    }

    private void onCardMapClick(View view) {
        Intent intent = new Intent(this, com.fptcampus.lostfoundfptcampus.controller.MapActivity.class);
        startActivity(intent);
    }

    private void onCardQRClick(View view) {
        Intent intent = new Intent(this, com.fptcampus.lostfoundfptcampus.controller.QrScanActivity.class);
        startActivity(intent);
    }

    private void onCardLeaderboardClick(View view) {
        Intent intent = new Intent(this, com.fptcampus.lostfoundfptcampus.controller.LeaderboardActivity.class);
        startActivity(intent);
    }

    private void onBtnLogoutClick(View view) {
        prefsManager.clearAll();
        ApiClient.reset();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
        loadUserInfo();
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