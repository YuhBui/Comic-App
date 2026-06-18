package com.yuhbui.comicapp.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.yuhbui.comicapp.data.model.DownloadedChapter;
import com.yuhbui.comicapp.data.model.DownloadedComic;
import com.yuhbui.comicapp.data.model.DownloadedImage;

@Database(entities = {DownloadedComic.class, DownloadedChapter.class, DownloadedImage.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;
    public abstract OfflineDao offlineDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "comic_offline_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}