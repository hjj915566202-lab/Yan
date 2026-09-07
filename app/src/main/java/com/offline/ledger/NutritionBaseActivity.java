package com.offline.ledger;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
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
    protected static final int GREEN = Color.rgb(24,167,125);
    protected static final int GREEN_DARK = Color.rgb(16,126,95);
    protected static final int SOFT_GREEN = Color.rgb(230,246,240);
    protected static final int BG = Color.rgb(246,248,247);
    protected static final int TEXT = Color.rgb(29,39,36);
    protected static final int MUTED = Color.rgb(105,120,115);
    protected static final int BORDER = Color.rgb(224,231,228);
    protected static final int SOFT = Color.rgb(241,245,243);
    protected static final int DANGER = Color.rgb(190,65,55);
    protected static final int SOFT_RED = Color.rgb(252,236,234);
    protected static final String[] MEALS = {"早餐","午餐","晚餐","加餐"};

    protected LinearLayout root, content, nav;
    protected LocalDate selectedDate = LocalDate.now();
    protected String screen = "今日";
    protected List<NutritionData.Entry> entries;
    protected List<NutritionData.Food> customFoods;
    protected List<NutritionData.Food> builtInFoods;
    protected NutritionData.Goal goal;

    private final List<Button> navButtons = new ArrayList<>();
    private final List<String> navTargets = new ArrayList<>();

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        reload();
        buildShell();
    }

    protected void reload(){
        entries=NutritionData.loadEntries(this);
        customFoods=NutritionData.loadCustomFoods(this);
        builtInFoods=FoodCatalog.commonFoods(this);
        goal=NutritionData.loadGoal(this);
    }

    protected int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    protected GradientDrawable rounded(int fill,int radius,int strokeColor){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if(strokeColor!=Color.TRANSPARENT)d.setStroke(dp(1),strokeColor);
        return d;
    }

    protected void setCardBackground(View view,int fill){
        view.setBackground(rounded(fill,18,Color.TRANSPARENT));
    }

    protected TextView text(String s,int sp,boolean bold){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(TEXT);
        v.setPadding(dp(4),dp(3),dp(4),dp(3));
        if(bold)v.setTypeface(null,Typeface.BOLD);
        return v;
    }

    protected TextView muted(String s){
        TextView v=text(s,12,false);
        v.setTextColor(MUTED);
        return v;
    }

    protected Button button(String s){
        Button b=new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTypeface(null,Typeface.BOLD);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(12),dp(9),dp(12),dp(9));
        b.setStateListAnimator(null);

        boolean danger=s.contains("删除")||s.contains("清空")||"×".equals(s);
        boolean accent=s.startsWith("＋")||s.startsWith("保存")||s.startsWith("创建")||s.startsWith("加入");
        if(danger){
            b.setTextColor(DANGER);
            b.setBackground(rounded(SOFT_RED,12,Color.TRANSPARENT));
        }else if(accent){
            b.setTextColor(GREEN_DARK);
            b.setBackground(rounded(SOFT_GREEN,12,Color.TRANSPARENT));
        }else{
            b.setTextColor(TEXT);
            b.setBackground(rounded(SOFT,12,BORDER));
        }
        return b;
    }

    protected Button primaryButton(String s){
        Button b=button(s);
        b.setTextColor(Color.WHITE);
        b.setBackground(rounded(GREEN,12,Color.TRANSPARENT));
        return b;
    }

    protected EditText input(String hint,boolean decimal){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(Color.rgb(145,157,153));
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setPadding(dp(13),dp(9),dp(13),dp(9));
        e.setBackground(rounded(Color.WHITE,12,BORDER));
        if(decimal)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));
        p.setMargins(0,dp(4),0,dp(5));
        e.setLayoutParams(p);
        return e;
    }

    protected LinearLayout box(){
        LinearLayout b=new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(14),dp(12),dp(14),dp(12));
        b.setBackground(rounded(Color.WHITE,18,BORDER));
        b.setElevation(dp(1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,dp(10));
        b.setLayoutParams(p);
        return b;
    }

    protected Spinner spinner(String[] values,String selected){
        Spinner s=new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));
        for(int i=0;i<values.length;i++)if(values[i].equals(selected))s.setSelection(i);
        return s;
    }

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
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout head=new LinearLayout(this);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16),dp(10),dp(16),dp(7));
        TextView title=text("营养簿",20,true);
        TextView offline=text("离线",11,true);
        offline.setTextColor(GREEN_DARK);
        offline.setGravity(android.view.Gravity.CENTER);
        offline.setPadding(dp(10),dp(5),dp(10),dp(5));
        offline.setBackground(rounded(SOFT_GREEN,20,Color.TRANSPARENT));
        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        head.addView(offline);
        root.addView(head);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12),dp(5),dp(12),dp(88));
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(6),dp(5),dp(6),dp(7));
        nav.setBackgroundColor(Color.WHITE);
        String[] labels={"今日","趋势","食物库","自定义","设置"};
        String[] targets={"今日","趋势","食物库","包装食品","设置"};
        for(int i=0;i<labels.length;i++){
            String target=targets[i];
            Button b=button(labels[i]);
            b.setTextSize(12);
            b.setOnClickListener(v->{screen=target;showScreen();});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);
            p.setMargins(dp(2),0,dp(2),0);
            nav.addView(b,p);
            navButtons.add(b);
            navTargets.add(target);
        }
        root.addView(nav);
        setContentView(root);
        showScreen();
    }

    private void updateNavStyle(){
        for(int i=0;i<navButtons.size();i++){
            Button b=navButtons.get(i);
            boolean selected=navTargets.get(i).equals(screen);
            b.setTextColor(selected?GREEN_DARK:MUTED);
            b.setTypeface(null,selected?Typeface.BOLD:Typeface.NORMAL);
            b.setBackground(rounded(selected?SOFT_GREEN:Color.TRANSPARENT,14,Color.TRANSPARENT));
        }
    }

    protected void showScreen(){
        content.removeAllViews();
        if("趋势".equals(screen))showTrend();
        else if("食物库".equals(screen))showFoodLibrary();
        else if("包装食品".equals(screen))showCustomFoodScreen();
        else if("设置".equals(screen))showSettings();
        else showToday();
        updateNavStyle();
    }

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
