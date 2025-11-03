package com.fptcampus.lostfoundfptcampus.model.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.fptcampus.lostfoundfptcampus.model.Converters;
import com.fptcampus.lostfoundfptcampus.model.History;
import com.fptcampus.lostfoundfptcampus.model.KarmaLog;
import com.fptcampus.lostfoundfptcampus.model.LostItem;
import com.fptcampus.lostfoundfptcampus.model.Notification;
import com.fptcampus.lostfoundfptcampus.model.Photo;
import com.fptcampus.lostfoundfptcampus.model.User;
import com.fptcampus.lostfoundfptcampus.model.dao.HistoryDao;
import com.fptcampus.lostfoundfptcampus.model.dao.KarmaLogDao;
import com.fptcampus.lostfoundfptcampus.model.dao.LostItemDao;
import com.fptcampus.lostfoundfptcampus.model.dao.NotificationDao;
import com.fptcampus.lostfoundfptcampus.model.dao.PhotoDao;
import com.fptcampus.lostfoundfptcampus.model.dao.UserDao;

@Database(
    entities = {
        User.class,
        LostItem.class,
        Photo.class,
        History.class,
        KarmaLog.class,
        Notification.class
    },
    version = 3,
    exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "lostfound_fptcampus.db";
    private static volatile AppDatabase INSTANCE;

    // Migration from version 2 to 3 - Add user role tracking fields
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new columns for tracking user roles in items table
            database.execSQL("ALTER TABLE items ADD COLUMN lost_user_id INTEGER");
            database.execSQL("ALTER TABLE items ADD COLUMN found_user_id INTEGER");
            database.execSQL("ALTER TABLE items ADD COLUMN returned_user_id INTEGER");
        }
    };

    // DAOs
    public abstract UserDao userDao();
    public abstract LostItemDao lostItemDao();
    public abstract PhotoDao photoDao();
    public abstract HistoryDao historyDao();
    public abstract KarmaLogDao karmaLogDao();
    public abstract NotificationDao notificationDao();

    // Singleton pattern
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_2_3) // Add migration support
                    .fallbackToDestructiveMigration() // Fallback if migration fails
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    // Để test hoặc clear database
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
