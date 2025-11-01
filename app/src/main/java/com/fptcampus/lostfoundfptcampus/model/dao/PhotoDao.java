package com.fptcampus.lostfoundfptcampus.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fptcampus.lostfoundfptcampus.model.Photo;

import java.util.List;

@Dao
public interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Photo photo);

    @Update
    void update(Photo photo);

    @Delete
    void delete(Photo photo);

    @Query("SELECT * FROM photos WHERE id = :photoId")
    Photo getPhotoById(long photoId);

    @Query("SELECT * FROM photos WHERE item_id = :itemId")
    List<Photo> getPhotosByItemId(long itemId);

    @Query("SELECT * FROM photos WHERE item_id = :itemId AND is_primary = 1 LIMIT 1")
    Photo getPrimaryPhotoByItemId(long itemId);

    @Query("DELETE FROM photos WHERE item_id = :itemId")
    void deleteAllByItemId(long itemId);

    @Query("DELETE FROM photos")
    void deleteAll();
}
