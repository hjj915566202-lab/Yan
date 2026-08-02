package com.offline.ledger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpensePieView extends View {
    public static final class Slice {
        public final String label;
        public final long value;
        public Slice(String label,long value){this.label=label;this.value=value;}
    }

    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NumberFormat yen=NumberFormat.getCurrencyInstance(Locale.JAPAN);
    private List<Slice> slices=new ArrayList<>();
    private static final int[] COLORS={
            Color.rgb(38,78,112),Color.rgb(220,94,75),Color.rgb(52,152,121),Color.rgb(227,151,53),
            Color.rgb(123,104,174),Color.rgb(55,135,190),Color.rgb(185,89,129),Color.rgb(104,121,130),
            Color.rgb(134,170,80),Color.rgb(196,122,65),Color.rgb(79,153,160),Color.rgb(157,116,157)
    };

    public ExpensePieView(Context context){
        super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        textPaint.setColor(Color.rgb(45,52,61));
    }

    public void setData(List<Slice> slices){this.slices=slices==null?new ArrayList<>():slices;requestLayout();invalidate();}

    @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        int width=MeasureSpec.getSize(widthMeasureSpec);
        int desired=dp(285+Math.max(1,slices.size())*30);
        setMeasuredDimension(width,resolveSize(desired,heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float w=getWidth();
        paint.setColor(Color.WHITE);canvas.drawRoundRect(new RectF(0,0,w,getHeight()),dp(10),dp(10),paint);
        long total=0;for(Slice s:slices)if(s.value>0)total+=s.value;
        if(total<=0){
            textPaint.setTextAlign(Paint.Align.CENTER);textPaint.setTextSize(sp(14));textPaint.setColor(Color.rgb(100,108,116));
            canvas.drawText("该区间暂无支出",w/2,dp(145),textPaint);return;
        }

        float cx=w/2,cy=dp(132),radius=Math.min(w*0.28f,dp(104));
        RectF pie=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);
        float start=-90;
        for(int i=0;i<slices.size();i++){
            Slice s=slices.get(i);if(s.value<=0)continue;
            float sweep=360f*s.value/total;
            paint.setColor(COLORS[i%COLORS.length]);
            canvas.drawArc(pie,start,sweep,true,paint);start+=sweep;
        }
        paint.setColor(Color.WHITE);canvas.drawCircle(cx,cy,radius*0.52f,paint);
        textPaint.setTextAlign(Paint.Align.CENTER);textPaint.setColor(Color.rgb(45,52,61));textPaint.setTextSize(sp(12));
        canvas.drawText("区间总支出",cx,cy-dp(5),textPaint);
        textPaint.setTextSize(sp(17));textPaint.setFakeBoldText(true);canvas.drawText(yen.format(total),cx,cy+dp(18),textPaint);textPaint.setFakeBoldText(false);

        float y=dp(270);
        for(int i=0;i<slices.size();i++){
            Slice s=slices.get(i);if(s.value<=0)continue;
            paint.setColor(COLORS[i%COLORS.length]);canvas.drawRoundRect(new RectF(dp(18),y-dp(10),dp(30),y+dp(2)),dp(2),dp(2),paint);
            textPaint.setTextAlign(Paint.Align.LEFT);textPaint.setTextSize(sp(13));textPaint.setTextColor(Color.rgb(45,52,61));
            String label=ellipsize(s.label,18);canvas.drawText(label,dp(38),y,textPaint);
            String value=yen.format(s.value)+"  "+String.format(Locale.CHINA,"%.1f%%",100d*s.value/total);
            textPaint.setTextAlign(Paint.Align.RIGHT);textPaint.setTextSize(sp(12));textPaint.setColor(Color.rgb(85,92,101));
            canvas.drawText(value,w-dp(16),y,textPaint);
            y+=dp(30);
        }
    }

    private String ellipsize(String s,int max){if(s==null)return "";return s.length()>max?s.substring(0,max-1)+"…":s;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private float sp(int v){return v*getResources().getDisplayMetrics().scaledDensity;}
}
