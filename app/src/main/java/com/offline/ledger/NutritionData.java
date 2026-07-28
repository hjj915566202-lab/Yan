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

    public static double safeNumber(double value) {
        return Double.isFinite(value) ? value : 0d;
    }

    private static double jsonNumber(JSONObject o, String key) {
        return safeNumber(o.optDouble(key, 0d));
    }

    private static double jsonNumber(JSONObject o, String key, double fallback) {
        double safeFallback = safeNumber(fallback);
        return safeNumber(o.optDouble(key, safeFallback));
    }

    private static double scaled(double value, double ratio) {
        return safeNumber(safeNumber(value) * safeNumber(ratio));
    }

    public static final class Food {
        public static final String BASIS_100G = "100g";
        public static final String BASIS_100ML = "100ml";
        public static final String BASIS_SERVING = "serving";

        public String id, name, brand, category, subCategory = "", basis;
        public String source = "", sourceCode = "", remark = "";
        public double kcal, protein, fat, carb, fiber, sodium, servingSize;
        public double edible, water, cholesterol, ash;
        public double vitaminA, carotene, retinol, thiamin, riboflavin, niacin, vitaminC, vitaminE;
        public double calcium, phosphorus, potassium, magnesium, iron, zinc, selenium, copper, manganese;

        public Food(String id, String name, String brand, String category,
                    double kcal, double protein, double fat, double carb,
                    double fiber, double sodium) {
            this(id, name, brand, category, kcal, protein, fat, carb, fiber, sodium,
                    BASIS_100G, 100d);
        }

        public Food(String id, String name, String brand, String category,
                    double kcal, double protein, double fat, double carb,
                    double fiber, double sodium, String basis, double servingSize) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.brand = brand == null ? "" : brand;
            this.category = category == null ? "" : category;
            this.kcal = safeNumber(kcal);
            this.protein = safeNumber(protein);
            this.fat = safeNumber(fat);
            this.carb = safeNumber(carb);
            this.fiber = safeNumber(fiber);
            this.sodium = safeNumber(sodium);
            this.basis = normalizeBasis(basis);
            this.servingSize = Math.max(0d, safeNumber(servingSize));
        }

        private static String normalizeBasis(String value) {
            if (BASIS_100ML.equals(value) || BASIS_SERVING.equals(value)) return value;
            return BASIS_100G;
        }

        public boolean isPerServing() { return BASIS_SERVING.equals(basis); }
        public boolean isPerMilliliter() { return BASIS_100ML.equals(basis); }
        public boolean hasExtendedNutrients() {
            return !source.isEmpty() || safeNumber(cholesterol) != 0 || safeNumber(calcium) != 0
                    || safeNumber(iron) != 0 || safeNumber(potassium) != 0
                    || safeNumber(magnesium) != 0 || safeNumber(vitaminA) != 0
                    || safeNumber(vitaminC) != 0;
        }
        public String amountUnit() { return isPerServing() ? "\u4efd" : (isPerMilliliter() ? "ml" : "g"); }
        public double defaultAmount() { return isPerServing() ? 1d : 100d; }
        public double ratio(double amount) {
            double safeAmount = Math.max(0d, safeNumber(amount));
            return isPerServing() ? safeAmount : safeAmount / 100d;
        }
        public String basisLabel() { return isPerServing() ? "\u6bcf1\u4efd" : (isPerMilliliter() ? "\u6bcf100\u6beb\u5347" : "\u6bcf100\u514b"); }
        public String basisSuffix() { return isPerServing() ? "/\u4efd" : (isPerMilliliter() ? "/100ml" : "/100g"); }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id); o.put("name", name); o.put("brand", brand);
                o.put("category", category); o.put("subCategory", subCategory);
                o.put("source", source); o.put("sourceCode", sourceCode); o.put("remark", remark);
                o.put("kcal", safeNumber(kcal)); o.put("protein", safeNumber(protein));
                o.put("fat", safeNumber(fat)); o.put("carb", safeNumber(carb));
                o.put("fiber", safeNumber(fiber)); o.put("sodium", safeNumber(sodium));
                o.put("basis", basis); o.put("servingSize", safeNumber(servingSize));
                putFoodExtended(o, this);
                o.put("schemaVersion", 4);
            } catch (Exception ignored) {}
            return o;
        }

        static Food fromJson(JSONObject o) {
            String basis = normalizeBasis(o.optString("basis", BASIS_100G));
            double defaultSize = BASIS_SERVING.equals(basis) ? 0d : 100d;
            Food f = new Food(o.optString("id"), o.optString("name"), o.optString("brand"),
                    o.optString("category", "\u6211\u7684\u98df\u54c1"), jsonNumber(o, "kcal"),
                    jsonNumber(o, "protein"), jsonNumber(o, "fat"), jsonNumber(o, "carb"),
                    jsonNumber(o, "fiber"), jsonNumber(o, "sodium"), basis,
                    jsonNumber(o, "servingSize", defaultSize));
            f.subCategory = o.optString("subCategory");
            f.source = o.optString("source");
            f.sourceCode = o.optString("sourceCode");
            f.remark = o.optString("remark");
            readFoodExtended(o, f);
            return f;
        }
    }

    public static final class Entry {
        public String id, date, meal, name, amountUnit = "g", source = "";
        public double amount, kcal, protein, fat, carb, fiber, sodium;
        public double cholesterol, vitaminA, thiamin, riboflavin, niacin, vitaminC, vitaminE;
        public double calcium, phosphorus, potassium, magnesium, iron, zinc, selenium, copper, manganese;

        public void applyFood(Food f, double ratio) {
            kcal = scaled(f.kcal, ratio); protein = scaled(f.protein, ratio);
            fat = scaled(f.fat, ratio); carb = scaled(f.carb, ratio);
            fiber = scaled(f.fiber, ratio); sodium = scaled(f.sodium, ratio);
            cholesterol = scaled(f.cholesterol, ratio); vitaminA = scaled(f.vitaminA, ratio);
            thiamin = scaled(f.thiamin, ratio); riboflavin = scaled(f.riboflavin, ratio);
            niacin = scaled(f.niacin, ratio); vitaminC = scaled(f.vitaminC, ratio);
            vitaminE = scaled(f.vitaminE, ratio); calcium = scaled(f.calcium, ratio);
            phosphorus = scaled(f.phosphorus, ratio); potassium = scaled(f.potassium, ratio);
            magnesium = scaled(f.magnesium, ratio); iron = scaled(f.iron, ratio);
            zinc = scaled(f.zinc, ratio); selenium = scaled(f.selenium, ratio);
            copper = scaled(f.copper, ratio); manganese = scaled(f.manganese, ratio);
            source = f.source;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id); o.put("date", date); o.put("meal", meal); o.put("name", name);
                o.put("amount", safeNumber(amount)); o.put("amountUnit", amountUnit); o.put("source", source);
                o.put("kcal", safeNumber(kcal)); o.put("protein", safeNumber(protein));
                o.put("fat", safeNumber(fat)); o.put("carb", safeNumber(carb));
                o.put("fiber", safeNumber(fiber)); o.put("sodium", safeNumber(sodium));
                o.put("cholesterol", safeNumber(cholesterol)); o.put("vitaminA", safeNumber(vitaminA));
                o.put("thiamin", safeNumber(thiamin)); o.put("riboflavin", safeNumber(riboflavin));
                o.put("niacin", safeNumber(niacin)); o.put("vitaminC", safeNumber(vitaminC));
                o.put("vitaminE", safeNumber(vitaminE)); o.put("calcium", safeNumber(calcium));
                o.put("phosphorus", safeNumber(phosphorus)); o.put("potassium", safeNumber(potassium));
                o.put("magnesium", safeNumber(magnesium)); o.put("iron", safeNumber(iron));
                o.put("zinc", safeNumber(zinc)); o.put("selenium", safeNumber(selenium));
                o.put("copper", safeNumber(copper)); o.put("manganese", safeNumber(manganese));
                o.put("schemaVersion", 4);
            } catch (Exception ignored) {}
            return o;
        }

        static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.id = o.optString("id"); e.date = o.optString("date"); e.meal = o.optString("meal");
            e.name = o.optString("name"); e.amount = jsonNumber(o, "amount");
            e.amountUnit = o.optString("amountUnit", "g"); if (e.amountUnit.isEmpty()) e.amountUnit = "g";
            e.source = o.optString("source");
            e.kcal = jsonNumber(o, "kcal"); e.protein = jsonNumber(o, "protein");
            e.fat = jsonNumber(o, "fat"); e.carb = jsonNumber(o, "carb");
            e.fiber = jsonNumber(o, "fiber"); e.sodium = jsonNumber(o, "sodium");
            e.cholesterol = jsonNumber(o, "cholesterol"); e.vitaminA = jsonNumber(o, "vitaminA");
            e.thiamin = jsonNumber(o, "thiamin"); e.riboflavin = jsonNumber(o, "riboflavin");
            e.niacin = jsonNumber(o, "niacin"); e.vitaminC = jsonNumber(o, "vitaminC");
            e.vitaminE = jsonNumber(o, "vitaminE"); e.calcium = jsonNumber(o, "calcium");
            e.phosphorus = jsonNumber(o, "phosphorus"); e.potassium = jsonNumber(o, "potassium");
            e.magnesium = jsonNumber(o, "magnesium"); e.iron = jsonNumber(o, "iron");
            e.zinc = jsonNumber(o, "zinc"); e.selenium = jsonNumber(o, "selenium");
            e.copper = jsonNumber(o, "copper"); e.manganese = jsonNumber(o, "manganese");
            return e;
        }
    }

    public static final class Goal {
        public double kcal = 2000;
        public double proteinPercent = 20, fatPercent = 30, carbPercent = 50;
        public double protein, fat, carb;
        public double fiber = 25, sodium = 2000;
        public Goal() { recalculateMacros(); }
        public void recalculateMacros() {
            kcal = safeNumber(kcal); proteinPercent = safeNumber(proteinPercent);
            fatPercent = safeNumber(fatPercent); carbPercent = safeNumber(carbPercent);
            protein = safeNumber(kcal * proteinPercent / 100d / 4d);
            fat = safeNumber(kcal * fatPercent / 100d / 9d);
            carb = safeNumber(kcal * carbPercent / 100d / 4d);
        }
        public double ratioTotal() { return safeNumber(proteinPercent + fatPercent + carbPercent); }
    }

    private static void putFoodExtended(JSONObject o, Food f) throws Exception {
        o.put("edible", safeNumber(f.edible)); o.put("water", safeNumber(f.water));
        o.put("cholesterol", safeNumber(f.cholesterol)); o.put("ash", safeNumber(f.ash));
        o.put("vitaminA", safeNumber(f.vitaminA)); o.put("carotene", safeNumber(f.carotene));
        o.put("retinol", safeNumber(f.retinol)); o.put("thiamin", safeNumber(f.thiamin));
        o.put("riboflavin", safeNumber(f.riboflavin)); o.put("niacin", safeNumber(f.niacin));
        o.put("vitaminC", safeNumber(f.vitaminC)); o.put("vitaminE", safeNumber(f.vitaminE));
        o.put("calcium", safeNumber(f.calcium)); o.put("phosphorus", safeNumber(f.phosphorus));
        o.put("potassium", safeNumber(f.potassium)); o.put("magnesium", safeNumber(f.magnesium));
        o.put("iron", safeNumber(f.iron)); o.put("zinc", safeNumber(f.zinc));
        o.put("selenium", safeNumber(f.selenium)); o.put("copper", safeNumber(f.copper));
        o.put("manganese", safeNumber(f.manganese));
    }

    private static void readFoodExtended(JSONObject o, Food f) {
        f.edible = jsonNumber(o, "edible"); f.water = jsonNumber(o, "water");
        f.cholesterol = jsonNumber(o, "cholesterol"); f.ash = jsonNumber(o, "ash");
        f.vitaminA = jsonNumber(o, "vitaminA"); f.carotene = jsonNumber(o, "carotene");
        f.retinol = jsonNumber(o, "retinol"); f.thiamin = jsonNumber(o, "thiamin");
        f.riboflavin = jsonNumber(o, "riboflavin"); f.niacin = jsonNumber(o, "niacin");
        f.vitaminC = jsonNumber(o, "vitaminC"); f.vitaminE = jsonNumber(o, "vitaminE");
        f.calcium = jsonNumber(o, "calcium"); f.phosphorus = jsonNumber(o, "phosphorus");
        f.potassium = jsonNumber(o, "potassium"); f.magnesium = jsonNumber(o, "magnesium");
        f.iron = jsonNumber(o, "iron"); f.zinc = jsonNumber(o, "zinc");
        f.selenium = jsonNumber(o, "selenium"); f.copper = jsonNumber(o, "copper");
        f.manganese = jsonNumber(o, "manganese");
    }

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static List<Entry> loadEntries(Context c) {
        List<Entry> out = new ArrayList<>();
        String raw = prefs(c).getString(KEY_LOGS, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) out.add(Entry.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        if (!out.isEmpty() && (raw.contains("NaN") || raw.contains("Infinity"))) saveEntries(c, out);
        return out;
    }

    public static void saveEntries(Context c, List<Entry> entries) {
        JSONArray a = new JSONArray(); for (Entry e : entries) a.put(e.toJson());
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
        JSONArray a = new JSONArray(); for (Food f : foods) a.put(f.toJson());
        prefs(c).edit().putString(KEY_CUSTOM, a.toString()).apply();
    }

    public static Goal loadGoal(Context c) {
        Goal g = new Goal();
        try {
            JSONObject o = new JSONObject(prefs(c).getString(KEY_GOAL, "{}"));
            g.kcal = jsonNumber(o, "kcal", g.kcal);
            if (o.has("proteinPercent") && o.has("fatPercent") && o.has("carbPercent")) {
                g.proteinPercent = jsonNumber(o, "proteinPercent", g.proteinPercent);
                g.fatPercent = jsonNumber(o, "fatPercent", g.fatPercent);
                g.carbPercent = jsonNumber(o, "carbPercent", g.carbPercent);
            } else if (o.has("protein") || o.has("fat") || o.has("carb")) {
                double pEnergy = jsonNumber(o, "protein") * 4d;
                double fEnergy = jsonNumber(o, "fat") * 9d;
                double cEnergy = jsonNumber(o, "carb") * 4d;
                double total = safeNumber(pEnergy + fEnergy + cEnergy);
                if (total > 0) {
                    g.proteinPercent = pEnergy / total * 100d;
                    g.fatPercent = fEnergy / total * 100d;
                    g.carbPercent = cEnergy / total * 100d;
                }
            }
            g.fiber = jsonNumber(o, "fiber", g.fiber);
            g.sodium = jsonNumber(o, "sodium", g.sodium);
            g.recalculateMacros();
        } catch (Exception ignored) {}
        return g;
    }

    public static void saveGoal(Context c, Goal g) {
        g.recalculateMacros(); JSONObject o = new JSONObject();
        try {
            o.put("kcal", safeNumber(g.kcal)); o.put("proteinPercent", safeNumber(g.proteinPercent));
            o.put("fatPercent", safeNumber(g.fatPercent)); o.put("carbPercent", safeNumber(g.carbPercent));
            o.put("protein", safeNumber(g.protein)); o.put("fat", safeNumber(g.fat));
            o.put("carb", safeNumber(g.carb)); o.put("fiber", safeNumber(g.fiber));
            o.put("sodium", safeNumber(g.sodium)); o.put("schemaVersion", 4);
        } catch (Exception ignored) {}
        prefs(c).edit().putString(KEY_GOAL, o.toString()).apply();
    }

    public static void clear(Context c) { prefs(c).edit().clear().apply(); }
}
