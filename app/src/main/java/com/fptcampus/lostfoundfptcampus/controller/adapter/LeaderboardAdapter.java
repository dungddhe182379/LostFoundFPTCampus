package com.fptcampus.lostfoundfptcampus.controller.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.model.User;

import java.util.List;

/**
 * Leaderboard Adapter - Display users in ranking
 * Following RecyclerView pattern from lostfound_project_summary.md
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardViewHolder> {
    private final List<User> userList;

    public LeaderboardAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LeaderboardViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_leaderboard, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        holder.bind(userList.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setItems(List<User> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }
}
