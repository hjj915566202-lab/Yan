package com.offline.ledger;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public abstract class NutritionTodayActivity extends NutritionBaseActivity {
    protected double[] totalsForDate() {
        double[] t = new double[6];
        String d = selectedDate.toString();
        for (NutritionData.Entry e : entries) {
            if (!d.equals(e.date)) continue;
            t[0] += e.kcal;
            t[1] += e.protein;
            t[2] += e.fat;
            t[3] += e.carb;
            t[4] += e.fiber;
            t[5] += e.sodium;
        }
        return t;
    }

    @Override protected void showToday() {
        screen = "今日";
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = button("‹"), next = button("›");
        TextView date = text(selectedDate.toString(), 17, true);
        date.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> {
            selectedDate = selectedDate.minusDays(1);
            refreshToday();
        });
        next.setOnClickListener(v -> {
            selectedDate = selectedDate.plusDays(1);
            refreshToday();
        });
        bar.addView(prev, new LinearLayout.LayoutParams(dp(60), -2));
        bar.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(next, new LinearLayout.LayoutParams(dp(60), -2));
        content.addView(bar);

        double[] t = totalsForDate();
        LinearLayout summary = box();
        summary.setBackgroundColor(GREEN);
        TextView a = text("今日摄入", 14, false);
        TextView b = text(one(t[0]) + " kcal", 31, true);
        TextView c = text("目标 " + one(goal.kcal) + " kcal · 剩余 "
                + one(Math.max(0, goal.kcal - t[0])) + " kcal", 13, false);
        a.setTextColor(Color.WHITE);
        b.setTextColor(Color.WHITE);
        c.setTextColor(Color.WHITE);
        summary.addView(a);
        summary.addView(b);
        summary.addView(c);
        LinearLayout macros = new LinearLayout(this);
        macros.setOrientation(LinearLayout.HORIZONTAL);
        String[] vs = {"蛋白\n" + one(t[1]) + "g", "脂肪\n" + one(t[2]) + "g",
                "碳水\n" + one(t[3]) + "g", "纤维\n" + one(t[4]) + "g"};
        for (String s : vs) {
            TextView v = text(s, 13, true);
            v.setTextColor(Color.WHITE);
            v.setGravity(Gravity.CENTER);
            macros.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
        }
        summary.addView(macros);
        content.addView(summary);

        content.addView(progressCard("热量", t[0], goal.kcal, "kcal"));
        content.addView(progressCard("蛋白质", t[1], goal.protein, "g"));
        content.addView(progressCard("脂肪", t[2], goal.fat, "g"));
        content.addView(progressCard("碳水", t[3], goal.carb, "g"));
        content.addView(progressCard("膳食纤维", t[4], goal.fiber, "g"));
        content.addView(progressCard("钠", t[5], goal.sodium, "mg"));
        for (String meal : MEALS) content.addView(mealCard(meal));
    }

    protected void refreshToday() {
        content.removeAllViews();
        showToday();
    }

    private LinearLayout progressCard(String name, double used, double target, String unit) {
        LinearLayout card = box(), top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(name, 14, true), new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(muted(one(used) + " / " + one(target) + " " + unit));
        card.addView(top);
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(1000);
        p.setProgress(target <= 0 ? 0 : (int) Math.min(1000, used / target * 1000));
        card.addView(p, new LinearLayout.LayoutParams(-1, dp(8)));
        return card;
    }

    private LinearLayout mealCard(String meal) {
        LinearLayout card = box(), head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        double kcal = 0;
        for (NutritionData.Entry e : entries) {
            if (selectedDate.toString().equals(e.date) && meal.equals(e.meal)) kcal += e.kcal;
        }
        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.addView(text(meal, 17, true));
        title.addView(muted(one(kcal) + " kcal"));
        Button add = button("＋ 添加");
        add.setOnClickListener(v -> showFoodPicker(meal));
        head.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(add);
        card.addView(head);

        boolean found = false;
        for (NutritionData.Entry e : new ArrayList<>(entries)) {
            if (!selectedDate.toString().equals(e.date) || !meal.equals(e.meal)) continue;
            found = true;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(7), 0, dp(7));
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(e.name, 14, true));
            detail.addView(muted(one(e.amount) + e.amountUnit + " · 蛋白" + one(e.protein)
                    + "g · 脂肪" + one(e.fat) + "g · 碳水" + one(e.carb) + "g"));
            TextView k = text(one(e.kcal) + " kcal", 13, true);
            Button del = button("×");
            del.setOnClickListener(v -> {
                List<NutritionData.Entry> keep = new ArrayList<>();
                for (NutritionData.Entry x : entries) if (!x.id.equals(e.id)) keep.add(x);
                entries = keep;
                NutritionData.saveEntries(this, entries);
                refreshToday();
            });
            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(k);
            row.addView(del, new LinearLayout.LayoutParams(dp(56), -2));
            card.addView(row);
        }
        if (!found) card.addView(muted("尚未记录"));
        return card;
    }
}
