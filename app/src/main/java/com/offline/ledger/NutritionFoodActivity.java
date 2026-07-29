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
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class NutritionFoodActivity extends NutritionTodayActivity {
    private static final int PICKER_LIMIT = 60;
    private static final int LIBRARY_LIMIT = 80;

    protected List<NutritionData.Food> filterFoods(String query, String category) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<NutritionData.Food> out = new ArrayList<>();
        for (NutritionData.Food f : allFoods()) {
            boolean c = "全部".equals(category) || category.equals(f.category);
            boolean m = q.isEmpty() || f.name.toLowerCase(Locale.ROOT).contains(q)
                    || f.brand.toLowerCase(Locale.ROOT).contains(q)
                    || f.subCategory.toLowerCase(Locale.ROOT).contains(q)
                    || f.sourceCode.toLowerCase(Locale.ROOT).contains(q);
            if (c && m) out.add(f);
        }
        return out;
    }

    protected String[] categories() {
        Set<String> s = new LinkedHashSet<>();
        s.add("全部");
        for (NutritionData.Food f : allFoods()) s.add(f.category);
        return s.toArray(new String[0]);
    }

    protected String basisSuffix(NutritionData.Food food) { return food.basisSuffix(); }
    protected String amountLabel(NutritionData.Food food) {
        if (food.isPerServing()) return "份数";
        return food.isPerMilliliter() ? "摄入量（毫升）" : "摄入量（克）";
    }

    private String foodMeta(NutritionData.Food f) {
        if (f.isCombo()) return f.category + " · " + one(f.kcal) + " kcal/份 · " + comboWeightSummary(f.components,1d);
        String category = f.category + (f.subCategory.isEmpty() ? "" : "/" + f.subCategory);
        return category + " · " + one(f.kcal) + " kcal" + basisSuffix(f)
                + " · 蛋白" + one(f.protein) + "g · 脂肪" + one(f.fat) + "g · 碳水" + one(f.carb) + "g";
    }

    @Override protected void showFoodPicker(String meal) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        EditText search = input("搜索食物名称、编码或子类", false);
        Spinner category = spinner(categories(), "全部");
        TextView count = muted("");
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(list);
        wrap.addView(search); wrap.addView(category); wrap.addView(count);
        wrap.addView(scroll, new LinearLayout.LayoutParams(-1, dp(450)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("添加到" + meal).setView(wrap).setNegativeButton("关闭", null).create();
        Runnable render = () -> {
            List<NutritionData.Food> foods = filterFoods(search.getText().toString(), String.valueOf(category.getSelectedItem()));
            count.setText("找到 " + foods.size() + " 条" + (foods.size() > PICKER_LIMIT ? "，显示前 " + PICKER_LIMIT + " 条" : ""));
            renderFoodRows(list, foods, meal, dialog);
        };
        search.addTextChangedListener(new SimpleWatcher(render));
        category.setOnItemSelectedListener(new SimpleItemSelected(render));
        dialog.setOnShowListener(x -> render.run()); dialog.show();
    }

    private void renderFoodRows(LinearLayout list, List<NutritionData.Food> foods, String meal, AlertDialog parent) {
        list.removeAllViews(); int shown = Math.min(PICKER_LIMIT, foods.size());
        for (int i = 0; i < shown; i++) {
            NutritionData.Food f = foods.get(i);
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail = new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(f.name + (f.brand.isEmpty() ? "" : " · " + f.brand), 14, true));
            detail.addView(muted(foodMeta(f)));
            if(f.isCombo())detail.addView(muted(componentSummary(f.components,1d)));
            Button info = button("详情"); info.setOnClickListener(v -> showFoodDetails(f));
            Button add = button("＋"); add.setOnClickListener(v -> { parent.dismiss(); showAmountDialog(f, meal); });
            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(info); row.addView(add); list.addView(row);
        }
        if (foods.isEmpty()) list.addView(muted("没有找到食物。可在“自定义”中自行添加。"));
        else if (foods.size() > shown) list.addView(muted("结果较多，请继续输入名称、编码或选择分类缩小范围。"));
    }

    @Override protected void showAmountDialog(NutritionData.Food food, String meal) {
        LinearLayout wrap = new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(dp(24), dp(8), dp(24), 0);
        Spinner mealSpinner = spinner(MEALS, meal);
        EditText amount = input(amountLabel(food), true); amount.setText(one(food.defaultAmount()));
        TextView preview = muted("");
        wrap.addView(text("餐次", 13, true)); wrap.addView(mealSpinner);
        wrap.addView(text(amountLabel(food), 13, true)); wrap.addView(amount); wrap.addView(preview);
        Runnable update = () -> {
            double a = parse(amount), r = food.ratio(a);
            String value = one(food.kcal * r) + " kcal · 蛋白" + one(food.protein * r)
                    + "g · 脂肪" + one(food.fat * r) + "g · 碳水" + one(food.carb * r)
                    + "g\n钙 " + one(food.calcium * r) + "mg · 铁 " + one(food.iron * r)
                    + "mg · 钾 " + one(food.potassium * r) + "mg";
            if(food.isCombo()) value += "\n" + one(a) + "份 · " + comboWeightSummary(food.components,r)
                    + "\n" + componentSummary(food.components,r);
            preview.setText(value);
        };
        amount.addTextChangedListener(new SimpleWatcher(update)); update.run();
        new AlertDialog.Builder(this).setTitle(food.name).setView(wrap)
                .setNeutralButton("营养详情", (d, w) -> showFoodDetails(food))
                .setNegativeButton("取消", null)
                .setPositiveButton("加入记录", (d, w) -> {
                    double a = parse(amount); if (a <= 0) return;
                    double r = food.ratio(a);
                    NutritionData.Entry e = new NutritionData.Entry();
                    e.id = "e" + System.currentTimeMillis(); e.date = selectedDate.toString();
                    e.meal = String.valueOf(mealSpinner.getSelectedItem()); e.name = food.name;
                    e.amount = a; e.amountUnit = food.amountUnit(); e.applyFood(food, r);
                    entries.add(e); NutritionData.saveEntries(this, entries);
                    screen = "今日"; showScreen();
                }).show();
    }

    @Override protected void showFoodLibrary() {
        content.addView(text("食物库", 21, true));
        LinearLayout source = box();
        source.addView(text("已载入 " + builtInFoods.size() + " 条中国食物成分数据", 16, true));
        source.addView(muted(FoodCatalog.sourceNotice(this)));
        source.addView(muted("数据源：" + FoodCatalog.SOURCE_REPOSITORY + " @ " + FoodCatalog.SOURCE_COMMIT.substring(0, 7)));
        content.addView(source);
        EditText search = input("搜索米饭、鸡蛋、牛奶、食物编码……", false);
        Spinner category = spinner(categories(), "全部"); TextView count = muted("");
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        content.addView(search); content.addView(category); content.addView(count); content.addView(list);
        Runnable render = () -> {
            List<NutritionData.Food> foods = filterFoods(search.getText().toString(), String.valueOf(category.getSelectedItem()));
            count.setText("找到 " + foods.size() + " 条" + (foods.size() > LIBRARY_LIMIT ? "，显示前 " + LIBRARY_LIMIT + " 条" : ""));
            renderLibraryRows(list, foods);
        };
        search.addTextChangedListener(new SimpleWatcher(render)); category.setOnItemSelectedListener(new SimpleItemSelected(render)); render.run();
    }

    private void renderLibraryRows(LinearLayout list, List<NutritionData.Food> foods) {
        list.removeAllViews(); int shown = Math.min(LIBRARY_LIMIT, foods.size());
        for (int i = 0; i < shown; i++) {
            NutritionData.Food f = foods.get(i);
            LinearLayout card = box(), row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail = new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(f.name + (f.brand.isEmpty() ? "" : " · " + f.brand), 15, true));
            detail.addView(muted(foodMeta(f)));
            if(f.isCombo())detail.addView(muted(componentSummary(f.components,1d)));
            Button info = button("详情"); info.setOnClickListener(v -> showFoodDetails(f));
            Button add = button("＋"); add.setOnClickListener(v -> showAmountDialog(f, "加餐"));
            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(info); row.addView(add);
            card.addView(row); list.addView(card);
        }
        if (foods.size() > shown) list.addView(muted("一次最多显示 " + LIBRARY_LIMIT + " 条，请使用搜索或分类继续筛选。"));
    }

    protected void showFoodDetails(NutritionData.Food f) {
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20), dp(8), dp(20), dp(16));
        if(f.isCombo()){
            body.addView(text("套餐配方（每1份）",16,true));
            body.addView(text(comboWeightSummary(f.components,1d)+" · "+f.components.size()+"项食材",14,true));
            for(NutritionData.Component c:f.components)body.addView(muted("• "+c.name+"  "+one(c.amount)+c.amountUnit));
        }
        body.addView(text(f.basisLabel() + "营养成分", 16, true));
        addDetail(body, "热量", f.kcal, "kcal"); addDetail(body, "蛋白质", f.protein, "g");
        addDetail(body, "脂肪", f.fat, "g"); addDetail(body, "碳水化合物", f.carb, "g");
        addDetail(body, "膳食纤维", f.fiber, "g"); addDetail(body, "水分", f.water, "g");
        addDetail(body, "胆固醇", f.cholesterol, "mg"); addDetail(body, "钠", f.sodium, "mg");
        body.addView(text("维生素", 16, true));
        addDetail(body, "维生素A", f.vitaminA, "μgRAE"); addDetail(body, "胡萝卜素", f.carotene, "μg");
        addDetail(body, "视黄醇", f.retinol, "μg"); addDetail(body, "维生素B1", f.thiamin, "mg");
        addDetail(body, "维生素B2", f.riboflavin, "mg"); addDetail(body, "烟酸", f.niacin, "mg");
        addDetail(body, "维生素C", f.vitaminC, "mg"); addDetail(body, "维生素E", f.vitaminE, "mg");
        body.addView(text("矿物质", 16, true));
        addDetail(body, "钙", f.calcium, "mg"); addDetail(body, "磷", f.phosphorus, "mg");
        addDetail(body, "钾", f.potassium, "mg"); addDetail(body, "镁", f.magnesium, "mg");
        addDetail(body, "铁", f.iron, "mg"); addDetail(body, "锌", f.zinc, "mg");
        addDetail(body, "硒", f.selenium, "μg"); addDetail(body, "铜", f.copper, "mg"); addDetail(body, "锰", f.manganese, "mg");
        if (f.edible > 0) addDetail(body, "可食部", f.edible, "%");
        if (!f.sourceCode.isEmpty()) body.addView(muted("食物编码：" + f.sourceCode));
        if (!f.remark.isEmpty()) body.addView(muted("备注：" + f.remark));
        if (!f.source.isEmpty()&&!f.isCombo()) body.addView(muted("数据源：" + f.source + "\n注：原始Tr、空值或无法解析值按0显示；请优先以商品标签或官方资料为准。"));
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        new AlertDialog.Builder(this).setTitle(f.name).setView(scroll).setNegativeButton("关闭", null)
                .setPositiveButton("记录", (d, w) -> showAmountDialog(f, "加餐")).show();
    }

    private void addDetail(LinearLayout body, String label, double value, String unit) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(label, 14, false), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text(one(value) + " " + unit, 14, true)); body.addView(row);
    }

    protected static final class SimpleWatcher implements TextWatcher {
        private final Runnable action; SimpleWatcher(Runnable a) { action = a; }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) { action.run(); }
        public void afterTextChanged(Editable s) {}
    }
    protected static final class SimpleItemSelected implements AdapterView.OnItemSelectedListener {
        private final Runnable action; SimpleItemSelected(Runnable a) { action = a; }
        public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { action.run(); }
        public void onNothingSelected(AdapterView<?> p) {}
    }
}
