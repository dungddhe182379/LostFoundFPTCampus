package com.fptcampus.lostfoundfptcampus.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fptcampus.lostfoundfptcampus.model.Notification;

import java.util.List;

@Dao
public interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Notification notification);

    @Update
    void update(Notification notification);

    @Delete
    void delete(Notification notification);

    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    Notification getNotificationById(long notificationId);

    @Query("SELECT * FROM notifications WHERE user_id = :userId ORDER BY created_at DESC")
    List<Notification> getNotificationsByUserId(long userId);

    @Query("SELECT * FROM notifications WHERE user_id = :userId AND is_read = 0 ORDER BY created_at DESC")
    List<Notification> getUnreadNotificationsByUserId(long userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = 0")
    int getUnreadCount(long userId);

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :notificationId")
    void markAsRead(long notificationId);

    @Query("UPDATE notifications SET is_read = 1 WHERE user_id = :userId")
    void markAllAsRead(long userId);

    @Query("DELETE FROM notifications WHERE user_id = :userId")
    void deleteAllByUserId(long userId);

    @Query("DELETE FROM notifications")
    void deleteAll();
}
