package com.personal.stickerstudio;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/**
 * Offline Video/GIF -> 512x512 animated WebP; separate WhatsApp pack.
 * Only lightweight Android platform codecs are used.
 */
public class AnimationActivity extends Activity {
  private static final int MAX_STICKERS=30;
  private int seconds=2, startSeconds=0;
  private boolean busy=false;
  private boolean isGif;
  private Uri sourceUri;
  private ImageView sample;
  private TextView status, durationLabel, startLabel;
  private Button generate;
  private SeekBar startBar;
  private int dp(int d){return Math.round(getResources().getDisplayMetrics().density*d);}
  private TextView text(String s,int size){
    TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);
    t.setPadding(dp(8),dp(7),dp(8),dp(7));return t;
  }
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  private void error(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  private File dir(){File f=new File(getFilesDir(),"stickers/animasi_pribadi");f.mkdirs();return f;}
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    isGif="gif".equals(getIntent().getStringExtra("kind"));
    sourceUri=getIntent().getData();
    if(sourceUri==null){finish();return;}
    ScrollView sc=new ScrollView(this);sc.setBackgroundColor(0xff191725);
    LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(18),dp(20),dp(18),dp(22));sc.addView(root);
    root.addView(text("✦ STIKER BERGERAK",22));
    root.addView(text(isGif?"Dari GIF":"Dari video",16));
    sample=new ImageView(this);sample.setBackgroundColor(0xff373440);
    sample.setScaleType(ImageView.ScaleType.FIT_CENTER);
    root.addView(sample,new LinearLayout.LayoutParams(-1,dp(240)));
    status=text("Menyiapkan sumber...",13);root.addView(status);
    startLabel=text("Mulai dari detik: 0",15);root.addView(startLabel);
    startBar=new SeekBar(this);startBar.setMax(30);startBar.setProgress(0);
    root.addView(startBar);
    startBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar v,int p,boolean f){
        startSeconds=p;startLabel.setText("Mulai dari detik: "+p);
      }
      public void onStartTrackingTouch(SeekBar v){}
      public void onStopTrackingTouch(SeekBar v){}
    });
    if(isGif){startLabel.setVisibility(View.GONE);startBar.setVisibility(View.GONE);}
    durationLabel=text("Durasi: 2 detik",15);root.addView(durationLabel);
    SeekBar duration=new SeekBar(this);duration.setMax(2);duration.setProgress(1);
    duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar v,int p,boolean f){
        seconds=p+1;durationLabel.setText("Durasi: "+seconds+" detik");
      }
      public void onStartTrackingTouch(SeekBar v){}
      public void onStopTrackingTouch(SeekBar v){}
    });root.addView(duration);
    root.addView(text("Hasil animasi 512×512, maksimal 500 KB. Pemrosesan offline.",13));
    generate=button("✨ BUAT STIKER BERGERAK");
    root.addView(generate,new LinearLayout.LayoutParams(-1,dp(64)));
    generate.setOnClickListener(v->createAnimation());
    setContentView(sc);
    showPreview();
  }
  private Bitmap frameToSticker(Bitmap frame){
    Bitmap out=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
    Canvas c=new Canvas(out);
    float s=Math.min(494f/frame.getWidth(),494f/frame.getHeight());
    float w=frame.getWidth()*s,h=frame.getHeight()*s;
    RectF area=new RectF((512-w)/2f,(512-h)/2f,(512+w)/2f,(512+h)/2f);
    c.drawBitmap(frame,null,area,new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
    return out;
  }
  private void showPreview(){
    new Thread(()->{
      try {
        Bitmap frame;
        if(isGif){
          try(InputStream in=getContentResolver().openInputStream(sourceUri)){
            Movie m=Movie.decodeStream(in);
            if(m==null)throw new IOException("GIF tidak bisa dibuka");
            Bitmap first=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(first);float scale=Math.min(494f/m.width(),494f/m.height());
            c.translate((512-m.width()*scale)/2,(512-m.height()*scale)/2);
            c.scale(scale,scale);m.setTime(0);m.draw(c,0,0);frame=first;
          }
        }else{
          MediaMetadataRetriever mm=new MediaMetadataRetriever();
          try{
            mm.setDataSource(this,sourceUri);
            frame=mm.getScaledFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST,512,512);
            if(frame==null)frame=mm.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST);
            String len=mm.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int total=len==null?0:(int)Math.min(Integer.MAX_VALUE,Long.parseLong(len));
            final int maxStart=Math.max(0,(total-300)/1000);
            runOnUiThread(()->{startBar.setMax(maxStart);status.setText("Panjang video: "+(total/1000)+" detik. Pilih bagian yang ingin dijadikan stiker.");});
          }finally{mm.release();}
        }
        if(frame==null)throw new IOException("Tidak bisa membaca frame");
        Bitmap thumb=frameToSticker(frame);if(frame!=thumb)frame.recycle();
        runOnUiThread(()->{
          if(isFinishing()){thumb.recycle();return;}
          sample.setImageBitmap(thumb);
          if(isGif)status.setText("GIF siap dikonversi. Pilih durasi 1–3 detik.");
        });
      }catch(Exception ex){
        runOnUiThread(()->status.setText("Gagal preview: "+ex.getMessage()));
      }
    }).start();
  }
  private void progress(String s){runOnUiThread(()->{if(!isFinishing())status.setText(s);});}
  private ArrayList<Bitmap> gifFrames(int n,int totalMs)throws Exception{
    ArrayList<Bitmap> frames=new ArrayList<>();
    try(InputStream in=getContentResolver().openInputStream(sourceUri)){
      Movie gif=Movie.decodeStream(in);
      if(gif==null||gif.width()<=0||gif.height()<=0)throw new IOException("GIF rusak atau tidak didukung");
      int length=gif.duration();if(length<=0)length=1000;
      int duration=Math.min(totalMs,length);
      for(int k=0;k<n;k++){
        Bitmap out=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out);
        float scale=Math.min(494f/gif.width(),494f/gif.height());
        c.translate((512-gif.width()*scale)/2,(512-gif.height()*scale)/2);
        c.scale(scale,scale);
        int time=(int)((long)k*duration/n);
        gif.setTime(time);gif.draw(c,0,0);
        frames.add(out);
        if(k%3==0)progress("Membaca frame GIF: "+(k+1)+"/"+n);
      }
    }catch(Exception e){for(Bitmap b:frames)b.recycle();throw e;}
    return frames;
  }
  private ArrayList<Bitmap> videoFrames(int n,int totalMs)throws Exception{
    ArrayList<Bitmap> frames=new ArrayList<>();
    MediaMetadataRetriever mm=new MediaMetadataRetriever();
    try{
      mm.setDataSource(this,sourceUri);
      String dur=mm.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
      long duration=dur==null?0:Long.parseLong(dur);
      if(duration<150)throw new IOException("Video terlalu pendek");
      long start=Math.min((long)startSeconds*1000,Math.max(0,duration-150));
      long actual=Math.min(totalMs,duration-start);
      if(actual<150)throw new IOException("Pilih waktu lain dalam video");
      for(int k=0;k<n;k++){
        long t=start+(long)k*actual/n;
        Bitmap frame=mm.getScaledFrameAtTime(t*1000,MediaMetadataRetriever.OPTION_CLOSEST,512,512);
        if(frame==null)frame=mm.getFrameAtTime(t*1000,MediaMetadataRetriever.OPTION_CLOSEST);
        if(frame==null)throw new IOException("Frame video "+(k+1)+" gagal dibaca");
        Bitmap fit=frameToSticker(frame);if(fit!=frame)frame.recycle();
        frames.add(fit);
        if(k%3==0)progress("Membaca frame video: "+(k+1)+"/"+n);
      }
      return frames;
    }catch(Exception e){for(Bitmap b:frames)b.recycle();throw e;}
    finally{mm.release();}
  }
  private static ArrayList<Bitmap> select(ArrayList<Bitmap> source,int count){
    ArrayList<Bitmap> selected=new ArrayList<>();
    for(int i=0;i<count;i++)selected.add(source.get((int)((long)i*source.size()/count)));
    return selected;
  }
  private void createAnimation(){
    if(busy)return;
    File[] existing=dir().listFiles((d,n)->n.endsWith(".webp"));
    if(existing!=null&&existing.length>=MAX_STICKERS){error("Maksimal 30 stiker bergerak");return;}
    busy=true;generate.setEnabled(false);
    final int lengthMs=seconds*1000;
    new Thread(()->{
      ArrayList<Bitmap> source=null;
      try{
        int originalN=seconds*6;
        source=isGif?gifFrames(originalN,lengthMs):videoFrames(originalN,lengthMs);
        byte[] encoded=null;
        int[] fps={6,5,4,3,2};
        int[] quality={70,45,25,12,4};
        outer:
        for(int f:fps){
          int count=f*seconds;
          ArrayList<Bitmap> frames=select(source,count);
          for(int q:quality){
            progress("Mengompresi WebP: "+f+" fps, kualitas "+q);
            try{
              encoded=AnimatedWebp.encode(frames,1000/f,q);
              break outer;
            }catch(IOException exception){
              if(!exception.getMessage().contains("besar"))throw exception;
            }
          }
        }
        if(encoded==null)throw new IOException("Animasi terlalu besar (500 KB). Coba durasi lebih pendek.");
        File folder=dir(), target=new File(folder,"anim_"+System.currentTimeMillis()+".webp");
        try(FileOutputStream out=new FileOutputStream(target)){out.write(encoded);}
        File icon=new File(folder,"tray.png");
        if(!icon.exists()){
          Bitmap tray=Bitmap.createScaledBitmap(source.get(0),96,96,true);
          try(FileOutputStream out=new FileOutputStream(icon)){tray.compress(Bitmap.CompressFormat.PNG,100,out);}
          tray.recycle();
        }
        getContentResolver().notifyChange(Uri.parse("content://com.personal.stickerstudio.provider/metadata"),null);
        runOnUiThread(()->{busy=false;setResult(RESULT_OK);error("Stiker bergerak berhasil disimpan!");finish();});
      }catch(Exception e){
        final String message=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
        runOnUiThread(()->{busy=false;generate.setEnabled(true);status.setText("Gagal: "+message);error(message);});
      }finally{
        if(source!=null)for(Bitmap b:source)if(!b.isRecycled())b.recycle();
      }
    }).start();
  }
}