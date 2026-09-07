package com.offline.ledger;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class NutritionTodayActivity extends NutritionBaseActivity {
    private static final int OVER_YELLOW = Color.rgb(236, 180, 45);
    private static final int OVER_RED = Color.rgb(205, 67, 67);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M月d日 E", Locale.SIMPLIFIED_CHINESE);

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
        content.addView(dateControl());

        double[] t = totalsForDate();
        double over = Math.max(0d, NutritionData.safeNumber(t[0] - goal.kcal));
        int summaryColor = over > 200d ? OVER_RED : (over > 100d ? OVER_YELLOW : GREEN);
        int summaryTextColor = (over > 100d && over <= 200d) ? TEXT : Color.WHITE;

        LinearLayout summary = box();
        setCardBackground(summary,summaryColor);
        summary.setPadding(dp(16),dp(15),dp(16),dp(14));
        TextView a = text("热量摄入",13,true);
        TextView b = text(one(t[0]) + " / " + one(goal.kcal) + " kcal",29,true);
        TextView c = text(over > 0d ? "已超出 " + one(over) + " kcal" : "还可摄入 " + one(Math.max(0d, goal.kcal - t[0])) + " kcal",13,false);
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

        content.addView(microCard(t));
        content.addView(sectionLabel("餐食记录"));
        for(String meal:MEALS)content.addView(mealCard(meal));
    }

    private LinearLayout dateControl(){
        LinearLayout card=box();
        card.setPadding(dp(8),dp(7),dp(8),dp(7));
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button prev=button("‹");
        Button next=button("›");
        TextView date=text(selectedDate.format(DATE_FMT),17,true);
        date.setGravity(Gravity.CENTER);

        Button today=button("今天");
        today.setVisibility(selectedDate.equals(LocalDate.now())?View.GONE:View.VISIBLE);
        Button copy=button("复制");

        prev.setOnClickListener(v->{selectedDate=selectedDate.minusDays(1);refreshToday();});
        next.setOnClickListener(v->{selectedDate=selectedDate.plusDays(1);refreshToday();});
        today.setOnClickListener(v->{selectedDate=LocalDate.now();refreshToday();});
        copy.setOnClickListener(v->showCopyRecordsDialog());

        row.addView(prev,new LinearLayout.LayoutParams(dp(44),dp(40)));
        row.addView(date,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(today);
        row.addView(copy);
        row.addView(next,new LinearLayout.LayoutParams(dp(44),dp(40)));
        card.addView(row);
        return card;
    }

    private TextView sectionLabel(String label){
        TextView title=text(label,14,true);
        title.setTextColor(MUTED);
        title.setPadding(dp(4),dp(7),dp(4),dp(7));
        return title;
    }

    private LinearLayout microCard(double[] t){
        LinearLayout micro = box();
        LinearLayout microHead = new LinearLayout(this);
        microHead.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleWrap=new LinearLayout(this);
        titleWrap.setOrientation(LinearLayout.VERTICAL);
        titleWrap.addView(text("微量营养素",15,true));
        titleWrap.addView(muted("钙、铁、钾、镁、锌、维C、胆固醇"));
        Button toggle = button("展开 ▾");
        microHead.addView(titleWrap,new LinearLayout.LayoutParams(0,-2,1));
        microHead.addView(toggle);
        micro.addView(microHead);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(View.GONE);
        details.setPadding(0,dp(7),0,0);
        details.addView(text("钙 " + one(t[6]) + "mg  ·  铁 " + one(t[7]) + "mg  ·  钾 " + one(t[8]) + "mg",13,false));
        details.addView(text("镁 " + one(t[9]) + "mg  ·  锌 " + one(t[10]) + "mg  ·  维C " + one(t[11]) + "mg",13,false));
        details.addView(text("胆固醇 " + one(t[12]) + "mg",13,false));
        details.addView(muted("旧版记录不含的微量营养字段按0计算。"));
        micro.addView(details);

        View.OnClickListener toggleAction=v->{
            boolean expand=details.getVisibility()!=View.VISIBLE;
            details.setVisibility(expand?View.VISIBLE:View.GONE);
            toggle.setText(expand?"收起 ▴":"展开 ▾");
        };
        toggle.setOnClickListener(toggleAction);
        titleWrap.setOnClickListener(toggleAction);
        return micro;
    }

    private void setSummaryTextColor(TextView view, int color) {
        view.setTextColor(color);
    }

    private void addSummaryMetric(LinearLayout row, String name, double used, double target,
                                  String unit, int color, float weight) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(dp(3),dp(7),dp(3),dp(3));
        TextView label = text(name, 11, false);
        TextView value = text(one(used) + " / " + one(target) + " " + unit, 12, true);
        label.setGravity(Gravity.CENTER); value.setGravity(Gravity.CENTER);
        label.setTextColor(color); value.setTextColor(color);
        cell.addView(label); cell.addView(value);
        row.addView(cell, new LinearLayout.LayoutParams(0, -2, weight));
    }

    protected void refreshToday(){content.removeAllViews();showToday();}

    private LinearLayout mealCard(String meal){
        LinearLayout card = box();
        card.setPadding(dp(12),dp(9),dp(12),dp(9));
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
        title.setPadding(dp(2),dp(2),dp(4),dp(2));
        title.addView(text(meal,16,true));
        title.addView(muted(itemCount>0 ? one(kcal)+" kcal · "+itemCount+"项" : "尚未记录"));

        final int count = itemCount;
        Button toggle = button(count > 0 ? "▾" : "—");
        toggle.setEnabled(count > 0);
        Button add = button("＋");
        add.setOnClickListener(v -> showFoodPicker(meal));

        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        head.addView(toggle,new LinearLayout.LayoutParams(dp(46),dp(40)));
        head.addView(add,new LinearLayout.LayoutParams(dp(48),dp(40)));
        card.addView(head);

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setVisibility(View.GONE);
        details.setPadding(0,dp(6),0,0);

        for (NutritionData.Entry e : new ArrayList<>(entries)) {
            if(!selectedDate.toString().equals(e.date) || !meal.equals(e.meal)) continue;
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(9),dp(8),dp(7),dp(8));
            item.setBackground(rounded(SOFT,12,Color.TRANSPARENT));
            LinearLayout.LayoutParams itemParams=new LinearLayout.LayoutParams(-1,-2);
            itemParams.setMargins(0,0,0,dp(6));
            item.setLayoutParams(itemParams);

            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(e.name,14,true));
            if(e.isCombo()){
                detail.addView(muted(one(e.amount)+"份 · "+comboWeightSummary(e.components,1d)));
                detail.addView(muted(componentSummary(e.components,1d)));
            }else{
                detail.addView(muted(one(e.amount)+e.amountUnit+" · 蛋白"+one(e.protein)+"g · 脂肪"+one(e.fat)+"g · 碳水"+one(e.carb)+"g"));
            }
            TextView k = text(one(e.kcal)+" kcal",13,true);
            k.setGravity(Gravity.CENTER);
            Button del = button("×");
            del.setOnClickListener(v -> {
                List<NutritionData.Entry> keep = new ArrayList<>();
                for(NutritionData.Entry x:entries) if(!x.id.equals(e.id)) keep.add(x);
                entries = keep;
                NutritionData.saveEntries(this,entries);
                refreshToday();
            });
            item.addView(detail,new LinearLayout.LayoutParams(0,-2,1));
            item.addView(k);
            item.addView(del,new LinearLayout.LayoutParams(dp(42),dp(38)));
            details.addView(item);
        }

        card.addView(details);
        View.OnClickListener toggleAction=v->{
            if(count<=0)return;
            boolean expand = details.getVisibility() != View.VISIBLE;
            details.setVisibility(expand ? View.VISIBLE : View.GONE);
            toggle.setText(expand ? "▴" : "▾");
        };
        toggle.setOnClickListener(toggleAction);
        title.setOnClickListener(toggleAction);
        return card;
    }
}
