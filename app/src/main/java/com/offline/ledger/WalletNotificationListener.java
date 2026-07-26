package com.offline.ledger;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WalletNotificationListener extends NotificationListenerService {
    private static final String WALLET = "com.google.android.apps.walletnfcrel";
    private static final Pattern[] AMOUNT_PATTERNS = new Pattern[] {
        Pattern.compile("[¥￥]\\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\\.[0-9]{1,2})?"),
        Pattern.compile("(?:JPY|円)\\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\\.[0-9]{1,2})?", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\\.[0-9]{1,2})?\\s*円")
    };

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !WALLET.equals(sbn.getPackageName())) return;
        Notification n=sbn.getNotification(); if(n==null) return;
        Bundle e=n.extras;
        String title=String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE,""));
        String text=String.valueOf(e.getCharSequence(Notification.EXTRA_TEXT,""));
        String big=String.valueOf(e.getCharSequence(Notification.EXTRA_BIG_TEXT,""));
        String all=(title+"\n"+text+"\n"+big).trim();
        long amount=parseAmount(all); if(amount<=0) return;

        LedgerStore.Tx tx=new LedgerStore.Tx();
        tx.id="wallet:"+sbn.getKey()+":"+amount;
        tx.date=LedgerStore.today(); tx.amount=amount;
        tx.merchant=merchant(title,text); tx.category=guessCategory(all);
        tx.wallet="个人"; tx.source="Google 钱包通知"; tx.raw=all; tx.pending=true;
        LedgerStore.addIfNew(this,tx);
    }

    static long parseAmount(String s) {
        for(Pattern p:AMOUNT_PATTERNS) {
            Matcher m=p.matcher(s); if(m.find()) {
                try { return Math.round(Double.parseDouble(m.group(1).replace(",",""))); }
                catch(Exception ignored) {}
            }
        }
        return 0;
    }

    private static String merchant(String title,String text) {
        String x=title.trim();
        if(x.isEmpty() || x.contains("Google Wallet") || x.contains("Google 钱包")) x=text.trim();
        return x.length()>40?x.substring(0,40):x;
    }

    private static String guessCategory(String s) {
        String x=s.toLowerCase(Locale.ROOT);
        if(x.matches(".*(train|metro|rail|suica|pasmo|交通|電車|バス|タクシー).*")) return "交通";
        if(x.matches(".*(restaurant|cafe|coffee|food|餐|食|コンビニ|スーパー).*")) return "饮食";
        if(x.matches(".*(drug|pharmacy|日用品|薬局|ドラッグ).*")) return "日用品";
        return "购物";
    }
}
