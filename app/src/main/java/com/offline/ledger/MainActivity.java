package com.offline.ledger;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends NutritionTrendActivity {
    @Override protected void showCustomFoodScreen() {
        content.addView(text("自定义食品与套餐", 21, true));

        LinearLayout comboBox = box();
        comboBox.addView(text("餐食套餐",17,true));
        comboBox.addView(muted("把多种食材和各自用量合并成1份。记录套餐时会保留食材明细和合计重量。"));
        Button createCombo = button("＋ 创建餐食套餐"); createCombo.setOnClickListener(v -> showComboBuilder());
        comboBox.addView(createCombo); content.addView(comboBox);

        content.addView(text("添加包装食品", 19, true));
        LinearLayout form = box();
        EditText name = input("食品名称", false);
        EditText brand = input("品牌（可选）", false);
        Spinner basis = spinner(new String[]{"每100克", "每100毫升", "每1份（1袋/1盒/1个）"}, "每100克");
        TextView hint = muted("下面填写的是每100克的营养值。");
        EditText serving = input("一份约多少克/毫升（可选）", true);
        EditText kcal = input("热量 kcal", true);
        EditText protein = input("蛋白质 g", true);
        EditText fat = input("脂肪 g", true);
        EditText carb = input("碳水 g", true);
        EditText fiber = input("膳食纤维 g", true);
        EditText sodium = input("钠 mg", true);
        form.addView(name); form.addView(brand); form.addView(text("营养标签的计量基准",13,true));
        form.addView(basis); form.addView(hint); form.addView(serving); serving.setVisibility(View.GONE);
        for (View x : new View[]{kcal,protein,fat,carb,fiber,sodium}) form.addView(x);
        basis.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String selected=String.valueOf(p.getItemAtPosition(pos));
                boolean perServing=selected.startsWith("每1份");
                serving.setVisibility(perServing?View.VISIBLE:View.GONE);
                hint.setText(perServing?"下面填写的是1整份的全部营养值。保存后不会换算，也不会×100。":
                        (selected.contains("毫升")?"下面填写的是每100毫升的营养值。":"下面填写的是每100克的营养值。"));
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        Button save=button("保存到食物库");
        save.setOnClickListener(v->{
            String n=name.getText().toString().trim();
            if(n.isEmpty()||parse(kcal)<=0){Toast.makeText(this,"请填写食品名称和热量",Toast.LENGTH_SHORT).show();return;}
            String selected=String.valueOf(basis.getSelectedItem());
            String code=selected.startsWith("每1份")?NutritionData.Food.BASIS_SERVING:
                    (selected.contains("毫升")?NutritionData.Food.BASIS_100ML:NutritionData.Food.BASIS_100G);
            NutritionData.Food food=new NutritionData.Food("c"+System.currentTimeMillis(),n,
                    brand.getText().toString().trim(),"我的食品",parse(kcal),parse(protein),parse(fat),parse(carb),
                    parse(fiber),parse(sodium),code,NutritionData.Food.BASIS_SERVING.equals(code)?parse(serving):100d);
            customFoods.add(0,food); NutritionData.saveCustomFoods(this,customFoods);
            customFoods=NutritionData.loadCustomFoods(this);
            NutritionData.Food stored=customFoods.isEmpty()?food:customFoods.get(0);
            Toast.makeText(this,"已保存："+one(stored.kcal)+" kcal"+stored.basisSuffix(),Toast.LENGTH_LONG).show();
            showScreen();
        });
        form.addView(save); content.addView(form);

        content.addView(text("我的套餐",19,true));
        boolean hasCombo=false;
        for(NutritionData.Food f:new ArrayList<>(customFoods))if(f.isCombo()){addFoodCard(f);hasCombo=true;}
        if(!hasCombo) content.addView(muted("还没有套餐。点击上方“创建餐食套餐”开始添加。"));

        content.addView(text("我的包装食品",19,true));
        boolean hasFood=false;
        for(NutritionData.Food f:new ArrayList<>(customFoods))if(!f.isCombo()){addFoodCard(f);hasFood=true;}
        if(!hasFood) content.addView(muted("还没有自定义包装食品。"));
    }

    private void addFoodCard(NutritionData.Food f){
        LinearLayout card=box(),row=new LinearLayout(this),detail=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL); detail.setOrientation(LinearLayout.VERTICAL);
        detail.addView(text(f.name,15,true));
        String meta;
        if(f.isCombo()){
            meta="套餐 · "+one(f.kcal)+" kcal/份 · "+comboWeightSummary(f.components,1d);
            detail.addView(muted(meta)); detail.addView(muted(componentSummary(f.components,1d)));
        }else{
            meta=(f.brand.isEmpty()?"自定义":f.brand)+" · "+one(f.kcal)+" kcal"+f.basisSuffix();
            if(f.isPerServing()&&f.servingSize>0) meta+=" · 1份约"+one(f.servingSize)+"克/毫升";
            detail.addView(muted(meta));
        }
        Button info=button("详情"); info.setOnClickListener(v->showFoodDetails(f));
        Button add=button("记录"); add.setOnClickListener(v->showAmountDialog(f,"加餐"));
        Button del=button("删除"); del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("删除"+f.name)
                .setMessage("只会删除保存的食品或套餐，不会删除已经记录到日期中的内容。")
                .setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{customFoods.remove(f);NutritionData.saveCustomFoods(this,customFoods);showScreen();}).show());
        row.addView(detail,new LinearLayout.LayoutParams(0,-2,1)); row.addView(info); row.addView(add); row.addView(del); card.addView(row); content.addView(card);
    }

    @Override protected void showSettings(){
        content.addView(text("每日营养计划",21,true));
        content.addView(muted("设置热量上限和三大营养素热量比例，目标克数会自动计算。"));
        LinearLayout form=box();
        form.addView(text("每日热量上限",14,true));
        EditText kcal=goalInput("热量 kcal",goal.kcal); form.addView(kcal);
        form.addView(text("三大营养素热量比例",14,true));
        EditText pp=goalInput("蛋白质 %",goal.proteinPercent),fp=goalInput("脂肪 %",goal.fatPercent),cp=goalInput("碳水 %",goal.carbPercent);
        form.addView(pp);form.addView(fp);form.addView(cp);
        TextView status=text("",14,true),calculated=muted(""); form.addView(status);form.addView(calculated);
        form.addView(text("其他目标",14,true));
        EditText fiber=goalInput("膳食纤维 g",goal.fiber),sodium=goalInput("钠 mg",goal.sodium);form.addView(fiber);form.addView(sodium);
        Runnable preview=()->updatePlanPreview(kcal,pp,fp,cp,status,calculated);
        for(EditText e:new EditText[]{kcal,pp,fp,cp})e.addTextChangedListener(new SimpleWatcher(preview)); preview.run();
        Button save=button("保存计划"); save.setOnClickListener(v->{
            double energy=parse(kcal),p=parse(pp),f=parse(fp),c=parse(cp);
            if(energy<=0){Toast.makeText(this,"请填写大于0的热量上限",Toast.LENGTH_SHORT).show();return;}
            if(Math.abs(p+f+c-100d)>=0.05d){Toast.makeText(this,"蛋白质、脂肪、碳水比例合计必须为100%",Toast.LENGTH_LONG).show();return;}
            goal.kcal=energy;goal.proteinPercent=p;goal.fatPercent=f;goal.carbPercent=c;
            goal.fiber=valueOr(fiber,25);goal.sodium=valueOr(sodium,2000);goal.recalculateMacros();NutritionData.saveGoal(this,goal);
            Toast.makeText(this,"已保存：蛋白"+one(goal.protein)+"g · 脂肪"+one(goal.fat)+"g · 碳水"+one(goal.carb)+"g",Toast.LENGTH_LONG).show();showScreen();
        });
        form.addView(save);content.addView(form);
        LinearLayout note=box(); note.addView(text("计算方式",16,true));
        note.addView(muted("蛋白质和碳水按4 kcal/g计算，脂肪按9 kcal/g计算。比例指各营养素提供的热量占每日总热量的比例。"));
        Button clear=button("清空全部数据");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("清空数据")
                .setMessage("确定清空全部饮食记录、自定义食品、套餐和目标设置吗？").setNegativeButton("取消",null)
                .setPositiveButton("清空",(d,w)->{NutritionData.clear(this);reload();screen="今日";showScreen();}).show());
        note.addView(clear);content.addView(note);
    }

    private void updatePlanPreview(EditText kcal,EditText pp,EditText fp,EditText cp,TextView status,TextView out){
        double energy=parse(kcal),p=parse(pp),f=parse(fp),c=parse(cp),total=p+f+c;
        boolean valid=energy>0&&Math.abs(total-100d)<0.05d;
        status.setText("比例合计 "+one(total)+"%"+(valid?" ✓":"（需要为100%）"));
        status.setTextColor(valid?GREEN:Color.rgb(190,65,55));
        out.setText("自动计算目标：\n蛋白质 "+one(energy*p/400d)+"g（"+one(energy*p/100d)+" kcal）\n脂肪 "+
                one(energy*f/900d)+"g（"+one(energy*f/100d)+" kcal）\n碳水 "+one(energy*c/400d)+"g（"+one(energy*c/100d)+" kcal）");
    }

    private EditText goalInput(String hint,double value){EditText e=input(hint,true);e.setText(one(value));return e;}
}
