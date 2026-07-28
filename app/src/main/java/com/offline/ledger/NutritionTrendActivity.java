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

public abstract class NutritionTrendActivity extends NutritionFoodActivity {
    private LocalDate trendStart = LocalDate.now().minusDays(29);
    private LocalDate trendEnd = LocalDate.now();

    @Override protected void showTrend() {
        screen = "\u8d8b\u52bf";
        content.addView(text("\u8425\u517b\u8d8b\u52bf", 21, true));
        content.addView(muted("\u6309\u5929\u67e5\u770b\u70ed\u91cf\u548c\u4e09\u5927\u8425\u517b\u7d20\u7684\u53d8\u5316\u3002\u6ca1\u6709\u8bb0\u5f55\u7684\u65e5\u671f\u63090\u8ba1\u7b97\u3002"));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        for (int days : new int[]{7, 14, 30, 90}) {
            Button b = button(days + "\u5929");
            b.setOnClickListener(v -> setQuickRange(days));
            quick.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        content.addView(quick);

        LinearLayout dates = box();
        dates.addView(text("\u65f6\u95f4\u8303\u56f4", 14, true));
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        Button startButton = button(trendStart.toString());
        Button endButton = button(trendEnd.toString());
        startButton.setOnClickListener(v -> pickDate(true));
        endButton.setOnClickListener(v -> pickDate(false));
        dateRow.addView(startButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        dateRow.addView(text("\u81f3", 14, false), new LinearLayout.LayoutParams(dp(40), -2));
        dateRow.addView(endButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        dates.addView(dateRow);
        long dayCount = ChronoUnit.DAYS.between(trendStart, trendEnd) + 1;
        dates.addView(muted("\u5171 " + dayCount + " \u5929\uff0c\u70b9\u51fb\u65e5\u671f\u53ef\u81ea\u5b9a\u4e49\u3002"));
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
                Toast.makeText(this, "\u5f00\u59cb\u65e5\u671f\u4e0d\u80fd\u665a\u4e8e\u7ed3\u675f\u65e5\u671f", Toast.LENGTH_SHORT).show();
                return;
            }
            long count = ChronoUnit.DAYS.between(newStart, newEnd) + 1;
            if (count > 730) {
                Toast.makeText(this, "\u4e00\u6b21\u6700\u591a\u663e\u793a730\u5929", Toast.LENGTH_SHORT).show();
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
                values[0][index] += entry.kcal;
                values[1][index] += entry.protein;
                values[2][index] += entry.fat;
                values[3][index] += entry.carb;
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
        summary.addView(text("\u9009\u5b9a\u671f\u95f4\u6982\u51b5", 16, true));
        summary.addView(muted("\u6709\u8bb0\u5f55 " + recordedDays + " / " + count + " \u5929"));
        if (recordedDays > 0) {
            summary.addView(text("\u8bb0\u5f55\u65e5\u5e73\u5747 " + one(kcalSum / recordedDays) + " kcal", 18, true));
            summary.addView(muted("\u86cb\u767d\u8d28 " + one(proteinSum / recordedDays) + "g  \u00b7  \u8102\u80aa "
                    + one(fatSum / recordedDays) + "g  \u00b7  \u78b3\u6c34 " + one(carbSum / recordedDays) + "g"));
        } else {
            summary.addView(muted("\u8be5\u65f6\u95f4\u8303\u56f4\u5185\u8fd8\u6ca1\u6709\u996e\u98df\u8bb0\u5f55\u3002"));
        }
        content.addView(summary);

        content.addView(text("\u6bcf\u65e5\u70ed\u91cf\u8d8b\u52bf", 18, true));
        content.addView(muted("\u5b9e\u7ebf\u4e3a\u5b9e\u9645\u6444\u5165\uff0c\u865a\u7ebf\u4e3a\u5f53\u524d\u8bbe\u7f6e\u7684\u6bcf\u65e5\u70ed\u91cf\u4e0a\u9650\u3002"));
        double[][] calorieSeries = new double[][]{values[0]};
        TrendChartView calorieChart = new TrendChartView(this, dates, calorieSeries,
                new String[]{"\u70ed\u91cf kcal"},
                new int[]{GREEN},
                new double[]{goal.kcal});
        LinearLayout calorieBox = box();
        calorieBox.setPadding(dp(4), dp(4), dp(4), dp(4));
        calorieBox.addView(calorieChart, new LinearLayout.LayoutParams(-1, dp(280)));
        content.addView(calorieBox);

        content.addView(text("\u4e09\u5927\u8425\u517b\u7d20\u8d8b\u52bf", 18, true));
        content.addView(muted("\u5355\u4f4d\u4e3a\u514b\uff1b\u5404\u989c\u8272\u865a\u7ebf\u4e3a\u5f53\u524d\u76ee\u6807\u514b\u6570\u3002"));
        double[][] macroSeries = new double[][]{values[1], values[2], values[3]};
        int proteinColor = Color.rgb(47, 111, 184);
        int fatColor = Color.rgb(225, 139, 45);
        int carbColor = Color.rgb(143, 86, 176);
        TrendChartView macroChart = new TrendChartView(this, dates, macroSeries,
                new String[]{"\u86cb\u767d\u8d28", "\u8102\u80aa", "\u78b3\u6c34"},
                new int[]{proteinColor, fatColor, carbColor},
                new double[]{goal.protein, goal.fat, goal.carb});
        LinearLayout macroBox = box();
        macroBox.setPadding(dp(4), dp(4), dp(4), dp(4));
        macroBox.addView(macroChart, new LinearLayout.LayoutParams(-1, dp(300)));
        content.addView(macroBox);
    }
}
