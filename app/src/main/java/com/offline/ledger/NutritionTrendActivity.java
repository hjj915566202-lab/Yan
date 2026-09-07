package com.offline.ledger;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public abstract class NutritionTrendActivity extends NutritionMealToolsActivity {
    private LocalDate trendStart = LocalDate.now().minusDays(29);
    private LocalDate trendEnd = LocalDate.now();

    @Override protected void showTrend() {
        screen = "趋势";
        content.addView(text("营养趋势", 21, true));
        content.addView(muted("按天查看热量和三大营养素的变化。没有记录的日期按0计算。"));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        for (int days : new int[]{7, 14, 30, 90}) {
            Button b = button(days + "天");
            b.setOnClickListener(v -> setQuickRange(days));
            quick.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        content.addView(quick);

        LinearLayout dates = box();
        dates.addView(text("时间范围", 14, true));
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        Button startButton = button(trendStart.toString());
        Button endButton = button(trendEnd.toString());
        startButton.setOnClickListener(v -> pickDate(true));
        endButton.setOnClickListener(v -> pickDate(false));
        dateRow.addView(startButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        dateRow.addView(text("至", 14, false), new LinearLayout.LayoutParams(dp(40), -2));
        dateRow.addView(endButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        dates.addView(dateRow);
        long dayCount = ChronoUnit.DAYS.between(trendStart, trendEnd) + 1;
        dates.addView(muted("共 " + dayCount + " 天，点击日期可自定义。"));
        content.addView(dates);

        renderTrendCharts();
    }

    private void setQuickRange(int days) {
        trendEnd = LocalDate.now();
        trendStart = trendEnd.minusDays(days - 1L);
        refreshTrend();
    }

    private void pickDate(boolean isStart) {
        LocalDate current = isStart ? trendStart : trendEnd;
        new DatePickerDialog(this, (view, year, month, day) -> {
            LocalDate selected = LocalDate.of(year, month + 1, day);
            LocalDate newStart = isStart ? selected : trendStart;
            LocalDate newEnd = isStart ? trendEnd : selected;
            if (newStart.isAfter(newEnd)) {
                Toast.makeText(this, "开始日期不能晚于结束日期", Toast.LENGTH_SHORT).show();
                return;
            }
            long count = ChronoUnit.DAYS.between(newStart, newEnd) + 1;
            if (count > 730) {
                Toast.makeText(this, "一次最多显示730天", Toast.LENGTH_SHORT).show();
                return;
            }
            trendStart = newStart;
            trendEnd = newEnd;
            refreshTrend();
        }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    private void refreshTrend() {
        content.removeAllViews();
        showTrend();
    }

    private void renderTrendCharts() {
        int count = (int) (ChronoUnit.DAYS.between(trendStart, trendEnd) + 1);
        List<LocalDate> dates = new ArrayList<>(count);
        double[][] values = new double[4][count];
        boolean[] recorded = new boolean[count];
        for (int i = 0; i < count; i++) dates.add(trendStart.plusDays(i));

        for (NutritionData.Entry entry : entries) {
            try {
                LocalDate date = LocalDate.parse(entry.date);
                if (date.isBefore(trendStart) || date.isAfter(trendEnd)) continue;
                int index = (int) ChronoUnit.DAYS.between(trendStart, date);
                values[0][index] += NutritionData.safeNumber(entry.kcal);
                values[1][index] += NutritionData.safeNumber(entry.protein);
                values[2][index] += NutritionData.safeNumber(entry.fat);
                values[3][index] += NutritionData.safeNumber(entry.carb);
                recorded[index] = true;
            } catch (Exception ignored) {}
        }

        int recordedDays = 0;
        double kcalSum = 0, proteinSum = 0, fatSum = 0, carbSum = 0;
        for (int i = 0; i < count; i++) {
            if (!recorded[i]) continue;
            recordedDays++;
            kcalSum += values[0][i];
            proteinSum += values[1][i];
            fatSum += values[2][i];
            carbSum += values[3][i];
        }

        LinearLayout summary = box();
        summary.addView(text("选定期间概况", 16, true));
        summary.addView(muted("有记录 " + recordedDays + " / " + count + " 天"));
        if (recordedDays > 0) {
            summary.addView(text("记录日平均 " + one(kcalSum / recordedDays) + " kcal", 18, true));
            summary.addView(muted("蛋白质 " + one(proteinSum / recordedDays) + "g  ·  脂肪 "
                    + one(fatSum / recordedDays) + "g  ·  碳水 " + one(carbSum / recordedDays) + "g"));
        } else {
            summary.addView(muted("该时间范围内还没有饮食记录。"));
        }
        content.addView(summary);

        content.addView(text("每日热量趋势", 18, true));
        content.addView(muted("实线为实际摄入，虚线为当前设置的每日热量上限。"));
        double[][] calorieSeries = new double[][]{values[0]};
        TrendChartView calorieChart = new TrendChartView(this, dates, calorieSeries,
                new String[]{"热量 kcal"},
                new int[]{GREEN},
                new double[]{goal.kcal});
        LinearLayout calorieBox = box();
        calorieBox.setPadding(dp(4), dp(4), dp(4), dp(4));
        calorieBox.addView(calorieChart, new LinearLayout.LayoutParams(-1, dp(280)));
        content.addView(calorieBox);

        content.addView(text("三大营养素趋势", 18, true));
        content.addView(muted("单位为克；各颜色虚线为当前目标克数。"));
        double[][] macroSeries = new double[][]{values[1], values[2], values[3]};
        int proteinColor = Color.rgb(47, 111, 184);
        int fatColor = Color.rgb(225, 139, 45);
        int carbColor = Color.rgb(143, 86, 176);
        TrendChartView macroChart = new TrendChartView(this, dates, macroSeries,
                new String[]{"蛋白质", "脂肪", "碳水"},
                new int[]{proteinColor, fatColor, carbColor},
                new double[]{goal.protein, goal.fat, goal.carb});
        LinearLayout macroBox = box();
        macroBox.setPadding(dp(4), dp(4), dp(4), dp(4));
        macroBox.addView(macroChart, new LinearLayout.LayoutParams(-1, dp(300)));
        content.addView(macroBox);
    }
}
