package com.czc.myapplication.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.czc.myapplication.R;
import com.czc.myapplication.data.Category;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.data.Record;
import com.czc.myapplication.data.RecordDao;
import com.czc.myapplication.databinding.FragmentTodayBinding;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

/** Tab1 今日：快速录入 + 今日流水列表 */
public class TodayFragment extends Fragment {

    private FragmentTodayBinding mB;
    private CategoryDao mCategoryDao;
    private RecordDao mRecordDao;
    private RecordAdapter mAdapter;

    private List<Category> mIncomeCats = new ArrayList<>();
    private List<Category> mExpenseCats = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mB = FragmentTodayBinding.inflate(inflater, container, false);
        mCategoryDao = new CategoryDao(requireContext());
        mRecordDao = new RecordDao(requireContext());
        return mB.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mIncomeCats = mCategoryDao.listByType(Category.TYPE_INCOME);
        mExpenseCats = mCategoryDao.listByType(Category.TYPE_EXPENSE);

        mAdapter = new RecordAdapter(mCategoryDao);
        mB.rvToday.setLayoutManager(new LinearLayoutManager(requireContext()));
        mB.rvToday.setAdapter(mAdapter);
        mAdapter.setOnItemClickListener(record ->
                startActivity(new Intent(requireContext(), EditRecordActivity.class)
                        .putExtra(EditRecordActivity.EXTRA_RECORD_ID, record.id)));

        mB.rgType.setOnCheckedChangeListener((group, checkedId) -> reloadCategorySpinner());
        reloadCategorySpinner();

        mB.btnSave.setOnClickListener(v -> saveRecord());

        refresh();
    }

    private void reloadCategorySpinner() {
        List<Category> cats = isIncomeSelected() ? mIncomeCats : mExpenseCats;
        List<String> names = new ArrayList<>();
        for (Category c : cats) names.add(c.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, names);
        mB.spCategory.setAdapter(adapter);
    }

    private boolean isIncomeSelected() {
        return mB.rbIncome.isChecked();
    }

    private void saveRecord() {
        String amountText = mB.etAmount.getText().toString().trim();
        long cents = MoneyUtils.parseToCents(amountText);
        if (cents <= 0) {
            Toast.makeText(requireContext(), "请输入正确的金额", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Category> cats = isIncomeSelected() ? mIncomeCats : mExpenseCats;
        int pos = mB.spCategory.getSelectedItemPosition();
        if (cats.isEmpty() || pos < 0 || pos >= cats.size()) {
            Toast.makeText(requireContext(), "请选择分类", Toast.LENGTH_SHORT).show();
            return;
        }
        Record r = new Record(
                System.currentTimeMillis(),
                isIncomeSelected() ? Record.TYPE_INCOME : Record.TYPE_EXPENSE,
                cents,
                cats.get(pos).id,
                mB.etRemark.getText().toString().trim());
        mRecordDao.insert(r);
        mB.etAmount.setText("");
        mB.etRemark.setText("");
        Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show();
        refresh();
    }

    /** 供 MainActivity 切换 Tab / 编辑返回后调用 */
    public void refresh() {
        if (mB == null || mRecordDao == null) return;
        mB.tvTodayDate.setText(DateUtils.todayLabel());
        long dayStart = DateUtils.todayStartMillis();
        long dayEnd = DateUtils.endOfDayExclusive(System.currentTimeMillis());
        long income = mRecordDao.sumByRangeAndType(dayStart, dayEnd, Record.TYPE_INCOME);
        long expense = mRecordDao.sumByRangeAndType(dayStart, dayEnd, Record.TYPE_EXPENSE);
        mB.tvTodayIncome.setText(MoneyUtils.format(income));
        mB.tvTodayExpense.setText(MoneyUtils.format(expense));
        mB.tvTodayBalance.setText(MoneyUtils.format(income - expense));

        List<Record> todayList = mRecordDao.queryByRange(dayStart, dayEnd);
        mAdapter.setData(todayList);
        mB.tvTodayListTitle.setText(getString(R.string.today_records) + "（" + todayList.size() + "）");
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
