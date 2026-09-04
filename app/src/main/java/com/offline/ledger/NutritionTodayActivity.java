package com.offline.ledger;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public abstract class NutritionTodayActivity extends NutritionBaseActivity {
    private static final int OVER_YELLOW = Color.rgb(236, 180, 45);
    private static final int OVER_RED = Color.rgb(205, 67, 67);

    private static void addSafe(double[] totals, int index, double value) {
        totals[index] = NutritionData.safeNumber(totals[index] + NutritionData.safeNumber(value));
    }

    protected double[] totalsForDate() {
        double[] t = new double[13];
        String d = selectedDate.toString();
        for (NutritionData.Entry e : entries) {
            if (!d.equals(e.date)) continue;
            addSafe(t,0,e.kcal); addSafe(t,1,e.protein); addSafe(t,2,e.fat); addSafe(t,3,e.carb);
            addSafe(t,4,e.fiber); addSafe(t,5,e.sodium); addSafe(t,6,e.calcium); addSafe(t,7,e.iron);
            addSafe(t,8,e.potassium); addSafe(t,9,e.magnesium); addSafe(t,10,e.zinc);
            addSafe(t,11,e.vitaminC); addSafe(t,12,e.cholesterol);
        }
        return t;
    }

    @Override protected void showToday() {
        screen = "今日";
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = button("‹"), next = button("›");
        TextView date = text(selectedDate.toString(), 17, true); date.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> { selectedDate = selectedDate.minusDays(1); refreshToday(); });
        next.setOnClickListener(v -> { selectedDate = selectedDate.plusDays(1); refreshToday(); });
        bar.addView(prev,new LinearLayout.LayoutParams(dp(60),-2));
        bar.addView(date,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(next,new LinearLayout.LayoutParams(dp(60),-2)); content.addView(bar);

        Button copy = button("从其他日期复制记录");
        copy.setOnClickListener(v -> showCopyRecordsDialog());
        content.addView(copy);

        double[] t = totalsForDate();
        double over = Math.max(0d, NutritionData.safeNumber(t[0] - goal.kcal));
        int summaryColor = over > 200d ? OVER_RED : (over > 100d ? OVER_YELLOW : GREEN);
        int summaryTextColor = (over > 100d && over <= 200d) ? TEXT : Color.WHITE;

        LinearLayout summary = box();
        summary.setBackgroundColor(summaryColor);
        TextView a = text("今日摄入 / 每日目标",14,false);
        TextView b = text(one(t[0]) + " / " + one(goal.kcal) + " kcal",30,true);
        TextView c = text(over > 0d ? "已超出 " + one(over) + " kcal" : "剩余 " + one(Math.max(0d, goal.kcal - t[0])) + " kcal",13,false);
        setSummaryTextColor(a, summaryTextColor); setSummaryTextColor(b, summaryTextColor); setSummaryTextColor(c, summaryTextColor);
        summary.addView(a); summary.addView(b); summary.addView(c);

        LinearLayout macroRow = new LinearLayout(this);
        macroRow.setOrientation(LinearLayout.HORIZONTAL);
        addSummaryMetric(macroRow, "蛋白质", t[1], goal.protein, "g", summaryTextColor, 1f);
        addSummaryMetric(macroRow, "脂肪", t[2], goal.fat, "g", summaryTextColor, 1f);
        addSummaryMetric(macroRow, "碳水", t[3], goal.carb, "g", summaryTextColor, 1f);
        summary.addView(macroRow);

        LinearLayout otherRow = new LinearLayout(this);
        otherRow.setOrientation(LinearLayout.HORIZONTAL);
        addSummaryMetric(otherRow, "膳食纤维", t[4], goal.fiber, "g", summaryTextColor, 1f);
        addSummaryMetric(otherRow, "钠", t[5], goal.sodium, "mg", summaryTextColor, 1f);
        summary.addView(otherRow);
        content.addView(summary);

        LinearLayout micro = box();
        LinearLayout microHead = new LinearLayout(this); microHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView microTitle = text("当日微量营养素",16,true);
        Button toggle = button("展开 ▾");
        microHead.addView(microTitle,new LinearLayout.LayoutParams(0,-2,1)); microHead.addView(toggle);
        micro.addView(microHead);
        LinearLayout microDetails = new LinearLayout(this); microDetails.setOrientation(LinearLayout.VERTICAL);
        microDetails.setVisibility(View.GONE);
        microDetails.addView(muted("来自已记录食物的合计；旧版记录不含的字段按0计算。"));
        microDetails.addView(text("钙 " + one(t[6]) + "mg  ·  铁 " + one(t[7]) + "mg  ·  钾 " + one(t[8]) + "mg",14,false));
        microDetails.addView(text("镁 " + one(t[9]) + "mg  ·  锌 " + one(t[10]) + "mg  ·  维C " + one(t[11]) + "mg",14,false));
        microDetails.addView(text("胆固醇 " + one(t[12]) + "mg",14,false));
        micro.addView(microDetails);
        toggle.setOnClickListener(v -> {
            boolean expand = microDetails.getVisibility() != View.VISIBLE;
            microDetails.setVisibility(expand ? View.VISIBLE : View.GONE);
            toggle.setText(expand ? "收起 ▴" : "展开 ▾");
        });
        content.addView(micro);

        for(String meal:MEALS)content.addView(mealCard(meal));
    }

    private void setSummaryTextColor(TextView view, int color) {
        view.setTextColor(color);
    }

    private void addSummaryMetric(LinearLayout row, String name, double used, double target,
                                  String unit, int color, float weight) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(4), dp(5), dp(4), dp(5));
        TextView label = text(name, 12, false);
        TextView value = text(one(used) + " / " + one(target) + " " + unit, 13, true);
        label.setGravity(Gravity.CENTER); value.setGravity(Gravity.CENTER);
        label.setTextColor(color); value.setTextColor(color);
        cell.addView(label); cell.addView(value);
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, weight));
    }

    protected void refreshToday(){content.removeAllViews();showToday();}

    private LinearLayout mealCard(String meal){
        LinearLayout card = box();
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);

        double kcal = 0d;
        int itemCount = 0;
        for (NutritionData.Entry e : entries) {
            if (selectedDate.toString().equals(e.date) && meal.equals(e.meal)) {
                kcal += NutritionData.safeNumber(e.kcal);
                itemCount++;
            }
        }

        LinearLayout title = new LinearLayout(this);
        title.setOrientation(LinearLayout.VERTICAL);
        title.addView(text(meal,17,true));
        title.addView(muted(one(kcal) + " kcal · " + itemCount + "项"));

        final int count = itemCount;
        Button toggle = button(count > 0 ? "展开 ▾" : "无记录");
        toggle.setEnabled(count > 0);
        Button add = button("＋ 添加");
        add.setOnClickListener(v -> showFoodPicker(meal));

        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        head.addView(toggle);
        head.addView(add);
        card.addView(head);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(View.GONE);

        for (NutritionData.Entry e : new ArrayList<>(entries)) {
            if(!selectedDate.toString().equals(e.date) || !meal.equals(e.meal)) continue;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0,dp(7),0,dp(7));
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(e.name,14,true));
            if(e.isCombo()){
                detail.addView(muted(one(e.amount)+"份 · "+comboWeightSummary(e.components,1d)));
                detail.addView(muted(componentSummary(e.components,1d)));
            }else{
                detail.addView(muted(one(e.amount)+e.amountUnit));
            }
            detail.addView(muted("蛋白"+one(e.protein)+"g · 脂肪"+one(e.fat)+"g · 碳水"+one(e.carb)+"g"));
            TextView k = text(one(e.kcal)+" kcal",13,true);
            Button del = button("×");
            del.setOnClickListener(v -> {
                List<NutritionData.Entry> keep = new ArrayList<>();
                for(NutritionData.Entry x:entries) if(!x.id.equals(e.id)) keep.add(x);
                entries = keep;
                NutritionData.saveEntries(this,entries);
                refreshToday();
            });
            row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(k);
            row.addView(del,new LinearLayout.LayoutParams(dp(56),-2));
            details.addView(row);
        }

        card.addView(details);
        toggle.setOnClickListener(v -> {
            boolean expand = details.getVisibility() != View.VISIBLE;
            details.setVisibility(expand ? View.VISIBLE : View.GONE);
            toggle.setText(expand ? "收起 ▴" : "展开 ▾");
        });
        return card;
    }
}
