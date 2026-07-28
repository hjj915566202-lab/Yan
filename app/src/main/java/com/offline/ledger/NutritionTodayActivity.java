package com.offline.ledger;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public abstract class NutritionTodayActivity extends NutritionBaseActivity {
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
        screen = "\u4eca\u65e5";
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = button("\u2039"), next = button("\u203a");
        TextView date = text(selectedDate.toString(), 17, true); date.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> { selectedDate = selectedDate.minusDays(1); refreshToday(); });
        next.setOnClickListener(v -> { selectedDate = selectedDate.plusDays(1); refreshToday(); });
        bar.addView(prev,new LinearLayout.LayoutParams(dp(60),-2));
        bar.addView(date,new LinearLayout.LayoutParams(0,-2,1));
        bar.addView(next,new LinearLayout.LayoutParams(dp(60),-2)); content.addView(bar);

        double[] t = totalsForDate();
        LinearLayout summary = box(); summary.setBackgroundColor(GREEN);
        TextView a=text("\u4eca\u65e5\u6444\u5165",14,false),b=text(one(t[0])+" kcal",31,true),
                c=text("\u76ee\u6807 "+one(goal.kcal)+" kcal \u00b7 \u5269\u4f59 "+one(Math.max(0,goal.kcal-t[0]))+" kcal",13,false);
        a.setTextColor(Color.WHITE);b.setTextColor(Color.WHITE);c.setTextColor(Color.WHITE);
        summary.addView(a);summary.addView(b);summary.addView(c);
        LinearLayout macros=new LinearLayout(this);macros.setOrientation(LinearLayout.HORIZONTAL);
        String[]vs={"\u86cb\u767d\n"+one(t[1])+"g","\u8102\u80aa\n"+one(t[2])+"g","\u78b3\u6c34\n"+one(t[3])+"g","\u7ea4\u7ef4\n"+one(t[4])+"g"};
        for(String s:vs){TextView v=text(s,13,true);v.setTextColor(Color.WHITE);v.setGravity(Gravity.CENTER);macros.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
        summary.addView(macros);content.addView(summary);

        content.addView(progressCard("\u70ed\u91cf",t[0],goal.kcal,"kcal"));
        content.addView(progressCard("\u86cb\u767d\u8d28",t[1],goal.protein,"g"));
        content.addView(progressCard("\u8102\u80aa",t[2],goal.fat,"g"));
        content.addView(progressCard("\u78b3\u6c34",t[3],goal.carb,"g"));
        content.addView(progressCard("\u81b3\u98df\u7ea4\u7ef4",t[4],goal.fiber,"g"));
        content.addView(progressCard("\u94a0",t[5],goal.sodium,"mg"));

        LinearLayout micro = box();
        LinearLayout microHead = new LinearLayout(this); microHead.setGravity(Gravity.CENTER_VERTICAL);
        TextView microTitle = text("\u5f53\u65e5\u5fae\u91cf\u8425\u517b\u7d20",16,true);
        Button toggle = button("\u5c55\u5f00 \u25be");
        microHead.addView(microTitle,new LinearLayout.LayoutParams(0,-2,1)); microHead.addView(toggle);
        micro.addView(microHead);

        LinearLayout microDetails = new LinearLayout(this); microDetails.setOrientation(LinearLayout.VERTICAL);
        microDetails.setVisibility(View.GONE);
        microDetails.addView(muted("\u6765\u81ea\u5df2\u8bb0\u5f55\u98df\u7269\u7684\u5408\u8ba1\uff1b\u65e7\u7248\u8bb0\u5f55\u4e0d\u542b\u7684\u5b57\u6bb5\u63090\u8ba1\u7b97\u3002"));
        microDetails.addView(text("\u9499 " + one(t[6]) + "mg  \u00b7  \u94c1 " + one(t[7]) + "mg  \u00b7  \u94be " + one(t[8]) + "mg",14,false));
        microDetails.addView(text("\u9541 " + one(t[9]) + "mg  \u00b7  \u950c " + one(t[10]) + "mg  \u00b7  \u7ef4C " + one(t[11]) + "mg",14,false));
        microDetails.addView(text("\u80c6\u56fa\u9187 " + one(t[12]) + "mg",14,false));
        micro.addView(microDetails);
        toggle.setOnClickListener(v -> {
            boolean expand = microDetails.getVisibility() != View.VISIBLE;
            microDetails.setVisibility(expand ? View.VISIBLE : View.GONE);
            toggle.setText(expand ? "\u6536\u8d77 \u25b4" : "\u5c55\u5f00 \u25be");
        });
        content.addView(micro);

        for(String meal:MEALS)content.addView(mealCard(meal));
    }

    protected void refreshToday(){content.removeAllViews();showToday();}

    private LinearLayout progressCard(String name,double used,double target,String unit){
        used=NutritionData.safeNumber(used); target=NutritionData.safeNumber(target);
        LinearLayout card=box(),top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(name,14,true),new LinearLayout.LayoutParams(0,-2,1));top.addView(muted(one(used)+" / "+one(target)+" "+unit));card.addView(top);
        ProgressBar p=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.setMax(1000);p.setProgress(target<=0?0:(int)Math.min(1000,used/target*1000));
        card.addView(p,new LinearLayout.LayoutParams(-1,dp(8)));return card;
    }

    private LinearLayout mealCard(String meal){
        LinearLayout card=box(),head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);double kcal=0;
        for(NutritionData.Entry e:entries)if(selectedDate.toString().equals(e.date)&&meal.equals(e.meal))kcal=NutritionData.safeNumber(kcal+NutritionData.safeNumber(e.kcal));
        LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.VERTICAL);title.addView(text(meal,17,true));title.addView(muted(one(kcal)+" kcal"));
        Button add=button("\uff0b \u6dfb\u52a0");add.setOnClickListener(v->showFoodPicker(meal));head.addView(title,new LinearLayout.LayoutParams(0,-2,1));head.addView(add);card.addView(head);
        boolean found=false;
        for(NutritionData.Entry e:new ArrayList<>(entries)){
            if(!selectedDate.toString().equals(e.date)||!meal.equals(e.meal))continue;found=true;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(7),0,dp(7));
            LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);detail.addView(text(e.name,14,true));
            detail.addView(muted(one(e.amount)+e.amountUnit+" \u00b7 \u86cb\u767d"+one(e.protein)+"g \u00b7 \u8102\u80aa"+one(e.fat)+"g \u00b7 \u78b3\u6c34"+one(e.carb)+"g"));
            TextView k=text(one(e.kcal)+" kcal",13,true);Button del=button("\u00d7");
            del.setOnClickListener(v->{List<NutritionData.Entry>keep=new ArrayList<>();for(NutritionData.Entry x:entries)if(!x.id.equals(e.id))keep.add(x);entries=keep;NutritionData.saveEntries(this,entries);refreshToday();});
            row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.addView(k);row.addView(del,new LinearLayout.LayoutParams(dp(56),-2));card.addView(row);
        }
        if(!found)card.addView(muted("\u5c1a\u672a\u8bb0\u5f55"));return card;
    }
}
