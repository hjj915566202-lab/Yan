from pathlib import Path

root=Path('.')
main=root/'app/src/main/java/com/offline/ledger/MainActivity.java'
pie=root/'app/src/main/java/com/offline/ledger/ExpensePieView.java'
build=root/'app/build.gradle'

b=build.read_text()
b=b.replace('versionCode 8','versionCode 9').replace("versionName '1.7.0'","versionName '1.8.0'")
build.write_text(b)

s=main.read_text()
s=s.replace('    private boolean analysisIncludeFixed=false;\n', '    private boolean analysisIncludeFixed=false;\n    private String transactionWalletFilter="全部";\n    private String transactionCategoryFilter="全部";\n')
s=s.replace('        long personalExpense=0, sharedExpense=0; int pending=0; long[] by=new long[cats.length];\n',
            '        long personalExpense=0, sharedExpense=0; int pending=0;\n        long[] personalBy=new long[cats.length],sharedBy=new long[cats.length];\n')
s=s.replace('            if("公用".equals(t.wallet))sharedExpense+=t.amount; else personalExpense+=t.amount;\n            for(int i=0;i<cats.length;i++)if(cats[i].equals(t.category))by[i]+=t.amount;\n',
            '            boolean shared="公用".equals(t.wallet);\n            if(shared)sharedExpense+=t.amount; else personalExpense+=t.amount;\n            for(int i=0;i<cats.length;i++)if(cats[i].equals(t.category)){if(shared)sharedBy[i]+=t.amount;else personalBy[i]+=t.amount;}\n')
s=s.replace('        sectionTitle("分类汇总",""); boolean any=false;\n        for(int i=0;i<cats.length;i++) if(by[i]>0){any=true;content.addView(text(cats[i]+"　"+yen.format(by[i]),16,false));}\n        if(!any)content.addView(muted("本月暂无日常支出记录。"));\n',
'''        sectionTitle("分类汇总","点击分类可查看当前月份、对应钱包与类别的流水。");
        addDashboardCategorySummary("个人",personalBy);
        addDashboardCategorySummary("公用",sharedBy);
''')
needle='    private void showTransactions(boolean pendingOnly){\n'
helper='''    private void addDashboardCategorySummary(String wallet,long[] totals){
        TextView heading=text(wallet+"钱包",16,true);heading.setPadding(8,14,8,4);content.addView(heading);
        boolean any=false;
        for(int i=0;i<cats.length;i++){
            if(totals[i]<=0)continue;
            any=true;final String category=cats[i];
            TextView row=text(category+"　"+yen.format(totals[i])+"　›",16,false);
            row.setPadding(18,12,18,12);row.setBackgroundColor(Color.WHITE);row.setClickable(true);
            row.setOnClickListener(v->openFilteredTransactions(wallet,category));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,6);content.addView(row,lp);
        }
        if(!any)content.addView(muted(wallet+"钱包本月暂无日常支出。"));
    }

    private void openFilteredTransactions(String wallet,String category){
        transactionWalletFilter=(wallet==null||wallet.isEmpty())?"全部":wallet;
        transactionCategoryFilter=(category==null||category.isEmpty())?"全部":category;
        currentScreen="流水";showTransactions(false);
    }

'''
if needle not in s: raise SystemExit('showTransactions needle missing')
s=s.replace(needle,helper+needle,1)
start=s.index('    private void showTransactions(boolean pendingOnly){')
end=s.index('\n    private Spinner spinner(',start)
new_method='''    private void showTransactions(boolean pendingOnly){
        currentScreen=pendingOnly?"待确认":"流水"; clear(); content.addView(text(pendingOnly?"待确认":"本月流水",20,true));
        if(!pendingOnly){
            content.addView(muted(monthDisplay(selectedMonth)+"的日常支出；可按钱包和类别组合筛选。"));
            LinearLayout filterBox=new LinearLayout(this);filterBox.setOrientation(LinearLayout.HORIZONTAL);filterBox.setPadding(0,6,0,12);
            LinearLayout walletBox=new LinearLayout(this);walletBox.setOrientation(LinearLayout.VERTICAL);
            TextView walletLabel=text("钱包筛选",13,true);walletLabel.setPadding(4,2,4,2);walletBox.addView(walletLabel);
            Spinner walletFilter=spinner(new String[]{"全部","个人","公用"},transactionWalletFilter);walletBox.addView(walletFilter);
            LinearLayout categoryBox=new LinearLayout(this);categoryBox.setOrientation(LinearLayout.VERTICAL);
            TextView categoryLabel=text("类别筛选",13,true);categoryLabel.setPadding(4,2,4,2);categoryBox.addView(categoryLabel);
            String[] categoryValues=new String[cats.length+1];categoryValues[0]="全部";System.arraycopy(cats,0,categoryValues,1,cats.length);
            Spinner categoryFilter=spinner(categoryValues,transactionCategoryFilter);categoryBox.addView(categoryFilter);
            filterBox.addView(walletBox,new LinearLayout.LayoutParams(0,-2,1));filterBox.addView(categoryBox,new LinearLayout.LayoutParams(0,-2,1));content.addView(filterBox);
            walletFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                @Override public void onItemSelected(AdapterView<?> parent,android.view.View view,int position,long id){
                    String selected=(String)parent.getItemAtPosition(position);
                    if(!selected.equals(transactionWalletFilter)){transactionWalletFilter=selected;showTransactions(false);}
                }
                @Override public void onNothingSelected(AdapterView<?> parent){}
            });
            categoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                @Override public void onItemSelected(AdapterView<?> parent,android.view.View view,int position,long id){
                    String selected=(String)parent.getItemAtPosition(position);
                    if(!selected.equals(transactionCategoryFilter)){transactionCategoryFilter=selected;showTransactions(false);}
                }
                @Override public void onNothingSelected(AdapterView<?> parent){}
            });
        }
        List<LedgerStore.Tx> list=LedgerStore.load(this); int shown=0;
        for(LedgerStore.Tx t:list){
            if(pendingOnly){ if(!t.pending)continue; }
            else {
                if(t.pending || !t.date.startsWith(selectedMonth) || "固定支出".equals(t.category))continue;
                if(!"全部".equals(transactionWalletFilter)&&!transactionWalletFilter.equals(t.wallet))continue;
                if(!"全部".equals(transactionCategoryFilter)&&!transactionCategoryFilter.equals(t.category))continue;
            }
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
        if(shown==0){
            if(pendingOnly)content.addView(text("暂无待确认记录。修正版会同时读取提醒、静默和常驻通知。",16,false));
            else if("全部".equals(transactionWalletFilter)&&"全部".equals(transactionCategoryFilter))content.addView(text("本月暂无日常流水。",16,false));
            else content.addView(text("当前筛选条件下暂无流水。",16,false));
        }
    }
'''
s=s[:start]+new_method+s[end:]
old='''        ExpensePieView pie=new ExpensePieView(this);pie.setData(data.pie);
        content.addView(pie,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
'''
new='''        ExpensePieView pie=new ExpensePieView(this);pie.setData(data.pie);
        pie.setOnSliceClickListener(slice->{
            boolean dailyCategory=false;
            for(String cat:cats)if(cat.equals(slice.label)){dailyCategory=true;break;}
            if(dailyCategory)openFilteredTransactions(wallet,slice.label);
            else Toast.makeText(this,"固定支出不属于日常流水",Toast.LENGTH_SHORT).show();
        });
        content.addView(pie,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
'''
if old not in s: raise SystemExit('pie listener anchor missing')
s=s.replace(old,new,1)
s=s.replace('                :"只显示实际日常流水的分类占比，不包含任何固定支出。");',
            '                :"只显示实际日常流水的分类占比，不包含任何固定支出。点击扇区或对应分类可查看当前月份流水。");')
main.write_text(s)

p=pie.read_text()
p=p.replace('import android.graphics.RectF;\nimport android.view.View;', 'import android.graphics.RectF;\nimport android.view.MotionEvent;\nimport android.view.View;')
p=p.replace('''    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);''', '''    public interface OnSliceClickListener { void onSliceClick(Slice slice); }\n\n    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);''')
p=p.replace('''    private List<Slice> slices=new ArrayList<>();''', '''    private List<Slice> slices=new ArrayList<>();\n    private OnSliceClickListener listener;''')
p=p.replace('''        super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);''', '''        super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setClickable(true);''')
p=p.replace('''    public void setData(List<Slice> slices){this.slices=slices==null?new ArrayList<>():slices;requestLayout();invalidate();}\n''', '''    public void setData(List<Slice> slices){this.slices=slices==null?new ArrayList<>():slices;requestLayout();invalidate();}\n    public void setOnSliceClickListener(OnSliceClickListener listener){this.listener=listener;}\n''')
p=p.replace('''        long total=0;for(Slice s:slices)if(s.value>0)total+=s.value;''', '''        long total=totalValue();''')
anchor='''    private String ellipsize(String s,int max){if(s==null)return "";return s.length()>max?s.substring(0,max-1)+"…":s;}'''
methods='''    @Override public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN)return true;
        if(event.getAction()!=MotionEvent.ACTION_UP)return super.onTouchEvent(event);
        Slice hit=findSlice(event.getX(),event.getY());
        if(hit!=null&&listener!=null){performClick();listener.onSliceClick(hit);return true;}
        return true;
    }

    @Override public boolean performClick(){super.performClick();return true;}

    private Slice findSlice(float x,float y){
        long total=totalValue();if(total<=0)return null;
        float w=getWidth(),cx=w/2,cy=dp(132),radius=Math.min(w*0.28f,dp(104));
        float dx=x-cx,dy=y-cy,dist=(float)Math.sqrt(dx*dx+dy*dy);
        if(dist>=radius*0.52f&&dist<=radius){
            double angle=Math.toDegrees(Math.atan2(dy,dx))+90d;
            if(angle<0)angle+=360d;
            double cursor=0;
            for(Slice s:slices){
                if(s.value<=0)continue;
                double sweep=360d*s.value/total;
                if(angle>=cursor&&angle<cursor+sweep)return s;
                cursor+=sweep;
            }
        }
        float legendY=dp(270);
        for(Slice s:slices){
            if(s.value<=0)continue;
            if(x>=dp(10)&&x<=w-dp(10)&&y>=legendY-dp(20)&&y<=legendY+dp(10))return s;
            legendY+=dp(30);
        }
        return null;
    }

    private long totalValue(){long total=0;for(Slice s:slices)if(s.value>0)total+=s.value;return total;}
'''
if anchor not in p: raise SystemExit('pie method anchor missing')
p=p.replace(anchor,methods+anchor,1)
pie.write_text(p)
