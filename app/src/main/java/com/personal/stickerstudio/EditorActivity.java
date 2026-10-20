package com.personal.stickerstudio;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class EditorActivity extends Activity {
  private static final int CROP = 1, TEXT = 2, BACKGROUND = 3;
  private static final int PURPLE = Color.rgb(113,80,236);
  private Bitmap image;
  private EditorView preview;
  private Button cropApply, saveButton;
  private LinearLayout cropPanel, bgPanel, textPanel;
  private TextView hint;
  private String overlay = "";
  private float overlayX, overlayY, textRatio = .10f;
  private int overlayColor = Color.WHITE, mode = CROP, tolerance = 55;
  private RectF crop;
  private boolean busy = false;
  private final ArrayDeque<Snapshot> history = new ArrayDeque<>();

  private static class Snapshot {
    Bitmap bitmap; String text; float x,y,size; int color;
    Snapshot(Bitmap b,String t,float px,float py,float s,int c) {
      bitmap=b;text=t;x=px;y=py;size=s;color=c;
    }
  }
  private int dp(int value) {return (int)(value*getResources().getDisplayMetrics().density+.5f);}
  private TextView label(String s,int size) {
    TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.WHITE);
    v.setPadding(dp(12),dp(10),dp(12),dp(10));return v;
  }
  private Button btn(String s) {
    Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);return b;
  }
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(27,24,40));
    getWindow().setNavigationBarColor(Color.rgb(27,24,40));
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.rgb(26,23,38));
    TextView header=label("✦ EDIT STIKER",20);header.setTextColor(Color.rgb(188,172,255));root.addView(header);
    preview=new EditorView(this);
    root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
    Button aiButton=btn("✨ HAPUS BACKGROUND AI (OFFLINE)");
    aiButton.setTextColor(PURPLE);
    root.addView(aiButton,new LinearLayout.LayoutParams(-1,dp(54)));
    aiButton.setOnClickListener(v->removeBackgroundAI());
    hint=label("Memuat foto...",13);root.addView(hint);
    HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);scroll.addView(tools);
    Button cropB=btn("✂ Crop");tools.addView(cropB);
    Button textB=btn("Teks");tools.addView(textB);
    Button colorB=btn("Warna Teks");tools.addView(colorB);
    Button bgB=btn("BG Manual");tools.addView(bgB);
    Button undoB=btn("↶ Undo");tools.addView(undoB);
    root.addView(scroll);
    cropPanel=new LinearLayout(this);cropPanel.setOrientation(LinearLayout.HORIZONTAL);
    cropApply=btn("✓ Terapkan Crop");cropPanel.addView(cropApply);
    Button reset=btn("Reset Kotak");cropPanel.addView(reset);root.addView(cropPanel);
    bgPanel=new LinearLayout(this);bgPanel.setOrientation(LinearLayout.VERTICAL);
    bgPanel.addView(label("Toleransi warna latar:",12));
    SeekBar bgSlider=new SeekBar(this);bgSlider.setMax(100);bgSlider.setProgress(tolerance);
    bgSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar s,int p,boolean from){tolerance=p;}
      public void onStartTrackingTouch(SeekBar s){}
      public void onStopTrackingTouch(SeekBar s){}
    });
    bgPanel.addView(bgSlider);root.addView(bgPanel);
    textPanel=new LinearLayout(this);textPanel.setOrientation(LinearLayout.VERTICAL);
    textPanel.addView(label("Ukuran teks:",12));
    SeekBar textSlider=new SeekBar(this);textSlider.setMax(24);textSlider.setProgress(8);
    textSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar s,int p,boolean from){if(from&&!busy){textRatio=.04f+p*.0075f;preview.invalidate();}}
      public void onStartTrackingTouch(SeekBar s){saveUndo();}
      public void onStopTrackingTouch(SeekBar s){}
    });
    textPanel.addView(textSlider);root.addView(textPanel);
    saveButton=btn("SIMPAN STIKER");saveButton.setTextColor(PURPLE);saveButton.setEnabled(false);
    root.addView(saveButton,new LinearLayout.LayoutParams(-1,dp(62)));
    setContentView(root);
    cropB.setOnClickListener(v->setMode(CROP));
    textB.setOnClickListener(v->editText());
    colorB.setOnClickListener(v->chooseColor());
    bgB.setOnClickListener(v->setMode(BACKGROUND));
    undoB.setOnClickListener(v->undo());
    cropApply.setOnClickListener(v->applyCrop());
    reset.setOnClickListener(v->{if(image!=null){resetCrop();preview.invalidate();}});
    saveButton.setOnClickListener(v->saveSticker());
    setMode(CROP);
    Uri uri=getIntent().getData();
    if(uri==null){toast("Foto tidak ditemukan");finish();return;}
    new Thread(()->{
      try{
        android.graphics.ImageDecoder.Source source=android.graphics.ImageDecoder.createSource(getContentResolver(),uri);
        Bitmap loaded=android.graphics.ImageDecoder.decodeBitmap(source,(decoder,info,s)->{
          int w=info.getSize().getWidth(),h=info.getSize().getHeight();
          float f=Math.min(1f,1600f/Math.max(w,h));
          decoder.setTargetSize(Math.max(1,Math.round(w*f)),Math.max(1,Math.round(h*f)));
          decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        runOnUiThread(()->{
          image=loaded;overlayX=image.getWidth()/2f;overlayY=image.getHeight()*.82f;
          resetCrop();preview.invalidate();saveButton.setEnabled(true);
          hint.setText("Tarik sudut kotak untuk crop, lalu tekan Terapkan Crop.");
        });
      }catch(Exception e){runOnUiThread(()->{toast("Gagal membuka foto: "+e.getMessage());finish();});}
    }).start();
  }
  private void resetCrop(){
    if(image==null)return;
    crop=new RectF(0,0,image.getWidth(),image.getHeight());
  }
  private void setMode(int which) {
    if(busy)return;
    mode=which;
    cropPanel.setVisibility(mode==CROP?View.VISIBLE:View.GONE);
    bgPanel.setVisibility(mode==BACKGROUND?View.VISIBLE:View.GONE);
    textPanel.setVisibility(mode==TEXT&&!overlay.isEmpty()?View.VISIBLE:View.GONE);
    if(mode==CROP)hint.setText("CROP: geser sudut atau seluruh kotak, lalu Terapkan Crop.");
    if(mode==TEXT)hint.setText("TEKS: geser tulisan di atas foto untuk mengubah posisinya.");
    if(mode==BACKGROUND)hint.setText("HAPUS BG: ketuk warna LATAR di foto. Coba atur toleransi.");
    preview.invalidate();
  }
  private void saveUndo(){
    if(image==null||busy)return;
    history.addLast(new Snapshot(image,overlay,overlayX,overlayY,textRatio,overlayColor));
    while(history.size()>7)history.removeFirst();
  }
  private void undo(){
    if(busy||history.isEmpty()){toast("Tidak ada perubahan untuk di-undo");return;}
    Snapshot s=history.removeLast();
    image=s.bitmap;overlay=s.text;overlayX=s.x;overlayY=s.y;textRatio=s.size;overlayColor=s.color;
    resetCrop();preview.invalidate();setMode(mode);toast("Perubahan dibatalkan");
  }
  private void editText(){
    if(image==null||busy)return;
    EditText field=new EditText(this);field.setSingleLine(true);field.setText(overlay);
    field.setSelectAllOnFocus(true);field.setHint("Teks stiker");
    LinearLayout holder=new LinearLayout(this);holder.setPadding(dp(20),0,dp(20),0);holder.addView(field,new LinearLayout.LayoutParams(-1,-2));
    new AlertDialog.Builder(this).setTitle("Tambah atau ubah teks").setView(holder)
      .setNegativeButton("Batal",null)
      .setNeutralButton("Hapus teks",(d,w)->{saveUndo();overlay="";setMode(TEXT);preview.invalidate();})
      .setPositiveButton("Terapkan",(d,w)->{
        saveUndo();boolean wasEmpty=overlay.isEmpty();overlay=field.getText().toString().trim();
        if(wasEmpty){overlayX=image.getWidth()/2f;overlayY=image.getHeight()*.82f;}
        setMode(TEXT);preview.invalidate();
      }).show();
  }
  private void chooseColor(){
    if(image==null||busy)return;
    String[] colors={"Putih","Kuning","Hitam","Merah","Biru muda"};
    int[] values={Color.WHITE,Color.YELLOW,Color.BLACK,Color.rgb(255,78,99),Color.CYAN};
    new AlertDialog.Builder(this).setTitle("Warna teks").setItems(colors,(d,n)->{
      saveUndo();overlayColor=values[n];setMode(TEXT);preview.invalidate();
    }).show();
  }
  private static float limit(float n,float low,float high){return Math.max(low,Math.min(n,high));}
  private void applyCrop(){
    if(image==null||crop==null||busy)return;
    int l=Math.max(0,(int)crop.left),t=Math.max(0,(int)crop.top);
    int r=Math.min(image.getWidth(),(int)Math.ceil(crop.right));
    int b=Math.min(image.getHeight(),(int)Math.ceil(crop.bottom));
    if(r-l<16||b-t<16){toast("Ukuran crop terlalu kecil");return;}
    if(l==0&&t==0&&r==image.getWidth()&&b==image.getHeight()){toast("Geser sudut kotak terlebih dahulu");return;}
    saveUndo();
    Bitmap cropped=Bitmap.createBitmap(image,l,t,r-l,b-t);
    overlayX-=l;overlayY-=t;
    image=cropped;resetCrop();preview.invalidate();toast("Crop diterapkan");
  }
  private void removeBg(int x,int y){
    if(image==null||busy)return;
    final Bitmap input=image;
    int w=input.getWidth(),h=input.getHeight();
    if(x<0||y<0||x>=w||y>=h)return;
    busy=true;saveButton.setEnabled(false);hint.setText("Menghapus warna background...");
    final int threshold=12+tolerance*2;
    new Thread(()->{
      try{
        int[] pixels=new int[w*h];input.getPixels(pixels,0,w,0,0,w,h);
        int seed=y*w+x,target=pixels[seed];
        if(Color.alpha(target)<12)throw new IllegalStateException("Ketuk bagian background yang masih berwarna.");
        int sr=Color.red(target),sg=Color.green(target),sb=Color.blue(target);
        boolean[] visited=new boolean[pixels.length];
        int[] queue=new int[pixels.length];
        int head=0,tail=0,removed=0;
        queue[tail++]=seed;visited[seed]=true;
        while(head<tail){
          int id=queue[head++],p=pixels[id];
          int dr=Math.abs(Color.red(p)-sr),dg=Math.abs(Color.green(p)-sg),db=Math.abs(Color.blue(p)-sb);
          if(Color.alpha(p)<12||Math.max(dr,Math.max(dg,db))>threshold)continue;
          pixels[id]=Color.TRANSPARENT;removed++;
          int px=id%w,py=id/w;
          if(px>0&&!visited[id-1]){visited[id-1]=true;queue[tail++]=id-1;}
          if(px+1<w&&!visited[id+1]){visited[id+1]=true;queue[tail++]=id+1;}
          if(py>0&&!visited[id-w]){visited[id-w]=true;queue[tail++]=id-w;}
          if(py+1<h&&!visited[id+w]){visited[id+w]=true;queue[tail++]=id+w;}
        }
        if(removed==0)throw new IllegalStateException("Tidak ada warna latar yang bisa dihapus.");
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        out.setPixels(pixels,0,w,0,0,w,h);
        int n=removed;
        runOnUiThread(()->{
          busy=false;saveUndo();image=out;saveButton.setEnabled(true);
          preview.invalidate();hint.setText("Background dihapus ("+n+" piksel). Ketuk area lain bila perlu.");
        });
      }catch(Exception e){runOnUiThread(()->{busy=false;saveButton.setEnabled(true);toast(e.getMessage());setMode(BACKGROUND);});}
    }).start();
  }

  private void removeBackgroundAI(){
    if(image==null||busy)return;
    final Bitmap original=image;
    busy=true;saveButton.setEnabled(false);
    hint.setText("AI mengenali objek... Mohon tunggu beberapa detik.");
    new Thread(()->{
      try{
        Bitmap edited=AiBackgroundRemover.remove(this,original);
        runOnUiThread(()->{
          if(isFinishing()||isDestroyed()){edited.recycle();return;}
          busy=false;saveUndo();
          image=edited;resetCrop();setMode(TEXT);
          hint.setText("Background AI sudah dihapus. Gunakan Undo jika perlu.");
          saveButton.setEnabled(true);preview.invalidate();
        });
      }catch(Exception e){
        runOnUiThread(()->{
          if(isFinishing()||isDestroyed())return;
          busy=false;saveButton.setEnabled(true);
          hint.setText("AI gagal. Coba ulangi atau pakai penghapus BG Manual.");
          toast("AI gagal: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
        });
      }catch(UnsatisfiedLinkError e){
        runOnUiThread(()->{
          if(isFinishing()||isDestroyed())return;
          busy=false;saveButton.setEnabled(true);
          hint.setText("Mesin AI tidak kompatibel dengan perangkat ini.");
          toast("Mesin AI ONNX gagal dimuat di perangkat.");
        });
      }
    }).start();
  }
  private void drawText(Canvas canvas,int w,int h){
    if(overlay.trim().isEmpty())return;
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));
    p.setTextAlign(Paint.Align.CENTER);
    float size=Math.max(18,Math.min(w,h)*textRatio);
    p.setTextSize(size);
    while(p.measureText(overlay)>w*.94f&&size>12){size*=.92f;p.setTextSize(size);}
    p.setStrokeJoin(Paint.Join.ROUND);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(3,size*.11f));
    p.setColor(overlayColor==Color.BLACK?Color.WHITE:Color.BLACK);
    canvas.drawText(overlay,overlayX,overlayY,p);
    p.setStyle(Paint.Style.FILL);p.setColor(overlayColor);
    canvas.drawText(overlay,overlayX,overlayY,p);
  }
  private Bitmap renderSticker(){
    Bitmap out=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
    Canvas c=new Canvas(out);
    float scale=Math.min(472f/image.getWidth(),472f/image.getHeight());
    float left=(512-image.getWidth()*scale)/2f,top=(512-image.getHeight()*scale)/2f;
    c.save();c.translate(left,top);c.scale(scale,scale);
    c.drawBitmap(image,0,0,new Paint(Paint.FILTER_BITMAP_FLAG|Paint.ANTI_ALIAS_FLAG));
    drawText(c,image.getWidth(),image.getHeight());c.restore();
    return out;
  }
  private File dir(){File d=new File(getFilesDir(),"stickers/foto_pribadi");d.mkdirs();return d;}
  private void saveSticker(){
    if(image==null||busy)return;
    File[] existing=dir().listFiles((d,n)->n.endsWith(".webp"));
    if(existing!=null&&existing.length>=30){toast("Paket sudah berisi 30 stiker");return;}
    busy=true;saveButton.setEnabled(false);hint.setText("Menyimpan WebP...");
    new Thread(()->{
      try{
        Bitmap out=renderSticker();byte[] best=null;
        int[] quality={95,85,75,65,50,35,20,10,5,1};
        for(int q:quality){
          ByteArrayOutputStream b=new ByteArrayOutputStream();
          out.compress(Bitmap.CompressFormat.WEBP_LOSSY,q,b);
          if(b.size()<=100000){best=b.toByteArray();break;}
        }
        if(best==null)throw new IOException("Foto terlalu kompleks. Coba crop lebih kecil.");
        File folder=dir(),file=new File(folder,"sticker_"+System.currentTimeMillis()+".webp");
        try(FileOutputStream stream=new FileOutputStream(file)){stream.write(best);}
        File tray=new File(folder,"tray.png");
        if(!tray.exists()){
          Bitmap small=Bitmap.createScaledBitmap(out,96,96,true);
          try(FileOutputStream stream=new FileOutputStream(tray)){small.compress(Bitmap.CompressFormat.PNG,100,stream);}
          small.recycle();
        }
        out.recycle();
        getContentResolver().notifyChange(Uri.parse("content://com.personal.stickerstudio.provider/metadata"),null);
        runOnUiThread(()->{busy=false;setResult(RESULT_OK);toast("Stiker berhasil disimpan");finish();});
      }catch(Exception e){runOnUiThread(()->{busy=false;saveButton.setEnabled(true);toast("Gagal simpan: "+e.getMessage());});}
    }).start();
  }
  private class EditorView extends View{
    RectF drawn=new RectF();int handle=0;float lastX,lastY;
    EditorView(Context context){super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    float imgX(float x){return (x-drawn.left)*image.getWidth()/Math.max(1,drawn.width());}
    float imgY(float y){return (y-drawn.top)*image.getHeight()/Math.max(1,drawn.height());}
    float scrX(float x){return drawn.left+x*drawn.width()/image.getWidth();}
    float scrY(float y){return drawn.top+y*drawn.height()/image.getHeight();}
    @Override protected void onDraw(Canvas c){
      super.onDraw(c);c.drawColor(Color.rgb(24,22,32));
      if(image==null)return;
      float s=Math.min((getWidth()-dp(24))/(float)image.getWidth(),(getHeight()-dp(24))/(float)image.getHeight());
      float wd=image.getWidth()*s,ht=image.getHeight()*s;
      drawn.set((getWidth()-wd)/2f,(getHeight()-ht)/2f,(getWidth()+wd)/2f,(getHeight()+ht)/2f);
      c.save();c.clipRect(drawn);
      Paint tile=new Paint();int tileSz=dp(14);
      for(float iy=drawn.top;iy<drawn.bottom;iy+=tileSz)
        for(float ix=drawn.left;ix<drawn.right;ix+=tileSz){
          boolean even=(((int)((ix-drawn.left)/tileSz)+(int)((iy-drawn.top)/tileSz))%2)==0;
          tile.setColor(even?0xffe0dfe5:0xfff5f4f7);
          c.drawRect(ix,iy,Math.min(ix+tileSz,drawn.right),Math.min(iy+tileSz,drawn.bottom),tile);
        }
      Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
      c.drawBitmap(image,null,drawn,bitmapPaint);
      c.save();c.translate(drawn.left,drawn.top);c.scale(s,s);
      drawText(c,image.getWidth(),image.getHeight());c.restore();
      c.restore();
      if(mode==CROP&&crop!=null){
        float l=scrX(crop.left),t=scrY(crop.top),r=scrX(crop.right),b=scrY(crop.bottom);
        Paint mask=new Paint();mask.setColor(0x99000000);
        c.drawRect(drawn.left,drawn.top,drawn.right,t,mask);
        c.drawRect(drawn.left,b,drawn.right,drawn.bottom,mask);
        c.drawRect(drawn.left,t,l,b,mask);c.drawRect(r,t,drawn.right,b,mask);
        Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);border.setColor(Color.WHITE);
        border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(dp(2));
        c.drawRect(l,t,r,b,border);border.setStyle(Paint.Style.FILL);
        for(float[] corner:new float[][]{{l,t},{r,t},{l,b},{r,b}})c.drawCircle(corner[0],corner[1],dp(7),border);
      }
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      if(image==null||busy)return true;
      float x=limit(imgX(e.getX()),0,image.getWidth()),y=limit(imgY(e.getY()),0,image.getHeight());
      if(mode==BACKGROUND){
        if(e.getAction()==MotionEvent.ACTION_UP&&drawn.contains(e.getX(),e.getY()))removeBg((int)Math.min(x,image.getWidth()-1),(int)Math.min(y,image.getHeight()-1));
        return true;
      }
      if(mode==TEXT&&!overlay.isEmpty()){
        if(e.getAction()==MotionEvent.ACTION_DOWN){saveUndo();overlayX=x;overlayY=y;invalidate();return true;}
        if(e.getAction()==MotionEvent.ACTION_MOVE){overlayX=x;overlayY=y;invalidate();return true;}
        return true;
      }
      if(mode!=CROP||crop==null)return true;
      if(e.getAction()==MotionEvent.ACTION_DOWN){
        float radius=dp(40)/Math.max(.001f,drawn.width()/image.getWidth());
        float[][] handles={{crop.left,crop.top},{crop.right,crop.top},{crop.left,crop.bottom},{crop.right,crop.bottom}};
        handle=0;
        for(int i=0;i<4;i++){
          if(Math.hypot(x-handles[i][0],y-handles[i][1])<radius){handle=i+1;break;}
        }
        if(handle==0&&crop.contains(x,y))handle=5;
        lastX=x;lastY=y;return true;
      }
      if(e.getAction()==MotionEvent.ACTION_MOVE&&handle!=0){
        float minSide=Math.min(40,Math.min(image.getWidth(),image.getHeight())*.2f);
        switch(handle){
          case 1:crop.left=limit(x,0,crop.right-minSide);crop.top=limit(y,0,crop.bottom-minSide);break;
          case 2:crop.right=limit(x,crop.left+minSide,image.getWidth());crop.top=limit(y,0,crop.bottom-minSide);break;
          case 3:crop.left=limit(x,0,crop.right-minSide);crop.bottom=limit(y,crop.top+minSide,image.getHeight());break;
          case 4:crop.right=limit(x,crop.left+minSide,image.getWidth());crop.bottom=limit(y,crop.top+minSide,image.getHeight());break;
          case 5:
            float dx=limit(x-lastX,-crop.left,image.getWidth()-crop.right);
            float dy=limit(y-lastY,-crop.top,image.getHeight()-crop.bottom);
            crop.offset(dx,dy);break;
        }
        lastX=x;lastY=y;invalidate();return true;
      }
      if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){handle=0;return true;}
      return true;
    }
  }
}