from pathlib import Path

main = Path('app/src/main/java/com/offline/ledger/MainActivity.java')
s = main.read_text(encoding='utf-8')
s = s.replace('import java.util.Date;\nimport java.util.LinkedHashMap;', 'import java.util.ArrayList;\nimport java.util.Date;\nimport java.util.LinkedHashMap;')
s = s.replace('''    private static final String[] INCOME_KEYS={"salary","transportAllowance","bonus","extraIncome"};
    private static final String[] PERSONAL_FIXED_KEYS={"electricityGas","water","rent","gym","creditCard","insurance","otherFixed"};
    private static final String[] SHARED_FIXED_KEYS={"sharedExpense"};
    private static final String[] CARRY_KEYS={"salary","transportAllowance","electricityGas","water","rent","gym","creditCard","insurance","otherFixed","sharedExpense"};''',
'''    private static final String[] INCOME_KEYS={"salary","transportAllowance","bonus","extraIncome"};
    private static final String[] CARRY_KEYS={"salary","transportAllowance"};''')
s = s.replace('''        else if("设置".equals(currentScreen))showSettings();
        else if("诊断".equals(currentScreen))showDiagnostics();''',
'''        else if("设置".equals(currentScreen))showSettings();
        else if("固定支出".equals(currentScreen))showFixedExpenses();
        else if("诊断".equals(currentScreen))showDiagnostics();''')
s = s.replace('''    private long incomeTotal(JSONObject p){ long x=0;for(String k:INCOME_KEYS)x+=p.optLong(k);return x; }
    private long sumKeys(JSONObject p,String[] keys){ long x=0;for(String k:keys)x+=p.optLong(k);return x; }
    private long personalFixedTotal(JSONObject p){ return sumKeys(p,PERSONAL_FIXED_KEYS); }
    private long sharedFixedTotal(JSONObject p){ return sumKeys(p,SHARED_FIXED_KEYS); }
    private long personalEnd(String month,JSONObject p){
        return p.optLong("personalPreviousBalance")+incomeTotal(p)-personalFixedTotal(p)-variableExpense(month,"个人")-p.optLong("sharedTransfer");
    }
    private long sharedEnd(String month,JSONObject p){
        return p.optLong("sharedPreviousBalance")+p.optLong("sharedTransfer")-sharedFixedTotal(p)-variableExpense(month,"公用");
    }''',
'''    private long incomeTotal(JSONObject p){ long x=0;for(String k:INCOME_KEYS)x+=p.optLong(k);return x; }
    private long fixedTotal(String month,String wallet){
        long total=0;
        for(LedgerStore.FixedExpense item:LedgerStore.fixedExpenses(this,month)){
            if(wallet.equals(item.wallet)) total+=item.amount;
        }
        return total;
    }
    private long personalFixedTotal(String month){ return fixedTotal(month,"个人"); }
    private long sharedFixedTotal(String month){ return fixedTotal(month,"公用"); }
    private long personalEnd(String month,JSONObject p){
        return p.optLong("personalPreviousBalance")+incomeTotal(p)-personalFixedTotal(month)-variableExpense(month,"个人")-p.optLong("sharedTransfer");
    }
    private long sharedEnd(String month,JSONObject p){
        return p.optLong("sharedPreviousBalance")+p.optLong("sharedTransfer")-sharedFixedTotal(month)-variableExpense(month,"公用");
    }''')
s = s.replace('long income=incomeTotal(plan), personalFixed=personalFixedTotal(plan), sharedFixed=sharedFixedTotal(plan);',
              'long income=incomeTotal(plan), personalFixed=personalFixedTotal(selectedMonth), sharedFixed=sharedFixedTotal(selectedMonth);')

start = s.index('    private void showPlan(){')
end = s.index('    private boolean notificationAccessEnabled(){')
replacement = r'''    private void showPlan(){
        currentScreen="规划"; clear(); JSONObject p=effectivePlan(selectedMonth); Map<String,EditText> fields=new LinkedHashMap<>();
        content.addView(text(monthDisplay(selectedMonth)+"双钱包资金规划",20,true));
        content.addView(muted("个人钱包和公用钱包分别结算。新月份会自动带入两个钱包上一月的预计余额。"));

        sectionTitle("1　上月结余","两个钱包分别填写；公用钱包支出会从公用余额中扣除。");
        addMoneyField("个人钱包上月结余","personalPreviousBalance",p,fields);
        addMoneyField("公用钱包上月结余","sharedPreviousBalance",p,fields);

        sectionTitle("2　本月收入","工资等收入默认进入个人钱包。");
        addMoneyField("工资","salary",p,fields);
        addMoneyField("交通费补贴","transportAllowance",p,fields);
        addMoneyField("奖金","bonus",p,fields);
        addMoneyField("额外收入","extraIncome",p,fields);

        sectionTitle("3　转入公用钱包","从个人钱包移到公用钱包，不会计入收入或支出。");
        addMoneyField("本月转入公用钱包","sharedTransfer",p,fields);

        List<LedgerStore.FixedExpense> fixedItems=LedgerStore.fixedExpenses(this,selectedMonth);
        sectionTitle("4　固定支出规则","可以自由新增、改名、修改金额、选择个人或公用钱包。保存后从本月起自动用于以后每个月，直到在更晚月份再次修改。");
        String effective=LedgerStore.fixedExpenseEffectiveMonth(this,selectedMonth);
        if(!effective.isEmpty()) content.addView(muted("当前采用 "+monthDisplay(effective)+" 开始生效的固定支出设定。"));
        long pf=0,sf=0;
        for(LedgerStore.FixedExpense item:fixedItems){
            if("公用".equals(item.wallet))sf+=item.amount;else pf+=item.amount;
            content.addView(text(item.name+"　"+yen.format(item.amount)+"　·　"+item.wallet+"钱包",15,false));
        }
        if(fixedItems.isEmpty())content.addView(muted("当前没有固定支出项目。"));
        content.addView(text("个人钱包固定支出合计　"+yen.format(pf)+"\n公用钱包固定支出合计　"+yen.format(sf),15,true));

        Button editFixed=button("编辑固定支出规则"); editFixed.setOnClickListener(v->{
            try{
                JSONObject o=new JSONObject();
                for(Map.Entry<String,EditText> e:fields.entrySet())o.put(e.getKey(),parseAmount(e.getValue()));
                LedgerStore.savePlan(this,selectedMonth,o);
                currentScreen="固定支出"; showFixedExpenses();
            }catch(Exception e){Toast.makeText(this,"请先检查规划中的金额",Toast.LENGTH_SHORT).show();}
        }); content.addView(editFixed);

        sectionTitle("5　计算预览","日常流水按钱包分别扣款；固定支出使用当前月份生效的规则。");
        TextView preview=text("",16,true); content.addView(preview);
        Runnable updatePreview=()->{
            try{
                JSONObject draft=new JSONObject(); for(Map.Entry<String,EditText> e:fields.entrySet())draft.put(e.getKey(),parseAmount(e.getValue()));
                long pi=incomeTotal(draft),personalFixed=personalFixedTotal(selectedMonth),sharedFixed=sharedFixedTotal(selectedMonth);
                long pv=variableExpense(selectedMonth,"个人"),sv=variableExpense(selectedMonth,"公用");
                long pe=personalEnd(selectedMonth,draft),se=sharedEnd(selectedMonth,draft);
                preview.setText("个人钱包固定支出　"+yen.format(personalFixed)+"\n公用钱包固定支出　"+yen.format(sharedFixed)+
                        "\n个人钱包日常支出　"+yen.format(pv)+"\n公用钱包日常支出　"+yen.format(sv)+
                        "\n个人钱包预计余额　"+yen.format(pe)+"\n公用钱包预计余额　"+yen.format(se)+
                        "\n两个钱包合计　"+yen.format(pe+se)+"\n本月收入　"+yen.format(pi)+
                        "\n本月实际存款变化　"+yen.format(pi-personalFixed-sharedFixed-pv-sv));
            }catch(Exception ignored){preview.setText("请输入整数金额");}
        };
        Button calculate=button("重新计算预览"); calculate.setOnClickListener(v->updatePreview.run()); content.addView(calculate); updatePreview.run();

        Button save=button("保存本月规划");save.setOnClickListener(v->{
            try{
                JSONObject o=new JSONObject(); for(Map.Entry<String,EditText> e:fields.entrySet())o.put(e.getKey(),parseAmount(e.getValue()));
                LedgerStore.savePlan(this,selectedMonth,o); Toast.makeText(this,"已保存 "+monthDisplay(selectedMonth)+"规划",Toast.LENGTH_SHORT).show(); showDashboard();
            }catch(Exception e){Toast.makeText(this,"请输入整数金额",Toast.LENGTH_SHORT).show();}
        });content.addView(save);
    }

    private void showFixedExpenses(){
        currentScreen="固定支出"; clear();
        content.addView(text(monthDisplay(selectedMonth)+"固定支出规则",20,true));
        content.addView(muted("这里保存的是一整套固定支出设定。从 "+monthDisplay(selectedMonth)+" 起，每个月自动使用这套设定；以后在其他月份修改时，再从那个新月份起改用新设定。"));
        Button back=button("返回资金规划"); back.setOnClickListener(v->{currentScreen="规划";showPlan();}); content.addView(back);
        Button add=button("＋ 新增固定支出"); add.setOnClickListener(v->editFixedExpense(null)); content.addView(add);

        List<LedgerStore.FixedExpense> items=LedgerStore.fixedExpenses(this,selectedMonth);
        if(items.isEmpty()){ content.addView(text("暂无固定支出项目。",16,false)); return; }

        long personal=0,shared=0;
        for(LedgerStore.FixedExpense item:items){
            if("公用".equals(item.wallet))shared+=item.amount;else personal+=item.amount;
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(18,12,18,12); row.setBackgroundColor(Color.WHITE);
            row.addView(text(item.name,17,true));
            row.addView(text(yen.format(item.amount)+" · "+item.wallet+"钱包",15,false));
            LinearLayout actions=new LinearLayout(this);
            Button edit=button("编辑"); edit.setOnClickListener(v->editFixedExpense(item)); actions.addView(edit);
            Button del=button("删除"); del.setOnClickListener(v->new AlertDialog.Builder(this)
                    .setTitle("删除固定支出？")
                    .setMessage(item.name+" 将从 "+monthDisplay(selectedMonth)+" 起不再计入固定支出。历史月份不会改变。")
                    .setNegativeButton("取消",null)
                    .setPositiveButton("删除",(d,w)->{
                        List<LedgerStore.FixedExpense> changed=new ArrayList<>(LedgerStore.fixedExpenses(this,selectedMonth));
                        changed.removeIf(x->x.id.equals(item.id));
                        LedgerStore.saveFixedExpensesFromMonth(this,selectedMonth,changed);
                        showFixedExpenses();
                    }).show());
            actions.addView(del); row.addView(actions);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);content.addView(row,lp);
        }
        sectionTitle("本月起每月固定扣除","");
        content.addView(text("个人钱包　"+yen.format(personal)+"\n公用钱包　"+yen.format(shared)+"\n合计　"+yen.format(personal+shared),16,true));
    }

    private void editFixedExpense(LedgerStore.FixedExpense old){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(30,10,30,0);
        EditText name=input("例如：房租",old==null?"":old.name,false);
        EditText amount=input("例如：80000",old==null?"":String.valueOf(old.amount),true);
        Spinner wallet=spinner(wallets,old==null?"个人":old.wallet);
        box.addView(labeledInput("固定支出名称",name));
        box.addView(labeledInput("每月金额（日元）",amount));
        box.addView(text("从哪个钱包扣除",14,true)); box.addView(wallet);

        new AlertDialog.Builder(this).setTitle(old==null?"新增固定支出":"编辑固定支出").setView(box)
                .setNegativeButton("取消",null)
                .setPositiveButton("保存",(d,w)->{
                    try{
                        String fixedName=name.getText().toString().trim();
                        long fixedAmount=parseAmount(amount);
                        if(fixedName.isEmpty()||fixedAmount<=0){
                            Toast.makeText(this,"请输入名称和大于0的金额",Toast.LENGTH_SHORT).show(); return;
                        }
                        List<LedgerStore.FixedExpense> changed=new ArrayList<>(LedgerStore.fixedExpenses(this,selectedMonth));
                        String id=old==null?"fixed:"+System.currentTimeMillis():old.id;
                        if(old!=null)changed.removeIf(x->x.id.equals(old.id));
                        changed.add(new LedgerStore.FixedExpense(id,fixedName,fixedAmount,(String)wallet.getSelectedItem()));
                        LedgerStore.saveFixedExpensesFromMonth(this,selectedMonth,changed);
                        showFixedExpenses();
                    }catch(Exception ignored){Toast.makeText(this,"请输入整数金额",Toast.LENGTH_SHORT).show();}
                }).show();
    }

'''
s = s[:start] + replacement + s[end:]
main.write_text(s, encoding='utf-8')

build = Path('app/build.gradle')
b = build.read_text(encoding='utf-8').replace('versionCode 3','versionCode 4').replace("versionName '1.2.0'","versionName '1.3.0'")
build.write_text(b, encoding='utf-8')

Path('tools/apply_v1_3_patch.py').unlink(missing_ok=True)
Path('.github/workflows/apply-v1-3.yml').unlink(missing_ok=True)
