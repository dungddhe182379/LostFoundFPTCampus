package com.fptcampus.lostfoundfptcampus.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fptcampus.lostfoundfptcampus.model.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(long userId);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE uuid = :uuid LIMIT 1")
    User getUserByUuid(String uuid);

    @Query("SELECT * FROM users ORDER BY karma DESC LIMIT :limit")
    List<User> getTopKarmaUsers(int limit);

    @Query("SELECT * FROM users ORDER BY karma DESC")
    List<User> getAllUsersByKarma();

    @Query("UPDATE users SET karma = karma + :karmaChange WHERE id = :userId")
    void updateKarma(long userId, int karmaChange);

    @Query("DELETE FROM users")
    void deleteAll();
}
