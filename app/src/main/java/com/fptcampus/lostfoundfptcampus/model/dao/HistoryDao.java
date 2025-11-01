package com.fptcampus.lostfoundfptcampus.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fptcampus.lostfoundfptcampus.model.History;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(History history);

    @Update
    void update(History history);

    @Delete
    void delete(History history);

    @Query("SELECT * FROM histories WHERE id = :historyId")
    History getHistoryById(long historyId);

    @Query("SELECT * FROM histories WHERE item_id = :itemId")
    List<History> getHistoriesByItemId(long itemId);

    @Query("SELECT * FROM histories WHERE qr_token = :qrToken LIMIT 1")
    History getHistoryByQrToken(String qrToken);

    @Query("SELECT * FROM histories WHERE giver_id = :userId OR receiver_id = :userId ORDER BY confirmed_at DESC")
    List<History> getHistoriesByUserId(long userId);

    @Query("DELETE FROM histories")
    void deleteAll();
}
