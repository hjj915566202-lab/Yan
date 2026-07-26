package com.offline.ledger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.*;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root, content;
    private final NumberFormat yen=NumberFormat.getCurrencyInstance(Locale.JAPAN);
    private final String[] cats={"饮食","交通","购物","日用品","固定支出","其他"};
    private final String[] wallets={"个人","公用"};

    @Override public void onCreate(Bundle b) { super.onCreate(b); showShell(); }
    @Override protected void onResume(){ super.onResume(); if(content!=null) showDashboard(); }

    private TextView text(String s,int sp,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(Color.rgb(30,35,45)); v.setPadding(16,12,16,12); if(bold)v.setTypeface(null,1); return v; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private void showShell(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(247,248,250));
        TextView title=text("离线存款账本",24,true); title.setPadding(24,24,24,16); root.addView(title);
        LinearLayout nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        String[] n={"概览","流水","待确认","规划","设置"};
        for(String x:n){ Button b=button(x); nav.addView(b,new LinearLayout.LayoutParams(0,-2,1));
            b.setOnClickListener(v->{ if(x.equals("概览"))showDashboard(); else if(x.equals("流水"))showTransactions(false); else if(x.equals("待确认"))showTransactions(true); else if(x.equals("规划"))showPlan(); else showSettings(); }); }
        root.addView(nav);
        ScrollView sc=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(20,20,20,100); sc.addView(content); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); showDashboard();
    }
    private void clear(){content.removeAllViews();}
    private void card(String title,String value){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(20,16,20,16); c.setBackgroundColor(Color.WHITE); c.addView(text(title,14,false)); c.addView(text(value,23,true)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,12); content.addView(c,lp); }

    private void showDashboard(){
        clear(); String month=LedgerStore.monthNow(); List<LedgerStore.Tx> all=LedgerStore.load(this);
        long total=0, personal=0, shared=0; int pending=0; long[] by=new long[cats.length];
        for(LedgerStore.Tx t:all){ if(t.pending){pending++;continue;} if(!t.date.startsWith(month))continue; total+=t.amount; if("公用".equals(t.wallet))shared+=t.amount; else personal+=t.amount; for(int i=0;i<cats.length;i++)if(cats[i].equals(t.category))by[i]+=t.amount; }
        content.addView(text(month+" 月度概览",20,true)); card("本月支出",yen.format(total)); card("个人钱包",yen.format(personal)); card("公用钱包",yen.format(shared));
        Button add=button("＋ 手动记一笔"); add.setOnClickListener(v->editTx(null)); content.addView(add);
        if(pending>0){ Button p=button("有 "+pending+" 笔 Google 钱包记录待确认"); p.setOnClickListener(v->showTransactions(true)); content.addView(p); }
        content.addView(text("分类汇总",18,true)); for(int i=0;i<cats.length;i++) if(by[i]>0) content.addView(text(cats[i]+"　"+yen.format(by[i]),16,false));
        JSONObject plan=LedgerStore.plan(this); long income=plan.optLong("income"), fixed=plan.optLong("fixed"), start=plan.optLong("start");
        if(income>0||fixed>0||start>0){ long end=start+income-fixed-total; card("预计月末余额",yen.format(end)); card("预计本月存款",yen.format(income-fixed-total)); }
    }

    private void showTransactions(boolean pendingOnly){
        clear(); content.addView(text(pendingOnly?"待确认":"全部流水",20,true));
        List<LedgerStore.Tx> list=LedgerStore.load(this); int shown=0;
        for(LedgerStore.Tx t:list){ if(t.pending!=pendingOnly && pendingOnly)continue; if(!pendingOnly && t.pending)continue; shown++;
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(18,12,18,12); row.setBackgroundColor(Color.WHITE);
            row.addView(text(t.merchant.isEmpty()?t.category:t.merchant,17,true)); row.addView(text(t.date+" · "+t.category+" · "+t.wallet+" · "+t.source,13,false)); row.addView(text(yen.format(t.amount),20,true));
            LinearLayout actions=new LinearLayout(this);
            Button edit=button(pendingOnly?"确认/编辑":"编辑"); edit.setOnClickListener(v->editTx(t)); actions.addView(edit);
            Button del=button("删除"); del.setOnClickListener(v->{ List<LedgerStore.Tx> a=LedgerStore.load(this); a.removeIf(x->x.id.equals(t.id)); LedgerStore.save(this,a); showTransactions(pendingOnly); }); actions.addView(del); row.addView(actions);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,12);content.addView(row,lp);
        }
        if(shown==0) content.addView(text(pendingOnly?"暂无待确认记录。支付后若 Google 钱包发出金额通知，会自动出现在这里。":"暂无流水。",16,false));
    }

    private Spinner spinner(String[] values,String selected){ Spinner s=new Spinner(this); ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);s.setAdapter(a);for(int i=0;i<values.length;i++)if(values[i].equals(selected))s.setSelection(i);return s; }
    private EditText input(String hint,String value,boolean number){ EditText e=new EditText(this);e.setHint(hint);e.setText(value);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e; }
    private void editTx(LedgerStore.Tx old){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(30,10,30,0);
        EditText amount=input("金额（日元）",old==null?"":String.valueOf(old.amount),true); EditText merchant=input("商家/备注",old==null?"":old.merchant,false); EditText date=input("日期 YYYY-MM-DD",old==null?LedgerStore.today():old.date,false);
        Spinner cat=spinner(cats,old==null?"购物":old.category); Spinner wallet=spinner(wallets,old==null?"个人":old.wallet);
        box.addView(amount);box.addView(merchant);box.addView(date);box.addView(cat);box.addView(wallet);
        new AlertDialog.Builder(this).setTitle(old==null?"新增流水":"确认/编辑流水").setView(box).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{
            try{ long a=Long.parseLong(amount.getText().toString().trim()); if(a<=0)return; List<LedgerStore.Tx> list=LedgerStore.load(this); LedgerStore.Tx t=old==null?new LedgerStore.Tx():old;
                if(old==null){t.id="manual:"+System.currentTimeMillis();t.source="手动";t.raw="";} t.amount=a;t.merchant=merchant.getText().toString().trim();t.date=date.getText().toString().trim();t.category=(String)cat.getSelectedItem();t.wallet=(String)wallet.getSelectedItem();t.pending=false;
                if(old==null)list.add(t); LedgerStore.save(this,list); showDashboard();
            }catch(Exception ignored){}
        }).show();
    }

    private void showPlan(){
        clear(); content.addView(text("存款规划",20,true)); JSONObject p=LedgerStore.plan(this);
        EditText start=input("月初余额",String.valueOf(p.optLong("start",0)),true); EditText income=input("本月工资/收入",String.valueOf(p.optLong("income",0)),true); EditText fixed=input("固定开销合计",String.valueOf(p.optLong("fixed",0)),true);
        content.addView(text("对应 Excel 的月初、工资/额外收入、房租/保险/信用卡/共同花销/生活费等固定支出。",14,false));content.addView(start);content.addView(income);content.addView(fixed);
        Button save=button("保存规划");save.setOnClickListener(v->{try{JSONObject o=new JSONObject();o.put("start",Long.parseLong(start.getText().toString()));o.put("income",Long.parseLong(income.getText().toString()));o.put("fixed",Long.parseLong(fixed.getText().toString()));LedgerStore.savePlan(this,o);Toast.makeText(this,"已保存",Toast.LENGTH_SHORT).show();showDashboard();}catch(Exception e){Toast.makeText(this,"请输入整数金额",Toast.LENGTH_SHORT).show();}});content.addView(save);
    }

    private void showSettings(){
        clear(); content.addView(text("设置与隐私",20,true));
        content.addView(text("本应用没有网络权限。账本、通知原文与金额只保存在本机。它只能在你手动授予“通知使用权”后读取新通知，无法补抓授权前的旧通知。",16,false));
        Button access=button("打开通知使用权设置");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));content.addView(access);
        Button test=button("测试金额解析");test.setOnClickListener(v->{LedgerStore.Tx t=new LedgerStore.Tx();t.id="test:"+System.currentTimeMillis();t.date=LedgerStore.today();t.amount=1280;t.merchant="测试商家";t.category="购物";t.wallet="个人";t.source="测试";t.raw="Google Wallet ¥1,280";t.pending=true;LedgerStore.addIfNew(this,t);showTransactions(true);});content.addView(test);
        Button clear=button("清空全部本地数据");clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("确认清空？").setMessage("该操作无法撤销。").setNegativeButton("取消",null).setPositiveButton("清空",(d,w)->{LedgerStore.save(this,new ArrayList<>());LedgerStore.savePlan(this,new JSONObject());showDashboard();}).show());content.addView(clear);
    }
}
