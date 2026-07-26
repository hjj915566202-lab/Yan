package com.offline.ledger;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class LedgerStore {
    private static final String PREFS = "offline_ledger";
    private static final String TX = "transactions";
    private static final String PLANS = "plans_by_month";
    private static final String LEGACY_PLAN = "plan";
    private LedgerStore() {}

    public static final class Tx {
        public String id, date, merchant, category, wallet, source, raw;
        public long amount;
        public boolean pending;

        JSONObject json() throws Exception {
            JSONObject o = new JSONObject();
            o.put("id", id); o.put("date", date); o.put("merchant", merchant);
            o.put("category", category); o.put("wallet", wallet); o.put("source", source);
            o.put("raw", raw); o.put("amount", amount); o.put("pending", pending);
            return o;
        }
        static Tx from(JSONObject o) {
            Tx t = new Tx();
            t.id=o.optString("id"); t.date=o.optString("date"); t.merchant=o.optString("merchant");
            t.category=o.optString("category","购物"); t.wallet=o.optString("wallet","个人");
            t.source=o.optString("source","手动"); t.raw=o.optString("raw");
            t.amount=o.optLong("amount"); t.pending=o.optBoolean("pending");
            return t;
        }
    }

    public static List<Tx> load(Context c) {
        List<Tx> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(c.getSharedPreferences(PREFS,0).getString(TX,"[]"));
            for(int i=0;i<a.length();i++) out.add(Tx.from(a.getJSONObject(i)));
        } catch(Exception ignored) {}
        Collections.sort(out, (a,b) -> b.date.compareTo(a.date));
        return out;
    }

    public static synchronized void save(Context c, List<Tx> list) {
        JSONArray a = new JSONArray();
        try { for(Tx t:list) a.put(t.json()); } catch(Exception ignored) {}
        c.getSharedPreferences(PREFS,0).edit().putString(TX,a.toString()).apply();
    }

    public static synchronized boolean addIfNew(Context c, Tx tx) {
        List<Tx> list = load(c);
        for(Tx t:list) if(t.id.equals(tx.id)) return false;
        list.add(tx); save(c,list); return true;
    }

    public static boolean hasPlan(Context c, String month) {
        migrateLegacyPlanIfNeeded(c);
        try {
            JSONObject all = new JSONObject(c.getSharedPreferences(PREFS,0).getString(PLANS,"{}"));
            return all.has(month);
        } catch(Exception ignored) { return false; }
    }

    public static JSONObject plan(Context c, String month) {
        migrateLegacyPlanIfNeeded(c);
        try {
            JSONObject all = new JSONObject(c.getSharedPreferences(PREFS,0).getString(PLANS,"{}"));
            JSONObject p = all.optJSONObject(month);
            return p == null ? new JSONObject() : new JSONObject(p.toString());
        } catch(Exception ignored) { return new JSONObject(); }
    }

    public static synchronized void savePlan(Context c, String month, JSONObject plan) {
        migrateLegacyPlanIfNeeded(c);
        try {
            JSONObject all = new JSONObject(c.getSharedPreferences(PREFS,0).getString(PLANS,"{}"));
            all.put(month, plan);
            c.getSharedPreferences(PREFS,0).edit().putString(PLANS,all.toString()).apply();
        } catch(Exception ignored) {}
    }

    public static synchronized void clearAll(Context c) {
        c.getSharedPreferences(PREFS,0).edit().remove(TX).remove(PLANS).remove(LEGACY_PLAN).apply();
    }

    private static void migrateLegacyPlanIfNeeded(Context c) {
        String existing = c.getSharedPreferences(PREFS,0).getString(PLANS,"");
        if(existing != null && !existing.isEmpty()) return;
        String legacy = c.getSharedPreferences(PREFS,0).getString(LEGACY_PLAN,"");
        if(legacy == null || legacy.isEmpty() || "{}".equals(legacy)) return;
        try {
            JSONObject old = new JSONObject(legacy);
            JSONObject p = new JSONObject();
            p.put("previousBalance", old.optLong("start",0));
            p.put("salary", old.optLong("income",0));
            p.put("otherFixed", old.optLong("fixed",0));
            JSONObject all = new JSONObject();
            all.put(monthNow(),p);
            c.getSharedPreferences(PREFS,0).edit().putString(PLANS,all.toString()).remove(LEGACY_PLAN).apply();
        } catch(Exception ignored) {}
    }

    public static String today() {
        Calendar x=Calendar.getInstance();
        return String.format(Locale.US,"%04d-%02d-%02d",x.get(Calendar.YEAR),x.get(Calendar.MONTH)+1,x.get(Calendar.DAY_OF_MONTH));
    }
    public static String monthNow() { return today().substring(0,7); }

    public static String shiftMonth(String month, int delta) {
        try {
            int year=Integer.parseInt(month.substring(0,4));
            int m=Integer.parseInt(month.substring(5,7));
            Calendar c=Calendar.getInstance();
            c.clear(); c.set(year,m-1,1); c.add(Calendar.MONTH,delta);
            return String.format(Locale.US,"%04d-%02d",c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1);
        } catch(Exception ignored) { return monthNow(); }
    }
}
