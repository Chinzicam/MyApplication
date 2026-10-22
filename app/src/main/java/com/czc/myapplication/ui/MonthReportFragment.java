package com.czc.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.czc.myapplication.R;
import com.czc.myapplication.data.Category;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.data.Record;
import com.czc.myapplication.data.RecordDao;
import com.czc.myapplication.databinding.FragmentMonthReportBinding;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.MoneyUtils;
import com.czc.myapplication.util.YearMonthPicker;
import com.czc.myapplication.view.BarChartView;
import com.czc.myapplication.view.PieChartView;

import java.util.ArrayList;
import java.util.List;

/** Tab3 月报：当月汇总、分类占比、本月 vs 上月对比、近 6 个月趋势 */
public class MonthReportFragment extends Fragment {

    private FragmentMonthReportBinding mB;
    private CategoryDao mCategoryDao;
    private RecordDao mRecordDao;

    private int mYear = DateUtils.currentYear();
    private int mMonth = DateUtils.currentMonth();

    private static final int[] PIE_COLORS = {
            R.color.chart1, R.color.chart2, R.color.chart3, R.color.chart4, R.color.chart5,
            R.color.chart6, R.color.chart7, R.color.chart8, R.color.chart9, R.color.chart10,
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mB = FragmentMonthReportBinding.inflate(inflater, container, false);
        mCategoryDao = new CategoryDao(requireContext());
        mRecordDao = new RecordDao(requireContext());
        return mB.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mB.btnPrevMonth.setOnClickListener(v -> shiftMonth(-1));
        mB.btnNextMonth.setOnClickListener(v -> shiftMonth(1));
        mB.tvMonthLabel.setOnClickListener(v ->
                YearMonthPicker.show(requireContext(), mYear, mMonth,
                        (y, m) -> {
                            mYear = y;
                            mMonth = m;
                            refresh();
                        }));

        refresh();
    }

    private void shiftMonth(int delta) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(mYear, mMonth - 1, 1);
        c.add(java.util.Calendar.MONTH, delta);
        mYear = c.get(java.util.Calendar.YEAR);
        mMonth = c.get(java.util.Calendar.MONTH) + 1;
        refresh();
    }

    public void refresh() {
        if (mB == null || mRecordDao == null) return;
        mB.tvMonthLabel.setText(DateUtils.formatMonth(mYear, mMonth));

        long start = DateUtils.monthStartMillis(mYear, mMonth);
        long end = DateUtils.monthEndExclusive(mYear, mMonth);
        long income = mRecordDao.sumByRangeAndType(start, end, Record.TYPE_INCOME);
        long expense = mRecordDao.sumByRangeAndType(start, end, Record.TYPE_EXPENSE);

        mB.tvMonthIncome.setText(MoneyUtils.format(income));
        mB.tvMonthExpense.setText(MoneyUtils.format(expense));
        mB.tvMonthBalance.setText(MoneyUtils.format(income - expense));

        // 上月
        java.util.Calendar pc = java.util.Calendar.getInstance();
        pc.clear();
        pc.set(mYear, mMonth - 1, 1);
        pc.add(java.util.Calendar.MONTH, -1);
        int py = pc.get(java.util.Calendar.YEAR);
        int pm = pc.get(java.util.Calendar.MONTH) + 1;
        long pStart = DateUtils.monthStartMillis(py, pm);
        long pEnd = DateUtils.monthEndExclusive(py, pm);
        long pIncome = mRecordDao.sumByRangeAndType(pStart, pEnd, Record.TYPE_INCOME);
        long pExpense = mRecordDao.sumByRangeAndType(pStart, pEnd, Record.TYPE_EXPENSE);

        setCompareText(mB.tvCompareIncome, "收入", income - pIncome);
        setCompareText(mB.tvCompareExpense, "支出", expense - pExpense);
        setCompareText(mB.tvCompareBalance, "结余", (income - expense) - (pIncome - pExpense));

        mB.barCompare.setData(
                new String[]{DateUtils.formatMonthShort(pm), DateUtils.formatMonthShort(mMonth)},
                new long[]{pIncome, income},
                new long[]{pExpense, expense});

        // 分类占比
        renderPie(mB.pieExpense, mB.llExpenseDetail, start, end, Record.TYPE_EXPENSE);
        renderPie(mB.pieIncome, mB.llIncomeDetail, start, end, Record.TYPE_INCOME);

        // 近 6 个月趋势
        renderTrend();
    }

    private void setCompareText(TextView tv, String label, long delta) {
        String text = label + " " + MoneyUtils.formatSigned(delta) + "（较上月）";
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(),
                delta > 0 ? R.color.income : delta < 0 ? R.color.expense : R.color.textSecondary));
    }

    private void renderPie(PieChartView pie, LinearLayout detailContainer,
                           long start, long end, int type) {
        List<RecordDao.CategorySum> sums = mRecordDao.sumByCategory(start, end, type);
        List<PieChartView.Slice> slices = new ArrayList<>();
        long total = 0;
        for (int i = 0; i < sums.size(); i++) {
            RecordDao.CategorySum s = sums.get(i);
            String name = mCategoryDao.getName(s.categoryId);
            slices.add(new PieChartView.Slice(name, s.totalCents,
                    ContextCompat.getColor(requireContext(), PIE_COLORS[i % PIE_COLORS.length])));
            total += s.totalCents;
        }
        pie.setData(slices);

        detailContainer.removeAllViews();
        if (sums.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText(R.string.no_data);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
            tv.setTextSize(13);
            detailContainer.addView(tv);
            return;
        }
        for (int i = 0; i < sums.size(); i++) {
            RecordDao.CategorySum s = sums.get(i);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_category_summary, detailContainer, false);
            TextView tvName = row.findViewById(R.id.tv_sum_name);
            TextView tvAmount = row.findViewById(R.id.tv_sum_amount);
            TextView tvPct = row.findViewById(R.id.tv_sum_pct);
            View dot = row.findViewById(R.id.view_dot);
            dot.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_dot));
            dot.getBackground().setTint(ContextCompat.getColor(requireContext(),
                    PIE_COLORS[i % PIE_COLORS.length]));
            tvName.setText(mCategoryDao.getName(s.categoryId));
            tvAmount.setText(MoneyUtils.format(s.totalCents) + " 元");
            tvPct.setText(String.format(java.util.Locale.getDefault(), "%.1f%%",
                    total > 0 ? s.totalCents * 100.0 / total : 0));
            detailContainer.addView(row);
        }
    }

    private void renderTrend() {
        // 近 6 个月（含当前月）
        String[] labels = new String[6];
        long[] income = new long[6];
        long[] expense = new long[6];
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(mYear, mMonth - 1, 1);
        c.add(java.util.Calendar.MONTH, -5);
        for (int i = 0; i < 6; i++) {
            int y = c.get(java.util.Calendar.YEAR);
            int m = c.get(java.util.Calendar.MONTH) + 1;
            labels[i] = DateUtils.formatMonthShort(m);
            long start = DateUtils.monthStartMillis(y, m);
            long end = DateUtils.monthEndExclusive(y, m);
            income[i] = mRecordDao.sumByRangeAndType(start, end, Record.TYPE_INCOME);
            expense[i] = mRecordDao.sumByRangeAndType(start, end, Record.TYPE_EXPENSE);
            c.add(java.util.Calendar.MONTH, 1);
        }
        mB.lineTrend.setData(labels, income, expense);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
