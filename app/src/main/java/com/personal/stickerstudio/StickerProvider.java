package com.personal.stickerstudio;

import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.content.res.AssetFileDescriptor;
import java.io.*;
import java.util.*;

public class StickerProvider extends ContentProvider{
  static final String AUTH="com.personal.stickerstudio.provider",ID="foto_pribadi";
  static final UriMatcher MATCH=new UriMatcher(UriMatcher.NO_MATCH);
  static {MATCH.addURI(AUTH,"metadata",1);MATCH.addURI(AUTH,"metadata/*",2);MATCH.addURI(AUTH,"stickers/*",3);MATCH.addURI(AUTH,"stickers_asset/*/*",4);}
  static final String[] META={"sticker_pack_identifier","sticker_pack_name","sticker_pack_publisher","sticker_pack_icon","android_play_store_link","ios_app_download_link","sticker_pack_publisher_email","sticker_pack_publisher_website","sticker_pack_privacy_policy_website","sticker_pack_license_agreement_website","image_data_version","whatsapp_will_not_cache_stickers","animated_sticker_pack"};
  static final String[] ITEMS={"sticker_file_name","sticker_emoji","sticker_accessibility_text"};
  File dir(){return new File(getContext().getFilesDir(),"stickers/"+ID);}
  File[] stickers(){File[] x=dir().listFiles((d,n)->n.endsWith(".webp"));if(x==null)return new File[0];Arrays.sort(x);return x;}
  public boolean onCreate(){return true;}
  public Cursor query(Uri u,String[] p,String s,String[] args,String order){
    int m=MATCH.match(u);if(m==1||m==2){MatrixCursor c=new MatrixCursor(META);if(stickers().length>=3&&(m==1||ID.equals(u.getLastPathSegment())))c.addRow(new Object[]{ID,"Stiker Pribadi","Pribadi","tray.png","","","","","","","1",0,0});return c;}
    if(m==3){MatrixCursor c=new MatrixCursor(ITEMS);if(ID.equals(u.getLastPathSegment()))for(File f:stickers())c.addRow(new Object[]{f.getName(),"😀","Stiker buatan sendiri"});return c;}
    throw new IllegalArgumentException("URI tidak valid");
  }
  File asset(Uri u)throws FileNotFoundException{
    if(MATCH.match(u)!=4||u.getPathSegments().size()!=3||!ID.equals(u.getPathSegments().get(1)))throw new FileNotFoundException();
    String n=u.getPathSegments().get(2);if(!n.equals("tray.png")){boolean ok=false;for(File f:stickers())if(f.getName().equals(n))ok=true;if(!ok)throw new FileNotFoundException();}
    File f=new File(dir(),n);if(!f.isFile())throw new FileNotFoundException();return f;
  }
  @Override public AssetFileDescriptor openAssetFile(Uri u,String mode)throws FileNotFoundException{File f=asset(u);return new AssetFileDescriptor(ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY),0,f.length());}
  @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(asset(u),ParcelFileDescriptor.MODE_READ_ONLY);}
  public String getType(Uri u){return MATCH.match(u)==4?(u.getLastPathSegment().equals("tray.png")?"image/png":"image/webp"):"vnd.android.cursor.dir/vnd."+AUTH+".metadata";}
  public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
  public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
  public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}