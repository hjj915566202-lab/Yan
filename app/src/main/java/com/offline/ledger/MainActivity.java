package com.offline.ledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private static final String[] PERSONAL_FIXED_KEYS={"electricityGas","water","rent","gym","creditCard","insurance","otherFixed"};
    private static final String[] SHARED_FIXED_KEYS={"sharedExpense"};
    private static final String[] CARRY_KEYS={"salary","transportAllowance","electricityGas","water","rent","gym","creditCard","insurance","otherFixed","sharedExpense"};

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
        for(String x:n){
            Button b=button(x); nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));
            b.setOnClickListener(v->{currentScreen=x;showCurrentScreen();});
        }
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
        else if("诊断".equals(currentScreen))showDiagnostics();
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

    private long variableExpense(String month,String wallet){
        long total=0;
        for(LedgerStore.Tx t:LedgerStore.load(this)){
            if(t.pending || !t.date.startsWith(month) || "固定支出".equals(t.category)) continue;
            if(wallet==null || wallet.equals(t.wallet)) total+=t.amount;
        }
        return total;
    }
    private long incomeTotal(JSONObject p){ long x=0;for(String k:INCOME_KEYS)x+=p.optLong(k);return x; }
    private long sumKeys(JSONObject p,String[] keys){ long x=0;for(String k:keys)x+=p.optLong(k);return x; }
    private long personalFixedTotal(JSONObject p){ return sumKeys(p,PERSONAL_FIXED_KEYS); }
    private long sharedFixedTotal(JSONObject p){ return sumKeys(p,SHARED_FIXED_KEYS); }
    private long personalEnd(String month,JSONObject p){
        return p.optLong("personalPreviousBalance")+incomeTotal(p)-personalFixedTotal(p)-variableExpense(month,"个人")-p.optLong("sharedTransfer");
    }
    private long sharedEnd(String month,JSONObject p){
        return p.optLong("sharedPreviousBalance")+p.optLong("sharedTransfer")-sharedFixedTotal(p)-variableExpense(month,"公用");
    }

    private void showDashboard(){
        currentScreen="概览"; clear(); List<LedgerStore.Tx> all=LedgerStore.load(this);
        long personalExpense=0, sharedExpense=0; int pending=0; long[] by=new long[cats.length];
        for(LedgerStore.Tx t:all){
            if(t.pending){pending++;continue;}
            if(!t.date.startsWith(selectedMonth) || "固定支出".equals(t.category))continue;
            if("公用".equals(t.wallet))sharedExpense+=t.amount; else personalExpense+=t.amount;
            for(int i=0;i<cats.length;i++)if(cats[i].equals(t.category))by[i]+=t.amount;
        }
        JSONObject plan=effectivePlan(selectedMonth);
        long income=incomeTotal(plan), personalFixed=personalFixedTotal(plan), sharedFixed=sharedFixedTotal(plan);
        long personalEnd=personalEnd(selectedMonth,plan), sharedEnd=sharedEnd(selectedMonth,plan);
        long totalExpense=personalExpense+sharedExpense+personalFixed+sharedFixed;

        content.addView(text(monthDisplay(selectedMonth)+"概览",20,true));
        card("个人钱包预计余额",yen.format(personalEnd));
        card("公用钱包预计余额",yen.format(sharedEnd));
        card("两个钱包合计",yen.format(personalEnd+sharedEnd));
        card("本月总支出",yen.format(totalExpense));
        content.addView(muted("公用钱包支出只扣公用余额；转入公用钱包只是内部转账，不计为收入或支出。"));

        Button add=button("＋ 手动记一笔日常支出"); add.setOnClickListener(v->editTx(null)); content.addView(add);
        if(pending>0){ Button p=button("有 "+pending+" 笔支付通知待确认"); p.setOnClickListener(v->{currentScreen="待确认";showTransactions(true);}); content.addView(p); }

        sectionTitle("本月钱包支出","固定支出在规划页单独计算。");
        content.addView(text("个人钱包日常支出　"+yen.format(personalExpense),16,false));
        content.addView(text("公用钱包日常支出　"+yen.format(sharedExpense),16,false));
        content.addView(text("个人钱包固定支出　"+yen.format(personalFixed),16,false));
        content.addView(text("公用钱包固定支出　"+yen.format(sharedFixed),16,false));

        sectionTitle("上月结余与转账","");
        content.addView(text("个人上月结余　"+yen.format(plan.optLong("personalPreviousBalance")),16,false));
        content.addView(text("公用上月结余　"+yen.format(plan.optLong("sharedPreviousBalance")),16,false));
        content.addView(text("本月转入公用钱包　"+yen.format(plan.optLong("sharedTransfer")),16,false));
        content.addView(text("本月收入　"+yen.format(income),16,false));

        sectionTitle("分类汇总",""); boolean any=false;
        for(int i=0;i<cats.length;i++) if(by[i]>0){any=true;content.addView(text(cats[i]+"　"+yen.format(by[i]),16,false));}
        if(!any)content.addView(muted("本月暂无日常支出记录。"));
    }

    private void showTransactions(boolean pendingOnly){
        currentScreen=pendingOnly?"待确认":"流水"; clear(); content.addView(text(pendingOnly?"待确认":"本月流水",20,true));
        if(!pendingOnly)content.addView(muted(monthDisplay(selectedMonth)+"的日常支出；每笔支出会扣除所选择的钱包余额。"));
        List<LedgerStore.Tx> list=LedgerStore.load(this); int shown=0;
        for(LedgerStore.Tx t:list){
            if(pendingOnly){ if(!t.pending)continue; }
            else { if(t.pending || !t.date.startsWith(selectedMonth) || "固定支出".equals(t.category))continue; }
            shown++;
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(18,12,18,12); row.setBackgroundColor(Color.WHITE);
            row.addView(text(t.merchant.isEmpty()?t.category:t.merchant,17,true));
            row.addView(text(t.date+" · "+t.category+" · "+t.wallet+"钱包 · "+t.source,13,false));
            row.addView(text(yen.format(t.amount),20,true));
            if(pendingOnly&&!t.raw.isEmpty()) row.addView(muted(t.raw));
            LinearLayout actions=new LinearLayout(this);
            Button edit=button(pendingOnly?"确认/编辑":"编辑"); edit.setOnClickListener(v->editTx(t)); actions.addView(edit);
            Button del=button("删除"); del.setOnClickListener(v->{ List<LedgerStore.Tx> a=LedgerStore.load(this); a.removeIf(x->x.id.equals(t.id)); LedgerStore.save(this,a); showTransactions(pendingOnly); }); actions.addView(del); row.addView(actions);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);content.addView(row,lp);
        }
        if(shown==0) content.addView(text(pendingOnly?"暂无待确认记录。修正版会同时读取提醒、静默和常驻通知。":"本月暂无日常流水。",16,false));
    }

    private Spinner spinner(String[] values,String selected){ Spinner s=new Spinner(this); ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);for(int i=0;i<values.length;i++)if(values[i].equals(selected))s.setSelection(i);return s; }
    private EditText input(String hint,String value,boolean number){ EditText e=new EditText(this);e.setHint(hint);e.setText(value);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e; }
    private LinearLayout labeledInput(String label,EditText input){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(4,4,4,8);
        TextView l=text(label,14,true); l.setPadding(6,4,6,2); box.addView(l); box.addView(input); return box;
    }

    private String defaultDateForSelectedMonth(){ return selectedMonth.equals(LedgerStore.monthNow())?LedgerStore.today():selectedMonth+"-01"; }

    private void editTx(LedgerStore.Tx old){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,10,30,0);
        EditText amount=input("例如 1280",old==null?"":String.valueOf(old.amount),true);
        EditText merchant=input("商家/备注",old==null?"":old.merchant,false);
        EditText date=input("YYYY-MM-DD",old==null?defaultDateForSelectedMonth():old.date,false);
        Spinner cat=spinner(cats,old==null?"购物":old.category);
        Spinner wallet=spinner(wallets,old==null?"个人":old.wallet);
        box.addView(labeledInput("金额（日元）",amount)); box.addView(labeledInput("商家或备注",merchant)); box.addView(labeledInput("日期",date));
        box.addView(text("支出分类",14,true)); box.addView(cat); box.addView(text("从哪个钱包支出",14,true)); box.addView(wallet);
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
    private void addMoneyField(String label,String key,JSONObject p,Map<String,EditText> fields){ EditText e=moneyField(p,key); fields.put(key,e); content.addView(labeledInput(label,e)); }

    private JSONObject effectivePlan(String month){
        JSONObject p=LedgerStore.plan(this,month);
        if(LedgerStore.hasPlan(this,month))return p;
        String previous=LedgerStore.shiftMonth(month,-1); JSONObject prev=LedgerStore.plan(this,previous);
        JSONObject draft=new JSONObject();
        try{
            if(LedgerStore.hasPlan(this,previous)){
                draft.put("personalPreviousBalance",personalEnd(previous,prev));
                draft.put("sharedPreviousBalance",sharedEnd(previous,prev));
                draft.put("sharedTransfer",0);
                for(String k:CARRY_KEYS)draft.put(k,prev.optLong(k));
            }
        }catch(Exception ignored){}
        return draft;
    }

    private void showPlan(){
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

        sectionTitle("4　个人钱包固定支出","直接扣除个人钱包，不需要录入流水。");
        addMoneyField("电费・煤气费","electricityGas",p,fields);
        addMoneyField("水费","water",p,fields);
        addMoneyField("房租","rent",p,fields);
        addMoneyField("健身房","gym",p,fields);
        addMoneyField("信用卡还款","creditCard",p,fields);
        addMoneyField("保险","insurance",p,fields);
        addMoneyField("其他个人固定支出","otherFixed",p,fields);

        sectionTitle("5　公用钱包固定支出","直接扣除公用钱包，不需要录入流水。");
        addMoneyField("公用钱包固定支出","sharedExpense",p,fields);

        sectionTitle("6　计算预览","日常流水会按照“个人/公用”标签分别扣款。");
        TextView preview=text("",16,true); content.addView(preview);
        Runnable updatePreview=()->{
            try{
                JSONObject draft=new JSONObject(); for(Map.Entry<String,EditText> e:fields.entrySet())draft.put(e.getKey(),parseAmount(e.getValue()));
                long pi=incomeTotal(draft),pf=personalFixedTotal(draft),sf=sharedFixedTotal(draft);
                long pv=variableExpense(selectedMonth,"个人"),sv=variableExpense(selectedMonth,"公用");
                long pe=personalEnd(selectedMonth,draft),se=sharedEnd(selectedMonth,draft);
                preview.setText("个人钱包日常支出　"+yen.format(pv)+"\n公用钱包日常支出　"+yen.format(sv)+
                        "\n个人钱包预计余额　"+yen.format(pe)+"\n公用钱包预计余额　"+yen.format(se)+
                        "\n两个钱包合计　"+yen.format(pe+se)+"\n本月收入　"+yen.format(pi)+
                        "\n本月实际存款变化　"+yen.format(pi-pf-sf-pv-sv));
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

    private boolean notificationAccessEnabled(){
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        return enabled!=null&&enabled.contains(getPackageName());
    }

    private void showSettings(){
        currentScreen="设置"; clear(); content.addView(text("设置与通知诊断",20,true));
        content.addView(text("应用没有网络权限。账本和诊断中的通知文本只保存在本机。诊断模式默认记录最近30条通知，便于确认系统有没有把 Google 钱包通知交给应用。",16,false));
        card("通知使用权",notificationAccessEnabled()?"已开启":"未开启");
        card("监听服务状态",LedgerStore.listenerState(this));

        Button access=button("打开通知使用权设置"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))); content.addView(access);
        Button rebind=button("重新连接通知监听服务"); rebind.setOnClickListener(v->{
            NotificationListenerService.requestRebind(new ComponentName(this,WalletNotificationListenerV2.class));
            Toast.makeText(this,"已请求重新连接；几秒后返回此页面查看状态",Toast.LENGTH_LONG).show();
        }); content.addView(rebind);

        boolean diagnostic=LedgerStore.diagnosticsEnabled(this);
        Button toggle=button(diagnostic?"关闭通知诊断记录":"开启通知诊断记录"); toggle.setOnClickListener(v->{
            LedgerStore.setDiagnosticsEnabled(this,!LedgerStore.diagnosticsEnabled(this)); showSettings();
        }); content.addView(toggle);
        Button logs=button("查看最近通知诊断"); logs.setOnClickListener(v->{currentScreen="诊断";showDiagnostics();}); content.addView(logs);

        Button test=button("测试金额解析（¥1,280）"); test.setOnClickListener(v->{
            long amount=WalletNotificationListener.parseAmount("Google Wallet お支払い ¥1,280 テスト商店");
            LedgerStore.Tx t=new LedgerStore.Tx(); t.id="test:"+System.currentTimeMillis(); t.date=LedgerStore.today(); t.amount=amount;
            t.merchant="测试商家";t.category="购物";t.wallet="个人";t.source="解析测试";t.raw="Google Wallet お支払い ¥1,280";t.pending=true;
            LedgerStore.addIfNew(this,t); showTransactions(true);
        }); content.addView(test);

        Button clear=button("清空全部本地数据");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("该操作会删除全部流水、各月份规划和通知诊断，无法撤销。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{LedgerStore.clearAll(this);showDashboard();}).show());content.addView(clear);
    }

    private void showDiagnostics(){
        currentScreen="诊断"; clear(); content.addView(text("最近通知诊断",20,true));
        content.addView(muted("先确认“监听服务状态”显示已连接。下一次支付后回到这里：如果能看到通知但金额为0，就是文本格式问题；完全没有记录则是系统没有把通知交给监听服务。"));
        Button back=button("返回设置"); back.setOnClickListener(v->{currentScreen="设置";showSettings();}); content.addView(back);
        Button clearLogs=button("清空诊断记录"); clearLogs.setOnClickListener(v->{LedgerStore.clearDiagnostics(this);showDiagnostics();}); content.addView(clearLogs);

        JSONArray a=LedgerStore.diagnostics(this);
        if(a.length()==0){ content.addView(text("尚无通知记录。请保持诊断开启，然后让任意应用产生一条普通通知；这样也能测试监听服务是否真的工作。",16,false)); return; }
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.JAPAN);
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i); if(o==null)continue;
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(18,12,18,12);row.setBackgroundColor(Color.WHITE);
            row.addView(text(o.optString("title","（无标题）"),16,true));
            row.addView(muted(f.format(new Date(o.optLong("time")))+"\n来源包名："+o.optString("package")));
            long amount=o.optLong("amount");
            row.addView(text("识别金额："+(amount>0?yen.format(amount):"未识别")+"\n结果："+o.optString("reason"),14,false));
            String detail=o.optString("detail"); if(!detail.isEmpty())row.addView(muted(detail));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);content.addView(row,lp);
        }
    }
}
