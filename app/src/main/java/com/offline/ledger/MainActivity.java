package com.offline.ledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private LinearLayout root, content;
    private TextView monthTitle;
    private final NumberFormat yen=NumberFormat.getCurrencyInstance(Locale.JAPAN);
    private final String[] cats={"饮食","交通","购物","日用品","医疗","娱乐","其他"};
    private final String[] wallets={"个人","公用"};
    private String selectedMonth=LedgerStore.monthNow();
    private String currentScreen="概览";

    private static final String[] INCOME_KEYS={"salary","transportAllowance","bonus","extraIncome"};
    private static final String[] FIXED_KEYS={"electricityGas","water","rent","gym","creditCard","insurance","sharedExpense","otherFixed"};

    @Override public void onCreate(Bundle b) { super.onCreate(b); showShell(); }
    @Override protected void onResume(){ super.onResume(); if(content!=null) showCurrentScreen(); }

    private TextView text(String s,int sp,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(Color.rgb(30,35,45));
        v.setPadding(16,12,16,12); if(bold)v.setTypeface(null,1); return v;
    }
    private TextView muted(String s){ TextView v=text(s,13,false); v.setTextColor(Color.rgb(100,108,116)); return v; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }

    private void showShell(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(247,248,250));
        TextView title=text("离线存款账本",24,true); title.setPadding(24,24,24,8); root.addView(title);

        LinearLayout monthBar=new LinearLayout(this); monthBar.setOrientation(LinearLayout.HORIZONTAL); monthBar.setGravity(Gravity.CENTER_VERTICAL); monthBar.setPadding(12,0,12,10);
        Button prev=button("‹"); Button next=button("›");
        monthTitle=text(monthDisplay(selectedMonth),18,true); monthTitle.setGravity(Gravity.CENTER);
        monthBar.addView(prev,new LinearLayout.LayoutParams(64,-2));
        monthBar.addView(monthTitle,new LinearLayout.LayoutParams(0,-2,1));
        monthBar.addView(next,new LinearLayout.LayoutParams(64,-2));
        prev.setOnClickListener(v->{selectedMonth=LedgerStore.shiftMonth(selectedMonth,-1);refreshMonth();});
        next.setOnClickListener(v->{selectedMonth=LedgerStore.shiftMonth(selectedMonth,1);refreshMonth();});
        root.addView(monthBar);

        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] n={"概览","流水","待确认","规划","设置"};
        for(String x:n){ Button b=button(x); nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));
            b.setOnClickListener(v->{currentScreen=x;showCurrentScreen();}); }
        root.addView(nav);
        ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(20,20,20,100); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); showDashboard();
    }

    private void refreshMonth(){ monthTitle.setText(monthDisplay(selectedMonth)); showCurrentScreen(); }
    private String monthDisplay(String month){ return month.substring(0,4)+"年"+Integer.parseInt(month.substring(5,7))+"月"; }
    private void showCurrentScreen(){
        if("流水".equals(currentScreen))showTransactions(false);
        else if("待确认".equals(currentScreen))showTransactions(true);
        else if("规划".equals(currentScreen))showPlan();
        else if("设置".equals(currentScreen))showSettings();
        else showDashboard();
    }
    private void clear(){content.removeAllViews();}
    private void card(String title,String value){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(20,16,20,16); c.setBackgroundColor(Color.WHITE);
        c.addView(text(title,14,false)); c.addView(text(value,23,true));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,12); content.addView(c,lp);
    }
    private void sectionTitle(String title,String note){
        TextView t=text(title,18,true); t.setPadding(8,24,8,4); content.addView(t);
        if(note!=null&&!note.isEmpty()){ TextView n=muted(note); n.setPadding(8,0,8,10); content.addView(n); }
    }

    private long variableExpense(String month){
        long total=0;
        for(LedgerStore.Tx t:LedgerStore.load(this)){
            if(t.pending || !t.date.startsWith(month) || "固定支出".equals(t.category)) continue;
            total+=t.amount;
        }
        return total;
    }
    private long incomeTotal(JSONObject p){ long x=0;for(String k:INCOME_KEYS)x+=p.optLong(k);return x; }
    private long fixedTotal(JSONObject p){ long x=0;for(String k:FIXED_KEYS)x+=p.optLong(k);return x; }
    private long projectedEnd(String month, JSONObject p){ return p.optLong("previousBalance")+incomeTotal(p)-fixedTotal(p)-variableExpense(month); }

    private void showDashboard(){
        currentScreen="概览"; clear(); List<LedgerStore.Tx> all=LedgerStore.load(this);
        long total=0, personal=0, shared=0; int pending=0; long[] by=new long[cats.length];
        for(LedgerStore.Tx t:all){
            if(t.pending){pending++;continue;}
            if(!t.date.startsWith(selectedMonth) || "固定支出".equals(t.category))continue;
            total+=t.amount; if("公用".equals(t.wallet))shared+=t.amount; else personal+=t.amount;
            for(int i=0;i<cats.length;i++)if(cats[i].equals(t.category))by[i]+=t.amount;
        }
        JSONObject plan=effectivePlan(selectedMonth);
        long income=incomeTotal(plan), fixed=fixedTotal(plan), opening=plan.optLong("previousBalance"), end=projectedEnd(selectedMonth,plan);
        content.addView(text(monthDisplay(selectedMonth)+"概览",20,true));
        card("本月变动支出",yen.format(total)); card("本月固定支出",yen.format(fixed)); card("本月收入",yen.format(income)); card("预计月末结余",yen.format(end));
        content.addView(muted("月末结余＝上月结余＋本月收入－固定支出－日常流水支出"));
        Button add=button("＋ 手动记一笔日常支出"); add.setOnClickListener(v->editTx(null)); content.addView(add);
        if(pending>0){ Button p=button("有 "+pending+" 笔 Google 钱包记录待确认"); p.setOnClickListener(v->{currentScreen="待确认";showTransactions(true);}); content.addView(p); }
        sectionTitle("钱包支出","这里只统计日常流水，不包含规划页中的固定支出。");
        content.addView(text("个人钱包　"+yen.format(personal),16,false)); content.addView(text("公用钱包　"+yen.format(shared),16,false));
        sectionTitle("分类汇总",""); boolean any=false;
        for(int i=0;i<cats.length;i++) if(by[i]>0){any=true;content.addView(text(cats[i]+"　"+yen.format(by[i]),16,false));}
        if(!any)content.addView(muted("本月暂无日常支出记录。"));
        sectionTitle("本月资金结果","");
        card("上月结余",yen.format(opening)); card("预计本月存款",yen.format(income-fixed-total));
    }

    private void showTransactions(boolean pendingOnly){
        currentScreen=pendingOnly?"待确认":"流水"; clear(); content.addView(text(pendingOnly?"待确认":"本月流水",20,true));
        if(!pendingOnly)content.addView(muted(monthDisplay(selectedMonth)+"的日常支出；固定支出请在“规划”页面修改。"));
        List<LedgerStore.Tx> list=LedgerStore.load(this); int shown=0;
        for(LedgerStore.Tx t:list){
            if(pendingOnly){ if(!t.pending)continue; }
            else { if(t.pending || !t.date.startsWith(selectedMonth) || "固定支出".equals(t.category))continue; }
            shown++;
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(18,12,18,12); row.setBackgroundColor(Color.WHITE);
            row.addView(text(t.merchant.isEmpty()?t.category:t.merchant,17,true)); row.addView(text(t.date+" · "+t.category+" · "+t.wallet+" · "+t.source,13,false)); row.addView(text(yen.format(t.amount),20,true));
            LinearLayout actions=new LinearLayout(this);
            Button edit=button(pendingOnly?"确认/编辑":"编辑"); edit.setOnClickListener(v->editTx(t)); actions.addView(edit);
            Button del=button("删除"); del.setOnClickListener(v->{ List<LedgerStore.Tx> a=LedgerStore.load(this); a.removeIf(x->x.id.equals(t.id)); LedgerStore.save(this,a); showTransactions(pendingOnly); }); actions.addView(del); row.addView(actions);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);content.addView(row,lp);
        }
        if(shown==0) content.addView(text(pendingOnly?"暂无待确认记录。支付后若 Google 钱包发出金额通知，会自动出现在这里。":"本月暂无日常流水。",16,false));
    }

    private Spinner spinner(String[] values,String selected){ Spinner s=new Spinner(this); ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);for(int i=0;i<values.length;i++)if(values[i].equals(selected))s.setSelection(i);return s; }
    private EditText input(String hint,String value,boolean number){ EditText e=new EditText(this);e.setHint(hint);e.setText(value);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e; }
    private LinearLayout labeledInput(String label, EditText input){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(4,4,4,8);
        TextView l=text(label,14,true); l.setPadding(6,4,6,2); box.addView(l); box.addView(input); return box;
    }

    private String defaultDateForSelectedMonth(){
        return selectedMonth.equals(LedgerStore.monthNow()) ? LedgerStore.today() : selectedMonth+"-01";
    }

    private void editTx(LedgerStore.Tx old){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,10,30,0);
        EditText amount=input("例如 1280",old==null?"":String.valueOf(old.amount),true); EditText merchant=input("商家/备注",old==null?"":old.merchant,false); EditText date=input("YYYY-MM-DD",old==null?defaultDateForSelectedMonth():old.date,false);
        Spinner cat=spinner(cats,old==null?"购物":old.category); Spinner wallet=spinner(wallets,old==null?"个人":old.wallet);
        box.addView(labeledInput("金额（日元）",amount));box.addView(labeledInput("商家或备注",merchant));box.addView(labeledInput("日期",date));
        box.addView(text("支出分类",14,true));box.addView(cat);box.addView(text("支出钱包",14,true));box.addView(wallet);
        new AlertDialog.Builder(this).setTitle(old==null?"新增日常支出":"确认/编辑流水").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            try{
                long a=Long.parseLong(amount.getText().toString().trim()); if(a<=0)return;
                List<LedgerStore.Tx> list=LedgerStore.load(this); LedgerStore.Tx t=old==null?new LedgerStore.Tx():old;
                if(old==null){t.id="manual:"+System.currentTimeMillis();t.source="手动";t.raw="";}
                t.amount=a;t.merchant=merchant.getText().toString().trim();t.date=date.getText().toString().trim();t.category=(String)cat.getSelectedItem();t.wallet=(String)wallet.getSelectedItem();t.pending=false;
                if(old!=null) list.removeIf(x->x.id.equals(old.id)); list.add(t); LedgerStore.save(this,list); showDashboard();
            }catch(Exception ignored){}
        }).show();
    }

    private long parseAmount(EditText e){ String s=e.getText().toString().trim().replace(",",""); return s.isEmpty()?0:Long.parseLong(s); }
    private EditText moneyField(JSONObject p,String key){ return input("0",String.valueOf(p.optLong(key,0)),true); }
    private void addMoneyField(String label, String key, JSONObject p, Map<String,EditText> fields){ EditText e=moneyField(p,key); fields.put(key,e); content.addView(labeledInput(label,e)); }

    private JSONObject effectivePlan(String month){
        JSONObject p=LedgerStore.plan(this,month);
        if(LedgerStore.hasPlan(this,month))return p;
        String previous=LedgerStore.shiftMonth(month,-1); JSONObject prev=LedgerStore.plan(this,previous);
        JSONObject draft=new JSONObject();
        try{
            if(LedgerStore.hasPlan(this,previous)){
                draft.put("previousBalance",projectedEnd(previous,prev));
                draft.put("salary",prev.optLong("salary"));
                draft.put("transportAllowance",prev.optLong("transportAllowance"));
                for(String k:FIXED_KEYS)draft.put(k,prev.optLong(k));
            }
        }catch(Exception ignored){}
        return draft;
    }

    private void showPlan(){
        currentScreen="规划"; clear(); JSONObject p=effectivePlan(selectedMonth); Map<String,EditText> fields=new LinkedHashMap<>();
        content.addView(text(monthDisplay(selectedMonth)+"资金规划",20,true));
        content.addView(muted("每个输入框都有独立标题。固定支出按月份保存，新月份会沿用上一月的固定金额，你只需修改发生变化的项目。"));

        sectionTitle("1　上月结余","本月开始时实际可用的总余额。新月份会自动带入上一月的预计月末结余，也可以手动修正。");
        addMoneyField("上月结余", "previousBalance", p, fields);

        sectionTitle("2　本月收入","收入单独填写并自动合计，不需要作为流水记录。");
        addMoneyField("工资", "salary", p, fields);
        addMoneyField("交通费补贴", "transportAllowance", p, fields);
        addMoneyField("奖金", "bonus", p, fields);
        addMoneyField("额外收入", "extraIncome", p, fields);

        sectionTitle("3　本月固定支出","这些金额直接计入本月固定支出，不需要在“流水”里再次记录。");
        addMoneyField("电费・煤气费", "electricityGas", p, fields);
        addMoneyField("水费", "water", p, fields);
        addMoneyField("房租", "rent", p, fields);
        addMoneyField("健身房", "gym", p, fields);
        addMoneyField("信用卡还款", "creditCard", p, fields);
        addMoneyField("保险", "insurance", p, fields);
        addMoneyField("共同花销", "sharedExpense", p, fields);
        addMoneyField("其他固定支出", "otherFixed", p, fields);

        sectionTitle("4　计算预览","日常支出取自本月已确认流水，固定支出不会重复计算。");
        TextView preview=text("",16,true); content.addView(preview);
        Runnable updatePreview=()->{
            try{
                JSONObject draft=new JSONObject(); for(Map.Entry<String,EditText> e:fields.entrySet())draft.put(e.getKey(),parseAmount(e.getValue()));
                long income=incomeTotal(draft),fixed=fixedTotal(draft),variable=variableExpense(selectedMonth),end=projectedEnd(selectedMonth,draft);
                preview.setText("收入合计　"+yen.format(income)+"\n固定支出合计　"+yen.format(fixed)+"\n日常支出　"+yen.format(variable)+"\n预计月末结余　"+yen.format(end)+"\n预计本月存款　"+yen.format(income-fixed-variable));
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

    private void showSettings(){
        currentScreen="设置"; clear(); content.addView(text("设置与隐私",20,true));
        content.addView(text("本应用没有网络权限。账本、通知原文与金额只保存在本机。它只能在你手动授予“通知使用权”后读取新通知，无法补抓授权前的旧通知。",16,false));
        Button access=button("打开通知使用权设置");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));content.addView(access);
        Button test=button("测试金额解析");test.setOnClickListener(v->{LedgerStore.Tx t=new LedgerStore.Tx();t.id="test:"+System.currentTimeMillis();t.date=LedgerStore.today();t.amount=1280;t.merchant="测试商家";t.category="购物";t.wallet="个人";t.source="测试";t.raw="Google Wallet ¥1,280";t.pending=true;LedgerStore.addIfNew(this,t);showTransactions(true);});content.addView(test);
        Button clear=button("清空全部本地数据");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("该操作会删除全部流水和各月份规划，无法撤销。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{LedgerStore.clearAll(this);showDashboard();}).show());content.addView(clear);
    }
}
