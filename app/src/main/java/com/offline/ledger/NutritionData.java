package com.offline.ledger;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class NutritionData {
    private static final String PREFS = "nutrition_ledger_v1";
    private static final String KEY_LOGS = "logs";
    private static final String KEY_CUSTOM = "custom_foods";
    private static final String KEY_GOAL = "goal";

    private NutritionData() {}

    public static final class Food {
        public static final String BASIS_100G = "100g";
        public static final String BASIS_100ML = "100ml";
        public static final String BASIS_SERVING = "serving";

        public String id, name, brand, category, basis;
        public double kcal, protein, fat, carb, fiber, sodium, servingSize;

        public Food(String id, String name, String brand, String category,
                    double kcal, double protein, double fat, double carb,
                    double fiber, double sodium) {
            this(id, name, brand, category, kcal, protein, fat, carb, fiber, sodium,
                    BASIS_100G, 100d);
        }

        public Food(String id, String name, String brand, String category,
                    double kcal, double protein, double fat, double carb,
                    double fiber, double sodium, String basis, double servingSize) {
            this.id = id;
            this.name = name;
            this.brand = brand;
            this.category = category;
            this.kcal = kcal;
            this.protein = protein;
            this.fat = fat;
            this.carb = carb;
            this.fiber = fiber;
            this.sodium = sodium;
            this.basis = normalizeBasis(basis);
            this.servingSize = Math.max(0d, servingSize);
        }

        private static String normalizeBasis(String value) {
            if (BASIS_100ML.equals(value) || BASIS_SERVING.equals(value)) return value;
            return BASIS_100G;
        }

        public boolean isPerServing() { return BASIS_SERVING.equals(basis); }
        public boolean isPerMilliliter() { return BASIS_100ML.equals(basis); }
        public String amountUnit() { return isPerServing() ? "份" : (isPerMilliliter() ? "ml" : "g"); }
        public double defaultAmount() { return isPerServing() ? 1d : 100d; }
        public double ratio(double amount) { return isPerServing() ? amount : amount / 100d; }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name", name);
                o.put("brand", brand);
                o.put("category", category);
                o.put("kcal", kcal);
                o.put("protein", protein);
                o.put("fat", fat);
                o.put("carb", carb);
                o.put("fiber", fiber);
                o.put("sodium", sodium);
                o.put("basis", basis);
                o.put("servingSize", servingSize);
            } catch (Exception ignored) {}
            return o;
        }

        static Food fromJson(JSONObject o) {
            String basis = normalizeBasis(o.optString("basis", BASIS_100G));
            double defaultSize = BASIS_SERVING.equals(basis) ? 0d : 100d;
            return new Food(o.optString("id"), o.optString("name"), o.optString("brand"),
                    o.optString("category", "我的食品"), o.optDouble("kcal"),
                    o.optDouble("protein"), o.optDouble("fat"), o.optDouble("carb"),
                    o.optDouble("fiber"), o.optDouble("sodium"), basis,
                    o.optDouble("servingSize", defaultSize));
        }
    }

    public static final class Entry {
        public String id, date, meal, name, amountUnit = "g";
        public double amount, kcal, protein, fat, carb, fiber, sodium;

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("date", date);
                o.put("meal", meal);
                o.put("name", name);
                o.put("amount", amount);
                o.put("amountUnit", amountUnit);
                o.put("kcal", kcal);
                o.put("protein", protein);
                o.put("fat", fat);
                o.put("carb", carb);
                o.put("fiber", fiber);
                o.put("sodium", sodium);
            } catch (Exception ignored) {}
            return o;
        }

        static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.id = o.optString("id");
            e.date = o.optString("date");
            e.meal = o.optString("meal");
            e.name = o.optString("name");
            e.amount = o.optDouble("amount");
            e.amountUnit = o.optString("amountUnit", "g");
            if (e.amountUnit.isEmpty()) e.amountUnit = "g";
            e.kcal = o.optDouble("kcal");
            e.protein = o.optDouble("protein");
            e.fat = o.optDouble("fat");
            e.carb = o.optDouble("carb");
            e.fiber = o.optDouble("fiber");
            e.sodium = o.optDouble("sodium");
            return e;
        }
    }

    public static final class Goal {
        public double kcal = 2000, protein = 80, fat = 60, carb = 250, fiber = 25, sodium = 2000;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Entry> loadEntries(Context c) {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(KEY_LOGS, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Entry.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveEntries(Context c, List<Entry> entries) {
        JSONArray a = new JSONArray();
        for (Entry e : entries) a.put(e.toJson());
        prefs(c).edit().putString(KEY_LOGS, a.toString()).apply();
    }

    public static List<Food> loadCustomFoods(Context c) {
        List<Food> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(KEY_CUSTOM, "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Food.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return out;
    }

    public static void saveCustomFoods(Context c, List<Food> foods) {
        JSONArray a = new JSONArray();
        for (Food f : foods) a.put(f.toJson());
        prefs(c).edit().putString(KEY_CUSTOM, a.toString()).apply();
    }

    public static Goal loadGoal(Context c) {
        Goal g = new Goal();
        try {
            JSONObject o = new JSONObject(prefs(c).getString(KEY_GOAL, "{}"));
            g.kcal = o.optDouble("kcal", g.kcal);
            g.protein = o.optDouble("protein", g.protein);
            g.fat = o.optDouble("fat", g.fat);
            g.carb = o.optDouble("carb", g.carb);
            g.fiber = o.optDouble("fiber", g.fiber);
            g.sodium = o.optDouble("sodium", g.sodium);
        } catch (Exception ignored) {}
        return g;
    }

    public static void saveGoal(Context c, Goal g) {
        JSONObject o = new JSONObject();
        try {
            o.put("kcal", g.kcal);
            o.put("protein", g.protein);
            o.put("fat", g.fat);
            o.put("carb", g.carb);
            o.put("fiber", g.fiber);
            o.put("sodium", g.sodium);
        } catch (Exception ignored) {}
        prefs(c).edit().putString(KEY_GOAL, o.toString()).apply();
    }

    public static void clear(Context c) {
        prefs(c).edit().clear().apply();
    }
}
