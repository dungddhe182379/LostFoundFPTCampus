package com.fptcampus.lostfoundfptcampus.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fptcampus.lostfoundfptcampus.R;
import com.fptcampus.lostfoundfptcampus.controller.AddItemActivity;
import com.fptcampus.lostfoundfptcampus.controller.adapter.ItemAdapter;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.util.ApiClient;
import com.fptcampus.lostfoundfptcampus.util.SharedPreferencesManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemsFragment extends Fragment {
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabAdd;
    private LinearLayout emptyState;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupDateFilter;
    private Chip chipToday, chipThisWeek, chipThisMonth, chipAll;
    private MaterialCardView btnToggleSearchFilter;
    private LinearLayout searchFilterContent;
    private ImageView ivExpandCollapse;
    private boolean isSearchFilterExpanded = false;

    private ItemAdapter adapter;
    private AppDatabase database;
    private ExecutorService executorService;
    private SharedPreferencesManager prefsManager;
    private com.fptcampus.lostfoundfptcampus.navigation.NavigationHost navigationHost;
    
    private String currentStatus = "all";
    private String searchQuery = "";
    private String dateFilter = "all"; // "today", "week", "month", "all"
    private List<LostItem> allItemsCache = new ArrayList<>();

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) {
            navigationHost = (com.fptcampus.lostfoundfptcampus.navigation.NavigationHost) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindingView(view);
        bindingAction();
        
        database = AppDatabase.getInstance(requireContext());
        executorService = Executors.newSingleThreadExecutor();
        prefsManager = new SharedPreferencesManager(requireContext());

        setupRecyclerView();
        setupSwipeRefresh();
        loadItemsFromApi();
    }

    private void bindingView(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        fabAdd = view.findViewById(R.id.fabAdd);
        emptyState = view.findViewById(R.id.emptyState);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroupDateFilter = view.findViewById(R.id.chipGroupDateFilter);
        chipToday = view.findViewById(R.id.chipToday);
        chipThisWeek = view.findViewById(R.id.chipThisWeek);
        chipThisMonth = view.findViewById(R.id.chipThisMonth);
        chipAll = view.findViewById(R.id.chipAll);
        
        // Collapsible search/filter views
        btnToggleSearchFilter = view.findViewById(R.id.btnToggleSearchFilter);
        searchFilterContent = view.findViewById(R.id.searchFilterContent);
        ivExpandCollapse = view.findViewById(R.id.ivExpandCollapse);
    }

    private void bindingAction() {
        // Toggle Search/Filter button
        btnToggleSearchFilter.setOnClickListener(v -> toggleSearchFilter());
        
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddItemActivity.class);
            startActivity(intent);
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                onTabChanged(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Search text change listener
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Date filter chip listeners
        chipGroupDateFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                dateFilter = "all";
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipToday) {
                    dateFilter = "today";
                } else if (checkedId == R.id.chipThisWeek) {
                    dateFilter = "week";
                } else if (checkedId == R.id.chipThisMonth) {
                    dateFilter = "month";
                } else {
                    dateFilter = "all";
                }
            }
            applyFilters();
        });
    }

    private void toggleSearchFilter() {
        isSearchFilterExpanded = !isSearchFilterExpanded;
        
        if (isSearchFilterExpanded) {
            // Expand with animation
            searchFilterContent.setVisibility(View.VISIBLE);
            
            // Animate arrow rotation
            ivExpandCollapse.animate()
                .rotation(180)
                .setDuration(200)
                .start();
        } else {
            // Collapse with animation
            searchFilterContent.setVisibility(View.GONE);
            
            // Animate arrow rotation
            ivExpandCollapse.animate()
                .rotation(0)
                .setDuration(200)
                .start();
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
            getResources().getColor(R.color.primary, null),
            getResources().getColor(R.color.secondary, null),
            getResources().getColor(R.color.accent, null)
        );
        swipeRefresh.setOnRefreshListener(this::loadItemsFromApi);
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter();
        adapter.setOnItemClickListener(item -> {
            // Navigate to DetailItemFragment
            if (navigationHost != null) {
                DetailItemFragment detailFragment = DetailItemFragment.newInstance(item);
                navigationHost.navigateTo(detailFragment, true);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void onTabChanged(int position) {
        switch (position) {
            case 0:
                currentStatus = "all";
                break;
            case 1:
                currentStatus = "lost";
                break;
            case 2:
                currentStatus = "found";
                break;
            case 3:
                currentStatus = "returned";
                break;
        }
        applyFilters();
    }

    private void loadItemsFromApi() {
        // Check network first
        if (!com.fptcampus.lostfoundfptcampus.util.NetworkUtil.isNetworkAvailable(requireContext())) {
            swipeRefresh.setRefreshing(false);
            if (isAdded()) {
                Toast.makeText(requireContext(), "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            }
            loadItemsFromLocal();
            return;
        }
        
        String token = "Bearer " + prefsManager.getToken();
        
        // Load all items from API
        Call<ApiResponse<List<LostItem>>> call = ApiClient.getItemApi().getAllItems(token);

        call.enqueue(new Callback<ApiResponse<List<LostItem>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<LostItem>>> call, Response<ApiResponse<List<LostItem>>> response) {
                if (!isAdded() || getActivity() == null) return;
                
                swipeRefresh.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<LostItem>> apiResponse = response.body();
                    
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        allItemsCache = apiResponse.getData();
                        
                        requireActivity().runOnUiThread(() -> {
                            applyFilters();
                        });
                        
                        // Save to local database for offline access
                        executorService.execute(() -> {
                            for (LostItem item : allItemsCache) {
                                item.setSynced(true);
                                database.lostItemDao().insert(item);
                            }
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), 
                                "Lỗi: " + (apiResponse.getError() != null ? apiResponse.getError() : "Không có dữ liệu"), 
                                Toast.LENGTH_SHORT).show();
                            loadItemsFromLocal();
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                            "Lỗi tải dữ liệu (Code: " + response.code() + ")", 
                            Toast.LENGTH_SHORT).show();
                        loadItemsFromLocal();
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<LostItem>>> call, Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                
                swipeRefresh.setRefreshing(false);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), 
                        "Lỗi kết nối: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    loadItemsFromLocal();
                });
            }
        });
    }

    private void loadItemsFromLocal() {
        executorService.execute(() -> {
            List<LostItem> items = database.lostItemDao().getAllItems();
            
            if (isAdded() && getActivity() != null) {
                allItemsCache = items;
                requireActivity().runOnUiThread(() -> {
                    applyFilters();
                });
            }
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Apply all filters: status, search query, and date
     */
    private void applyFilters() {
        List<LostItem> filteredItems = new ArrayList<>();

        for (LostItem item : allItemsCache) {
            // Filter by status
            if (!"all".equals(currentStatus) && !currentStatus.equalsIgnoreCase(item.getStatus())) {
                continue;
            }

            // Filter by search query
            if (!searchQuery.isEmpty()) {
                String query = searchQuery.toLowerCase();
                String title = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                String description = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                String category = item.getCategory() != null ? item.getCategory().toLowerCase() : "";

                if (!title.contains(query) && !description.contains(query) && !category.contains(query)) {
                    continue;
                }
            }

            // Filter by date
            if (!"all".equals(dateFilter)) {
                Date itemDate = item.getCreatedAt();
                if (itemDate != null && !isWithinDateRange(itemDate, dateFilter)) {
                    continue;
                }
            }

            filteredItems.add(item);
        }

        adapter.setItems(filteredItems);
        updateEmptyState(filteredItems.isEmpty());
    }

    /**
     * Check if date is within the specified range
     */
    private boolean isWithinDateRange(Date itemDate, String range) {
        Calendar itemCal = Calendar.getInstance();
        itemCal.setTime(itemDate);

        Calendar now = Calendar.getInstance();

        switch (range) {
            case "today":
                // Same day, month, and year
                return itemCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                       itemCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

            case "week":
                // Within current week (Sunday to Saturday)
                Calendar weekStart = (Calendar) now.clone();
                weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
                weekStart.set(Calendar.HOUR_OF_DAY, 0);
                weekStart.set(Calendar.MINUTE, 0);
                weekStart.set(Calendar.SECOND, 0);
                weekStart.set(Calendar.MILLISECOND, 0);

                Calendar weekEnd = (Calendar) weekStart.clone();
                weekEnd.add(Calendar.DAY_OF_WEEK, 7);

                return itemDate.getTime() >= weekStart.getTimeInMillis() &&
                       itemDate.getTime() < weekEnd.getTimeInMillis();

            case "month":
                // Same month and year
                return itemCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                       itemCal.get(Calendar.MONTH) == now.get(Calendar.MONTH);

            default:
                return true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItemsFromApi();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
