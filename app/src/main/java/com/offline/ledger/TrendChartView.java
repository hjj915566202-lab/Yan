package com.offline.ledger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class TrendChartView extends View {
    private final List<LocalDate> dates;
    private final double[][] series;
    private final String[] names;
    private final int[] colors;
    private final double[] targets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    public TrendChartView(Context context, List<LocalDate> dates, double[][] series,
                          String[] names, int[] colors, double[] targets) {
        super(context);
        this.dates = dates;
        this.series = series;
        this.names = names;
        this.colors = colors;
        this.targets = targets;
        this.density = getResources().getDisplayMetrics().density;
        setBackgroundColor(Color.WHITE);
    }

    private float dp(float value) { return value * density; }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = resolveSize((int) dp(280), heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float left = dp(52), right = w - dp(12), top = dp(46), bottom = h - dp(34);
        if (right <= left || bottom <= top) return;

        double max = 0;
        for (double[] values : series) for (double value : values) max = Math.max(max, value);
        if (targets != null) for (double value : targets) max = Math.max(max, value);
        double axisMax = niceMaximum(max <= 0 ? 1 : max);

        drawLegend(canvas, left, dp(20));
        drawGrid(canvas, left, right, top, bottom, axisMax);
        drawTargets(canvas, left, right, top, bottom, axisMax);
        drawSeries(canvas, left, right, top, bottom, axisMax);
        drawDates(canvas, left, right, bottom);
    }

    private void drawLegend(Canvas canvas, float left, float y) {
        paint.setTextSize(dp(11));
        paint.setStrokeWidth(dp(2));
        float x = left;
        for (int i = 0; i < names.length; i++) {
            paint.setColor(colors[i]);
            canvas.drawCircle(x, y, dp(4), paint);
            x += dp(8);
            paint.setColor(Color.rgb(55, 69, 64));
            canvas.drawText(names[i], x, y + dp(4), paint);
            x += paint.measureText(names[i]) + dp(16);
        }
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom, double axisMax) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setPathEffect(null);
        paint.setTextSize(dp(10));
        for (int i = 0; i <= 4; i++) {
            float y = bottom - (bottom - top) * i / 4f;
            paint.setColor(Color.rgb(225, 232, 229));
            canvas.drawLine(left, y, right, y, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(105, 120, 114));
            String label = compact(axisMax * i / 4d);
            canvas.drawText(label, left - dp(6) - paint.measureText(label), y + dp(3), paint);
            paint.setStyle(Paint.Style.STROKE);
        }
        paint.setPathEffect(null);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTargets(Canvas canvas, float left, float right, float top, float bottom, double axisMax) {
        if (targets == null) return;
        paint.setStrokeWidth(dp(1.2f));
        paint.setPathEffect(new DashPathEffect(new float[]{dp(6), dp(5)}, 0));
        for (int i = 0; i < Math.min(targets.length, colors.length); i++) {
            if (targets[i] <= 0) continue;
            float y = yFor(targets[i], top, bottom, axisMax);
            paint.setColor(withAlpha(colors[i], 150));
            canvas.drawLine(left, y, right, y, paint);
        }
        paint.setPathEffect(null);
    }

    private void drawSeries(Canvas canvas, float left, float right, float top, float bottom, double axisMax) {
        int count = dates.size();
        if (count == 0) return;
        for (int s = 0; s < series.length; s++) {
            Path path = new Path();
            for (int i = 0; i < count; i++) {
                float x = count == 1 ? (left + right) / 2f : left + (right - left) * i / (count - 1f);
                float y = yFor(series[s][i], top, bottom, axisMax);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.2f));
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(colors[s]);
            canvas.drawPath(path, paint);
            if (count <= 31) {
                paint.setStyle(Paint.Style.FILL);
                for (int i = 0; i < count; i++) {
                    float x = count == 1 ? (left + right) / 2f : left + (right - left) * i / (count - 1f);
                    float y = yFor(series[s][i], top, bottom, axisMax);
                    canvas.drawCircle(x, y, dp(2.8f), paint);
                }
            }
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawDates(Canvas canvas, float left, float right, float bottom) {
        int count = dates.size();
        if (count == 0) return;
        paint.setColor(Color.rgb(105, 120, 114));
        paint.setTextSize(dp(9.5f));
        int labelCount = Math.min(5, count);
        for (int i = 0; i < labelCount; i++) {
            int index = labelCount == 1 ? 0 : Math.round(i * (count - 1f) / (labelCount - 1f));
            float x = count == 1 ? (left + right) / 2f : left + (right - left) * index / (count - 1f);
            String label = DATE_LABEL.format(dates.get(index));
            float textWidth = paint.measureText(label);
            float drawX = Math.max(dp(2), Math.min(getWidth() - textWidth - dp(2), x - textWidth / 2f));
            canvas.drawText(label, drawX, bottom + dp(20), paint);
        }
    }

    private float yFor(double value, float top, float bottom, double axisMax) {
        return bottom - (float) (Math.max(0, value) / axisMax) * (bottom - top);
    }

    private double niceMaximum(double value) {
        double exponent = Math.floor(Math.log10(value));
        double magnitude = Math.pow(10, exponent);
        double normalized = value / magnitude;
        double nice = normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10;
        return nice * magnitude;
    }

    private String compact(double value) {
        if (value >= 1000) {
            double k = value / 1000d;
            return Math.abs(k - Math.rint(k)) < 0.001 ? String.format("%.0fk", k) : String.format("%.1fk", k);
        }
        return Math.abs(value - Math.rint(value)) < 0.01 ? String.format("%.0f", value) : String.format("%.1f", value);
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
