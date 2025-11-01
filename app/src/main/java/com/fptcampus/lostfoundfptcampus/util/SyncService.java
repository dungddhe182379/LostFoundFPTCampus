package com.fptcampus.lostfoundfptcampus.util;

import android.content.Context;
import android.util.Log;

import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.api.ApiResponse;
import com.fptcampus.lostfoundfptcampus.model.database.AppDatabase;
import com.fptcampus.lostfoundfptcampus.model.dto.CreateItemRequest;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Service to sync offline items to server
 */
public class SyncService {
    private static final String TAG = "SyncService";
    private final Context context;
    private final AppDatabase database;
    private final SharedPreferencesManager prefsManager;
    private final ExecutorService executorService;

    public interface SyncCallback {
        void onSyncComplete(int successCount, int failCount);
        void onSyncProgress(String itemTitle);
    }

    public SyncService(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.prefsManager = new SharedPreferencesManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Sync all unsynced items to server
     */
    public void syncUnsyncedItems(SyncCallback callback) {
        executorService.execute(() -> {
            List<LostItem> unsyncedItems = database.lostItemDao().getUnsyncedItems();
            
            if (unsyncedItems == null || unsyncedItems.isEmpty()) {
                Log.d(TAG, "No unsynced items to sync");
                if (callback != null) {
                    callback.onSyncComplete(0, 0);
                }
                return;
            }

            Log.d(TAG, "Found " + unsyncedItems.size() + " unsynced items");

            final int[] successCount = {0};
            final int[] failCount = {0};
            final int totalItems = unsyncedItems.size();

            for (LostItem item : unsyncedItems) {
                syncSingleItem(item, new SingleItemSyncCallback() {
                    @Override
                    public void onSuccess(LostItem serverItem) {
                        successCount[0]++;
                        
                        if (callback != null) {
                            callback.onSyncProgress(item.getTitle());
                        }
                        
                        // Check if all items processed
                        if (successCount[0] + failCount[0] == totalItems) {
                            if (callback != null) {
                                callback.onSyncComplete(successCount[0], failCount[0]);
                            }
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        failCount[0]++;
                        Log.e(TAG, "Failed to sync item: " + item.getTitle() + " - " + error);
                        
                        // Check if all items processed
                        if (successCount[0] + failCount[0] == totalItems) {
                            if (callback != null) {
                                callback.onSyncComplete(successCount[0], failCount[0]);
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Sync a single item to server
     */
    private void syncSingleItem(LostItem item, SingleItemSyncCallback callback) {
        String token = prefsManager.getToken();
        
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "No token available for sync");
            if (callback != null) {
                callback.onFailure("No authentication token");
            }
            return;
        }

        // Create DTO - Server generates uuid and userId from token
        CreateItemRequest request = new CreateItemRequest(
            item.getTitle(),
            item.getDescription(),
            item.getCategory(),
            item.getStatus(),
            item.getLatitude(),
            item.getLongitude(),
            item.getImageUrl()
        );

        Call<ApiResponse<LostItem>> call = ApiClient.getItemApi().createItem(
            "Bearer " + token,
            request
        );

        call.enqueue(new Callback<ApiResponse<LostItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<LostItem>> call, Response<ApiResponse<LostItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LostItem> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        LostItem serverItem = apiResponse.getData();
                        
                        // Update local database
                        executorService.execute(() -> {
                            // Delete old local item with temporary ID
                            database.lostItemDao().delete(item);
                            
                            // Insert server item with real ID
                            serverItem.setSynced(true);
                            database.lostItemDao().insert(serverItem);
                            
                            Log.d(TAG, "Successfully synced: " + item.getTitle());
                        });

                        if (callback != null) {
                            callback.onSuccess(serverItem);
                        }
                    } else {
                        String error = apiResponse.getError() != null ? apiResponse.getError() : "Unknown error";
                        if (callback != null) {
                            callback.onFailure(error);
                        }
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure("HTTP " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LostItem>> call, Throwable t) {
                if (callback != null) {
                    callback.onFailure(t.getMessage());
                }
            }
        });
    }

    /**
     * Check if there are unsynced items
     */
    public void hasUnsyncedItems(HasUnsyncedCallback callback) {
        executorService.execute(() -> {
            List<LostItem> unsyncedItems = database.lostItemDao().getUnsyncedItems();
            int count = unsyncedItems != null ? unsyncedItems.size() : 0;
            
            if (callback != null) {
                callback.onResult(count > 0, count);
            }
        });
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private interface SingleItemSyncCallback {
        void onSuccess(LostItem serverItem);
        void onFailure(String error);
    }

    public interface HasUnsyncedCallback {
        void onResult(boolean hasUnsynced, int count);
    }
}
