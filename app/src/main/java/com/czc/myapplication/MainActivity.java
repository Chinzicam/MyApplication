package com.czc.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.czc.myapplication.databinding.ActivityMainBinding;
import com.czc.myapplication.ui.MonthReportFragment;
import com.czc.myapplication.ui.RecordsFragment;
import com.czc.myapplication.ui.TodayFragment;
import com.czc.myapplication.ui.YearReportFragment;

/**
 * 主页面：底部 4 个 Tab（今日 / 流水 / 月报 / 年报）。
 * Fragment 采用 show/hide 保持各页状态，切换时刷新当前页数据。
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;
    private FragmentManager mFragmentManager;
    private Fragment mCurrent;

    private TodayFragment mTodayFrag;
    private RecordsFragment mRecordsFrag;
    private MonthReportFragment mMonthFrag;
    private YearReportFragment mYearFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mFragmentManager = getSupportFragmentManager();

        mBinding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) showTab(0);
            else if (id == R.id.nav_records) showTab(1);
            else if (id == R.id.nav_month) showTab(2);
            else if (id == R.id.nav_year) showTab(3);
            return true;
        });

        showTab(0);
    }

    private Fragment fragmentForTab(int index) {
        switch (index) {
            case 0:
                if (mTodayFrag == null) mTodayFrag = new TodayFragment();
                return mTodayFrag;
            case 1:
                if (mRecordsFrag == null) mRecordsFrag = new RecordsFragment();
                return mRecordsFrag;
            case 2:
                if (mMonthFrag == null) mMonthFrag = new MonthReportFragment();
                return mMonthFrag;
            case 3:
                if (mYearFrag == null) mYearFrag = new YearReportFragment();
                return mYearFrag;
            default:
                if (mTodayFrag == null) mTodayFrag = new TodayFragment();
                return mTodayFrag;
        }
    }

    private void showTab(int index) {
        Fragment target = fragmentForTab(index);
        if (target == mCurrent) {
            refreshCurrent();
            return;
        }
        FragmentTransaction ft = mFragmentManager.beginTransaction().setReorderingAllowed(true);
        if (mCurrent != null) ft.hide(mCurrent);
        if (!target.isAdded()) ft.add(R.id.fragment_container, target);
        ft.show(target);
        ft.commitNow();
        mCurrent = target;
        refreshCurrent();
    }

    private void refreshCurrent() {
        if (mCurrent == null) return;
        if (mCurrent instanceof TodayFragment) ((TodayFragment) mCurrent).refresh();
        else if (mCurrent instanceof RecordsFragment) ((RecordsFragment) mCurrent).refresh();
        else if (mCurrent instanceof MonthReportFragment) ((MonthReportFragment) mCurrent).refresh();
        else if (mCurrent instanceof YearReportFragment) ((YearReportFragment) mCurrent).refresh();
    }
}
