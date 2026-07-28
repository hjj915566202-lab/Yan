package com.offline.ledger;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends NutritionFoodActivity {
    @Override protected void showCustomFoodScreen(){
        content.addView(text("添加包装食品",21,true));LinearLayout form=box();EditText name=input("食品名称",false),brand=input("品牌（可选）",false),serving=input("每份重量/容量",true);serving.setText("100");Spinner basis=spinner(new String[]{"每100克/毫升","每份"},"每100克/毫升");EditText kcal=input("热量 kcal",true),protein=input("蛋白质 g",true),fat=input("脂肪 g",true),carb=input("碳水 g",true),fiber=input("膳食纤维 g",true),sodium=input("钠 mg",true);for(View v:new View[]{name,brand,basis,serving,kcal,protein,fat,carb,fiber,sodium})form.addView(v);Button save=button("保存到食物库");save.setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()||parse(kcal)<=0){Toast.makeText(this,"请填写食品名称和热量",Toast.LENGTH_SHORT).show();return;}double factor=basis.getSelectedItemPosition()==1?100d/Math.max(1,parse(serving)):1d;NutritionData.Food food=new NutritionData.Food("c"+System.currentTimeMillis(),name.getText().toString().trim(),brand.getText().toString().trim(),"我的食品",parse(kcal)*factor,parse(protein)*factor,parse(fat)*factor,parse(carb)*factor,parse(fiber)*factor,parse(sodium)*factor);customFoods.add(0,food);NutritionData.saveCustomFoods(this,customFoods);showScreen();});form.addView(save);content.addView(form);content.addView(text("我的食品",19,true));if(customFoods.isEmpty())content.addView(muted("还没有自定义食品。"));for(NutritionData.Food f:new ArrayList<>(customFoods)){LinearLayout card=box(),row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);detail.addView(text(f.name,15,true));detail.addView(muted((f.brand.isEmpty()?"自定义":f.brand)+" · "+one(f.kcal)+" kcal/100g"));Button add=button("记录");add.setOnClickListener(v->showAmountDialog(f,"加餐"));Button del=button("删除");del.setOnClickListener(v->{customFoods.remove(f);NutritionData.saveCustomFoods(this,customFoods);showScreen();});row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.addView(add);row.addView(del);card.addView(row);content.addView(card);}
    }

    @Override protected void showSettings(){
        content.addView(text("每日营养目标",21,true));LinearLayout form=box();EditText kcal=goalInput("热量 kcal",goal.kcal),protein=goalInput("蛋白质 g",goal.protein),fat=goalInput("脂肪 g",goal.fat),carb=goalInput("碳水 g",goal.carb),fiber=goalInput("膳食纤维 g",goal.fiber),sodium=goalInput("钠 mg",goal.sodium);for(EditText e:new EditText[]{kcal,protein,fat,carb,fiber,sodium})form.addView(e);Button save=button("保存目标");save.setOnClickListener(v->{goal.kcal=valueOr(kcal,2000);goal.protein=valueOr(protein,80);goal.fat=valueOr(fat,60);goal.carb=valueOr(carb,250);goal.fiber=valueOr(fiber,25);goal.sodium=valueOr(sodium,2000);NutritionData.saveGoal(this,goal);Toast.makeText(this,"目标已保存",Toast.LENGTH_SHORT).show();});form.addView(save);content.addView(form);LinearLayout note=box();note.addView(text("数据说明",16,true));note.addView(muted("内置食物为常见参考值，实际包装食品请以商品营养标签为准。全部记录仅保存在本机。"));Button clear=button("清空全部数据");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("清空数据").setMessage("确定清空全部饮食记录、自定义食品和目标设置吗？").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{NutritionData.clear(this);reload();screen="今日";showScreen();}).show());note.addView(clear);content.addView(note);
    }

    private EditText goalInput(String hint,double value){EditText e=input(hint,true);e.setText(one(value));return e;}
}
