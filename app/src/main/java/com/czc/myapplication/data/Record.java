package com.czc.myapplication.data;

/** 记账记录模型。金额以“分”为单位的整数存储，避免浮点误差 */
public class Record {
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;

    public long id;
    public long recordTime;      // epoch millis
    public int type;             // 1 收入 2 支出
    public long amountCents;     // 金额（分）
    public long categoryId;
    public String remark = "";

    public Record() {}

    public Record(long recordTime, int type, long amountCents, long categoryId, String remark) {
        this.recordTime = recordTime;
        this.type = type;
        this.amountCents = amountCents;
        this.categoryId = categoryId;
        this.remark = remark == null ? "" : remark;
    }
}
