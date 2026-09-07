package com.offline.ledger;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public abstract class NutritionMealToolsActivity extends NutritionFoodActivity {
    private static final int INGREDIENT_LIMIT = 60;

    private interface DateReceiver { void accept(LocalDate date); }
    private interface FoodReceiver { void accept(NutritionData.Food food); }
    private interface ComponentReceiver { void accept(NutritionData.Component component); }

    private void pickDate(LocalDate current, DateReceiver receiver) {
        new DatePickerDialog(this, (view, year, month, day) -> receiver.accept(LocalDate.of(year, month + 1, day)),
                current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }

    @Override protected void showCopyRecordsDialog() {
        final LocalDate[] sourceDate = {selectedDate.minusDays(1)};
        final LocalDate[] targetDate = {selectedDate};
        LinearLayout wrap = new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(dp(20),dp(8),dp(20),0);

        LinearLayout sourceBox = box(); sourceBox.addView(text("来源",15,true));
        Button sourceDateButton = button(sourceDate[0].toString());
        Spinner sourceMeal = spinner(MEALS, "早餐");
        sourceBox.addView(sourceDateButton); sourceBox.addView(sourceMeal); wrap.addView(sourceBox);

        LinearLayout targetBox = box(); targetBox.addView(text("复制到",15,true));
        Button targetDateButton = button(targetDate[0].toString());
        Spinner targetMeal = spinner(MEALS, "早餐");
        targetBox.addView(targetDateButton); targetBox.addView(targetMeal); wrap.addView(targetBox);

        TextView found = muted(""); wrap.addView(found);
        LinearLayout selection = new LinearLayout(this); selection.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(selection);
        wrap.addView(scroll,new LinearLayout.LayoutParams(-1,dp(310)));

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button all = button("全选"), none = button("取消全选");
        actions.addView(all,new LinearLayout.LayoutParams(0,dp(48),1)); actions.addView(none,new LinearLayout.LayoutParams(0,dp(48),1));
        wrap.addView(actions);

        Runnable render = () -> {
            selection.removeAllViews(); int count = 0;
            String meal = String.valueOf(sourceMeal.getSelectedItem());
            for (NutritionData.Entry e : entries) {
                if (!sourceDate[0].toString().equals(e.date) || !meal.equals(e.meal)) continue;
                CheckBox check = new CheckBox(this); check.setChecked(true); check.setTag(e);
                String detail = e.name + " · " + one(e.kcal) + " kcal";
                if (e.isCombo()) detail += "\n" + one(e.amount) + "份 · " + componentSummary(e.components,1d);
                else detail += " · " + one(e.amount) + e.amountUnit;
                check.setText(detail); selection.addView(check); count++;
            }
            found.setText(count == 0 ? "该日期和餐次没有记录。" : "找到 " + count + " 条记录，可取消不需要复制的项目。");
        };

        sourceDateButton.setOnClickListener(v -> pickDate(sourceDate[0], d -> {sourceDate[0]=d;sourceDateButton.setText(d.toString());render.run();}));
        targetDateButton.setOnClickListener(v -> pickDate(targetDate[0], d -> {targetDate[0]=d;targetDateButton.setText(d.toString());}));
        sourceMeal.setOnItemSelectedListener(new SimpleItemSelected(render));
        all.setOnClickListener(v -> setAllChecks(selection,true));
        none.setOnClickListener(v -> setAllChecks(selection,false));

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("复制饮食记录").setView(wrap)
                .setNegativeButton("取消",null).setPositiveButton("复制",null).create();
        dialog.setOnShowListener(x -> {
            render.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                List<NutritionData.Entry> selected = checkedEntries(selection);
                if (selected.isEmpty()) {Toast.makeText(this,"请选择至少一条记录",Toast.LENGTH_SHORT).show();return;}
                String targetMealValue = String.valueOf(targetMeal.getSelectedItem());
                long seed = System.currentTimeMillis(); int i = 0;
                for (NutritionData.Entry e : selected) {
                    entries.add(e.copyTo(targetDate[0].toString(),targetMealValue,"e"+seed+"_"+(i++)));
                }
                NutritionData.saveEntries(this,entries); selectedDate=targetDate[0]; screen="今日";
                dialog.dismiss(); showScreen();
                Toast.makeText(this,"已复制 "+selected.size()+" 条记录到 "+targetDate[0]+" · "+targetMealValue,Toast.LENGTH_LONG).show();
            });
        });
        dialog.show();
    }

    private void setAllChecks(LinearLayout selection, boolean checked) {
        for(int i=0;i<selection.getChildCount();i++) if(selection.getChildAt(i) instanceof CheckBox) ((CheckBox)selection.getChildAt(i)).setChecked(checked);
    }

    private List<NutritionData.Entry> checkedEntries(LinearLayout selection) {
        List<NutritionData.Entry> out = new ArrayList<>();
        for(int i=0;i<selection.getChildCount();i++) {
            if(!(selection.getChildAt(i) instanceof CheckBox)) continue;
            CheckBox c=(CheckBox)selection.getChildAt(i);
            if(c.isChecked() && c.getTag() instanceof NutritionData.Entry) out.add((NutritionData.Entry)c.getTag());
        }
        return out;
    }

    @Override protected void showComboBuilder() {
        List<NutritionData.Component> components = new ArrayList<>();
        LinearLayout wrap = new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(dp(20),dp(8),dp(20),0);
        EditText name = input("套餐名称，例如：清炒青菜",false); wrap.addView(name);
        TextView summary = muted("先添加食材。所有内容将合计为1份。"); wrap.addView(summary);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(list); wrap.addView(scroll,new LinearLayout.LayoutParams(-1,dp(330)));
        Button addIngredient = button("＋ 添加食材"); wrap.addView(addIngredient);

        Runnable render = () -> renderComboComponents(list,summary,components);
        addIngredient.setOnClickListener(v -> showIngredientPicker(food -> showIngredientAmountDialog(food, component -> {components.add(component);render.run();})));

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("创建餐食套餐").setView(wrap)
                .setNegativeButton("取消",null).setPositiveButton("保存套餐",null).create();
        dialog.setOnShowListener(x -> {
            render.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String comboName = name.getText().toString().trim();
                if(comboName.isEmpty()){Toast.makeText(this,"请填写套餐名称",Toast.LENGTH_SHORT).show();return;}
                if(components.isEmpty()){Toast.makeText(this,"请至少添加一种食材",Toast.LENGTH_SHORT).show();return;}
                NutritionData.Food combo = NutritionData.Food.createCombo("combo"+System.currentTimeMillis(),comboName,components);
                customFoods.add(0,combo); NutritionData.saveCustomFoods(this,customFoods);
                customFoods=NutritionData.loadCustomFoods(this); dialog.dismiss(); showScreen();
                Toast.makeText(this,"已保存："+comboName+" · "+one(combo.kcal)+" kcal/份 · "+comboWeightSummary(combo.components,1d),Toast.LENGTH_LONG).show();
            });
        });
        dialog.show();
    }

    private void renderComboComponents(LinearLayout list, TextView summary, List<NutritionData.Component> components) {
        list.removeAllViews();
        NutritionData.Food preview = NutritionData.Food.createCombo("preview","预览",components);
        if(components.isEmpty()) summary.setText("先添加食材。所有内容将合计为1份。");
        else summary.setText("1份 · "+comboWeightSummary(components,1d)+" · "+one(preview.kcal)+" kcal\n蛋白"+one(preview.protein)+"g · 脂肪"+one(preview.fat)+"g · 碳水"+one(preview.carb)+"g");
        for(NutritionData.Component c:new ArrayList<>(components)){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(c.name,14,true));detail.addView(muted(one(c.amount)+c.amountUnit+(c.weightKnown?" · "+one(c.weightGrams)+"g":" · 重量未知")));
            Button del=button("删除");del.setOnClickListener(v->{components.remove(c);renderComboComponents(list,summary,components);});
            row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.addView(del);list.addView(row);
        }
        if(components.isEmpty())list.addView(muted("尚未添加食材。"));
    }

    private void showIngredientPicker(FoodReceiver receiver) {
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(20),dp(8),dp(20),0);
        EditText search=input("搜索食材名称、编码或子类",false);Spinner category=spinner(categories(),"全部");TextView count=muted("");
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.addView(list);
        wrap.addView(search);wrap.addView(category);wrap.addView(count);wrap.addView(scroll,new LinearLayout.LayoutParams(-1,dp(430)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("选择套餐食材").setView(wrap).setNegativeButton("关闭",null).create();
        Runnable render=()->{
            List<NutritionData.Food> foods=filterFoods(search.getText().toString(),String.valueOf(category.getSelectedItem()));
            List<NutritionData.Food> usable=new ArrayList<>();for(NutritionData.Food f:foods)if(!f.isCombo())usable.add(f);
            count.setText("找到 "+usable.size()+" 条，显示前 "+Math.min(INGREDIENT_LIMIT,usable.size())+" 条");
            list.removeAllViews();int shown=Math.min(INGREDIENT_LIMIT,usable.size());
            for(int i=0;i<shown;i++){
                NutritionData.Food f=usable.get(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout detail=new LinearLayout(this);detail.setOrientation(LinearLayout.VERTICAL);detail.addView(text(f.name,14,true));
                detail.addView(muted(one(f.kcal)+" kcal"+f.basisSuffix()+" · 蛋白"+one(f.protein)+"g · 脂肪"+one(f.fat)+"g"));
                Button choose=button("选择");choose.setOnClickListener(v->{dialog.dismiss();receiver.accept(f);});
                row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.addView(choose);list.addView(row);
            }
            if(usable.isEmpty())list.addView(muted("没有找到食材。"));
        };
        search.addTextChangedListener(new SimpleWatcher(render));category.setOnItemSelectedListener(new SimpleItemSelected(render));
        dialog.setOnShowListener(x->render.run());dialog.show();
    }

    private void showIngredientAmountDialog(NutritionData.Food food, ComponentReceiver receiver) {
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(24),dp(8),dp(24),0);
        EditText amount=input(amountLabel(food),true);amount.setText(one(food.defaultAmount()));TextView preview=muted("");
        wrap.addView(text(amountLabel(food),13,true));wrap.addView(amount);wrap.addView(preview);
        Runnable update=()->{double a=parse(amount),r=food.ratio(a);preview.setText(one(food.kcal*r)+" kcal · 蛋白"+one(food.protein*r)+"g · 脂肪"+one(food.fat*r)+"g · 碳水"+one(food.carb*r)+"g");};
        amount.addTextChangedListener(new SimpleWatcher(update));update.run();
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(food.name).setView(wrap).setNegativeButton("取消",null).setPositiveButton("加入套餐",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{double a=parse(amount);if(a<=0){Toast.makeText(this,"请输入大于0的用量",Toast.LENGTH_SHORT).show();return;}receiver.accept(NutritionData.Component.fromFood(food,a));dialog.dismiss();}));
        dialog.show();
    }
}
