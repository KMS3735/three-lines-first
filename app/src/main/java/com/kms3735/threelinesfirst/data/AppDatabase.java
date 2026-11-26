package com.kms3735.threelinesfirst.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** 앱 전체에서 하나만 만들어 쓴다 */
@Database(entities = {Notice.class, Todo.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract NoticeDao noticeDao();

    private static volatile AppDatabase instance;

    /** 6단계: 문의 전화와 장소 열 추가. 저장해 둔 안내문은 그대로 남는다 */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE notice ADD COLUMN phone TEXT");
            db.execSQL("ALTER TABLE notice ADD COLUMN place TEXT");
        }
    };

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "three_lines_first.db")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return instance;
    }
}
