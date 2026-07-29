package com.offline.ledger;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class NutritionBaseActivity extends Activity {
    protected static final int GREEN = Color.rgb(24,167,125), BG = Color.rgb(244,247,246), TEXT = Color.rgb(24,35,31), MUTED = Color.rgb(108,125,119);
    protected static final String[] MEALS = {"早餐","午餐","晚餐","加餐"};
    protected LinearLayout root, content, nav;
    protected LocalDate selectedDate = LocalDate.now();
    protected String screen = "今日";
    protected List<NutritionData.Entry> entries;
    protected List<NutritionData.Food> customFoods;
    protected List<NutritionData.Food> builtInFoods;
    protected NutritionData.Goal goal;

    @Override public void onCreate(Bundle state){super.onCreate(state);reload();buildShell();}
    protected void reload(){
        entries=NutritionData.loadEntries(this);
        customFoods=NutritionData.loadCustomFoods(this);
        builtInFoods=FoodCatalog.commonFoods(this);
        goal=NutritionData.loadGoal(this);
    }
    protected int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    protected TextView text(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(TEXT);v.setPadding(dp(8),dp(7),dp(8),dp(7));if(bold)v.setTypeface(null,1);return v;}
    protected TextView muted(String s){TextView v=text(s,12,false);v.setTextColor(MUTED);return v;}
    protected Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    protected EditText input(String hint,boolean decimal){EditText e=new EditText(this);e.setHint(hint);if(decimal)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);return e;}
    protected LinearLayout box(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(13),dp(16),dp(13));b.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(10));b.setLayoutParams(p);return b;}
    protected Spinner spinner(String[] values,String selected){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));for(int i=0;i<values.length;i++)if(values[i].equals(selected))s.setSelection(i);return s;}
    protected double parse(EditText e){try{return NutritionData.safeNumber(Double.parseDouble(e.getText().toString().trim()));}catch(Exception x){return 0;}}
    protected double valueOr(EditText e,double fallback){double v=parse(e);return v>0?v:fallback;}
    protected String one(double n){n=NutritionData.safeNumber(n);return String.format(Locale.US,n==Math.rint(n)?"%.0f":"%.1f",n);}
    protected List<NutritionData.Food> allFoods(){List<NutritionData.Food>a=new ArrayList<>(customFoods.size()+builtInFoods.size());a.addAll(customFoods);a.addAll(builtInFoods);return a;}

    protected String componentSummary(List<NutritionData.Component> components,double scale){
        if(components==null||components.isEmpty())return "";
        StringBuilder out=new StringBuilder();
        for(NutritionData.Component c:components){
            if(c==null)continue;
            if(out.length()>0)out.append(" · ");
            out.append(c.name).append(one(c.amount*scale)).append(c.amountUnit);
        }
        return out.toString();
    }

    protected String comboWeightSummary(List<NutritionData.Component> components,double scale){
        if(components==null||components.isEmpty())return "";
        double total=0;boolean any=false,complete=true;
        for(NutritionData.Component c:components){
            if(c==null)continue;
            if(c.weightKnown){total+=NutritionData.safeNumber(c.weightGrams)*scale;any=true;}else complete=false;
        }
        if(!any)return "重量未填写";
        return (complete?"合计":"已知重量")+one(total)+"g";
    }

    private void buildShell(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(15),dp(18),dp(9));head.addView(text("营养簿",23,true));TextView sub=muted("离线热量与营养素记录");sub.setPadding(dp(8),0,dp(8),dp(6));head.addView(sub);root.addView(head);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(8),dp(14),dp(100));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setBackgroundColor(Color.WHITE);
        String[] labels={"今日","趋势","食物库","自定义","设置"};
        String[] targets={"今日","趋势","食物库","包装食品","设置"};
        for(int i=0;i<labels.length;i++){String target=targets[i];Button b=button(labels[i]);b.setTextSize(12);b.setOnClickListener(v->{screen=target;showScreen();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));}
        root.addView(nav);setContentView(root);showScreen();
    }

    protected void showScreen(){content.removeAllViews();if("趋势".equals(screen))showTrend();else if("食物库".equals(screen))showFoodLibrary();else if("包装食品".equals(screen))showCustomFoodScreen();else if("设置".equals(screen))showSettings();else showToday();}
    protected abstract void showToday();
    protected abstract void showTrend();
    protected abstract void showFoodLibrary();
    protected abstract void showCustomFoodScreen();
    protected abstract void showSettings();
    protected abstract void showFoodPicker(String meal);
    protected abstract void showAmountDialog(NutritionData.Food food,String meal);
    protected abstract void showCopyRecordsDialog();
    protected abstract void showComboBuilder();
}
