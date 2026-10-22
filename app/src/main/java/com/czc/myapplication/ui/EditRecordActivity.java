package com.czc.myapplication.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.czc.myapplication.R;
import com.czc.myapplication.data.Category;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.data.Record;
import com.czc.myapplication.data.RecordDao;
import com.czc.myapplication.databinding.ActivityEditRecordBinding;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.MoneyUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 页面 A：账目编辑页（新增 / 修改 / 删除）。
 * 支持手动修改记录日期时间（补记过去账目）。
 */
public class EditRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "record_id";

    private ActivityEditRecordBinding mB;
    private RecordDao mRecordDao;
    private CategoryDao mCategoryDao;

    private long mRecordId = -1;
    private long mDateTime = System.currentTimeMillis();

    private List<Category> mIncomeCats = new ArrayList<>();
    private List<Category> mExpenseCats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mB = ActivityEditRecordBinding.inflate(getLayoutInflater());
        setContentView(mB.getRoot());

        mRecordDao = new RecordDao(this);
        mCategoryDao = new CategoryDao(this);

        mB.toolbar.setNavigationIcon(R.drawable.ic_back);
        mB.toolbar.setNavigationOnClickListener(v -> finish());

        mIncomeCats = mCategoryDao.listByType(Category.TYPE_INCOME);
        mExpenseCats = mCategoryDao.listByType(Category.TYPE_EXPENSE);

        mRecordId = getIntent().getLongExtra(EXTRA_RECORD_ID, -1);
        boolean editMode = mRecordId > 0;
        mB.tvTitle.setText(editMode ? R.string.edit_title_update : R.string.edit_title_add);
        mB.btnDelete.setVisibility(editMode ? android.view.View.VISIBLE : android.view.View.GONE);

        mB.rgType.setOnCheckedChangeListener((g, checkedId) -> reloadCategorySpinner());
        reloadCategorySpinner();

        if (editMode) {
            Record r = mRecordDao.getById(mRecordId);
            if (r == null) {
                Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            mDateTime = r.recordTime;
            mB.etAmount.setText(MoneyUtils.format(r.amountCents));
            mB.etRemark.setText(r.remark);
            if (r.type == Record.TYPE_INCOME) {
                mB.rbIncome.setChecked(true);
                mB.rbExpense.setChecked(false);
            } else {
                mB.rbExpense.setChecked(true);
                mB.rbIncome.setChecked(false);
            }
            selectCategory(r.categoryId);
        }

        updateDateTimeText();
        mB.tvDateTime.setOnClickListener(v -> showDateTimePicker());

        mB.btnSave.setOnClickListener(v -> save());
        mB.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private boolean isIncomeSelected() {
        return mB.rbIncome.isChecked();
    }

    private void reloadCategorySpinner() {
        List<Category> cats = isIncomeSelected() ? mIncomeCats : mExpenseCats;
        List<String> names = new ArrayList<>();
        for (Category c : cats) names.add(c.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        mB.spCategory.setAdapter(adapter);
        // 若类型切换前已有选中分类（仅编辑模式），保持原分类不可行则选第一个
        if (mB.spCategory.getSelectedItemPosition() < 0 && !cats.isEmpty()) {
            mB.spCategory.setSelection(0);
        }
    }

    private void selectCategory(long categoryId) {
        List<Category> cats = isIncomeSelected() ? mIncomeCats : mExpenseCats;
        for (int i = 0; i < cats.size(); i++) {
            if (cats.get(i).id == categoryId) {
                mB.spCategory.setSelection(i);
                return;
            }
        }
        if (!cats.isEmpty()) mB.spCategory.setSelection(0);
    }

    private void showDateTimePicker() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mDateTime);
        new DatePickerDialog(this, (dv, y, mo, d) -> {
            Calendar pick = Calendar.getInstance();
            pick.setTimeInMillis(mDateTime);
            pick.set(y, mo, d);
            mDateTime = pick.getTimeInMillis();
            new TimePickerDialog(this, (tv, hour, minute) -> {
                pick.set(Calendar.HOUR_OF_DAY, hour);
                pick.set(Calendar.MINUTE, minute);
                pick.set(Calendar.SECOND, 0);
                pick.set(Calendar.MILLISECOND, 0);
                mDateTime = pick.getTimeInMillis();
                updateDateTimeText();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateTimeText() {
        mB.tvDateTime.setText(DateUtils.formatDateTime(mDateTime));
    }

    private void save() {
        long cents = MoneyUtils.parseToCents(mB.etAmount.getText().toString().trim());
        if (cents <= 0) {
            Toast.makeText(this, "请输入正确的金额", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Category> cats = isIncomeSelected() ? mIncomeCats : mExpenseCats;
        int pos = mB.spCategory.getSelectedItemPosition();
        if (cats.isEmpty() || pos < 0 || pos >= cats.size()) {
            Toast.makeText(this, "请选择分类", Toast.LENGTH_SHORT).show();
            return;
        }
        int type = isIncomeSelected() ? Record.TYPE_INCOME : Record.TYPE_EXPENSE;
        long categoryId = cats.get(pos).id;
        String remark = mB.etRemark.getText().toString().trim();

        if (mRecordId > 0) {
            Record r = new Record(mDateTime, type, cents, categoryId, remark);
            r.id = mRecordId;
            mRecordDao.update(r);
            Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
        } else {
            mRecordDao.insert(new Record(mDateTime, type, cents, categoryId, remark));
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_record_confirm)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    mRecordDao.delete(mRecordId);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
