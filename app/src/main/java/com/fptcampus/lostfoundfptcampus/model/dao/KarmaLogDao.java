package com.fptcampus.lostfoundfptcampus.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fptcampus.lostfoundfptcampus.model.KarmaLog;

import java.util.List;

@Dao
public interface KarmaLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(KarmaLog karmaLog);

    @Update
    void update(KarmaLog karmaLog);

    @Delete
    void delete(KarmaLog karmaLog);

    @Query("SELECT * FROM karma_logs WHERE id = :logId")
    KarmaLog getKarmaLogById(long logId);

    @Query("SELECT * FROM karma_logs WHERE user_id = :userId ORDER BY created_at DESC")
    List<KarmaLog> getKarmaLogsByUserId(long userId);

    @Query("SELECT SUM(change_value) FROM karma_logs WHERE user_id = :userId")
    int getTotalKarmaByUserId(long userId);

    @Query("DELETE FROM karma_logs")
    void deleteAll();
}
