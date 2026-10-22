package com.czc.myapplication.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.czc.myapplication.R;
import com.czc.myapplication.data.Category;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.data.Record;
import com.czc.myapplication.data.RecordDao;
import com.czc.myapplication.databinding.ActivitySettingsBinding;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.MoneyUtils;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 页面 C：设置页。
 * 导出全部数据为 CSV（系统文件选择器保存）；分类管理入口；
 * 清空全部记账记录（高危操作强确认）；关于版本号。
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding mB;
    private RecordDao mRecordDao;
    private CategoryDao mCategoryDao;

    private final ActivityResultLauncher<String> mExportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) {
                    exportCsv(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mB = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(mB.getRoot());

        mRecordDao = new RecordDao(this);
        mCategoryDao = new CategoryDao(this);

        mB.toolbar.setNavigationIcon(R.drawable.ic_back);
        mB.toolbar.setNavigationOnClickListener(v -> finish());

        mB.tvExport.setOnClickListener(v ->
                mExportLauncher.launch("极简记账_数据_" + DateUtils.formatDate(System.currentTimeMillis()) + ".csv"));

        mB.tvManageCategory.setOnClickListener(v ->
                startActivity(new Intent(this, CategoryActivity.class)));

        mB.tvClear.setOnClickListener(v -> confirmClear());

        mB.tvVersion.setText(getString(R.string.about_version, "1.0"));
        showVersion();
    }

    private void showVersion() {
        try {
            String v = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            mB.tvVersion.setText(getString(R.string.about_version, v));
        } catch (Exception ignored) {
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_confirm_title)
                .setMessage(R.string.clear_confirm_msg)
                .setPositiveButton(R.string.confirm, (d, w) -> {
                    mRecordDao.deleteAll();
                    Toast.makeText(this, R.string.clear_done, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** 导出全部记录为 CSV（UTF-8 with BOM，Excel 可直接打开） */
    private void exportCsv(Uri uri) {
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);
            if (os == null) {
                Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            // BOM
            writer.write('\uFEFF');
            writer.write("id,时间,类型,分类,金额(元),备注\n");
            for (Record r : mRecordDao.queryPage(0, 1000000)) {
                Category c = mCategoryDao.getById(r.categoryId);
                String catName = c == null ? "" : c.name;
                String typeName = r.type == Record.TYPE_INCOME ? "收入" : "支出";
                String remark = r.remark == null ? "" : r.remark.replace("\"", "\"\"");
                writer.write(String.format(java.util.Locale.US,
                        "%d,\"%s\",%s,\"%s\",%s,\"%s\"\n",
                        r.id,
                        DateUtils.formatDateTime(r.recordTime),
                        typeName,
                        catName,
                        String.format(java.util.Locale.US, "%.2f", r.amountCents / 100.0),
                        remark));
            }
            writer.flush();
            writer.close();
            Toast.makeText(this, getString(R.string.export_done, uri.getLastPathSegment()),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从分类管理返回时无状态需要刷新（列表自刷新）
    }
}
