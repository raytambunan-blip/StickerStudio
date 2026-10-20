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
    LinearLayout main=new LinearLayout(this);main.setOrientation(1);
    main.setPadding(dp(22),dp(22),dp(22),dp(22));sc.addView(main);
    TextView title=text("✦ STICKER STUDIO",17);title.setTextColor(purple);main.addView(title);
    main.addView(text("Buat stiker tanpa iklan",26));
    main.addView(text("Pilih foto, edit, lalu simpan ke koleksi.",14));
    Button photo=button("＋ Pilih foto untuk diedit");main.addView(photo);
    photo.setOnClickListener(v->{
      Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");
      i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,1);
    });
    File[] files=stickers();main.addView(text("Koleksi stiker: "+files.length+"/30",19));
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
    HorizontalScrollView hs=new HorizontalScrollView(this);hs.addView(row);main.addView(hs);
    for(File f:files){
      ImageView iv=new ImageView(this);iv.setLayoutParams(new LinearLayout.LayoutParams(dp(94),dp(94)));
      iv.setScaleType(ImageView.ScaleType.FIT_CENTER);Bitmap bmp=BitmapFactory.decodeFile(f.getPath());
      if(bmp!=null)iv.setImageBitmap(bmp);row.addView(iv);
      iv.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("Hapus stiker?")
        .setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w)->{f.delete();draw();}).show();return true;});
    }
    Button add=button("Tambahkan ke WhatsApp");add.setEnabled(files.length>=3);main.addView(add);
    add.setOnClickListener(v->{
      Intent i=new Intent("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
      i.putExtra("sticker_pack_id",PACK);i.putExtra("sticker_pack_authority",AUTH);
      i.putExtra("sticker_pack_name","Stiker Pribadi");
      try{startActivityForResult(i,2);}catch(Exception e){showToast("WhatsApp tidak ditemukan");}
    });
    main.addView(text("Minimal 3 stiker untuk ekspor. Tekan lama stiker untuk menghapus.",13));
    main.addView(text("Editor foto v1.1: crop, teks, hapus background warna serupa. Semua offline.",13));
    setContentView(sc);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==1&&result==RESULT_OK&&data!=null&&data.getData()!=null){
      if(stickers().length>=30){showToast("Paket sudah berisi 30 stiker");return;}
      Intent edit=new Intent(this,EditorActivity.class);edit.setData(data.getData());
      edit.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(edit,3);
    }else if(request==3)draw();
  }
}