package com.personal.stickerstudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
  static final String AUTH="com.personal.stickerstudio.provider";
  static final String PACK="foto_pribadi";
  LinearLayout main; EditText caption; TextView status;
  int purple=Color.rgb(110,79,235);
  File folder(){File d=new File(getFilesDir(),"stickers/"+PACK);d.mkdirs();return d;}
  File[] stickers(){File[] x=folder().listFiles((d,n)->n.endsWith(".webp"));if(x==null)return new File[0];Arrays.sort(x);return x;}
  int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(37,33,61));t.setPadding(0,dp(8),0,dp(8));return t;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  void showToast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override public void onCreate(Bundle b){super.onCreate(b);draw();}
  void draw(){
    ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Color.rgb(247,246,252));
    main=new LinearLayout(this);main.setOrientation(1);main.setPadding(dp(22),dp(22),dp(22),dp(22));sc.addView(main);
    TextView title=text("✦ STICKER STUDIO",17);title.setTextColor(purple);main.addView(title);
    main.addView(text("Stiker pribadi. Tanpa iklan.",27));
    main.addView(text("Foto diproses langsung di HP, tanpa server.",14));
    caption=new EditText(this);caption.setSingleLine(true);caption.setHint("Teks stiker (opsional)");main.addView(caption);
    Button photo=button("＋ Buat dari foto");main.addView(photo);
    photo.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);});
    File[] files=stickers();main.addView(text("Paket stiker: "+files.length+"/30",19));
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);HorizontalScrollView hs=new HorizontalScrollView(this);hs.addView(row);main.addView(hs);
    for(File f:files){ImageView iv=new ImageView(this);iv.setLayoutParams(new LinearLayout.LayoutParams(dp(94),dp(94)));iv.setScaleType(ImageView.ScaleType.FIT_CENTER);Bitmap bmp=BitmapFactory.decodeFile(f.getPath());if(bmp!=null)iv.setImageBitmap(bmp);row.addView(iv);iv.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Hapus stiker?").setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w)->{f.delete();draw();}).show();return true;});}
    Button add=button("Tambahkan ke WhatsApp");add.setEnabled(files.length>=3);main.addView(add);
    add.setOnClickListener(v->{Intent i=new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");i.putExtra("sticker_pack_id",PACK);i.putExtra("sticker_pack_authority",AUTH);i.putExtra("sticker_pack_name","Stiker Pribadi");try{startActivityForResult(i,2);}catch(Exception e){showToast("WhatsApp tidak ditemukan");}});
    main.addView(text("Minimal 3 foto untuk membuat paket. Tekan lama stiker untuk menghapus.",13));
    main.addView(text("Versi awal: stiker foto. Stiker animasi menyusul.",12));
    setContentView(sc);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=1||result!=RESULT_OK||data==null||data.getData()==null)return;if(stickers().length>=30){showToast("Paket penuh");return;}
    Uri uri=data.getData();String txt=caption.getText().toString();new Thread(()->{try{
      android.graphics.ImageDecoder.Source source=android.graphics.ImageDecoder.createSource(getContentResolver(),uri);
      Bitmap src=android.graphics.ImageDecoder.decodeBitmap(source,(decoder,info,s)->{int w=info.getSize().getWidth(),h=info.getSize().getHeight();float factor=Math.min(1f,1400f/Math.max(w,h));decoder.setTargetSize(Math.max(1,Math.round(w*factor)),Math.max(1,Math.round(h*factor)));decoder.setAllocator(android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE);});
      Bitmap out=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);
      float scale=Math.min(472f/src.getWidth(),472f/src.getHeight());int w=Math.round(src.getWidth()*scale),h=Math.round(src.getHeight()*scale);
      Rect dest=new Rect((512-w)/2,(512-h)/2,(512+w)/2,(512+h)/2);
      c.drawBitmap(src,null,dest,new Paint(Paint.FILTER_BITMAP_FLAG));src.recycle();
      if(!txt.trim().isEmpty()){Paint p=new Paint(3);p.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(47);while(p.measureText(txt)>470&&p.getTextSize()>16)p.setTextSize(p.getTextSize()-2);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(Color.BLACK);c.drawText(txt,256,461,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawText(txt,256,461,p);}
      byte[] bytes=null;
      for(int q:new int[]{85,70,55,40,25,12,5}){ByteArrayOutputStream b=new ByteArrayOutputStream();out.compress(Bitmap.CompressFormat.WEBP_LOSSY,q,b);if(b.size()<=100000){bytes=b.toByteArray();break;}}
      if(bytes==null)throw new IOException("Foto terlalu rumit untuk batas WhatsApp");
      File d=folder(),f=new File(d,"sticker_"+System.currentTimeMillis()+".webp");try(FileOutputStream o=new FileOutputStream(f)){o.write(bytes);}
      File tray=new File(d,"tray.png");if(!tray.exists()){Bitmap small=Bitmap.createScaledBitmap(out,96,96,true);try(FileOutputStream o=new FileOutputStream(tray)){small.compress(Bitmap.CompressFormat.PNG,100,o);}small.recycle();}
      out.recycle();getContentResolver().notifyChange(Uri.parse("content://"+AUTH+"/metadata"),null);
      runOnUiThread(()->{showToast("Stiker dibuat");draw();});
    }catch(Exception e){runOnUiThread(()->showToast("Gagal: "+e.getMessage()));}}).start();
  }
}