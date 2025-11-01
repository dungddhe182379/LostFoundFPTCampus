package com.fptcampus.lostfoundfptcampus.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fptcampus.lostfoundfptcampus.model.LostItem;

import java.util.List;

@Dao
public interface LostItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LostItem item);

    @Update
    void update(LostItem item);

    @Delete
    void delete(LostItem item);

    @Query("SELECT * FROM items WHERE id = :itemId")
    LostItem getItemById(long itemId);

    @Query("SELECT * FROM items WHERE uuid = :uuid LIMIT 1")
    LostItem getItemByUuid(String uuid);

    @Query("SELECT * FROM items ORDER BY created_at DESC")
    List<LostItem> getAllItems();

    @Query("SELECT * FROM items WHERE user_id = :userId ORDER BY created_at DESC")
    List<LostItem> getItemsByUserId(long userId);

    @Query("SELECT * FROM items WHERE status = :status ORDER BY created_at DESC")
    List<LostItem> getItemsByStatus(String status);

    @Query("SELECT * FROM items WHERE category = :category ORDER BY created_at DESC")
    List<LostItem> getItemsByCategory(String category);

    @Query("SELECT * FROM items WHERE title LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    List<LostItem> searchItems(String keyword);

    @Query("SELECT * FROM items WHERE synced = 0")
    List<LostItem> getUnsyncedItems();

    @Query("UPDATE items SET synced = 1 WHERE id = :itemId")
    void markAsSynced(long itemId);

    @Query("UPDATE items SET status = :status WHERE id = :itemId")
    void updateStatus(long itemId, String status);

    @Query("DELETE FROM items")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM items WHERE user_id = :userId")
    int getItemCountByUser(long userId);
}
