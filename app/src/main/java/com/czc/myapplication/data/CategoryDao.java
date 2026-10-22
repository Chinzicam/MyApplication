package com.czc.myapplication.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/** 分类表操作 */
public class CategoryDao {

    private final SQLiteDatabase db;

    public CategoryDao(Context context) {
        this.db = DbHelper.get(context).getWritableDatabase();
    }

    public List<Category> listByType(int type) {
        List<Category> list = new ArrayList<>();
        Cursor c = db.query("category", null, "type = ?", new String[]{String.valueOf(type)},
                null, null, "id ASC");
        while (c.moveToNext()) {
            list.add(read(c));
        }
        c.close();
        return list;
    }

    public List<Category> listAll() {
        List<Category> list = new ArrayList<>();
        Cursor c = db.query("category", null, null, null, null, null, "type ASC, id ASC");
        while (c.moveToNext()) {
            list.add(read(c));
        }
        c.close();
        return list;
    }

    public Category getById(long id) {
        Cursor c = db.query("category", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null);
        Category category = null;
        if (c.moveToFirst()) {
            category = read(c);
        }
        c.close();
        return category;
    }

    public String getName(long id) {
        Category category = getById(id);
        return category == null ? "" : category.name;
    }

    /** 新增分类，返回新 id；-1 表示失败（重名） */
    public long insert(String name, int type) {
        if (name == null || name.trim().isEmpty()) return -1;
        if (exists(name.trim(), type)) return -1;
        return db.insert("category", null, values(name.trim(), type));
    }

    /** 删除分类；该分类下已有记录时禁止删除 */
    public int delete(long categoryId) {
        RecordDao recordDao = new RecordDao(db);
        if (recordDao.countByCategory(categoryId) > 0) return 0;
        return db.delete("category", "id = ?", new String[]{String.valueOf(categoryId)});
    }

    private boolean exists(String name, int type) {
        Cursor c = db.query("category", new String[]{"id"},
                "name = ? AND type = ?", new String[]{name, String.valueOf(type)},
                null, null, null);
        boolean found = c.moveToFirst();
        c.close();
        return found;
    }

    private static android.content.ContentValues values(String name, int type) {
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("name", name);
        v.put("type", type);
        return v;
    }

    private static Category read(Cursor c) {
        long id = c.getLong(c.getColumnIndexOrThrow("id"));
        String name = c.getString(c.getColumnIndexOrThrow("name"));
        int type = c.getInt(c.getColumnIndexOrThrow("type"));
        return new Category(id, name, type);
    }
}
