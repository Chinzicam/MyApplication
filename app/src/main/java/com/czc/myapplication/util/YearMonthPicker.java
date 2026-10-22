package com.czc.myapplication.util;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

/** 年-月选择对话框（用于月报月份切换、流水按月筛选） */
public class YearMonthPicker {

    public interface Listener {
        void onPick(int year, int month); // month 1-12
    }

    public static void show(Context context, int initialYear, int initialMonth, Listener listener) {
        int thisYear = DateUtils.currentYear();
        java.util.List<Integer> years = new java.util.ArrayList<>();
        for (int y = thisYear; y >= 2000; y--) years.add(y);

        java.util.List<String> months = new java.util.ArrayList<>();
        for (int m = 1; m <= 12; m++) months.add(m + "月");

        Spinner yearSp = new Spinner(context);
        Spinner monthSp = new Spinner(context);
        yearSp.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, years));
        monthSp.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, months));

        int yearIdx = years.indexOf(initialYear);
        if (yearIdx < 0) yearIdx = 0;
        yearSp.setSelection(yearIdx);
        monthSp.setSelection(Math.max(0, initialMonth - 1));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(yearSp, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        layout.addView(monthSp, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        new AlertDialog.Builder(context)
                .setTitle("选择月份")
                .setView(layout)
                .setPositiveButton("确定", (d, w) -> {
                    int y = years.get(yearSp.getSelectedItemPosition());
                    int m = monthSp.getSelectedItemPosition() + 1;
                    if (listener != null) listener.onPick(y, m);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
