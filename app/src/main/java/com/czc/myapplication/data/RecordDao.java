package com.czc.myapplication.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 记账记录表操作 + 全部统计聚合查询。
 * 月报/年报数据一律实时 SQL 聚合（SUM/GROUP BY），不单独存储统计结果。
 */
public class RecordDao {

    private final SQLiteDatabase db;

    public RecordDao(Context context) {
        this.db = DbHelper.get(context).getWritableDatabase();
    }

    /** 供 CategoryDao 内部使用（复用同一数据库连接） */
    public RecordDao(SQLiteDatabase db) {
        this.db = db;
    }

    // ---------------- 增删改 ----------------

    public long insert(Record r) {
        ContentValues v = new ContentValues();
        v.put("record_time", r.recordTime);
        v.put("type", r.type);
        v.put("amount_cents", r.amountCents);
        v.put("category_id", r.categoryId);
        v.put("remark", r.remark == null ? "" : r.remark);
        return db.insert("records", null, v);
    }

    public int update(Record r) {
        ContentValues v = new ContentValues();
        v.put("record_time", r.recordTime);
        v.put("type", r.type);
        v.put("amount_cents", r.amountCents);
        v.put("category_id", r.categoryId);
        v.put("remark", r.remark == null ? "" : r.remark);
        return db.update("records", v, "id = ?", new String[]{String.valueOf(r.id)});
    }

    public int delete(long id) {
        return db.delete("records", "id = ?", new String[]{String.valueOf(id)});
    }

    /** 清空全部记账记录（不删分类） */
    public int deleteAll() {
        return db.delete("records", null, null);
    }

    // ---------------- 明细查询 ----------------

    public Record getById(long id) {
        Cursor c = db.query("records", null, "id = ?", new String[]{String.valueOf(id)},
                null, null, null);
        Record r = null;
        if (c.moveToFirst()) {
            r = read(c);
        }
        c.close();
        return r;
    }

    /** 按时间范围查询，时间倒序 */
    public List<Record> queryByRange(long start, long end) {
        List<Record> list = new ArrayList<>();
        Cursor c = db.query("records", null,
                "record_time >= ? AND record_time < ?",
                new String[]{String.valueOf(start), String.valueOf(end)},
                null, null, "record_time DESC, id DESC");
        while (c.moveToNext()) {
            list.add(read(c));
        }
        c.close();
        return list;
    }

    /** 全部记录，时间倒序，分页（offset/limit 用于无限滚动） */
    public List<Record> queryPage(int offset, int limit) {
        List<Record> list = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM records ORDER BY record_time DESC, id DESC LIMIT ? OFFSET ?",
                new String[]{String.valueOf(limit), String.valueOf(offset)});
        while (c.moveToNext()) {
            list.add(read(c));
        }
        c.close();
        return list;
    }

    /** 备注关键词搜索（匹配 remark，时间倒序） */
    public List<Record> searchByKeyword(String keyword, int limit) {
        List<Record> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM records WHERE remark LIKE ? ORDER BY record_time DESC, id DESC LIMIT ?",
                new String[]{"%" + keyword + "%", String.valueOf(limit)});
        while (c.moveToNext()) {
            list.add(read(c));
        }
        c.close();
        return list;
    }

    public int countAll() {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM records", null);
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        return n;
    }

    public int countByCategory(long categoryId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM records WHERE category_id = ?",
                new String[]{String.valueOf(categoryId)});
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        return n;
    }

    // ---------------- 统计聚合（实时计算） ----------------

    /** 时间范围 [start, end) 内某类型的总额（分） */
    public long sumByRangeAndType(long start, long end, int type) {
        Cursor c = db.rawQuery(
                "SELECT COALESCE(SUM(amount_cents), 0) FROM records WHERE record_time >= ? AND record_time < ? AND type = ?",
                new String[]{String.valueOf(start), String.valueOf(end), String.valueOf(type)});
        c.moveToFirst();
        long sum = c.getLong(0);
        c.close();
        return sum;
    }

    /** 分类汇总：时间范围 [start, end) 内某类型按分类分组 */
    public List<CategorySum> sumByCategory(long start, long end, int type) {
        List<CategorySum> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT category_id, COALESCE(SUM(amount_cents), 0) AS total FROM records " +
                        "WHERE record_time >= ? AND record_time < ? AND type = ? " +
                        "GROUP BY category_id ORDER BY total DESC",
                new String[]{String.valueOf(start), String.valueOf(end), String.valueOf(type)});
        while (c.moveToNext()) {
            list.add(new CategorySum(c.getLong(0), c.getLong(1)));
        }
        c.close();
        return list;
    }

    /** 全年 12 个月收支序列（下标 0-11 对应 1-12 月），单位分 */
    public long[][] monthlySeries(int year, int localOffsetMillis) {
        long[] income = new long[12];
        long[] expense = new long[12];
        // 全年范围：1 月 1 日 00:00（本地） ~ 次年 1 月 1 日 00:00（本地）
        long start = java.util.Calendar.getInstance().getTimeInMillis();
        // 用工具计算，避免重复逻辑：这里直接计算本地时间戳
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.clear();
        cal.set(year, java.util.Calendar.JANUARY, 1, 0, 0, 0);
        long yearStart = cal.getTimeInMillis();
        cal.set(year + 1, java.util.Calendar.JANUARY, 1, 0, 0, 0);
        long yearEnd = cal.getTimeInMillis();

        Cursor c = db.rawQuery(
                "SELECT strftime('%m', record_time/1000, 'unixepoch', 'localtime') AS m, type, COALESCE(SUM(amount_cents),0) AS total " +
                        "FROM records WHERE record_time >= ? AND record_time < ? GROUP BY m, type",
                new String[]{String.valueOf(yearStart), String.valueOf(yearEnd)});
        while (c.moveToNext()) {
            String m = c.getString(0);
            int type = c.getInt(1);
            long total = c.getLong(2);
            if (m == null || m.isEmpty()) continue;
            int idx = Integer.parseInt(m) - 1;
            if (idx < 0 || idx > 11) continue;
            if (type == Record.TYPE_INCOME) income[idx] = total;
            else if (type == Record.TYPE_EXPENSE) expense[idx] = total;
        }
        c.close();
        return new long[][]{income, expense};
    }

    public static class CategorySum {
        public long categoryId;
        public long totalCents;
        public CategorySum(long categoryId, long totalCents) {
            this.categoryId = categoryId;
            this.totalCents = totalCents;
        }
    }

    private static Record read(Cursor c) {
        Record r = new Record();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.recordTime = c.getLong(c.getColumnIndexOrThrow("record_time"));
        r.type = c.getInt(c.getColumnIndexOrThrow("type"));
        r.amountCents = c.getLong(c.getColumnIndexOrThrow("amount_cents"));
        r.categoryId = c.getLong(c.getColumnIndexOrThrow("category_id"));
        r.remark = c.getString(c.getColumnIndexOrThrow("remark"));
        return r;
    }
}
