package com.offline.ledger;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WalletNotificationListener extends NotificationListenerService {
    private static final Pattern[] AMOUNT_PATTERNS = new Pattern[] {
        Pattern.compile("[¥￥]\\s*([0-9][0-9,]*)(?:\\.[0-9]{1,2})?"),
        Pattern.compile("(?:JPY)\\s*([0-9][0-9,]*)(?:\\.[0-9]{1,2})?",Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9][0-9,]*)(?:\\.[0-9]{1,2})?\\s*(?:円|JPY)",Pattern.CASE_INSENSITIVE)
    };

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        LedgerStore.setListenerState(this,"已连接："+nowText());
    }

    @Override public void onListenerDisconnected() {
        LedgerStore.setListenerState(this,"连接已断开："+nowText());
        requestRebind(new ComponentName(this,WalletNotificationListener.class));
        super.onListenerDisconnected();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn==null) return;
        Notification n=sbn.getNotification();
        if(n==null) return;

        Bundle e=n.extras==null?new Bundle():n.extras;
        String title=chars(e.getCharSequence(Notification.EXTRA_TITLE));
        String detail=extractText(n,e);
        String all=(title+"\n"+detail).trim();
        String pkg=sbn.getPackageName()==null?"":sbn.getPackageName();
        long amount=parseAmount(all);
        boolean walletPackage=isWalletPackage(pkg);
        boolean paymentText=looksLikePayment(all);
        boolean candidate=walletPackage || (amount>0 && paymentText);
        boolean imported=false;
        String reason;

        if(amount<=0) reason=walletPackage?"收到钱包来源通知，但没有识别到日元金额":"未识别到日元金额";
        else if(!candidate) reason="识别到金额，但文本不像支付通知";
        else {
            LedgerStore.Tx tx=new LedgerStore.Tx();
            tx.id="wallet:"+sbn.getKey()+":"+amount;
            tx.date=LedgerStore.today(); tx.amount=amount;
            tx.merchant=merchant(title,detail); tx.category=guessCategory(all);
            tx.wallet="个人"; tx.source=walletPackage?"Google 钱包通知":"支付通知："+pkg;
            tx.raw="来源："+pkg+"\n"+all; tx.pending=true;
            imported=LedgerStore.addIfNew(this,tx);
            reason=imported?"已加入待确认":"重复通知，未重复加入";
        }

        if(LedgerStore.diagnosticsEnabled(this) || candidate || walletPackage) {
            LedgerStore.recordNotification(this,pkg,title,detail,amount,candidate,imported,reason,sbn.getPostTime());
        }
    }

    public static long parseAmount(String source) {
        String s=normalize(source);
        for(Pattern p:AMOUNT_PATTERNS) {
            Matcher m=p.matcher(s);
            if(m.find()) {
                try { return Math.round(Double.parseDouble(m.group(1).replace(",",""))); }
                catch(Exception ignored) {}
            }
        }
        return 0;
    }

    private static String extractText(Notification n,Bundle e) {
        StringBuilder b=new StringBuilder();
        append(b,e.getCharSequence(Notification.EXTRA_TEXT));
        append(b,e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        append(b,e.getCharSequence(Notification.EXTRA_SUB_TEXT));
        append(b,e.getCharSequence(Notification.EXTRA_INFO_TEXT));
        append(b,e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        CharSequence[] lines=e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if(lines!=null) for(CharSequence line:lines) append(b,line);
        Parcelable[] messages=e.getParcelableArray(Notification.EXTRA_MESSAGES);
        if(messages!=null) for(Parcelable p:messages) {
            if(p instanceof Bundle) {
                Bundle m=(Bundle)p;
                append(b,m.getCharSequence("sender"));
                append(b,m.getCharSequence("text"));
            }
        }
        append(b,n.tickerText);
        return dedupeLines(b.toString());
    }

    private static void append(StringBuilder b,CharSequence value) {
        String s=chars(value).trim();
        if(!s.isEmpty()) b.append(s).append('\n');
    }

    private static String dedupeLines(String text) {
        StringBuilder out=new StringBuilder();
        String previous="";
        for(String line:text.split("\\r?\\n")) {
            String x=line.trim();
            if(x.isEmpty() || x.equals(previous)) continue;
            out.append(x).append('\n'); previous=x;
            if(out.length()>3500) break;
        }
        return out.toString().trim();
    }

    private static String normalize(String s) {
        String x=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC);
        return x.replace('，',',').replace('￥','¥');
    }

    private static boolean isWalletPackage(String pkg) {
        return "com.google.android.apps.walletnfcrel".equals(pkg)
                || "com.google.android.gms".equals(pkg)
                || pkg.contains("google") && (pkg.contains("wallet") || pkg.contains("pay"));
    }

    private static boolean looksLikePayment(String s) {
        String x=normalize(s).toLowerCase(Locale.ROOT);
        return x.matches("(?s).*(google wallet|google pay|g pay|ウォレット|支払|お支払|決済|利用|購入|payment|paid|purchase|transaction|消费|支付|付款).*")
                || x.contains("¥") || x.contains("円") || x.contains("jpy");
    }

    private static String merchant(String title,String detail) {
        String x=title==null?"":title.trim();
        String low=x.toLowerCase(Locale.ROOT);
        if(x.isEmpty() || low.contains("google wallet") || low.contains("google pay") || x.contains("ウォレット")) {
            for(String line:detail.split("\\r?\\n")) {
                String t=line.trim();
                if(!t.isEmpty() && parseAmount(t)==0 && t.length()>1) { x=t; break; }
            }
        }
        if(x.isEmpty()) x="Google 钱包付款";
        return x.length()>60?x.substring(0,60):x;
    }

    private static String guessCategory(String s) {
        String x=normalize(s).toLowerCase(Locale.ROOT);
        if(x.matches("(?s).*(train|metro|rail|suica|pasmo|交通|電車|バス|タクシー|jr).*") ) return "交通";
        if(x.matches("(?s).*(restaurant|cafe|coffee|food|餐|食|コンビニ|スーパー|ローソン|ファミリーマート|セブン).*") ) return "饮食";
        if(x.matches("(?s).*(drug|pharmacy|日用品|薬局|ドラッグ).*") ) return "日用品";
        if(x.matches("(?s).*(hospital|clinic|medical|病院|クリニック|医療).*") ) return "医疗";
        return "购物";
    }

    private static String chars(CharSequence s) { return s==null?"":s.toString(); }
    private static String nowText() { return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.JAPAN).format(new Date()); }
}
