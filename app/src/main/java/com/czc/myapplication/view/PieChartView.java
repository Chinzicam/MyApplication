package com.czc.myapplication.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 自绘饼图 + 下方图例（分类明细）。
 * 数据为空时显示"暂无数据"。无需交互，纯展示。
 */
public class PieChartView extends View {

    public static class Slice {
        public String label;
        public long value;
        public int color;
        public Slice(String label, long value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Slice> mSlices = new ArrayList<>();
    private final Paint mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mSwatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mArcRect = new RectF();

    private float mPieDiameter = 220f;   // dp
    private float mLegendHeight = 0f;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mArcPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(0xFF757575);
        mLabelPaint.setStyle(Paint.Style.FILL);
        mLabelPaint.setColor(0xFF424242);
        mSwatchPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<Slice> slices) {
        mSlices.clear();
        if (slices != null) {
            mSlices.addAll(slices);
        }
        requestLayout();
        invalidate();
    }

    public long totalValue() {
        long sum = 0;
        for (Slice s : mSlices) sum += s.value;
        return sum;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        float legendHeight = 0;
        int rows = (mSlices.size() + 1) / 2;
        if (rows > 0) legendHeight = rows * 26f * density;
        int legendBottom = (int) (legendHeight > 0 ? legendHeight + 12 * density : 0);
        int h = (int) (mPieDiameter * density + legendBottom);
        setMeasuredDimension(resolveSize(getDefaultSize(0, widthMeasureSpec), widthMeasureSpec),
                h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        long total = totalValue();

        float diameter = mPieDiameter * density;
        float left = (getWidth() - diameter) / 2f;
        float top = density * 8f;
        mArcRect.set(left, top, left + diameter, top + diameter);

        if (total <= 0 || mSlices.isEmpty()) {
            mTextPaint.setTextSize(14 * density);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无数据", getWidth() / 2f, top + diameter / 2f, mTextPaint);
            return;
        }

        float startAngle = -90f;
        mArcPaint.setColor(0xFFE0E0E0);
        canvas.drawArc(mArcRect, 0, 360, true, mArcPaint); // 底圈
        for (Slice s : mSlices) {
            if (s.value <= 0) continue;
            float sweep = s.value * 360f / total;
            mArcPaint.setColor(s.color);
            canvas.drawArc(mArcRect, startAngle, sweep - 0.6f, true, mArcPaint);
            startAngle += sweep;
        }

        // 中心文字：总额（元）
        mTextPaint.setTextSize(16 * density);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        String center = "共" + com.czc.myapplication.util.MoneyUtils.format(total) + "元";
        canvas.drawText(center, getWidth() / 2f, top + diameter / 2f + 6 * density, mTextPaint);

        // 图例：最多显示全部，2 列
        mLegendHeight = 0;
        int rows = (mSlices.size() + 1) / 2;
        float legendTop = top + diameter + 14 * density;
        mLabelPaint.setTextSize(13 * density);
        mTextPaint.setTextSize(12 * density);
        float colWidth = getWidth() / 2f;
        for (int i = 0; i < mSlices.size(); i++) {
            Slice s = mSlices.get(i);
            int col = i % 2;
            int row = i / 2;
            float x = col * colWidth + 24 * density;
            float y = legendTop + row * 26 * density + 12 * density;
            mSwatchPaint.setColor(s.color);
            canvas.drawCircle(x, y - 5 * density, 6 * density, mSwatchPaint);
            float pct = total > 0 ? s.value * 100f / total : 0;
            mLabelPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(s.label, x + 12 * density, y, mLabelPaint);
            mTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", pct),
                    col * colWidth + colWidth - 12 * density, y, mTextPaint);
        }
        mLegendHeight = rows * 26 * density;
    }
}
