package com.offline.ledger;

import android.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class NutritionFoodActivity extends NutritionTodayActivity {
    protected List<NutritionData.Food> filterFoods(String query,String category){String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);List<NutritionData.Food>out=new ArrayList<>();for(NutritionData.Food f:allFoods()){boolean c="全部".equals(category)||category.equals(f.category);boolean m=q.isEmpty()||f.name.toLowerCase(Locale.ROOT).contains(q)||f.brand.toLowerCase(Locale.ROOT).contains(q);if(c&&m)out.add(f);}return out;}
    protected String[] categories(){Set<String>s=new LinkedHashSet<>();s.add("全部");for(NutritionData.Food f:allFoods())s.add(f.category);return s.toArray(new String[0]);}

    @Override protected void showFoodPicker(String meal){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(20),dp(8),dp(20),0);EditText search=input("搜索食物",false);Spinner category=spinner(categories(),"全部");LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.addView(list);wrap.addView(search);wrap.addView(category);wrap.addView(scroll,new LinearLayout.LayoutParams(-1,dp(430)));AlertDialog dialog=new AlertDialog.Builder(this).setTitle("添加到"+meal).setView(wrap).setNegativeButton("关闭",null).create();Runnable render=()->renderFoodRows(list,filterFoods(search.getText().toString(),String.valueOf(category.getSelectedItem())),meal,dialog);search.addTextChangedListener(new SimpleWatcher(render));category.setOnItemSelectedListener(new SimpleItemSelected(render));dialog.setOnShowListener(x->render.run());dialog.show();
    }

    private void renderFoodRows(LinearLayout list,List<NutritionData.Food>foods,String meal,AlertDialog parent){list.removeAllViews();for(NutritionData.Food f:foods){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);detail.addView(text(f.name+(f.brand.isEmpty()?"":" · "+f.brand),14,true));detail.addView(muted(one(f.kcal)+" kcal/100g · 蛋白"+one(f.protein)+"g · 脂肪"+one(f.fat)+"g · 碳水"+one(f.carb)+"g"));Button add=button("＋");add.setOnClickListener(v->{parent.dismiss();showAmountDialog(f,meal);});row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.addView(add);list.addView(row);}if(foods.isEmpty())list.addView(muted("没有找到食物。可在“包装食品”中自行添加。"));}

    @Override protected void showAmountDialog(NutritionData.Food food,String meal){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(24),dp(8),dp(24),0);Spinner mealSpinner=spinner(MEALS,meal);EditText amount=input("摄入量（克/毫升）",true);amount.setText("100");android.widget.TextView preview=muted("");wrap.addView(text("餐次",13,true));wrap.addView(mealSpinner);wrap.addView(text("摄入量（克/毫升）",13,true));wrap.addView(amount);wrap.addView(preview);Runnable update=()->{double a=parse(amount),r=a/100d;preview.setText(one(food.kcal*r)+" kcal · 蛋白"+one(food.protein*r)+"g · 脂肪"+one(food.fat*r)+"g · 碳水"+one(food.carb*r)+"g");};amount.addTextChangedListener(new SimpleWatcher(update));update.run();new AlertDialog.Builder(this).setTitle(food.name).setView(wrap).setNegativeButton("取消",null).setPositiveButton("加入记录",(d,w)->{double a=parse(amount);if(a<=0)return;double r=a/100d;NutritionData.Entry e=new NutritionData.Entry();e.id="e"+System.currentTimeMillis();e.date=selectedDate.toString();e.meal=String.valueOf(mealSpinner.getSelectedItem());e.name=food.name;e.amount=a;e.kcal=food.kcal*r;e.protein=food.protein*r;e.fat=food.fat*r;e.carb=food.carb*r;e.fiber=food.fiber*r;e.sodium=food.sodium*r;entries.add(e);NutritionData.saveEntries(this,entries);screen="今日";showScreen();}).show();
    }

    @Override protected void showFoodLibrary(){content.addView(text("食物库",21,true));EditText search=input("搜索米饭、鸡蛋、牛奶……",false);Spinner category=spinner(categories(),"全部");LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(search);content.addView(category);content.addView(list);Runnable render=()->renderLibraryRows(list,filterFoods(search.getText().toString(),String.valueOf(category.getSelectedItem())));search.addTextChangedListener(new SimpleWatcher(render));category.setOnItemSelectedListener(new SimpleItemSelected(render));render.run();}
    private void renderLibraryRows(LinearLayout list,List<NutritionData.Food>foods){list.removeAllViews();for(NutritionData.Food f:foods){LinearLayout card=box(),row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);detail.addView(text(f.name+(f.brand.isEmpty()?"":" · "+f.brand),15,true));detail.addView(muted(f.category+" · "+one(f.kcal)+" kcal/100g · 蛋白"+one(f.protein)+"g · 脂肪"+one(f.fat)+"g · 碳水"+one(f.carb)+"g"));Button add=button("＋");add.setOnClickListener(v->showAmountDialog(f,"加餐"));row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.addView(add);card.addView(row);list.addView(card);}}

    protected static final class SimpleWatcher implements TextWatcher{private final Runnable action;SimpleWatcher(Runnable a){action=a;}public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){action.run();}public void afterTextChanged(Editable s){}}
    protected static final class SimpleItemSelected implements AdapterView.OnItemSelectedListener{private final Runnable action;SimpleItemSelected(Runnable a){action=a;}public void onItemSelected(AdapterView<?>p,View v,int pos,long id){action.run();}public void onNothingSelected(AdapterView<?>p){}}
}
