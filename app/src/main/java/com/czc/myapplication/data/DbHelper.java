package com.czc.myapplication.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** 本地数据库：两张核心表（category / records），统计一律实时 SQL 聚合，不缓存冗余结果 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ledger.db";
    private static final int DB_VERSION = 1;

    private static volatile DbHelper sInstance;

    public static DbHelper get(Context context) {
        if (sInstance == null) {
            synchronized (DbHelper.class) {
                if (sInstance == null) {
                    sInstance = new DbHelper(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE category (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "type INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "record_time INTEGER NOT NULL, " +
                "type INTEGER NOT NULL, " +
                "amount_cents INTEGER NOT NULL, " +
                "category_id INTEGER NOT NULL, " +
                "remark TEXT DEFAULT '')");
        db.execSQL("CREATE INDEX idx_records_time ON records(record_time)");
        db.execSQL("CREATE INDEX idx_records_cat ON records(category_id)");

        seedCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS records");
        db.execSQL("DROP TABLE IF EXISTS category");
        onCreate(db);
    }

    /** 预置默认分类（首次安装写入） */
    private void seedCategories(SQLiteDatabase db) {
        String[][] seeds = {
                {"工资", "1"}, {"奖金", "1"}, {"理财", "1"}, {"红包", "1"}, {"其他收入", "1"},
                {"餐饮", "2"}, {"交通", "2"}, {"购物", "2"}, {"居住", "2"}, {"娱乐", "2"},
                {"医疗", "2"}, {"教育", "2"}, {"人情", "2"}, {"通讯", "2"}, {"其他支出", "2"},
        };
        for (String[] s : seeds) {
            db.execSQL("INSERT INTO category(name, type) VALUES(?, ?)",
                    new Object[]{s[0], Integer.parseInt(s[1])});
        }
    }
}
