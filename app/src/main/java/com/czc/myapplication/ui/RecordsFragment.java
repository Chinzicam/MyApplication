package com.czc.myapplication.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.czc.myapplication.R;
import com.czc.myapplication.data.CategoryDao;
import com.czc.myapplication.data.Record;
import com.czc.myapplication.data.RecordDao;
import com.czc.myapplication.databinding.FragmentRecordsBinding;
import com.czc.myapplication.util.DateUtils;
import com.czc.myapplication.util.YearMonthPicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Tab2 流水：按日期分组展示全部历史记录，支持单日/单月筛选、备注搜索、下拉刷新、无限滚动 */
public class RecordsFragment extends Fragment {

    private static final int PAGE_SIZE = 50;

    /** 筛选模式 */
    private static final int MODE_ALL = 0;
    private static final int MODE_DAY = 1;
    private static final int MODE_MONTH = 2;

    private FragmentRecordsBinding mB;
    private CategoryDao mCategoryDao;
    private RecordDao mRecordDao;
    private RecordsAdapter mAdapter;

    private int mMode = MODE_ALL;
    private long mFilterDay = DateUtils.todayStartMillis();
    private int mFilterYear = DateUtils.currentYear();
    private int mFilterMonth = DateUtils.currentMonth();

    private boolean mLoading = false;
    private boolean mHasMore = true;
    private String mKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mB = FragmentRecordsBinding.inflate(inflater, container, false);
        mCategoryDao = new CategoryDao(requireContext());
        mRecordDao = new RecordDao(requireContext());
        return mB.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAdapter = new RecordsAdapter(mCategoryDao);
        mAdapter.setOnItemClickListener(record ->
                startActivity(new android.content.Intent(requireContext(), EditRecordActivity.class)
                        .putExtra(EditRecordActivity.EXTRA_RECORD_ID, record.id)));

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        mB.rvRecords.setLayoutManager(lm);
        mB.rvRecords.setAdapter(mAdapter);

        // 无限滚动：接近底部加载下一页
        mB.rvRecords.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (mMode != MODE_ALL || mLoading || !mHasMore) return;
                int total = lm.getItemCount();
                int lastVisible = lm.findLastVisibleItemPosition();
                if (lastVisible >= total - 8) {
                    loadNextPage();
                }
            }
        });

        // 下拉刷新
        mB.swipeRefresh.setOnRefreshListener(this::refresh);
        mB.swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        // 筛选模式
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"全部记录", "按日查看", "按月查看"});
        mB.spFilterMode.setAdapter(modeAdapter);
        mB.spFilterMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                int old = mMode;
                mMode = position;
                if (mMode != old) {
                    updateFilterLabel();
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mB.tvFilterDate.setOnClickListener(v -> {
            if (mMode == MODE_DAY) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(mFilterDay);
                new DatePickerDialog(requireContext(), (dv, y, mo, d) -> {
                    Calendar pick = Calendar.getInstance();
                    pick.clear();
                    pick.set(y, mo, d);
                    mFilterDay = pick.getTimeInMillis();
                    updateFilterLabel();
                    reload();
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
            } else if (mMode == MODE_MONTH) {
                YearMonthPicker.show(requireContext(), mFilterYear, mFilterMonth,
                        (y, m) -> {
                            mFilterYear = y;
                            mFilterMonth = m;
                            updateFilterLabel();
                            reload();
                        });
            } else {
                // 全部模式点日期无意义
            }
        });

        // 备注搜索
        mB.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mKeyword = mB.etSearch.getText().toString().trim();
                reload();
                return true;
            }
            return false;
        });

        updateFilterLabel();
        reload();
    }

    private void updateFilterLabel() {
        if (mMode == MODE_DAY) {
            mB.tvFilterDate.setText(DateUtils.formatDate(mFilterDay));
        } else if (mMode == MODE_MONTH) {
            mB.tvFilterDate.setText(DateUtils.formatMonth(mFilterYear, mFilterMonth));
        } else {
            mB.tvFilterDate.setText(R.string.filter_all);
        }
    }

    public void refresh() {
        reload();
        mB.swipeRefresh.setRefreshing(false);
    }

    private void reload() {
        mHasMore = true;
        mLoading = false;
        loadNextPage();
    }

    private void loadNextPage() {
        if (mLoading || !mHasMore) return;
        mLoading = true;

        List<Record> page;
        if (mMode == MODE_DAY) {
            long start = mFilterDay;
            long end = DateUtils.endOfDayExclusive(mFilterDay);
            page = mKeyword.isEmpty()
                    ? mRecordDao.queryByRange(start, end)
                    : filterByKeyword(mRecordDao.queryByRange(start, end));
            mHasMore = false;
        } else if (mMode == MODE_MONTH) {
            long start = DateUtils.monthStartMillis(mFilterYear, mFilterMonth);
            long end = DateUtils.monthEndExclusive(mFilterYear, mFilterMonth);
            page = mKeyword.isEmpty()
                    ? mRecordDao.queryByRange(start, end)
                    : filterByKeyword(mRecordDao.queryByRange(start, end));
            mHasMore = false;
        } else {
            if (mKeyword.isEmpty()) {
                page = mRecordDao.queryPage(mAdapter.itemCount(), PAGE_SIZE);
                mHasMore = page.size() == PAGE_SIZE;
            } else {
                int offset = mAdapter.itemCount();
                page = mRecordDao.searchByKeyword(mKeyword, offset + PAGE_SIZE);
                mHasMore = page.size() > offset;
                page = page.size() > offset ? page.subList(offset, page.size()) : new ArrayList<>();
            }
        }
        mLoading = false;

        if (mAdapter.itemCount() == 0 && mHasMore) {
            mAdapter.setData(page);
        } else {
            mAdapter.appendData(page);
        }
        mB.tvRecordsEmpty.setVisibility(mAdapter.itemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private List<Record> filterByKeyword(List<Record> all) {
        if (mKeyword.isEmpty()) return all;
        List<Record> out = new ArrayList<>();
        for (Record r : all) {
            if (r.remark != null && r.remark.contains(mKeyword)) out.add(r);
        }
        return out;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }
}
