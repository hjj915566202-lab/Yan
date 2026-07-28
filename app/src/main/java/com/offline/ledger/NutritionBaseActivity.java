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
    protected static final String[] MEALS = {"\u65e9\u9910","\u5348\u9910","\u665a\u9910","\u52a0\u9910"};
    protected LinearLayout root, content, nav;
    protected LocalDate selectedDate = LocalDate.now();
    protected String screen = "\u4eca\u65e5";
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
    protected double parse(EditText e){try{return Double.parseDouble(e.getText().toString().trim());}catch(Exception x){return 0;}}
    protected double valueOr(EditText e,double fallback){double v=parse(e);return v>0?v:fallback;}
    protected String one(double n){return String.format(Locale.US,n==Math.rint(n)?"%.0f":"%.1f",n);}
    protected List<NutritionData.Food> allFoods(){List<NutritionData.Food>a=new ArrayList<>(customFoods.size()+builtInFoods.size());a.addAll(customFoods);a.addAll(builtInFoods);return a;}

    private void buildShell(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.VERTICAL);head.setPadding(dp(18),dp(15),dp(18),dp(9));head.addView(text("\u8425\u517b\u7c3f",23,true));TextView sub=muted("\u79bb\u7ebf\u70ed\u91cf\u4e0e\u8425\u517b\u7d20\u8bb0\u5f55");sub.setPadding(dp(8),0,dp(8),dp(6));head.addView(sub);root.addView(head);
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(14),dp(8),dp(14),dp(100));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setBackgroundColor(Color.WHITE);
        String[] labels={"\u4eca\u65e5","\u8d8b\u52bf","\u98df\u7269\u5e93","\u5305\u88c5","\u8bbe\u7f6e"};
        String[] targets={"\u4eca\u65e5","\u8d8b\u52bf","\u98df\u7269\u5e93","\u5305\u88c5\u98df\u54c1","\u8bbe\u7f6e"};
        for(int i=0;i<labels.length;i++){String target=targets[i];Button b=button(labels[i]);b.setTextSize(12);b.setOnClickListener(v->{screen=target;showScreen();});nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));}
        root.addView(nav);setContentView(root);showScreen();
    }

    protected void showScreen(){content.removeAllViews();if("\u8d8b\u52bf".equals(screen))showTrend();else if("\u98df\u7269\u5e93".equals(screen))showFoodLibrary();else if("\u5305\u88c5\u98df\u54c1".equals(screen))showCustomFoodScreen();else if("\u8bbe\u7f6e".equals(screen))showSettings();else showToday();}
    protected abstract void showToday();
    protected abstract void showTrend();
    protected abstract void showFoodLibrary();
    protected abstract void showCustomFoodScreen();
    protected abstract void showSettings();
    protected abstract void showFoodPicker(String meal);
    protected abstract void showAmountDialog(NutritionData.Food food,String meal);
}
