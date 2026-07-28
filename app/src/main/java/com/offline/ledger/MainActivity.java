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

public class MainActivity extends NutritionFoodActivity {
    @Override protected void showCustomFoodScreen() {
        content.addView(text("æ·»åŠ åŒ…è£…é£Ÿå“", 21, true));

        LinearLayout form = box();
        EditText name = input("é£Ÿå“åç§°", false);
        EditText brand = input("å“ç‰Œï¼ˆå¯é€‰ï¼‰", false);
        Spinner basis = spinner(new String[]{"æ¯100å…‹", "æ¯100æ¯«å‡", "æ¯1ä»½ï¼ˆ1è¢‹/1ç›’/1ä¸ªï¼‰"}, "æ¯100å…‹");
        EditText serving = input("ä¸€ä»½çº¦å¤šå°‘å…‹/æ¯«å‡ï¼ˆå¯é€‰ï¼‰", true);
        TextView basisHint = muted("ä¸‹é¢å¡«å†™çš„æ˜¯æ¯100å…‹çš„è¥å…»å€¼ã€‚");

        EditText kcal = input("çƒ­é‡ kcal", true);
        EditText protein = input("è›‹ç™½è´¨ g", true);
        EditText fat = input("è„‚è‚ª g", true);
        EditText carb = input("ç¢³æ°´ g", true);
        EditText fiber = input("è†³é£Ÿçº¤ç»´ g", true);
        EditText sodium = input("é’  mg", true);

        form.addView(name);
        form.addView(brand);
        form.addView(text("è¥å…»æ ‡ç­¾çš„è®¡é‡åŸºå‡†", 13, true));
        form.addView(basis);
        form.addView(basisHint);
        form.addView(serving);
        serving.setVisibility(View.GONE);
        form.addView(kcal);
        form.addView(protein);
        form.addView(fat);
        form.addView(carb);
        form.addView(fiber);
        form.addView(sodium);

        basis.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = String.valueOf(parent.getItemAtPosition(position));
                boolean perServing = selected.startsWith("æ¯1ä»½");
                serving.setVisibility(perServing ? View.VISIBLE : View.GONE);
                if (perServing) {
                    basisHint.setText("ä¸‹é¢å¡«å†™çš„æ˜¯1æ•´ä»½çš„å…¨éƒ¨è¥å…»å€¼ã€‚ä¿å­˜åä¸ä¼šæ¢ç®—ï¼Œä¹Ÿä¸ä¼šÃ—100ã€‚");
                } else if (selected.contains("æ¯«å‡")) {
                    basisHint.setText("ä¸‹é¢å¡«å†™çš„æ˜¯æ¯100æ¯«å‡çš„è¥å…»å€¼ã€‚");
                } else {
                    basisHint.setText("ä¸‹é¢å¡«å†™çš„æ˜¯æ¯100å…‹çš„è¥å…»å€¼ã€‚");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button save = button("ä¿å­˜åˆ°á£Ÿç‰©åº“");
        save.setOnClickListener(v -> {
            String foodName = name.getText().toString().trim();
            if (foodName.isEmpty() || parse(kcal) <= 0) {
                Toast.makeText(this, "è¯·å¡«å†™é£Ÿå“åç§°å’Œçƒ­é‡", Toast.LENGTH_SHORT).show();
                return;
            }

            String selected = String.valueOf(basis.getSelectedItem());
            String basisCode;
            if (selected.startsWith("æ¯1ä»½")) {
                basisCode = NutritionData.Food.BASIS_SERVING;
            } else if (selected.contains("æ¯«å‡")) {
                basisCode = NutritionData.Food.BASIS_100ML;
            } else {
                basisCode = NutritionData.Food.BASIS_100G;
            }

            NutritionData.Food food = new NutritionData.Food(
                    "c" + System.currentTimeMillis(),
                    foodName,
                    brand.getText().toString().trim(),
                    "æˆ‘çš„é£Ÿå“",
                    parse(kcal),
                    parse(protein),
                    parse(fat),
                    parse(carb),
                    parse(fiber),
                    parse(sodium),
                    basisCode,
                    NutritionData.Food.BASIS_SERVING.equals(basisCode) ? parse(serving) : 100d
            );

            customFoods.add(0, food);
            NutritionData.saveCustomFoods(this, customFoods);

            // Read it back from storage before refreshing, so the displayed basis is the persisted value.
            customFoods = NutritionData.loadCustomFoods(this);
            NutritionData.Food stored = customFoods.isEmpty() ? food : customFoods.get(0);
            Toast.makeText(this,
                    "å·²ä¿å­˜ï¼š" + one(stored.kcal) + " kcal" + stored.basisSuffix(),
                    Toast.LENGTH_LONG).show();
            showScreen();
        });
        form.addView(save);
        content.addView(form);

        content.addView(text("æˆ‘çš„é£Ÿå“", 19, true));
        if (customFoods.isEmpty()) content.addView(muted("è¿˜æ²¡æœ‰è‡ªå®šä¹‰é£Ÿå“ã€‚"));

        for (NutritionData.Food f : new ArrayList<>(customFoods)) {
            LinearLayout card = box();
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout detail = new LinearLayout(this);
            detail.setOrientation(LinearLayout.VERTICAL);
            detail.addView(text(f.name, 15, true));

            String meta = (f.brand.isEmpty() ? "è‡ªå®šä¹‰" : f.brand)
                    + " Â· " + one(f.kcal) + " kcal" + f.basisSuffix();
            if (f.isPerServing() && f.servingSize > 0) {
                meta += " Â· 1ä»½çº¦" + one(f.servingSize) + "å…‹/æ¯«å‡";
            }
            detail.addView(muted(meta));

            Button add = button("‹¢ºÃ–öTˆ¤ì(€€€€€€€€€€€…‘¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø€´øÍ¡½İµ½Õ¹Ñ¥…±½œ¡˜°€‹–*ƒ¦’@ˆ¤¤ì(€€€€€€€€€€€	ÕÑÑ½¸‘•°€ô‰ÕÑÑ½¸ ‹–"ƒ¦fˆ¤ì(€€€€€€€€€€€‘•°¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø€´øì(€€€€€€€€€€€€€€€ÕÍÑ½µ½½‘Ì¹É•µ½Ù”¡˜¤ì(€€€€€€€€€€€€€€€9ÕÑÉ¥Ñ¥½¹…Ñ„¹Í…Ù•ÕÍÑ½µ½½‘Ì¡Ñ¡¥Ì°ÕÍÑ½µ½½‘Ì¤ì(€€€€€€€€€€€€€€€Í¡½İMÉ••¸ ¤ì(€€€€€€€€€€€ô¤ì((€€€€€€€€€€€É½Ü¹…‘‘Y¥•Ü¡‘•Ñ…¥°°¹•Ü1¥¹•…É1…å½ÕĞ¹1…å½ÕÑA…É…µÌ À°€´È°€Ä¤¤ì(€€€€€€€€€€€É½Ü¹…‘‘Y¥•Ü¡…‘¤ì(€€€€€€€€€€€É½Ü¹…‘‘Y¥•Ü¡‘•°¤ì(€€€€€€€€€€€…É¹…‘‘Y¥•Ü¡É½Ü¤ì(€€€€€€€€€€€½¹Ñ•¹Ğ¹…‘‘Y¥•Ü¡…É¤ì(€€€€€€€ô(€€€ô((€€€=Ù•ÉÉ¥‘”ÁÉ½Ñ•Ñ•Ù½¥Í¡½İM•ÑÑ¥¹Ì ¤ì(€€€€€€€½¹Ñ•¹Ğ¹…‘‘Y¥•Ü¡Ñ•áĞ ‹š¾?š^—¢B—–ï¢º‡–"Hˆ°€ÈÄ°ÑÉÕ”¤¤ì(€€€€€€€½¹Ñ•¹Ğ¹…‘‘Y¥•Ü¡µÕÑ• ‹¢ºûö»·¦?’â+¦fC–J3’â'–’Ÿ¢B—–ïÒƒ·¦?š¾S’ú/¾ò3n»š‚–/šVÃ’òk¢«–*£¢º‡º_ˆ¤¤ì((€€€€€€€1¥¹•…É1…å½ÕĞ™½É´€ô‰½à ¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡Ñ•áĞ ‹š¾?š^—·¦?’â+¦f@ˆ°€ÄĞ°ÑÉÕ”¤¤ì(€€€€€€€‘¥ÑQ•áĞ­…°€ô½…±%¹ÁÕĞ ‹·¦<­…°ˆ°½…°¹­…°¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡­…°¤ì((€€€€€€€™½É´¹…‘‘Y¥•Ü¡Ñ•áĞ ‹’â'–’Ÿ¢B—–ïÒƒ·¦?š¾S’ú,ˆ°€ÄĞ°ÑÉÕ”¤¤ì(€€€€€€€‘¥ÑQ•áĞÁÉ½Ñ•¥¹A•É•¹Ğ€ô½…±%¹ÁÕĞ ‹¢n/f÷¢Ò €”ˆ°½…°¹ÁÉ½Ñ•¥¹A•É•¹Ğ¤ì(€€€€€€€‘¥ÑQ•áĞ™…ÑA•É•¹Ğ€ô½…±%¹ÁÕĞ ‹¢¢
¨€”ˆ°½…°¹™…ÑA•É•¹Ğ¤ì(€€€€€€€‘¥ÑQ•áĞ…É‰A•É•¹Ğ€ô½…±%¹ÁÕĞ ‹ŠÏšÂĞ€”ˆ°½…°¹…É‰A•É•¹Ğ¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡ÁÉ½Ñ•¥¹A•É•¹Ğ¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡™…ÑA•É•¹Ğ¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡…É‰A•É•¹Ğ¤ì((€€€€€€€Q•áÑY¥•ÜÉ…Ñ¥½MÑ…ÑÕÌ€ôÑ•áĞ ˆˆ°€ÄĞ°ÑÉÕ”¤ì(€€€€€€€Q•áÑY¥•Ü…±Õ±…Ñ•€ôµÕÑ• ˆˆ¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡É…Ñ¥½MÑ…ÑÕÌ¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡…±Õ±…Ñ•¤ì((€€€€€€€™½É´¹…‘‘Y¥•Ü¡Ñ•áĞ ‹–Û’î[n»š‚ˆ°€ÄĞ°ÑÉÕ”¤¤ì(€€€€€€€‘¥ÑQ•áĞ™¥‰•È€ô½…±%¹ÁÕĞ ‹¢Ï¦ê“îĞœˆ°½…°¹™¥‰•È¤ì(€€€€€€€‘¥ÑQ•áĞÍ½‘¥Õ´€ô½…±%¹ÁÕĞ ‹¦J€µœˆ°½…°¹Í½‘¥Õ´¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡™¥‰•È¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡Í½‘¥Õ´¤ì((€€€€€€€IÕ¹¹…‰±”ÕÁ‘…Ñ•AÉ•Ù¥•Ü€ô€ ¤€´øì(€€€€€€€€€€€‘½Õ‰±”•¹•Éä€ôÁ…ÉÍ”¡­…°¤ì(€€€€€€€€€€€‘½Õ‰±”À€ôÁ…ÉÍ”¡ÁÉ½Ñ•¥¹A•É•¹Ğ¤ì(€€€€€€€€€€€‘½Õ‰±”˜€ôÁ…ÉÍ”¡™…ÑA•É•¹Ğ¤ì(€€€€€€€€€€€‘½Õ‰±”Œ€ôÁ…ÉÍ”¡…É‰A•É•¹Ğ¤ì(€€€€€€€€€€€‘½Õ‰±”Ñ½Ñ…°€ôÀ€¬˜€¬Œì(€€€€€€€€€€€‰½½±•…¸Ù…±¥€ô•¹•Éä€ø€À€˜˜5…Ñ ¹…‰Ì¡Ñ½Ñ…°€´€ÄÀÁ¤€ğ€À¸ÀÕì(€€€€€€€€€€€É…Ñ¥½MÑ…ÑÕÌ¹Í•ÑQ•áĞ ‹š¾S’ú/–B#¢º„€ˆ€¬½¹”¡Ñ½Ñ…°¤€¬€ˆ”ˆ€¬€¡Ù…±¥€ü€ˆƒŠrLˆ€è€‹¾ò#¦r¢š’âèÄÀÀ—¾ò$ˆ¤¤ì(€€€€€€€€€€€É…Ñ¥½MÑ…ÑÕÌ¹Í•ÑQ•áÑ½±½È¡Ù…±¥€üI8€è½±½È¹Éˆ ÄäÀ°€ØÔ°€ÔÔ¤¤ì((€€€€€€€€€€€‘½Õ‰±”ÁÉ½Ñ•¥¹É…µÌ€ô•¹•Éä€¨À€¼€ÄÀÁ€¼€Ñì(€€€€€€€€€€€‘½Õ‰±”™…ÑÉ…µÌ€ô•¹•Éä€¨˜€¼€ÄÀÁ€¼€åì(€€€€€€€€€€€‘½Õ‰±”…É‰É…µÌ€ô•¹•Éä€¨Œ€¼€ÄÀÁ€¼€Ñì(€€€€€€€€€€€…±Õ±…Ñ•¹Í•ÑQ•áĞ ‹¢«–*£¢º‡º_n»š‚¾òiq¸ˆ(€€€€€€€€€€€€€€€€€€€€¬€‹¢n/f÷¢Ò €ˆ€¬½¹”¡ÁÉ½Ñ•¥¹É…µÌ¤€¬€‰Ÿ¾ò ˆ€¬½¹”¡•¹•Éä€¨À€¼€ÄÀÁ¤€¬€ˆ­…³¾ò%q¸ˆ(€€€€€€€€€€€€€€€€€€€€¬€‹¢¢
¨€ˆ€¬½¹”¡™…ÑÉ…µÌ¤€¬€‰Ÿ¾ò ˆ€¬½¹”¡•¹•Éä€¨˜€¼€ÄÀÁ¤€¬€ˆ­…³¾ò%q¸ˆ(€€€€€€€€€€€€€€€€€€€€¬€‹ŠÏšÂĞ€ˆ€¬½¹”¡…É‰É…µÌ¤€¬€‰Ÿ¾ò ˆ€¬½¹”¡•¹•Éä€¨Œ€¼€ÄÀÁ¤€¬€ˆ­…³¾ò$ˆ¤ì(€€€€€€€ôì((€€€€€€€™½È€¡‘¥ÑQ•áĞ”€è¹•Ü‘¥ÑQ•áÑmuí­…°°ÁÉ½Ñ•¥¹A•É•¹Ğ°™…ÑA•É•¹Ğ°…É‰A•É•¹Ñô¤ì(€€€€€€€€€€€”¹…‘‘Q•áÑ¡…¹•‘1¥ÍÑ•¹•È¡¹•ÜM¥µÁ±•]…Ñ¡•È¡ÕÁ‘…Ñ•AÉ•Ù¥•Ü¤¤ì(€€€€€€€ô(€€€€€€€ÕÁ‘…Ñ•AÉ•Ù¥•Ü¹ÉÕ¸ ¤ì((€€€€€€€	ÕÑÑ½¸Í…Ù”€ô‰ÕÑÑ½¸ ‹’şw–¶c¢º‡–"Hˆ¤ì(€€€€€€€Í…Ù”¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø€´øì(€€€€€€€€€€€‘½Õ‰±”•¹•Éä€ôÁ…ÉÍ”¡­…°¤ì(€€€€€€€€€€€‘½Õ‰±”À€ôÁ…ÉÍ”¡ÁÉ½Ñ•¥¹A•É•¹Ğ¤ì(€€€€€€€€€€€‘½Õ‰±”˜€ôÁ…ÉÍ”¡™…ÑA•É•¹Ğ¤ì(€€€€€€€€€€€‘½Õ‰±”Œ€ôÁ…ÉÍ”¡…É‰A•É•¹Ğ¤ì(€€€€€€€€€€€¥˜€¡•¹•Éä€ğô€À¤ì(€€€€€€€€€€€€€€€Q½…ÍĞ¹µ…­•Q•áĞ¡Ñ¡¥Ì°€‹¢¾ß–†¯–g–’Ÿ’ê8Ãj·¦?’â+¦f@ˆ°Q½…ÍĞ¹19Q!}M!=IP¤¹Í¡½Ü ¤ì(€€€€€€€€€€€€€€€É•ÑÕÉ¸ì(€€€€€€€€€€€ô(€€€€€€€€€€€¥˜€¡5…Ñ ¹…‰Ì¡À€¬˜€¬Œ€´€ÄÀÁ¤€øô€À¸ÀÕ¤ì(€€€€€€€€€€€€€€€Q½…ÍĞ¹µ…­•Q•áĞ¡Ñ¡¥Ì°€‹¢n/f÷¢Ò£¢¢
«ŠÏšÂÓš¾S’ú/–B#¢º‡–ş¦†ï’âèÄÀÀ”ˆ°Q½…ÍĞ¹19Q!}1=9¤¹Í¡½Ü ¤ì(€€€€€€€€€€€€€€€É•ÑÕÉ¸ì(€€€€€€€€€€€ô((€€€€€€€€€€€½…°¹­…°€ô•¹•Éäì(€€€€€€€€€€€½…°¹ÁÉ½Ñ•¥¹A•É•¹Ğ€ôÀì(€€€€€€€€€€€½…°¹™…ÑA•É•¹Ğ€ô˜ì(€€€€€€€€€€€½…°¹…É‰A•É•¹Ğ€ôŒì(€€€€€€€€€€€½…°¹™¥‰•È€ôÙ…±Õ•=È¡™¥‰•È°€ÈÔ¤ì(€€€€€€€€€€€½…°¹Í½‘¥Õ´€ôÙ…±Õ•=È¡Í½‘¥Õ´°€ÈÀÀÀ¤ì(€€€€€€€€€€€½…°¹É•…±Õ±…Ñ•5…É½Ì ¤ì(€€€€€€€€€€€9ÕÑÉ¥Ñ¥½¹…Ñ„¹Í…Ù•½…°¡Ñ¡¥Ì°½…°¤ì(€€€€€€€€€€€Q½…ÍĞ¹µ…­•Q•áĞ¡Ñ¡¥Ì°(€€€€€€€€€€€€€€€€€€€€‹–ŞË’şw–¶c¾òk¢n/fôˆ€¬½¹”¡½…°¹ÁÉ½Ñ•¥¸¤€¬€‰œƒ
Üƒ¢¢
¨ˆ€¬½¹”¡½…°¹™…Ğ¤(€€€€€€€€€€€€€€€€€€€€€€€€€€€€¬€‰œƒ
ÜƒŠÏšÂĞˆ€¬½¹”¡½…°¹…Éˆ¤€¬€‰œˆ°(€€€€€€€€€€€€€€€€€€€Q½…ÍĞ¹19Q!}1=9¤¹Í¡½Ü ¤ì(€€€€€€€€€€€Í¡½İMÉ••¸ ¤ì(€€€€€€€ô¤ì(€€€€€€€™½É´¹…‘‘Y¥•Ü¡Í…Ù”¤ì(€€€€€€€½¹Ñ•¹Ğ¹…‘‘Y¥•Ü¡™½É´¤ì((€€€€€€€1¥¹•…É1…å½ÕĞ¹½Ñ”€ô‰½à ¤ì(€€€€€€€¹½Ñ”¹…‘‘Y¥•Ü¡Ñ•áĞ ‹¢º‡º_šZç–ò<ˆ°€ÄØ°ÑÉÕ”¤¤ì(€€€€€€€¹½Ñ”¹…‘‘Y¥•Ü¡µÕÑ• ‹¢n/f÷¢Ò£–J3ŠÏšÂÓš2$Ğ­…°½Ÿ¢º‡º_¾ò3¢¢
«š2$ä­…°½Ÿ¢º‡º_š¾S’ú/š2–B¢B—–ïÒƒš>C’úoj·¦?–6ƒš¾?š^—šï·¦?jš¾S’ú/ˆ¤¤ì(€€€€€€€¹½Ñ”¹…‘‘Y¥•Ü¡Ñ•áĞ ‹šVÃš6»¢¾Óšb8ˆ°€ÄØ°ÑÉÕ”¤¤ì(€€€€€€€¹½Ñ”¹…‘‘Y¥•Ü¡µÕÑ• ‹–ö»¦&§’âë–âã¢–>¢–ó¾ò3–º{¦f–2¢¦–N¢¾ß’î—–V–N¢B—–ïš‚¶û’âë––£¦£¢ºÃ–öW’î’şw–¶c–r£šr³šrëˆ¤¤ì(€€€€€€€	ÕÑÑ½¸±•…È€ô‰ÕÑÑ½¸ ‹šâ¦ë–£¦£šVÃš6¸ˆ¤ì(€€€€€€€±•…È¹Í•Ñ=¹±¥­1¥ÍÑ•¹•È¡Ø€´ø¹•Ü±•ÉÑ¥…±½œ¹	Õ¥±‘•È¡Ñ¡¥Ì¤(€€€€€€€€€€€€€€€€¹Í•ÑQ¥Ñ±” ‹šâ¦ëšVÃš6¸ˆ¤(€€€€€€€€€€€€€€€€¹Í•Ñ5•ÍÍ…” ‹†»–ºkšâ¦ë–£¦£¦–»¦¢ºÃ–öW¢«–ºk’æ'¦–N–J3n»š‚¢ºûö»–B_¾ò|ˆ¤(€€€€€€€€€€€€€€€€¹Í•Ñ9•…Ñ¥Ù•	ÕÑÑ½¸ ‹–>[šÚ ˆ°¹Õ±°¤(€€€€€€€€€€€€€€€€¹Í•ÑA½Í¥Ñ¥Ù•	ÕÑÑ½¸ ‹šâ¦èˆ°€¡°Ü¤€´øì(€€€€€€€€€€€€€€€€€€€9ÕÑÉ¥Ñ¥½¹…Ñ„¹±•…È¡Ñ¡¥Ì¤ì(€€€€€€€€€€€€€€€€€€€É•±½… ¤ì(€€€€€€€€€€€€€€€€€€€ÍÉ••¸€ô€‹’î+š^”ˆì(€€€€€€€€€€€€€€€€€€€Í¡½İMÉ••¸ ¤ì(€€€€€€€€€€€€€€€ô¤¹Í¡½Ü ¤¤ì(€€€€€€€¹½Ñ”¹…‘‘Y¥•Ü¡±•…È¤ì(€€€€€€€½¹Ñ•¹Ğ¹…‘‘Y¥•Ü¡¹½Ñ”¤ì(€€€ô((€€€ÁÉ¥Ù…Ñ”‘¥ÑQ•áĞ½…±%¹ÁÕĞ¡MÑÉ¥¹œ¡¥¹Ğ°‘½Õ‰±”Ù…±Õ”¤ì(€€€€€€€‘¥ÑQ•áĞ”€ô¥¹ÁÕĞ¡¡¥¹Ğ°ÑÉÕ”¤ì(€€€€€€€”¹Í•ÑQ•áĞ¡½¹”¡Ù…±Õ”¤¤ì(€€€€€€€É•ÑÕÉ¸”ì(€€€ô)ô