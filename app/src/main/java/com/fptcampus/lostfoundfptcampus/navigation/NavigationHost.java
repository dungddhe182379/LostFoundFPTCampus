package com.fptcampus.lostfoundfptcampus.navigation;

import androidx.fragment.app.Fragment;

/**
 * NavigationHost interface for fragment communication
 * Implemented by MainActivity to handle fragment navigation
 */
public interface NavigationHost {
    
    /**
     * Navigate to a specific fragment
     * @param fragment The fragment to navigate to
     * @param addToBackStack Whether to add this transaction to back stack
     */
    void navigateTo(Fragment fragment, boolean addToBackStack);
    
    /**
     * Navigate to a specific tab by position
     * @param position The tab position (0-4)
     */
    void navigateToTab(int position);
    
    /**
     * Show a message to the user
     * @param message The message to display
     */
    void showMessage(String message);
    
    /**
     * Logout the current user
     */
    void logout();
    
    /**
     * Get current user ID
     * @return The current user's ID
     */
    int getCurrentUserId();
    
    /**
     * Get current username
     * @return The current username
     */
    String getCurrentUsername();
}
