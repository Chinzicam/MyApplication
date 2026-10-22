package com.czc.myapplication.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.czc.myapplication.util.MoneyUtils;

import java.util.Locale;

/**
 * 自绘分组柱状图（每组最多两根柱：收入/支出）。
 * 用于：月报双月对比、年报 12 个月收支趋势。
 * y 轴按数据最大值自动分档，柱顶显示金额（万元/千元缩写）。
 */
public class BarChartView extends View {

    private String[] mLabels = new String[0];
    private long[] mValues1 = new long[0]; // 收入
    private long[] mValues2 = new long[0]; // 支出
    private int mColor1 = 0xFF43A047;
    private int mColor2 = 0xFFF4511E;

    private final Paint mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mAxisPaint.setColor(0xFFE0E0E0);
        mAxisPaint.setStyle(Paint.Style.STROKE);
        mAxisPaint.setStrokeWidth(1.5f);
        mLabelPaint.setColor(0xFF616161);
        mValuePaint.setColor(0xFF424242);
    }

    public void setData(String[] labels, long[] income, long[] expense) {
        mLabels = labels == null ? new String[0] : labels;
        mValues1 = income == null ? new long[0] : income;
        mValues2 = expense == null ? new long[0] : expense;
        int n = Math.max(mLabels.length, Math.max(mValues1.length, mValues2.length));
        if (mLabels.length != n) mLabels = java.util.Arrays.copyOf(mLabels, n);
        if (mValues1.length != n) mValues1 = java.util.Arrays.copyOf(mValues1, n);
        if (mValues2.length != n) mValues2 = java.util.Arrays.copyOf(mValues2, n);
        invalidate();
    }

    public void setColors(int color1, int color2) {
        mColor1 = color1;
        mColor2 = color2;
        invalidate();
    }

    private long maxValue() {
        long max = 0;
        for (long v : mValues1) max = Math.max(max, v);
        for (long v : mValues2) max = Math.max(max, v);
        return max;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = (int) (200 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(getDefaultSize(0, widthMeasureSpec), widthMeasureSpec), h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        int n = mLabels.length;
        if (n == 0) return;

        float chartH = getHeight() - 36 * density; // 底部留标签
        float chartTop = 18 * density;
        float chartBottom = getHeight() - 14 * density;
        float left = 8 * density;
        float right = getWidth() - 8 * density;
        float chartW = right - left;

        long max = maxValue();
        float groupW = chartW / n;
        float barW = Math.min(groupW * 0.32f, 34 * density);

        // 基线
        mAxisPaint.setColor(0xFFBDBDBD);
        canvas.drawLine(left, chartBottom, right, chartBottom, mAxisPaint);

        if (max <= 0) {
            mLabelPaint.setTextSize(13 * density);
            mLabelPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无数据", getWidth() / 2f, chartTop + (chartBottom - chartTop) / 2, mLabelPaint);
            return;
        }

        // 网格线（4 档）
        mAxisPaint.setColor(0xFFF0F0F0);
        for (int i = 1; i <= 4; i++) {
            float y = chartBottom - (chartBottom - chartTop) * i / 4f;
            canvas.drawLine(left, y, right, y, mAxisPaint);
        }

        float unit = (chartBottom - chartTop) / (float) max;

        for (int i = 0; i < n; i++) {
            float cx = left + groupW * (i + 0.5f);
            float center = cx;

            // 收入柱
            if (mValues1[i] > 0) {
                float h = mValues1[i] * unit;
                mBarPaint.setColor(mColor1);
                canvas.drawRect(center - barW - 2 * density, chartBottom - h, center - 2 * density, chartBottom, mBarPaint);
                drawValue(canvas, mValues1[i], center - barW / 2f - 2 * density, chartBottom - h - 4 * density, density);
            }
            // 支出柱
            if (mValues2[i] > 0) {
                float h = mValues2[i] * unit;
                mBarPaint.setColor(mColor2);
                canvas.drawRect(center + 2 * density, chartBottom - h, center + barW + 2 * density, chartBottom, mBarPaint);
                drawValue(canvas, mValues2[i], center + barW / 2f + 2 * density, chartBottom - h - 4 * density, density);
            }

            // x 轴标签
            mLabelPaint.setTextSize(11 * density);
            mLabelPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(mLabels[i], center, getHeight() - 2 * density, mLabelPaint);
        }
    }

    private void drawValue(Canvas canvas, long cents, float cx, float y, float density) {
        if (cents <= 0) return;
        mValuePaint.setTextSize(10 * density);
        mValuePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(shortAmount(cents), cx, y, mValuePaint);
    }

    /** 金额缩写：>=1万 显示 x.xx万，否则显示整数元（去小数） */
    private String shortAmount(long cents) {
        double yuan = cents / 100.0;
        if (yuan >= 10000) {
            return String.format(Locale.getDefault(), "%.1f万", yuan / 10000.0);
        }
        return MoneyUtils.format(cents);
    }
}
