package com.offline.ledger;

import android.app.AlertDialog;
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

public class MainActivity extends NutritionFoodActivity {
    @Override protected void showCustomFoodScreen() {
        content.addView(text("添加包装食品", 21, true));

        LinearLayout form = box();
        EditText name = input("食品名称", false);
        EditText brand = input("品牌（可选）", false);
        Spinner basis = spinner(new String[]{"每100克", "每100毫升", "每1份"}, "每100克");
        EditText serving = input("每份重量/容量（可选）", true);
        TextView servingNote = muted("填写后仅作为份量参考，不参与营养素换算。营养值仍按1份保存。");
        serving.setVisibility(View.GONE);
        servingNote.setVisibility(View.GONE);

        EditText kcal = input("热量 kcal", true);
        EditText protein = input("蛋白质 g", true);
        EditText fat = input("脂肪 g", true);
        EditText carb = input("碳水 g", true);
        EditText fiber = input("膳食纤维 g", true);
        EditText sodium = input("钠 mg", true);

        form.addView(name);
        form.addView(brand);
        form.addView(text("营养标签基准", 13, true));
        form.addView(basis);
        form.addView(serving);
        form.addView(servingNote);
        form.addView(kcal);
        form.addView(protein);
        form.addView(fat);
        form.addView(carb);
        form.addView(fiber);
        form.addView(sodium);

        basis.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean perServing = position == 2;
                serving.setVisibility(perServing ? View.VISIBLE : View.GONE);
                servingNote.setVisibility(perServing ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button save = button("保存到食物库");
        save.setOnClickListener(v -> {
            if (name.getText().toString().trim().isEmpty() || parse(kcal) <= 0) {
                Toast.makeText(this, "请填写食品名称和热量", Toast.LENGTH_SHORT).show();
                return;
            }

            int position = basis.getSelectedItemPosition();
            String basisCode = position == 2
                    ? NutritionData.Food.BASIS_SERVING
                    : (position == 1 ? NutritionData.Food.BASIS_100ML : NutritionData.Food.BASIS_100G);
            double servingSize = position == 2 ? parse(serving) : 100d;

            NutritionData.Food food = new NutritionData.Food(
                    "c" + System.currentTimeMillis(),
                    name.getText().toString().trim(),
                    brand.getText().toString().trim(),
                    "我的食品",
                    parse(kcal),
                    parse(protein),
                    parse(fat),
                    parse(carb),
                    parse(fiber),
                    parse(sodium),
                    basisCode,
                    servingSize
            );
            customFoods.add(0, food);
            NutritionData.saveCustomFoods(this, customFoods);
            Toast.makeText(this, "已按" + String.valueOf(basis.getSelectedItem()) + "保存", Toast.LENGTH_SHORT).show();
            showScreen();
        });
        form.addView(save);
        content.addView(form);

        content.addView(text("我的食品", 19, true));
        if (customFoods.isEmpty()) content.addView(muted("还没有自定义食品。"));

        for (NutritionData.Food f : new ArrayList<>(customFoods)) {
            LinearLayout card = box();
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(f.name, 15, true));

            String meta = (f.brand.isEmpty() ? "自定义" : f.brand)
                    + " · " + one(f.kcal) + " kcal" + basisSuffix(f);
            if (f.isPerServing() && f.servingSize > 0) {
                meta += " · 每份约" + one(f.servingSize) + "克/毫升";
            }
            detail.addView(muted(meta));

            Button add = button("记录");
            add.setOnClickListener(v -> showAmountDialog(f, "加餐"));
            Button del = button("删除");
            del.setOnClickListener(v -> {
                customFoods.remove(f);
                NutritionData.saveCustomFoods(this, customFoods);
                showScreen();
            });

            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(add);
            row.addView(del);
            card.addView(row);
            content.addView(card);
        }
    }

    @Override protected void showSettings() {
        content.addView(text("每日营养目标", 21, true));
        LinearLayout form = box();
        EditText kcal = goalInput("热量 kcal", goal.kcal);
        EditText protein = goalInput("蛋白质 g", goal.protein);
        EditText fat = goalInput("脂肪 g", goal.fat);
        EditText carb = goalInput("碳水 g", goal.carb);
        EditText fiber = goalInput("膳食纤维 g", goal.fiber);
        EditText sodium = goalInput("钠 mg", goal.sodium);
        for (EditText e : new EditText[]{kcal, protein, fat, carb, fiber, sodium}) form.addView(e);

        Button save = button("保存目标");
        save.setOnClickListener(v -> {
            goal.kcal = valueOr(kcal, 2000);
            goal.protein = valueOr(protein, 80);
            goal.fat = valueOr(fat, 60);
            goal.carb = valueOr(carb, 250);
            goal.fiber = valueOr(fiber, 25);
            goal.sodium = valueOr(sodium, 2000);
            NutritionData.saveGoal(this, goal);
            Toast.makeText(this, "目标已保存", Toast.LENGTH_SHORT).show();
        });
        form.addView(save);
        content.addView(form);

        LinearLayout note = box();
        note.addView(text("数据说明", 16, true));
        note.addView(muted("内置食物为常见参考值，实际包装食品请以商品营养标签为准。全部记录仅保存在本机。"));
        Button clear = button("清空全部数据");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("清空数据")
                .setMessage("确定清空全部饮食记录、自定义食品和目标设置吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (d, w) -> {
                    NutritionData.clear(this);
                    reload();
                    screen = "今日";
                    showScreen();
                }).show());
        note.addView(clear);
        content.addView(note);
    }

    private EditText goalInput(String hint, double value) {
        EditText e = input(hint, true);
        e.setText(one(value));
        return e;
    }
}
