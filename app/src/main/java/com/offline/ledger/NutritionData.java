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
        return safeNumber(o.optDouble(key, safeNumber(fallback)));
    }

    private static double scaled(double value, double ratio) {
        return safeNumber(safeNumber(value) * safeNumber(ratio));
    }

    public static final class Component {
        public String name = "", amountUnit = "g";
        public double amount, weightGrams;
        public boolean weightKnown = true;
        public double kcal, protein, fat, carb, fiber, sodium;
        public double cholesterol, vitaminA, thiamin, riboflavin, niacin, vitaminC, vitaminE;
        public double calcium, phosphorus, potassium, magnesium, iron, zinc, selenium, copper, manganese;

        public static Component fromFood(Food food, double amount) {
            Component c = new Component();
            double safeAmount = Math.max(0d, safeNumber(amount));
            double ratio = food.ratio(safeAmount);
            c.name = food.name;
            c.amount = safeAmount;
            c.amountUnit = food.amountUnit();
            if (food.isPerMilliliter()) {
                c.weightKnown = false;
                c.weightGrams = 0d;
            } else if (food.isPerServing()) {
                c.weightKnown = food.servingSize > 0d;
                c.weightGrams = c.weightKnown ? scaled(food.servingSize, safeAmount) : 0d;
            } else {
                c.weightKnown = true;
                c.weightGrams = safeAmount;
            }
            c.kcal = scaled(food.kcal, ratio); c.protein = scaled(food.protein, ratio);
            c.fat = scaled(food.fat, ratio); c.carb = scaled(food.carb, ratio);
            c.fiber = scaled(food.fiber, ratio); c.sodium = scaled(food.sodium, ratio);
            c.cholesterol = scaled(food.cholesterol, ratio); c.vitaminA = scaled(food.vitaminA, ratio);
            c.thiamin = scaled(food.thiamin, ratio); c.riboflavin = scaled(food.riboflavin, ratio);
            c.niacin = scaled(food.niacin, ratio); c.vitaminC = scaled(food.vitaminC, ratio);
            c.vitaminE = scaled(food.vitaminE, ratio); c.calcium = scaled(food.calcium, ratio);
            c.phosphorus = scaled(food.phosphorus, ratio); c.potassium = scaled(food.potassium, ratio);
            c.magnesium = scaled(food.magnesium, ratio); c.iron = scaled(food.iron, ratio);
            c.zinc = scaled(food.zinc, ratio); c.selenium = scaled(food.selenium, ratio);
            c.copper = scaled(food.copper, ratio); c.manganese = scaled(food.manganese, ratio);
            return c;
        }

        public Component scaledCopy(double ratio) {
            Component c = new Component();
            c.name = name;
            c.amountUnit = amountUnit;
            c.amount = scaled(amount, ratio);
            c.weightKnown = weightKnown;
            c.weightGrams = scaled(weightGrams, ratio);
            c.kcal = scaled(kcal, ratio); c.protein = scaled(protein, ratio);
            c.fat = scaled(fat, ratio); c.carb = scaled(carb, ratio);
            c.fiber = scaled(fiber, ratio); c.sodium = scaled(sodium, ratio);
            c.cholesterol = scaled(cholesterol, ratio); c.vitaminA = scaled(vitaminA, ratio);
            c.thiamin = scaled(thiamin, ratio); c.riboflavin = scaled(riboflavin, ratio);
            c.niacin = scaled(niacin, ratio); c.vitaminC = scaled(vitaminC, ratio);
            c.vitaminE = scaled(vitaminE, ratio); c.calcium = scaled(calcium, ratio);
            c.phosphorus = scaled(phosphorus, ratio); c.potassium = scaled(potassium, ratio);
            c.magnesium = scaled(magnesium, ratio); c.iron = scaled(iron, ratio);
            c.zinc = scaled(zinc, ratio); c.selenium = scaled(selenium, ratio);
            c.copper = scaled(copper, ratio); c.manganese = scaled(manganese, ratio);
            return c;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("name", name); o.put("amount", safeNumber(amount)); o.put("amountUnit", amountUnit);
                o.put("weightGrams", safeNumber(weightGrams)); o.put("weightKnown", weightKnown);
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
            } catch (Exception ignored) {}
            return o;
        }

        static Component fromJson(JSONObject o) {
            Component c = new Component();
            c.name = o.optString("name"); c.amount = jsonNumber(o, "amount");
            c.amountUnit = o.optString("amountUnit", "g");
            c.weightGrams = jsonNumber(o, "weightGrams"); c.weightKnown = o.optBoolean("weightKnown", true);
            c.kcal = jsonNumber(o, "kcal"); c.protein = jsonNumber(o, "protein");
            c.fat = jsonNumber(o, "fat"); c.carb = jsonNumber(o, "carb");
            c.fiber = jsonNumber(o, "fiber"); c.sodium = jsonNumber(o, "sodium");
            c.cholesterol = jsonNumber(o, "cholesterol"); c.vitaminA = jsonNumber(o, "vitaminA");
            c.thiamin = jsonNumber(o, "thiamin"); c.riboflavin = jsonNumber(o, "riboflavin");
            c.niacin = jsonNumber(o, "niacin"); c.vitaminC = jsonNumber(o, "vitaminC");
            c.vitaminE = jsonNumber(o, "vitaminE"); c.calcium = jsonNumber(o, "calcium");
            c.phosphorus = jsonNumber(o, "phosphorus"); c.potassium = jsonNumber(o, "potassium");
            c.magnesium = jsonNumber(o, "magnesium"); c.iron = jsonNumber(o, "iron");
            c.zinc = jsonNumber(o, "zinc"); c.selenium = jsonNumber(o, "selenium");
            c.copper = jsonNumber(o, "copper"); c.manganese = jsonNumber(o, "manganese");
            return c;
        }
    }

    private static List<Component> copyComponents(List<Component> source, double ratio) {
        List<Component> out = new ArrayList<>();
        if (source == null) return out;
        for (Component c : source) if (c != null) out.add(c.scaledCopy(ratio));
        return out;
    }

    private static void putComponents(JSONObject o, List<Component> components) throws Exception {
        JSONArray a = new JSONArray();
        if (components != null) for (Component c : components) if (c != null) a.put(c.toJson());
        o.put("components", a);
    }

    private static List<Component> readComponents(JSONObject o) {
        List<Component> out = new ArrayList<>();
        JSONArray a = o.optJSONArray("components");
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject item = a.optJSONObject(i);
            if (item != null) out.add(Component.fromJson(item));
        }
        return out;
    }

    public static final class Food {
        public static final String BASIS_100G = "100g";
        public static final String BASIS_100ML = "100ml";
        public static final String BASIS_SERVING = "serving";
        public static final String SOURCE_COMBO = "meal_combo";

        public String id, name, brand, category, subCategory = "", basis;
        public String source = "", sourceCode = "", remark = "";
        public double kcal, protein, fat, carb, fiber, sodium, servingSize;
        public double edible, water, cholesterol, ash;
        public double vitaminA, carotene, retinol, thiamin, riboflavin, niacin, vitaminC, vitaminE;
        public double calcium, phosphorus, potassium, magnesium, iron, zinc, selenium, copper, manganese;
        public List<Component> components = new ArrayList<>();

        public Food(String id, String name, String brand, String category,
                    double kcal, double protein, double fat, double carb,
                    double fiber, double sodium) {
            this(id, name, brand, category, kcal, protein, fat, carb, fiber, sodium, BASIS_100G, 100d);
        }

        public Food(String id, String name, String brand, String category,
                    double kcal, double protein, double fat, double carb,
                    double fiber, double sodium, String basis, double servingSize) {
            this.id = id == null ? "" : id; this.name = name == null ? "" : name;
            this.brand = brand == null ? "" : brand; this.category = category == null ? "" : category;
            this.kcal = safeNumber(kcal); this.protein = safeNumber(protein);
            this.fat = safeNumber(fat); this.carb = safeNumber(carb);
            this.fiber = safeNumber(fiber); this.sodium = safeNumber(sodium);
            this.basis = normalizeBasis(basis); this.servingSize = Math.max(0d, safeNumber(servingSize));
        }

        public static Food createCombo(String id, String name, List<Component> sourceComponents) {
            Food f = new Food(id, name, "", "我的套餐", 0, 0, 0, 0, 0, 0, BASIS_SERVING, 0);
            f.source = SOURCE_COMBO;
            f.components = copyComponents(sourceComponents, 1d);
            f.recalculateFromComponents();
            return f;
        }

        public void recalculateFromComponents() {
            kcal = protein = fat = carb = fiber = sodium = 0d;
            cholesterol = vitaminA = thiamin = riboflavin = niacin = vitaminC = vitaminE = 0d;
            calcium = phosphorus = potassium = magnesium = iron = zinc = selenium = copper = manganese = 0d;
            servingSize = 0d;
            for (Component c : components) {
                kcal += safeNumber(c.kcal); protein += safeNumber(c.protein); fat += safeNumber(c.fat);
                carb += safeNumber(c.carb); fiber += safeNumber(c.fiber); sodium += safeNumber(c.sodium);
                cholesterol += safeNumber(c.cholesterol); vitaminA += safeNumber(c.vitaminA);
                thiamin += safeNumber(c.thiamin); riboflavin += safeNumber(c.riboflavin);
                niacin += safeNumber(c.niacin); vitaminC += safeNumber(c.vitaminC); vitaminE += safeNumber(c.vitaminE);
                calcium += safeNumber(c.calcium); phosphorus += safeNumber(c.phosphorus);
                potassium += safeNumber(c.potassium); magnesium += safeNumber(c.magnesium);
                iron += safeNumber(c.iron); zinc += safeNumber(c.zinc); selenium += safeNumber(c.selenium);
                copper += safeNumber(c.copper); manganese += safeNumber(c.manganese);
                if (c.weightKnown) servingSize += safeNumber(c.weightGrams);
            }
            kcal = safeNumber(kcal); protein = safeNumber(protein); fat = safeNumber(fat); carb = safeNumber(carb);
            fiber = safeNumber(fiber); sodium = safeNumber(sodium); servingSize = safeNumber(servingSize);
        }

        private static String normalizeBasis(String value) {
            if (BASIS_100ML.equals(value) || BASIS_SERVING.equals(value)) return value;
            return BASIS_100G;
        }

        public boolean isPerServing() { return BASIS_SERVING.equals(basis); }
        public boolean isPerMilliliter() { return BASIS_100ML.equals(basis); }
        public boolean isCombo() { return SOURCE_COMBO.equals(source) || (components != null && !components.isEmpty()); }
        public boolean comboWeightComplete() {
            if (!isCombo() || components.isEmpty()) return false;
            for (Component c : components) if (!c.weightKnown) return false;
            return true;
        }
        public boolean hasExtendedNutrients() {
            return !source.isEmpty() || cholesterol != 0 || calcium != 0 || iron != 0
                    || potassium != 0 || magnesium != 0 || vitaminA != 0 || vitaminC != 0;
        }
        public String amountUnit() { return isPerServing() ? "份" : (isPerMilliliter() ? "ml" : "g"); }
        public double defaultAmount() { return isPerServing() ? 1d : 100d; }
        public double ratio(double amount) {
            double safeAmount = Math.max(0d, safeNumber(amount));
            return isPerServing() ? safeAmount : safeAmount / 100d;
        }
        public String basisLabel() { return isPerServing() ? "每1份" : (isPerMilliliter() ? "每100毫升" : "每100克"); }
        public String basisSuffix() { return isPerServing() ? "/份" : (isPerMilliliter() ? "/100ml" : "/100g"); }

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
                putFoodExtended(o, this); putComponents(o, components);
                o.put("schemaVersion", 5);
            } catch (Exception ignored) {}
            return o;
        }

        static Food fromJson(JSONObject o) {
            String basis = normalizeBasis(o.optString("basis", BASIS_100G));
            double defaultSize = BASIS_SERVING.equals(basis) ? 0d : 100d;
            Food f = new Food(o.optString("id"), o.optString("name"), o.optString("brand"),
                    o.optString("category", "我的食品"), jsonNumber(o, "kcal"),
                    jsonNumber(o, "protein"), jsonNumber(o, "fat"), jsonNumber(o, "carb"),
                    jsonNumber(o, "fiber"), jsonNumber(o, "sodium"), basis,
                    jsonNumber(o, "servingSize", defaultSize));
            f.subCategory = o.optString("subCategory"); f.source = o.optString("source");
            f.sourceCode = o.optString("sourceCode"); f.remark = o.optString("remark");
            readFoodExtended(o, f); f.components = readComponents(o);
            if (f.isCombo()) { f.source = SOURCE_COMBO; f.basis = BASIS_SERVING; f.recalculateFromComponents(); }
            return f;
        }
    }

    public static final class Entry {
        public String id, date, meal, name, amountUnit = "g", source = "";
        public double amount, kcal, protein, fat, carb, fiber, sodium;
        public double cholesterol, vitaminA, thiamin, riboflavin, niacin, vitaminC, vitaminE;
        public double calcium, phosphorus, potassium, magnesium, iron, zinc, selenium, copper, manganese;
        public List<Component> components = new ArrayList<>();

        public boolean isCombo() { return components != null && !components.isEmpty(); }

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
            source = f.source; components = copyComponents(f.components, ratio);
        }

        public Entry copyTo(String newDate, String newMeal, String newId) {
            Entry copy = fromJson(toJson());
            copy.id = newId; copy.date = newDate; copy.meal = newMeal;
            return copy;
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
                putComponents(o, components); o.put("schemaVersion", 5);
            } catch (Exception ignored) {}
            return o;
        }

        static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.id = o.optString("id"); e.date = o.optString("date"); e.meal = o.optString("meal");
            e.name = o.optString("name"); e.amount = jsonNumber(o, "amount");
            e.amountUnit = o.optString("amountUnit", "g"); if (e.amountUnit.isEmpty()) e.amountUnit = "g";
            e.source = o.optString("source"); e.kcal = jsonNumber(o, "kcal");
            e.protein = jsonNumber(o, "protein"); e.fat = jsonNumber(o, "fat");
            e.carb = jsonNumber(o, "carb"); e.fiber = jsonNumber(o, "fiber");
            e.sodium = jsonNumber(o, "sodium"); e.cholesterol = jsonNumber(o, "cholesterol");
            e.vitaminA = jsonNumber(o, "vitaminA"); e.thiamin = jsonNumber(o, "thiamin");
            e.riboflavin = jsonNumber(o, "riboflavin"); e.niacin = jsonNumber(o, "niacin");
            e.vitaminC = jsonNumber(o, "vitaminC"); e.vitaminE = jsonNumber(o, "vitaminE");
            e.calcium = jsonNumber(o, "calcium"); e.phosphorus = jsonNumber(o, "phosphorus");
            e.potassium = jsonNumber(o, "potassium"); e.magnesium = jsonNumber(o, "magnesium");
            e.iron = jsonNumber(o, "iron"); e.zinc = jsonNumber(o, "zinc");
            e.selenium = jsonNumber(o, "selenium"); e.copper = jsonNumber(o, "copper");
            e.manganese = jsonNumber(o, "manganese"); e.components = readComponents(o);
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
            o.put("sodium", safeNumber(g.sodium)); o.put("schemaVersion", 5);
        } catch (Exception ignored) {}
        prefs(c).edit().putString(KEY_GOAL, o.toString()).apply();
    }

    public static void clear(Context c) { prefs(c).edit().clear().apply(); }
}
