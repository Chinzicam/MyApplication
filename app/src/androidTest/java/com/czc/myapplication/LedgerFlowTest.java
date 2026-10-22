package com.czc.myapplication;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.czc.myapplication.data.RecordDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * 端到端流程测试：录入 → 统计联动 → 编辑 → 删除 → 分类管理 → 清空数据。
 */
@RunWith(AndroidJUnit4.class)
public class LedgerFlowTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void clearData() {
        Context ctx = ApplicationProvider.getApplicationContext();
        new RecordDao(ctx).deleteAll();
    }

    @After
    public void cleanup() {
        Context ctx = ApplicationProvider.getApplicationContext();
        new RecordDao(ctx).deleteAll();
    }

    @Test
    public void fullLedgerFlow() {
        // ========== 1. 今日页录入支出 32.50（餐饮 / 备注午餐） ==========
        onView(withId(R.id.et_amount)).perform(replaceText("32.50"));
        onView(withId(R.id.et_remark)).perform(replaceText("午餐"));
        onView(withId(R.id.btn_save)).perform(click());

        onView(withId(R.id.tv_today_expense)).check(matches(withText("32.50")));
        onView(withId(R.id.tv_today_balance)).check(matches(withText("-32.50")));
        onView(allOf(withText("午餐"), isDisplayed())).check(matches(isDisplayed()));

        // ========== 2. 录入收入 100（工资） ==========
        onView(withId(R.id.rb_income)).perform(click());
        onView(withId(R.id.et_amount)).perform(replaceText("100"));
        onView(withId(R.id.btn_save)).perform(click());
        onView(withId(R.id.tv_today_income)).check(matches(withText("100")));
        onView(withId(R.id.tv_today_balance)).check(matches(withText("67.50")));

        // ========== 3. 月报联动 ==========
        onView(withId(R.id.nav_month)).perform(click());
        onView(withId(R.id.tv_month_income)).check(matches(withText("100")));
        onView(withId(R.id.tv_month_expense)).check(matches(withText("32.50")));
        onView(withId(R.id.tv_month_balance)).check(matches(withText("67.50")));
        // 分类明细可能在折叠区下方，先滚动到可见
        onView(allOf(withId(R.id.tv_sum_name), withText("餐饮"))).perform(ViewActions.scrollTo())
                .check(matches(isDisplayed()));
        onView(allOf(withId(R.id.tv_sum_name), withText("工资"))).perform(ViewActions.scrollTo())
                .check(matches(isDisplayed()));

        // ========== 4. 年报联动 ==========
        onView(withId(R.id.nav_year)).perform(click());
        onView(withId(R.id.tv_year_income)).check(matches(withText("100")));
        onView(withId(R.id.tv_year_expense)).check(matches(withText("32.50")));
        onView(withId(R.id.tv_year_balance)).check(matches(withText("67.50")));

        // ========== 5. 流水页：分组列表 + 备注搜索 ==========
        onView(withId(R.id.nav_records)).perform(click());
        onView(allOf(withId(R.id.tv_group_date), isDisplayed())).check(matches(isDisplayed()));
        onView(withId(R.id.et_search)).perform(replaceText("午餐"));
        // 搜索后仍显示分组头与匹配记录
        onView(allOf(withId(R.id.tv_group_date), isDisplayed())).check(matches(isDisplayed()));
        onView(allOf(withId(R.id.tv_remark), isDisplayed())).check(matches(isDisplayed()));

        // ========== 6. 编辑记录：支出 32.50 → 40 ==========
        onView(withId(R.id.et_search)).perform(replaceText(""));
        onView(allOf(withText("午餐"), isDisplayed())).perform(click());
        onView(withId(R.id.et_amount)).perform(replaceText("40"));
        onView(withId(R.id.btn_save)).perform(click());
        onView(withId(R.id.tv_today_expense)).check(matches(withText("40")));

        // ========== 7. 删除记录（二次确认） ==========
        onView(allOf(withText("午餐"), isDisplayed())).perform(click());
        onView(withId(R.id.btn_delete)).perform(click());
        onView(withText("确认")).perform(click());
        onView(withId(R.id.tv_today_expense)).check(matches(withText("0")));

        // ========== 8. 分类管理 ==========
        onView(withId(R.id.nav_year)).perform(click());
        onView(withId(R.id.btn_settings_entry)).perform(ViewActions.scrollTo(), click());
        onView(withId(R.id.tv_manage_category)).perform(click());

        // 8a. 有记录的“工资”分类禁止删除（工资仍有 1 笔收入）
        onView(withId(R.id.rv_income)).perform(scrollTo(hasDescendant(withText("工资"))));
        onView(allOf(withId(R.id.btn_delete_cat), hasSibling(withText("工资")))).perform(click());
        onView(withText("确认")).perform(click());
        onView(allOf(withText("工资"), isDisplayed())).check(matches(isDisplayed()));

        // 8b. 新增分类“测试分类”并删除（无记录，可删）
        onView(withId(R.id.btn_add_category)).perform(click());
        onView(withId(R.id.et_category_name_input)).perform(replaceText("测试分类"));
        onView(withText("确认")).perform(click());
        onView(withId(R.id.rv_expense)).perform(scrollTo(hasDescendant(withText("测试分类"))));
        onView(allOf(withText("测试分类"), isDisplayed())).check(matches(isDisplayed()));

        onView(allOf(withId(R.id.btn_delete_cat), hasSibling(withText("测试分类")))).perform(click());
        onView(withText("确认")).perform(click());
        onView(allOf(withText("测试分类"), isDisplayed())).check(doesNotExist());

        // ========== 9. 设置页清空数据（强确认） ==========
        pressBack(); // 分类管理 → 设置
        onView(withId(R.id.tv_clear)).perform(click());
        onView(withText("确认")).perform(click());
        pressBack(); // 设置 → 主界面
        onView(withId(R.id.nav_today)).perform(click());
        onView(withId(R.id.tv_today_income)).check(matches(withText("0")));
        onView(withId(R.id.tv_today_expense)).check(matches(withText("0")));
    }
}
