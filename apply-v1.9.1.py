from pathlib import Path

store = Path("app/src/main/java/com/offline/ledger/LedgerStore.java")
s = store.read_text()

save_block = """    public static synchronized void savePlan(Context c, String month, JSONObject plan) {
        migrateLegacyPlanIfNeeded(c);
        try {
            normalizePlan(plan);
            JSONObject all = new JSONObject(c.getSharedPreferences(PREFS,0).getString(PLANS,"{}"));
            all.put(month, plan);
            c.getSharedPreferences(PREFS,0).edit().putString(PLANS,all.toString()).apply();
        } catch(Exception ignored) {}
    }
"""
reset_block = save_block + """
    public static synchronized void resetPlan(Context c, String month) {
        migrateLegacyPlanIfNeeded(c);
        try {
            JSONObject all = new JSONObject(c.getSharedPreferences(PREFS,0).getString(PLANS,"{}"));
            all.remove(month);
            c.getSharedPreferences(PREFS,0).edit().putString(PLANS,all.toString()).apply();
        } catch(Exception ignored) {}
    }
"""
if save_block not in s:
    raise SystemExit("savePlan block not found")
store.write_text(s.replace(save_block, reset_block))

main = Path("app/src/main/java/com/offline/ledger/MainActivity.java")
s = main.read_text()
save_ui = """        Button save=primaryButton("保存 "+monthDisplay(selectedMonth)+"规划");save.setOnClickListener(v->{
            try{JSONObject o=new JSONObject();for(Map.Entry<String,EditText> e:fields.entrySet())o.put(e.getKey(),parseAmount(e.getValue()));LedgerStore.savePlan(this,selectedMonth,o);Toast.makeText(this,"规划已保存",Toast.LENGTH_SHORT).show();showDashboard();}
            catch(Exception e){Toast.makeText(this,"请输入整数金额",Toast.LENGTH_SHORT).show();}
        });content.addView(save);
"""
reset_ui = save_ui + """
        Button reset=button("重置当前月份为默认规划");
        LinearLayout.LayoutParams resetLp=new LinearLayout.LayoutParams(-1,-2);resetLp.setMargins(0,dp(10),0,0);content.addView(reset,resetLp);
        reset.setOnClickListener(v->new AlertDialog.Builder(this)
                .setTitle("重置 "+monthDisplay(selectedMonth)+"规划？")
                .setMessage("将删除当前月份手动保存的规划，并重新使用系统默认值。流水、固定支出规则和其他月份规划不会被删除。")
                .setNegativeButton("取消",null)
                .setPositiveButton("重置",(d,w)->{
                    LedgerStore.resetPlan(this,selectedMonth);
                    Toast.makeText(this,"已恢复默认规划",Toast.LENGTH_SHORT).show();
                    showPlan();
                }).show());
"""
if save_ui not in s:
    raise SystemExit("plan save button block not found")
main.write_text(s.replace(save_ui, reset_ui))

gradle = Path("app/build.gradle")
s = gradle.read_text()
old = """        versionCode 10
        versionName '1.9.0'"""
new = """        versionCode 11
        versionName '1.9.1'"""
if old not in s:
    raise SystemExit("version block not found")
gradle.write_text(s.replace(old, new))
