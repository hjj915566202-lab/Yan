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
        content.addView(text("\u6dfb\u52a0\u5305\u88c5\u98df\u54c1", 21, true));
        LinearLayout form = box();
        EditText name = input("\u98df\u54c1\u540d\u79f0", false);
        EditText brand = input("\u54c1\u724c\uff08\u53ef\u9009\uff09", false);
        Spinner basis = spinner(new String[]{"\u6bcf100\u514b", "\u6bcf100\u6beb\u5347", "\u6bcf1\u4efd\uff081\u888b/1\u76d2/1\u4e2a\uff09"}, "\u6bcf100\u514b");
        TextView hint = muted("\u4e0b\u9762\u586b\u5199\u7684\u662f\u6bcf100\u514b\u7684\u8425\u517b\u503c\u3002");
        EditText serving = input("\u4e00\u4efd\u7ea6\u591a\u5c11\u514b/\u6beb\u5347\uff08\u53ef\u9009\uff09", true);
        EditText kcal = input("\u70ed\u91cf kcal", true);
        EditText protein = input("\u86cb\u767d\u8d28 g", true);
        EditText fat = input("\u8102\u80aa g", true);
        EditText carb = input("\u78b3\u6c34 g", true);
        EditText fiber = input("\u81b3\u98df\u7ea4\u7ef4 g", true);
        EditText sodium = input("\u94a0 mg", true);
        form.addView(name); form.addView(brand); form.addView(text("\u8425\u517b\u6807\u7b7e\u7684\u8ba1\u91cf\u57fa\u51c6",13,true));
        form.addView(basis); form.addView(hint); form.addView(serving); serving.setVisibility(View.GONE);
        for (View x : new View[]{kcal,protein,fat,carb,fiber,sodium}) form.addView(x);
        basis.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                String selected=String.valueOf(p.getItemAtPosition(pos));
                boolean perServing=selected.startsWith("\u6bcf1\u4efd");
                serving.setVisibility(perServing?View.VISIBLE:View.GONE);
                hint.setText(perServing?"\u4e0b\u9762\u586b\u5199\u7684\u662f1\u6574\u4efd\u7684\u5168\u90e8\u8425\u517b\u503c\u3002\u4fdd\u5b58\u540e\u4e0d\u4f1a\u6362\u7b97\uff0c\u4e5f\u4e0d\u4f1a\u00d7100\u3002":
                        (selected.contains("\u6beb\u5347")?"\u4e0b\u9762\u586b\u5199\u7684\u662f\u6bcf100\u6beb\u5347\u7684\u8425\u517b\u503c\u3002":"\u4e0b\u9762\u586b\u5199\u7684\u662f\u6bcf100\u514b\u7684\u8425\u517b\u503c\u3002"));
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        Button save=button("\u4fdf\u5b58\u5230\u98df\u7269\u5e93");
        save.setOnClickListener(v->{
            String n=name.getText().toString().trim();
            if(n.isEmpty()||parse(kcal)<=0){Toast.makeText(this,"\u8bf7\u586b\u5199\u98df\u54c1\u540d\u79f0\u548c\u70ed\u91cf",Toast.LENGTH_SHORT).show();return;}
            String selected=String.valueOf(basis.getSelectedItem());
            String code=selected.startsWith("\u6bcf1\u4efd")?NutritionData.Food.BASIS_SERVING:
                    (selected.contains("\u6beb\u5347")?NutritionData.Food.BASIS_100ML:NutritionData.Food.BASIS_100G);
            NutritionData.Food food=new NutritionData.Food("c"+System.currentTimeMillis(),n,
                    brand.getText().toString().trim(),"\u6211\u7684\u98df\u54c1",parse(kcal),parse(protein),parse(fat),parse(carb),
                    parse(fiber),parse(sodium),code,NutritionData.Food.BASIS_SERVING.equals(code)?parse(serving):100d);
            customFoods.add(0,food); NutritionData.saveCustomFoods(this,customFoods);
            customFoods=NutritionData.loadCustomFoods(this);
            NutritionData.Food stored=customFoods.isEmpty()?food:customFoods.get(0);
            Toast.makeText(this,"\u5df2\u4fdd\u5b58\uff1a"+one(stored.kcal)+" kcal"+stored.basisSuffix(),Toast.LENGTH_LONG).show();
            showScreen();
        });
        form.addView(save); content.addView(form); content.addView(text("\u6211\u7684\u98df\u54c1",19,true));
        if(customFoods.isEmpty()) content.addView(muted("\u8fd8\u6ca1\u6709\u81ea\u5b9a\u4e49\u98df\u54c1\u3002"));
        for(NutritionData.Food f:new ArrayList<>(customFoods)) addFoodCard(f);
    }

    private void addFoodCard(NutritionData.Food f){
        LinearLayout card=box(),row=new LinearLayout(this),detail=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL); detail.setOrientation(LinearLayout.VERTICAL);
        detail.addView(text(f.name,15,true));
        String meta=(f.brand.isEmpty()?"\u81ea\u5b9a\u4e49":f.brand)+" \u00b7 "+one(f.kcal)+" kcal"+f.basisSuffix();
        if(f.isPerServing()&&f.servingSize>0) meta+=" \u00b7 1\u4efd\u7ea6"+one(f.servingSize)+"\u514b/\u6beb\u5347";
        detail.addView(muted(meta));
        Button add=button("\u8bb0\u5f55"); add.setOnClickListener(v->showAmountDialog(f,"\u52a0\u9910"));
        Button del=button("\u5220\u9664"); del.setOnClickListener(v->{customFoods.remove(f);NutritionData.saveCustomFoods(this,customFoods);showScreen();});
        row.addView(detail,new LinearLayout.LayoutParams(0,-2,1)); row.addView(add); row.addView(del); card.addView(row); content.addView(card);
    }

    @Override protected void showSettings(){
        content.addView(text("\u6bcf\u65e5\u8425\u517b\u8ba1\u5212",21,true));
        content.addView(muted("\u8bbe\u7f6e\u70ed\u91cf\u4e0a\u9650\u548c\u4e09\u5927\u8425\u517b\u7d20\u70ed\u91cf\u6bd4\u4f8b\uff0c\u76ee\u6807\u514b\u6570\u4f1a\u81ea\u52a8\u8ba1\u7b97\u3002"));
        LinearLayout form=box();
        form.addView(text("\u6bcf\u65e5\u70ed\u91cf\u4e0a\u9650",14,true));
        EditText kcal=goalInput("\u70ed\u91cf kcal",goal.kcal); form.addView(kcal);
        form.addView(text("\u4e09\u5927\u8425\u517b\u7d20\u70ed\u91cf\u6bd4\u4f8b",14,true));
        EditText pp=goalInput("\u86cb\u767d\u8d28 %",goal.proteinPercent),fp=goalInput("\u8102\u80aa %",goal.fatPercent),cp=goalInput("\u78b3\u6c34 %",goal.carbPercent);
        form.addView(pp);form.addView(fp);form.addView(cp);
        TextView status=text("",14,true),calculated=muted(""); form.addView(status);form.addView(calculated);
        form.addView(text("\u5176\u4ed6\u76ee\u6807",14,true));
        EditText fiber=goalInput("\u81b3\u98df\u7ea4\u7ef4 g",goal.fiber),sodium=goalInput("\u94a0 mg",goal.sodium);form.addView(fiber);form.addView(sodium);
        Runnable preview=()->updatePlanPreview(kcal,pp,fp,cp,status,calculated);
        for(EditText e:new EditText[]{kcal,pp,fp,cp})e.addTextChangedListener(new SimpleWatcher(preview)); preview.run();
        Button save=button("\u4fdf\u5b58\u8ba1\u5212"); save.setOnClickListener(v->{
            double energy=parse(kcal),p=parse(pp),f=parse(fp),c=parse(cp);
            if(energy<=0){Toast.makeText(this,"\u8bf7\u586b\u5199\u5927\u4e8e0\u7684\u70ed\u91cf\u4e0a\u9650",Toast.LENGTH_SHORT).show();return;}
            if(Math.abs(p+f+c-100d)>=0.05d){Toast.makeText(this,"\u86cb\u767d\u8d28\u3001\u8102\u80aa\u3001\u78b3\u6c34\u6bd4\u4f8b\u5408\u8ba1\u5fc5\u987b\u4e3a100%",Toast.LENGTH_LONG).show();return;}
            goal.kcal=energy;goal.proteinPercent=p;goal.fatPercent=f;goal.carbPercent=c;
            goal.fiber=valueOr(fiber,25);goal.sodium=valueOr(sodium,2000);goal.recalculateMacros();NutritionData.saveGoal(this,goal);
            Toast.makeText(this,"\u5df2\u4fdd\u5b58\uff1a\u86cb\u767d"+one(goal.protein)+"g \u00b7 \u8102\u80aa"+one(goal.fat)+"g \u00b7 \u78b3\u6c34"+one(goal.carb)+"g",Toast.LENGTH_LONG).show();showScreen();
        });
        form.addView(save);content.addView(form);
        LinearLayout note=box(); note.addView(text("\u8ba1\u7b97\u65b9\u5f0f",16,true));
        note.addView(muted("\u86cb\u767d\u8d28\u548c\u78b3\u6c34\u63094 kcal/g\u8ba1\u7b97\uff0c\u8102\u80aa\u63099 kcal/g\u8ba1\u7b97\u3002\u6bd4\u4f8b\u6307\u5404\u8425\u517b\u7d20\u63d0\u4f9b\u7684\u70ed\u91cf\u5360\u6bcf\u65e5\u603b\u70ed\u91cf\u7684\u6bd4\u4f8b\u3002"));
        Button clear=button("\u6e05\u7a7a\u5168\u90e8\u6570\u636e");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("\u6e05\u7a7a\u6570\u636e")
                .setMessage("\u786e\u5b9a\u6e05\u7a7a\u5168\u90e8\u996e\u98df\u8bb0\u5f55\u3001\u81ea\u5b9a\u4e49\u98df\u54c1\u548c\u76ee\u6807\u8bbe\u7f6e\u5417\uff1f").setNegativeButton("\u53d6\u6d88",null)
                .setPositiveButton("\u6e05\u7a7a",(d,w)->{NutritionData.clear(this);reload();screen="\u4eca\u65e5";showScreen();}).show());
        note.addView(clear);content.addView(note);
    }

    private void updatePlanPreview(EditText kcal,EditText pp,EditText fp,EditText cp,TextView status,TextView out){
        double energy=parse(kcal),p=parse(pp),f=parse(fp),c=parse(cp),total=p+f+c;
        boolean valid=energy>0&&Math.abs(total-100d)<0.05d;
        status.setText("\u6bd4\u4f8b\u5408\u8ba1 "+one(total)+"%"+(valid?" \u2713":"\uff08\u9700\u8981\u4e3a100%\uff09"));
        status.setTextColor(valid?GREEN:Color.rgb(190,65,55));
        out.setText("\u81ea\u52a8\u8ba1\u7b97\u76ee\u6807\uff1a\n\u86cb\u767d\u8d28 "+one(energy*p/400d)+"g\uff08"+one(energy*p/100d)+" kcal\uff09\n\u8102\u80aa "+
                one(energy*f/900d)+"g\uff08"+one(energy*f/100d)+" kcal\uff09\n\u78b3\u6c34 "+one(energy*c/400d)+"g\uff08"+one(energy*c/100d)+" kcal\uff09");
    }

    private EditText goalInput(String hint,double value){EditText e=input(hint,true);e.setText(one(value));return e;}
}
