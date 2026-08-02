package com.offline.ledger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpenseTrendView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<String> dates = new ArrayList<>();
    private Map<String,long[]> series = new LinkedHashMap<>();
    private String focus = "全体趋势";

    private static final String TOTAL = "总支出";
    private static final int[] COLORS = new int[]{
            Color.rgb(38,78,112), Color.rgb(220,94,75), Color.rgb(52,152,121),
            Color.rgb(227,151,53), Color.rgb(123,104,174), Color.rgb(55,135,190),
            Color.rgb(185,89,129), Color.rgb(104,121,130)
    };

    public ExpenseTrendView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        textPaint.setColor(Color.rgb(70,78,88));
        textPaint.setTextSize(sp(11));
    }

    public void setData(List<String> dates, Map<String,long[]> series) {
        this.dates = dates == null ? new ArrayList<>() : dates;
        this.series = series == null ? new LinkedHashMap<>() : series;
        invalidate();
    }

    public void setFocus(String focus) {
        this.focus = focus == null ? "全体趋势" : focus;
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
        int width=MeasureSpec.getSize(widthMeasureSpec);
        int desired=dp(390);
        int height=resolveSize(desired,heightMeasureSpec);
        setMeasuredDimension(width,height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w=getWidth(), h=getHeight();
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(new RectF(0,0,w,h),dp(10),dp(10),paint);

        if(dates.isEmpty() || series.isEmpty()) {
            drawCentered(canvas,"该区间暂无可绘制的数据",w/2,h/2);
            return;
        }

        long max=0;
        for(long[] values:series.values()) for(long v:values) if(v>max)max=v;
        if(max<=0) {
            drawLegend(canvas);
            drawCentered(canvas,"该区间暂无日常支出",w/2,h/2+dp(25));
            return;
        }

        drawLegend(canvas);
        float left=dp(62), right=w-dp(16), top=dp(112), bottom=h-dp(42);
        float chartW=Math.max(1,right-left), chartH=Math.max(1,bottom-top);

        paint.setStrokeWidth(dpF(1));
        paint.setColor(Color.rgb(226,229,233));
        textPaint.setTextSize(sp(10));
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for(int i=0;i<=4;i++) {
            float y=top+chartH*i/4f;
            canvas.drawLine(left,y,right,y,paint);
            long value=Math.round(max*(4-i)/4d);
            canvas.drawText(formatAxis(value),left-dp(7),y+dp(4),textPaint);
        }

        paint.setColor(Color.rgb(160,166,174));
        canvas.drawLine(left,top,left,bottom,paint);
        canvas.drawLine(left,bottom,right,bottom,paint);

        int n=dates.size();
        int ticks=Math.min(5,n);
        textPaint.setTextAlign(Paint.Align.CENTER);
        for(int i=0;i<ticks;i++) {
            int index=ticks==1?0:Math.round(i*(n-1)/(float)(ticks-1));
            float x=n==1?(left+right)/2:left+chartW*index/(n-1f);
            canvas.drawLine(x,bottom,x,bottom+dp(4),paint);
            canvas.drawText(shortDate(dates.get(index)),x,bottom+dp(18),textPaint);
        }

        String focusKey="全体趋势".equals(focus)?TOTAL:focus;
        int colorIndex=0;
        for(Map.Entry<String,long[]> entry:series.entrySet()) {
            if(entry.getKey().equals(focusKey)){colorIndex++;continue;}
            drawSeries(canvas,entry.getValue(),colorFor(entry.getKey(),colorIndex),45,dpF(1.2f),left,right,top,bottom,max,false);
            colorIndex++;
        }
        long[] selected=series.get(focusKey);
        if(selected!=null) {
            int selectedIndex=indexOfKey(focusKey);
            drawSeries(canvas,selected,colorFor(focusKey,selectedIndex),255,dpF(3),left,right,top,bottom,max,n<=62);
        }
    }

    private void drawLegend(Canvas canvas) {
        String focusKey="全体趋势".equals(focus)?TOTAL:focus;
        int i=0;
        for(String key:series.keySet()) {
            int column=i%2, row=i/2;
            float x=dp(18)+column*(getWidth()/2f);
            float y=dp(22)+row*dp(21);
            int color=colorFor(key,i);
            paint.setColor(withAlpha(color,key.equals(focusKey)?255:80));
            canvas.drawCircle(x,y-dp(3),dpF(key.equals(focusKey)?4.5f:3.5f),paint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(sp(key.equals(focusKey)?12:11));
            textPaint.setColor(key.equals(focusKey)?Color.rgb(32,38,46):Color.rgb(110,116,124));
            canvas.drawText(key,x+dp(10),y,textPaint);
            i++;
        }
    }

    private void drawSeries(Canvas canvas,long[] values,int color,int alpha,float stroke,
                            float left,float right,float top,float bottom,long max,boolean points) {
        if(values==null || values.length==0)return;
        float chartW=right-left, chartH=bottom-top;
        Path path=new Path();
        for(int i=0;i<values.length;i++) {
            float x=values.length==1?(left+right)/2:left+chartW*i/(values.length-1f);
            float y=bottom-chartH*(values[i]/(float)max);
            if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
        }
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(stroke);paint.setColor(withAlpha(color,alpha));canvas.drawPath(path,paint);
        paint.setStyle(Paint.Style.FILL);
        if(points) {
            paint.setColor(withAlpha(color,alpha));
            for(int i=0;i<values.length;i++) {
                float x=values.length==1?(left+right)/2:left+chartW*i/(values.length-1f);
                float y=bottom-chartH*(values[i]/(float)max);
                canvas.drawCircle(x,y,dpF(2.4f),paint);
            }
        }
    }

    private int indexOfKey(String key) {
        int i=0;for(String k:series.keySet()){if(k.equals(key))return i;i++;}return 0;
    }

    private int colorFor(String key,int index) {
        if(TOTAL.equals(key))return COLORS[0];
        return COLORS[(Math.max(1,index))%COLORS.length];
    }

    private int withAlpha(int color,int alpha){return Color.argb(alpha,Color.red(color),Color.green(color),Color.blue(color));}

    private String shortDate(String iso) {
        if(iso==null)return "";
        if(dates.size()>370 && iso.length()>=7)return iso.substring(0,7).replace("-","/");
        if(iso.length()>=10)return iso.substring(5,10).replace("-","/");
        return iso;
    }

    private String formatAxis(long value) {
        if(value>=100000000)return String.format(Locale.CHINA,"%.1f亿",value/100000000d);
        if(value>=10000)return String.format(Locale.CHINA,"%.1f万",value/10000d);
        return String.valueOf(value);
    }

    private void drawCentered(Canvas canvas,String text,float x,float y){
        textPaint.setTextAlign(Paint.Align.CENTER);textPaint.setTextSize(sp(14));textPaint.setColor(Color.rgb(100,108,116));
        canvas.drawText(text,x,y,textPaint);
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private float dpF(float v){return v*getResources().getDisplayMetrics().density;}
    private float sp(int v){return v*getResources().getDisplayMetrics().scaledDensity;}
}
