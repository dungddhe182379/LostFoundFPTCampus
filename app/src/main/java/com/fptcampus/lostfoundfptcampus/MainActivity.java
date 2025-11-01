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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextView tvWelcome, tvKarma;
    private MaterialCardView cardItems, cardMap, cardQR, cardLeaderboard;
    private MaterialButton btnLogout;

    private SharedPreferencesManager prefsManager;

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

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }
}