package com.czc.myapplication.ui;

import android.content.Intent;
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
import com.czc.myapplication.databinding.FragmentYearReportBinding;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.MoneyUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tab4 年报：全年汇总、12 个月趋势、本年 vs 去年、全年分类汇总，底部设置入口 */
public class YearReportFragment extends Fragment {

    private FragmentYearReportBinding mB;
    private CategoryDao mCategoryDao;
    private RecordDao mRecordDao;

    private int mYear = DateUtils.currentYear();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mB = FragmentYearReportBinding.inflate(inflater, container, false);
        mCategoryDao = new CategoryDao(requireContext());
        mRecordDao = new RecordDao(requireContext());
        return mB.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mB.btnPrevYear.setOnClickListener(v -> {
            mYear--;
            refresh();
        });
        mB.btnNextYear.setOnClickListener(v -> {
            mYear++;
            refresh();
        });
        mB.btnSettingsEntry.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        refresh();
    }

    public void refresh() {
        if (mB == null || mRecordDao == null) return;
        mB.tvYearLabel.setText(DateUtils.formatYear(mYear));

        long start = DateUtils.yearStartMillis(mYear);
        long end = DateUtils.yearEndExclusive(mYear);
        long income = mRecordDao.sumByRangeAndType(start, end, Record.TYPE_INCOME);
        long expense = mRecordDao.sumByRangeAndType(start, end, Record.TYPE_EXPENSE);

        mB.tvYearIncome.setText(MoneyUtils.format(income));
        mB.tvYearExpense.setText(MoneyUtils.format(expense));
        mB.tvYearBalance.setText(MoneyUtils.format(income - expense));

        // 去年
        long pStart = DateUtils.yearStartMillis(mYear - 1);
        long pEnd = DateUtils.yearEndExclusive(mYear - 1);
        long pIncome = mRecordDao.sumByRangeAndType(pStart, pEnd, Record.TYPE_INCOME);
        long pExpense = mRecordDao.sumByRangeAndType(pStart, pEnd, Record.TYPE_EXPENSE);

        setCompareText(mB.tvCompareIncome, "收入", income - pIncome);
        setCompareText(mB.tvCompareExpense, "支出", expense - pExpense);
        setCompareText(mB.tvCompareBalance, "结余", (income - expense) - (pIncome - pExpense));

        // 12 个月趋势
        long[][] series = mRecordDao.monthlySeries(mYear, 0);
        String[] labels = new String[12];
        for (int i = 0; i < 12; i++) labels[i] = DateUtils.formatMonthShort(i + 1);
        mB.barYearTrend.setData(labels, series[0], series[1]);

        // 全年分类汇总（收入 + 支出）
        renderYearCategory(start, end);
    }

    private void setCompareText(TextView tv, String label, long delta) {
        String text = label + " " + MoneyUtils.formatSigned(delta) + "（较去年）";
        tv.setText(text);
        tv.setTextColor(ContextCompat.getColor(requireContext(),
                delta > 0 ? R.color.income : delta < 0 ? R.color.expense : R.color.textSecondary));
    }

    private void renderYearCategory(long start, long end) {
        mB.llYearCategory.removeAllViews();

        List<Category> incomeCats = mCategoryDao.listByType(Category.TYPE_INCOME);
        List<Category> expenseCats = mCategoryDao.listByType(Category.TYPE_EXPENSE);

        Map<Long, Long> incomeSum = sumMap(start, end, Record.TYPE_INCOME);
        Map<Long, Long> expenseSum = sumMap(start, end, Record.TYPE_EXPENSE);

        addSectionTitle("收入");
        for (Category c : incomeCats) {
            addCategoryRow(c.name, incomeSum.getOrDefault(c.id, 0L));
        }
        addSectionTitle("支出");
        for (Category c : expenseCats) {
            addCategoryRow(c.name, expenseSum.getOrDefault(c.id, 0L));
        }
    }

    private Map<Long, Long> sumMap(long start, long end, int type) {
        Map<Long, Long> map = new HashMap<>();
        for (RecordDao.CategorySum s : mRecordDao.sumByCategory(start, end, type)) {
            map.put(s.categoryId, s.totalCents);
        }
        return map;
    }

    private void addSectionTitle(String title) {
        TextView tv = new TextView(requireContext());
        tv.setText(title);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        tv.setTextSize(13);
        tv.setPadding(0, 14, 0, 4);
        mB.llYearCategory.addView(tv);
    }

    private void addCategoryRow(String name, long cents) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 6, 0, 6);

        TextView nameTv = new TextView(requireContext());
        nameTv.setText(name);
        nameTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
        nameTv.setTextSize(13);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(nameTv, nameLp);

        TextView amountTv = new TextView(requireContext());
        amountTv.setText(cents > 0 ? MoneyUtils.format(cents) + " 元" : "—");
        amountTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.textPrimary));
        amountTv.setTextSize(13);
        row.addView(amountTv);
        mB.llYearCategory.addView(row);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
