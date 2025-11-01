package com.fptcampus.lostfoundfptcampus.controller.adapter;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.User;

/**
 * Leaderboard ViewHolder - Bind user data for ranking
 * Following ViewHolder pattern from lostfound_project_summary.md
 */
public class LeaderboardViewHolder extends RecyclerView.ViewHolder {
    private TextView tvRank, tvUserName, tvUserEmail, tvKarma;

    public LeaderboardViewHolder(@NonNull View itemView) {
        super(itemView);
        bindingView();
    }

    private void bindingView() {
        tvRank = itemView.findViewById(R.id.tvRank);
        tvUserName = itemView.findViewById(R.id.tvUserName);
        tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
        tvKarma = itemView.findViewById(R.id.tvKarma);
    }

    public void bind(User user, int rank) {
        tvRank.setText(String.valueOf(rank));
        tvUserName.setText(user.getName());
        tvUserEmail.setText(user.getEmail());
        tvKarma.setText(String.valueOf(user.getKarma()));

        // Highlight top 3 with different colors
        int bgColor;
        if (rank == 1) {
            bgColor = 0xFFFF9800; // Orange
        } else if (rank == 2) {
            bgColor = 0xFF9E9E9E; // Grey
        } else if (rank == 3) {
            bgColor = 0xFFFFCC80; // Light Orange
        } else {
            bgColor = 0xFF607D8B; // Blue Grey
        }
        tvRank.setBackgroundColor(bgColor);
    }
}
