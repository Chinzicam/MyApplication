package com.czc.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.czc.myapplication.R;
import com.czc.myapplication.data.Category;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.databinding.ActivityCategoryBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面 B：分类管理页。
 * 收入分类 / 支出分类分开展示；新增需填写名称并选择类型；
 * 删除有二次确认，分类下已有记录时禁止删除。
 */
public class CategoryActivity extends AppCompatActivity {

    private ActivityCategoryBinding mB;
    private CategoryDao mCategoryDao;

    private final List<Category> mIncomeCats = new ArrayList<>();
    private final List<Category> mExpenseCats = new ArrayList<>();
    private CategoryAdapter mIncomeAdapter;
    private CategoryAdapter mExpenseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mB = ActivityCategoryBinding.inflate(getLayoutInflater());
        setContentView(mB.getRoot());

        mCategoryDao = new CategoryDao(this);

        mB.toolbar.setNavigationIcon(R.drawable.ic_back);
        mB.toolbar.setNavigationOnClickListener(v -> finish());

        mIncomeAdapter = new CategoryAdapter(mIncomeCats);
        mExpenseAdapter = new CategoryAdapter(mExpenseCats);
        mB.rvIncome.setLayoutManager(new LinearLayoutManager(this));
        mB.rvExpense.setLayoutManager(new LinearLayoutManager(this));
        mB.rvIncome.setAdapter(mIncomeAdapter);
        mB.rvExpense.setAdapter(mExpenseAdapter);

        mIncomeAdapter.setOnDeleteListener(this::confirmDelete);
        mExpenseAdapter.setOnDeleteListener(this::confirmDelete);

        mB.btnAddCategory.setOnClickListener(v -> showAddDialog());

        reload();
    }

    private void reload() {
        mIncomeCats.clear();
        mIncomeCats.addAll(mCategoryDao.listByType(Category.TYPE_INCOME));
        mExpenseCats.clear();
        mExpenseCats.addAll(mCategoryDao.listByType(Category.TYPE_EXPENSE));
        mIncomeAdapter.notifyDataSetChanged();
        mExpenseAdapter.notifyDataSetChanged();
        mB.tvIncomeTitle.setText(getString(R.string.income_categories) + "（" + mIncomeCats.size() + "）");
        mB.tvExpenseTitle.setText(getString(R.string.expense_categories) + "（" + mExpenseCats.size() + "）");
    }

    private void showAddDialog() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);

        final EditText nameInput = new EditText(this);
        nameInput.setId(R.id.et_category_name_input);
        nameInput.setHint(getString(R.string.category_name_hint));
        nameInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(nameInput);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton rbExpense = new RadioButton(this);
        rbExpense.setText(R.string.expense);
        rbExpense.setChecked(true);
        RadioButton rbIncome = new RadioButton(this);
        rbIncome.setText(R.string.income);
        rg.addView(rbExpense);
        rg.addView(rbIncome);
        android.widget.LinearLayout.LayoutParams rp = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = 16;
        layout.addView(rg, rp);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_category)
                .setView(layout)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    int type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
                    long id = mCategoryDao.insert(name, type);
                    if (id < 0) {
                        if (name.isEmpty()) {
                            Toast.makeText(this, "名称不能为空", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, R.string.category_duplicate, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        reload();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_category_confirm, category.name))
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    int result = mCategoryDao.delete(category.id);
                    if (result == 0) {
                        Toast.makeText(this, R.string.category_in_use, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        reload();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** 简单持有布局引用（避免多余类） */

    static class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

        interface OnDeleteListener {
            void onDelete(Category category);
        }

        private final List<Category> mList;
        private OnDeleteListener mListener;

        CategoryAdapter(List<Category> list) {
            this.mList = list;
        }

        void setOnDeleteListener(OnDeleteListener listener) {
            this.mListener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Category c = mList.get(position);
            holder.tvName.setText(c.name);
            holder.btnDelete.setOnClickListener(v -> {
                if (mListener != null) mListener.onDelete(c);
            });
        }

        @Override
        public int getItemCount() {
            return mList.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName;
            android.widget.ImageButton btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_cat_name);
                btnDelete = itemView.findViewById(R.id.btn_delete_cat);
            }
        }
    }
}
