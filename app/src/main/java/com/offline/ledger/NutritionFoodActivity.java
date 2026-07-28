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
            boolean c = "\u5168\u90e8".equals(category) || category.equals(f.category);
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
        s.add("\u5168\u90e8");
        for (NutritionData.Food f : allFoods()) s.add(f.category);
        return s.toArray(new String[0]);
    }

    protected String basisSuffix(NutritionData.Food food) { return food.basisSuffix(); }
    protected String amountLabel(NutritionData.Food food) {
        if (food.isPerServing()) return "\u4efd\u6570";
        return food.isPerMilliliter() ? "\u6444\u5165\u91cf\uff08\u6beb\u5347\uff09" : "\u6444\u5165\u91cf\uff08\u514b\uff09";
    }

    @Override protected void showFoodPicker(String meal) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(20), dp(8), dp(20), 0);
        EditText search = input("\u641c\u7d22\u98df\u7269\u540d\u79f0\u3001\u7f16\u7801\u6216\u5b50\u7c7b", false);
        Spinner category = spinner(categories(), "\u5168\u90e8");
        TextView count = muted("");
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        wrap.addView(search); wrap.addView(category); wrap.addView(count);
        wrap.addView(scroll, new LinearLayout.LayoutParams(-1, dp(450)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("\u6dfb\u52a0\u5230" + meal).setView(wrap).setNegativeButton("\u5173\u95ed", null).create();
        Runnable render = () -> {
            List<NutritionData.Food> foods = filterFoods(search.getText().toString(), String.valueOf(category.getSelectedItem()));
            count.setText("\u627e\u5230 " + foods.size() + " \u6761" + (foods.size() > PICKER_LIMIT ? "\uff0c\u663e\u793a\u524d " + PICKER_LIMIT + " \u6761" : ""));
            renderFoodRows(list, foods, meal, dialog);
        };
        search.addTextChangedListener(new SimpleWatcher(render));
        category.setOnItemSelectedListener(new SimpleItemSelected(render));
        dialog.setOnShowListener(x -> render.run());
        dialog.show();
    }

    private void renderFoodRows(LinearLayout list, List<NutritionData.Food> foods,
                                String meal, AlertDialog parent) {
        list.removeAllViews();
        int shown = Math.min(PICKER_LIMIT, foods.size());
        for (int i = 0; i < shown; i++) {
            NutritionData.Food f = foods.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(f.name + (f.brand.isEmpty() ? "" : " \u00b7 " + f.brand), 14, true));
            String category = f.category + (f.subCategory.isEmpty() ? "" : "/" + f.subCategory);
            detail.addView(muted(category + " \u00b7 " + one(f.kcal) + " kcal" + basisSuffix(f)
                    + " \u00b7 \u86cb\u767d" + one(f.protein) + "g \u00b7 \u8102\u80aa" + one(f.fat)
                    + "g \u00b7 \u78b3\u6c34" + one(f.carb) + "g"));
            Button info = button("\u8be6\u60c5");
            info.setOnClickListener(v -> showFoodDetails(f));
            Button add = button("\uff0b");
            add.setOnClickListener(v -> { parent.dismiss(); showAmountDialog(f, meal); });
            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(info); row.addView(add);
            list.addView(row);
        }
        if (foods.isEmpty()) list.addView(muted("\u6ca1\u6709\u627e\u5230\u98df\u7269\u3002\u53ef\u5728\u201c\u5305\u88c5\u98df\u54c1\u201d\u4e2d\u81ea\u884c\u6dfb\u52a0\u3002"));
        else if (foods.size() > shown) list.addView(muted("\u7ed3\u679c\u8f83\u591a\uff0c\u8bf7\u7ee7\u7eed\u8f93\u5165\u540d\u79f0\u3001\u7f16\u7801\u6216\u9009\u62e9\u5206\u7c7b\u7f29\u5c0f\u8303\u56f4\u3002"));
    }

    @Override protected void showAmountDialog(NutritionData.Food food, String meal) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(dp(24), dp(8), dp(24), 0);
        Spinner mealSpinner = spinner(MEALS, meal);
        EditText amount = input(amountLabel(food), true);
        amount.setText(one(food.defaultAmount()));
        TextView preview = muted("");
        wrap.addView(text("\u9910\u6b21", 13, true)); wrap.addView(mealSpinner);
        wrap.addView(text(amountLabel(food), 13, true)); wrap.addView(amount); wrap.addView(preview);
        Runnable update = () -> {
            double a = parse(amount), r = food.ratio(a);
            preview.setText(one(food.kcal * r) + " kcal \u00b7 \u86cb\u767d" + one(food.protein * r)
                    + "g \u00b7 \u8102\u80aa" + one(food.fat * r) + "g \u00b7 \u78b3\u6c34" + one(food.carb * r)
                    + "g\n\u9499 " + one(food.calcium * r) + "mg \u00b7 \u94c1 " + one(food.iron * r)
                    + "mg \u00b7 \u94be " + one(food.potassium * r) + "mg");
        };
        amount.addTextChangedListener(new SimpleWatcher(update)); update.run();
        new AlertDialog.Builder(this).setTitle(food.name).setView(wrap)
                .setNeutralButton("\u8425\u517b\u8be6\u60c5", (d, w) -> showFoodDetails(food))
                .setNegativeButton("\u53d6\u6d88", null)
                .setPositiveButton("\u52a0\u5165\u8bb0\u5f55", (d, w) -> {
                    double a = parse(amount); if (a <= 0) return;
                    double r = food.ratio(a);
                    NutritionData.Entry e = new NutritionData.Entry();
                    e.id = "e" + System.currentTimeMillis(); e.date = selectedDate.toString();
                    e.meal = String.valueOf(mealSpinner.getSelectedItem()); e.name = food.name;
                    e.amount = a; e.amountUnit = food.amountUnit(); e.applyFood(food, r);
                    entries.add(e); NutritionData.saveEntries(this, entries);
                    screen = "\u4eca\u65e5"; showScreen();
                }).show();
    }

    @Override protected void showFoodLibrary() {
        content.addView(text("\u98df\u7269\u5e93", 21, true));
        LinearLayout source = box();
        source.addView(text("\u5df2\u8f7d\u5165 " + builtInFoods.size() + " \u6761\u4e2d\u56fd\u98df\u7269\u6210\u5206\u6570\u636e", 16, true));
        source.addView(muted(FoodCatalog.sourceNotice(this)));
        source.addView(muted("\u6570\u636e\u6e90\uff1a" + FoodCatalog.SOURCE_REPOSITORY + " @ " + FoodCatalog.SOURCE_COMMIT.substring(0, 7)));
        content.addView(source);
        EditText search = input("\u641c\u7d22\u7c73\u996d\u3001\u9e21\u86cb\u3001\u725b\u5976\u3001\u98df\u7269\u7f16\u7801\u2026\u2026", false);
        Spinner category = spinner(categories(), "\u5168\u90e8");
        TextView count = muted("");
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        content.addView(search); content.addView(category); content.addView(count); content.addView(list);
        Runnable render = () -> {
            List<NutritionData.Food> foods = filterFoods(search.getText().toString(), String.valueOf(category.getSelectedItem()));
            count.setText("\u627e\u5230 " + foods.size() + " \u6761" + (foods.size() > LIBRARY_LIMIT ? "\uff0c\u663e\u793a\u524d " + LIBRARY_LIMIT + " \u6761" : ""));
            renderLibraryRows(list, foods);
        };
        search.addTextChangedListener(new SimpleWatcher(render));
        category.setOnItemSelectedListener(new SimpleItemSelected(render)); render.run();
    }

    private void renderLibraryRows(LinearLayout list, List<NutritionData.Food> foods) {
        list.removeAllViews();
        int shown = Math.min(LIBRARY_LIMIT, foods.size());
        for (int i = 0; i < shown; i++) {
            NutritionData.Food f = foods.get(i);
            LinearLayout card = box(), row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail = new LinearLayout(this); detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(f.name + (f.brand.isEmpty() ? "" : " \u00b7 " + f.brand), 15, true));
            String category = f.category + (f.subCategory.isEmpty() ? "" : "/" + f.subCategory);
            detail.addView(muted(category + " \u00b7 " + one(f.kcal) + " kcal" + basisSuffix(f)
                    + " \u00b7 \u86cb\u767d" + one(f.protein) + "g \u00b7 \u8102\u80aa" + one(f.fat)
                    + "g \u00b7 \u78b3\u6c34" + one(f.carb) + "g"));
            Button info = button("\u8be6\u60c5"); info.setOnClickListener(v -> showFoodDetails(f));
            Button add = button("\uff0b"); add.setOnClickListener(v -> showAmountDialog(f, "\u52a0\u9910"));
            row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(info); row.addView(add);
            card.addView(row); list.addView(card);
        }
        if (foods.size() > shown) list.addView(muted("\u4e00\u6b21\u6700\u591a\u663e\u793a " + LIBRARY_LIMIT + " \u6761\uff0c\u8bf7\u4f7f\u7528\u641c\u7d22\u6216\u5206\u7c7b\u7ee7\u7eed\u7b5b\u9009\u3002"));
    }

    protected void showFoodDetails(NutritionData.Food f) {
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20), dp(8), dp(20), dp(16));
        body.addView(text(f.basisLabel() + "\u8425\u517b\u6210\u5206", 16, true));
        addDetail(body, "\u70ed\u91cf", f.kcal, "kcal"); addDetail(body, "\u86cb\u767d\u8d28", f.protein, "g");
        addDetail(body, "\u8102\u80aa", f.fat, "g"); addDetail(body, "\u78b3\u6c34\u5316\u5408\u7269", f.carb, "g");
        addDetail(body, "\u81b3\u98df\u7ea4\u7ef4", f.fiber, "g"); addDetail(body, "\u6c34\u5206", f.water, "g");
        addDetail(body, "\u80c6\u56fa\u9187", f.cholesterol, "mg"); addDetail(body, "\u94a0", f.sodium, "mg");
        body.addView(text("\u7ef4\u751f\u7d20", 16, true));
        addDetail(body, "\u7ef4\u751f\u7d20A", f.vitaminA, "\u03bcgRAE"); addDetail(body, "\u80e1\u841d\u535c\u7d20", f.carotene, "\u03bcg");
        addDetail(body, "\u89c6\u9ec4\u9187", f.retinol, "\u03bcg"); addDetail(body, "\u7ef4\u751f\u7d20B1", f.thiamin, "mg");
        addDetail(body, "\u7ef4\u751f\u7d20B2", f.riboflavin, "mg"); addDetail(body, "\u70df\u9178", f.niacin, "mg");
        addDetail(body, "\u7ef4\u751f\u7d20C", f.vitaminC, "mg"); addDetail(body, "\u7ef4\u751f\u7d20E", f.vitaminE, "mg");
        body.addView(text("\u77ff\u7269\u8d28", 16, true));
        addDetail(body, "\u9499", f.calcium, "mg"); addDetail(body, "\u78f7", f.phosphorus, "mg");
        addDetail(body, "\u94be", f.potassium, "mg"); addDetail(body, "\u9541", f.magnesium, "mg");
        addDetail(body, "\u94c1", f.iron, "mg"); addDetail(body, "\u950c", f.zinc, "mg");
        addDetail(body, "\u7852", f.selenium, "\u03bcg"); addDetail(body, "\u94dc", f.copper, "mg");
        addDetail(body, "\u9530", f.manganese, "mg");
        if (f.edible > 0) addDetail(body, "\u53ef\u98df\u90e8", f.edible, "%");
        if (!f.sourceCode.isEmpty()) body.addView(muted("\u98df\u7269\u7f16\u7801\uff1a" + f.sourceCode));
        if (!f.remark.isEmpty()) body.addView(muted("\u5907\u6ce8\uff1a" + f.remark));
        if (!f.source.isEmpty()) body.addView(muted("\u6570\u636e\u6e90\uff1a" + f.source + "\n\u6ce8\uff1a\u539f\u59cbTr\u3001\u7a7a\u503c\u6216\u65e0\u6cd5\u89e3\u6790\u503c\u63090\u663e\u793a\uff1b\u8bf7\u4f18\u5148\u4ee5\u5546\u54c1\u6807\u7b7e\u6216\u5b98\u65b9\u8d44\u6599\u4e3a\u51c6\u3002"));
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        new AlertDialog.Builder(this).setTitle(f.name).setView(scroll).setNegativeButton("\u5173\u95ed", null)
                .setPositiveButton("\u8bb0\u5f55", (d, w) -> showAmountDialog(f, "\u52a0\u9910")).show();
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
