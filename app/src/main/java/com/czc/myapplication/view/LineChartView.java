package com.czc.myapplication.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.czc.myapplication.util.MoneyUtils;

import java.util.Locale;

/**
 * 自绘双折线趋势图（近 N 个月收入/支出）。
 */
public class LineChartView extends View {

    private String[] mLabels = new String[0];
    private long[] mValues1 = new long[0];
    private long[] mValues2 = new long[0];
    private int mColor1 = 0xFF43A047;
    private int mColor2 = 0xFFF4511E;

    private final Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(3f);
        mDotPaint.setStyle(Paint.Style.FILL);
        mLabelPaint.setColor(0xFF616161);
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = (int) (170 * getResources().getDisplayMetrics().density);
        setMeasuredDimension(resolveSize(getDefaultSize(0, widthMeasureSpec), widthMeasureSpec), h);
    }

    private long maxValue() {
        long max = 0;
        for (long v : mValues1) max = Math.max(max, v);
        for (long v : mValues2) max = Math.max(max, v);
        return max;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        int n = mLabels.length;
        if (n < 2) return;

        float chartTop = 16 * density;
        float chartBottom = getHeight() - 26 * density;
        float left = 30 * density;
        float right = getWidth() - 14 * density;
        float chartH = chartBottom - chartTop;
        long max = maxValue();

        // 基线 + 网格
        Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
        grid.setColor(0xFFF0F0F0);
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(1.5f);
        canvas.drawLine(left, chartBottom, right, chartBottom, grid);
        for (int i = 1; i <= 3; i++) {
            float y = chartBottom - chartH * i / 4f;
            canvas.drawLine(left, y, right, y, grid);
        }

        if (max <= 0) {
            mLabelPaint.setTextSize(13 * density);
            mLabelPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无数据", getWidth() / 2f, chartTop + chartH / 2, mLabelPaint);
            return;
        }

        float unit = chartH / (float) max;
        float step = (right - left) / (n - 1);

        drawSeries(canvas, mValues1, mColor1, left, chartBottom, step, unit, density);
        drawSeries(canvas, mValues2, mColor2, left, chartBottom, step, unit, density);

        mLabelPaint.setTextSize(11 * density);
        mLabelPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            canvas.drawText(mLabels[i], left + step * i, getHeight() - 4 * density, mLabelPaint);
        }
    }

    private void drawSeries(Canvas canvas, long[] values, int color,
                            float left, float bottom, float step, float unit, float density) {
        Path path = new Path();
        float[] xs = new float[values.length];
        float[] ys = new float[values.length];
        boolean any = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) continue;
            float x = left + step * i;
            float y = bottom - values[i] * unit;
            xs[i] = x;
            ys[i] = y;
            any = true;
            if (path.isEmpty()) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        if (!any) return;
        mLinePaint.setColor(color);
        canvas.drawPath(path, mLinePaint);
        mDotPaint.setColor(color);
        for (int i = 0; i < xs.length; i++) {
            if (values[i] > 0) {
                canvas.drawCircle(xs[i], ys[i], 3.5f * density, mDotPaint);
            }
        }
    }
}
