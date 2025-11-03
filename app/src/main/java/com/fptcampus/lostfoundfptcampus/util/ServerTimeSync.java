package com.fptcampus.lostfoundfptcampus.util;

import android.util.Log;

/**
 * Utility class to sync client time with server time
 * Helps avoid timezone issues when generating QR tokens
 */
public class ServerTimeSync {
    private static final String TAG = "ServerTimeSync";
    
    // Server time offset in milliseconds (server time - client time)
    private static long serverTimeOffset = 0;
    
    // Last sync timestamp
    private static long lastSyncTime = 0;
    
    // Sync validity period (5 minutes)
    private static final long SYNC_VALIDITY_MS = 5 * 60 * 1000;
    
    /**
     * Update server time offset from API response
     * @param serverTimestamp timestamp from API response
     */
    public static void updateServerTime(long serverTimestamp) {
        long clientTime = System.currentTimeMillis();
        serverTimeOffset = serverTimestamp - clientTime;
        lastSyncTime = clientTime;
        
        Log.d(TAG, "Server time synced:");
        Log.d(TAG, "  Client time: " + clientTime);
        Log.d(TAG, "  Server time: " + serverTimestamp);
        Log.d(TAG, "  Offset: " + serverTimeOffset + "ms (" + (serverTimeOffset / 1000.0 / 3600.0) + " hours)");
    }
    
    /**
     * Get current server time (synced)
     * @return server timestamp in milliseconds
     */
    public static long getServerTime() {
        long currentTime = System.currentTimeMillis();
        
        // Check if sync is still valid
        if (lastSyncTime > 0 && (currentTime - lastSyncTime) > SYNC_VALIDITY_MS) {
            Log.w(TAG, "Server time sync is outdated (>" + (SYNC_VALIDITY_MS / 1000 / 60) + " minutes)");
        }
        
        return currentTime + serverTimeOffset;
    }
    
    /**
     * Check if server time is synced
     * @return true if synced within validity period
     */
    public static boolean isSynced() {
        if (lastSyncTime == 0) return false;
        
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSyncTime) <= SYNC_VALIDITY_MS;
    }
    
    /**
     * Get server time offset in milliseconds
     * @return offset (server time - client time)
     */
    public static long getServerTimeOffset() {
        return serverTimeOffset;
    }
    
    /**
     * Get server time offset in hours
     * @return offset in hours
     */
    public static double getServerTimeOffsetHours() {
        return serverTimeOffset / 1000.0 / 3600.0;
    }
    
    /**
     * Reset server time sync
     */
    public static void reset() {
        serverTimeOffset = 0;
        lastSyncTime = 0;
        Log.d(TAG, "Server time sync reset");
    }
}
