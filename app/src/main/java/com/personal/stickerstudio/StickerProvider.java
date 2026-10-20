package com.personal.stickerstudio;

import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.content.res.AssetFileDescriptor;
import java.io.*;
import java.util.*;

public class StickerProvider extends ContentProvider{
  static final String AUTH="com.personal.stickerstudio.provider";
  static final String STATIC="foto_pribadi", ANIM="animasi_pribadi";
  static final UriMatcher MATCH=new UriMatcher(UriMatcher.NO_MATCH);
  static {
    MATCH.addURI(AUTH,"metadata",1);
    MATCH.addURI(AUTH,"metadata/*",2);
    MATCH.addURI(AUTH,"stickers/*",3);
    MATCH.addURI(AUTH,"stickers_asset/*/*",4);
  }
  static final String[] META={
    "sticker_pack_identifier","sticker_pack_name","sticker_pack_publisher","sticker_pack_icon",
    "android_play_store_link","ios_app_download_link","sticker_pack_publisher_email",
    "sticker_pack_publisher_website","sticker_pack_privacy_policy_website",
    "sticker_pack_license_agreement_website","image_data_version",
    "whatsapp_will_not_cache_stickers","animated_sticker_pack"
  };
  static final String[] ITEMS={"sticker_file_name","sticker_emoji","sticker_accessibility_text"};
  private boolean known(String id){return STATIC.equals(id)||ANIM.equals(id);}
  private File dir(String id){return new File(getContext().getFilesDir(),"stickers/"+id);}
  private File[] files(String id){
    File[] x=dir(id).listFiles((d,n)->n.endsWith(".webp"));
    if(x==null)return new File[0];
    Arrays.sort(x);return x;
  }
  @Override public boolean onCreate(){return true;}
  private void addMeta(MatrixCursor c,String id){
    if(!known(id)||files(id).length<3||!new File(dir(id),"tray.png").isFile())return;
    boolean animation=ANIM.equals(id);
    c.addRow(new Object[]{
      id,animation?"Stiker Bergerak":"Stiker Pribadi","Pribadi","tray.png",
      "","","","","","","1",0,animation?1:0
    });
  }
  @Override public Cursor query(Uri u,String[] projection,String selection,String[] selectionArgs,String order){
    int m=MATCH.match(u);
    if(m==1||m==2){
      MatrixCursor c=new MatrixCursor(META);
      if(m==1){addMeta(c,STATIC);addMeta(c,ANIM);}
      else addMeta(c,u.getLastPathSegment());
      return c;
    }
    if(m==3){
      MatrixCursor c=new MatrixCursor(ITEMS);
      String id=u.getLastPathSegment();
      if(known(id))for(File f:files(id))
        c.addRow(new Object[]{f.getName(),ANIM.equals(id)?"🎬":"😀","Stiker pribadi"});
      return c;
    }
    throw new IllegalArgumentException("URI tidak didukung");
  }
  private File getAsset(Uri uri)throws FileNotFoundException{
    if(MATCH.match(uri)!=4||uri.getPathSegments().size()!=3)throw new FileNotFoundException();
    String id=uri.getPathSegments().get(1),name=uri.getPathSegments().get(2);
    if(!known(id))throw new FileNotFoundException();
    if(!"tray.png".equals(name)){
      boolean matched=false;
      for(File f:files(id))if(f.getName().equals(name)){matched=true;break;}
      if(!matched)throw new FileNotFoundException();
    }
    File result=new File(dir(id),name);
    if(!result.isFile())throw new FileNotFoundException();
    return result;
  }
  @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
    return ParcelFileDescriptor.open(getAsset(uri),ParcelFileDescriptor.MODE_READ_ONLY);
  }
  @Override public AssetFileDescriptor openAssetFile(Uri uri,String mode)throws FileNotFoundException{
    File f=getAsset(uri);
    return new AssetFileDescriptor(ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY),0,f.length());
  }
  @Override public String getType(Uri uri){
    if(MATCH.match(uri)==4)
      return uri.getLastPathSegment().equals("tray.png")?"image/png":"image/webp";
    return "vnd.android.cursor.dir/vnd."+AUTH+".metadata";
  }
  @Override public Uri insert(Uri u,ContentValues values){throw new UnsupportedOperationException();}
  @Override public int delete(Uri u,String where,String[] args){throw new UnsupportedOperationException();}
  @Override public int update(Uri u,ContentValues v,String where,String[] args){throw new UnsupportedOperationException();}
}