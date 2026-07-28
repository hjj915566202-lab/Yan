package com.offline.ledger;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class FoodCatalog {
    public static final String SOURCE_REPOSITORY = "Sanotsu/china-food-composition-data";
    public static final String SOURCE_COMMIT = "095034a96376d893582b412900fa8fdf792b4194";
    private static List<NutritionData.Food> cache;
    private static String sourceNotice = "";

    private FoodCatalog() {}

    public static synchronized List<NutritionData.Food> commonFoods(Context context) {
        if (cache == null) cache = loadAsset(context);
        return new ArrayList<>(cache);
    }

    public static synchronized int count(Context context) {
        if (cache == null) cache = loadAsset(context);
        return cache.size();
    }

    public static synchronized String sourceNotice(Context context) {
        if (cache == null) cache = loadAsset(context);
        return sourceNotice;
    }

    private static double n(JSONObject o, String key) {
        return NutritionData.safeNumber(o.optDouble(key, 0d));
    }

    private static List<NutritionData.Food> loadAsset(Context context) {
        List<NutritionData.Food> foods = new ArrayList<>();
        try (InputStream input = context.getAssets().open("china_food_composition.json")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16384];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            JSONObject root = new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            sourceNotice = root.optString("sourceNotice", "");
            JSONArray array = root.optJSONArray("foods");
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject o = array.optJSONObject(i);
                    if (o == null) continue;
                    NutritionData.Food f = new NutritionData.Food(
                            o.optString("id"), o.optString("name"), "",
                            o.optString("category", "\u5176\u4ed6\u7c7b"),
                            n(o, "kcal"), n(o, "protein"), n(o, "fat"), n(o, "carb"),
                            n(o, "fiber"), n(o, "sodium"));
                    f.subCategory = o.optString("subCategory");
                    f.source = SOURCE_REPOSITORY;
                    f.sourceCode = o.optString("sourceCode");
                    f.remark = o.optString("remark");
                    f.edible = n(o, "edible"); f.water = n(o, "water");
                    f.cholesterol = n(o, "cholesterol"); f.ash = n(o, "ash");
                    f.vitaminA = n(o, "vitaminA"); f.carotene = n(o, "carotene");
                    f.retinol = n(o, "retinol"); f.thiamin = n(o, "thiamin");
                    f.riboflavin = n(o, "riboflavin"); f.niacin = n(o, "niacin");
                    f.vitaminC = n(o, "vitaminC"); f.vitaminE = n(o, "vitaminE");
                    f.calcium = n(o, "calcium"); f.phosphorus = n(o, "phosphorus");
                    f.potassium = n(o, "potassium"); f.magnesium = n(o, "magnesium");
                    f.iron = n(o, "iron"); f.zinc = n(o, "zinc");
                    f.selenium = n(o, "selenium"); f.copper = n(o, "copper");
                    f.manganese = n(o, "manganese");
                    if (!f.name.isEmpty()) foods.add(f);
                }
            }
        } catch (Exception ignored) {
            sourceNotice = "\u6269\u5c55\u98df\u7269\u5e93\u52a0\u8f7d\u5931\u8d25\uff0c\u5df2\u4f7f\u7528\u5c11\u91cf\u5907\u7528\u6570\u636e\u3002";
        }
        if (foods.isEmpty()) foods.addAll(fallbackFoods());
        return foods;
    }

    private static NutritionData.Food f(String id, String name, String category,
            double kcal, double protein, double fat, double carb, double fiber, double sodium) {
        return new NutritionData.Food(id, name, "", category, kcal, protein, fat, carb, fiber, sodium);
    }

    private static List<NutritionData.Food> fallbackFoods() {
        return new ArrayList<>(Arrays.asList(
                f("rice", "\u7c73\u996d", "\u8c37\u7c7b\u53ca\u5176\u5236\u54c1", 116, 2.6, 0.3, 25.9, 0.3, 1),
                f("egg", "\u9e21\u86cb", "\u86cb\u7c7b\u53ca\u5176\u5236\u54c1", 144, 13.3, 8.8, 2.8, 0, 131),
                f("milk", "\u725b\u5976", "\u4e73\u7c7b\u53ca\u5176\u5236\u54c1", 61, 3.2, 3.3, 4.8, 0, 43),
                f("chicken", "\u9e21\u8089", "\u79bd\u8089\u7c7b\u53ca\u5176\u5236\u54c1", 145, 20.3, 6.7, 0.9, 0, 62.8),
                f("apple", "\u82f9\u679c", "\u6c34\u679c\u7c7b\u53ca\u5176\u5236\u54c1", 52, 0.3, 0.2, 13.8, 2.4, 1)
        ));
    }
}
