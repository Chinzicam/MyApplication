package com.czc.myapplication.data;

/** 分类模型：1=收入分类，2=支出分类 */
public class Category {
    public static final int TYPE_INCOME = 1;
    public static final int TYPE_EXPENSE = 2;

    public long id;
    public String name;
    public int type;

    public Category(long id, String name, int type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
