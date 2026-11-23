package com.kms3735.threelinesfirst.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** 앱 전체에서 하나만 만들어 쓴다 */
@Database(entities = {Notice.class, Todo.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NoticeDao noticeDao();

    private static volatile AppDatabase instance;

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "three_lines_first.db").build();
                }
            }
        }
        return instance;
    }
}
