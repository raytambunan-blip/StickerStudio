package com.personal.stickerstudio;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * RFC 9649 WebP animation container writer.
 * Android's native Bitmap WebP compressor encodes each independent frame;
 * we mux its ALPH+VP8 / VP8L chunks into VP8X+ANIM+ANMF.
 * No native dependencies and no cloud processing.
 */
public final class AnimatedWebp {
  private AnimatedWebp(){}
  private static final int SIZE=512, MAX_BYTES=500000;
  private static void u24(ByteArrayOutputStream out,int n){
    out.write(n&255);out.write((n>>8)&255);out.write((n>>16)&255);
  }
  private static void u32(ByteArrayOutputStream out,int n){
    out.write(n&255);out.write((n>>8)&255);out.write((n>>16)&255);out.write((n>>24)&255);
  }
  private static int u32(byte[] data,int off){
    return (data[off]&255)|((data[off+1]&255)<<8)|((data[off+2]&255)<<16)|((data[off+3]&255)<<24);
  }
  private static boolean tag(byte[] data,int off,String text){
    if(off<0||off+4>data.length)return false;
    for(int i=0;i<4;i++)if((byte)text.charAt(i)!=data[off+i])return false;
    return true;
  }
  private static void ascii(ByteArrayOutputStream out,String tag){
    for(int i=0;i<4;i++)out.write((byte)tag.charAt(i));
  }
  private static void chunk(ByteArrayOutputStream out,String id,byte[] body){
    ascii(out,id);u32(out,body.length);out.write(body,0,body.length);
    if((body.length&1)!=0)out.write(0);
  }
  private static byte[] encodeImageChunks(Bitmap bitmap,int quality)throws IOException{
    ByteArrayOutputStream encoded=new ByteArrayOutputStream();
    if(!bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY,quality,encoded))
      throw new IOException("Tidak bisa mengompresi frame WebP");
    byte[] raw=encoded.toByteArray();
    if(raw.length<20||!tag(raw,0,"RIFF")||!tag(raw,8,"WEBP"))
      throw new IOException("Frame bukan format WebP");
    ByteArrayOutputStream chunks=new ByteArrayOutputStream();
    int p=12, imageChunks=0, alpha=0;
    while(p+8<=raw.length){
      long size=u32(raw,p+4)&0xffffffffL;
      long next=(long)p+8+size+(size&1);
      if(next>raw.length)throw new IOException("Data frame WebP rusak");
      if(tag(raw,p,"ALPH")){chunks.write(raw,p,(int)(next-p));alpha++;}
      if(tag(raw,p,"VP8 ")||tag(raw,p,"VP8L")){
        chunks.write(raw,p,(int)(next-p));imageChunks++;
      }
      p=(int)next;
    }
    if(imageChunks!=1||alpha>1||chunks.size()<10)throw new IOException("Frame WebP tidak didukung");
    return chunks.toByteArray();
  }
  /** Frame duration >= 100ms (WhatsApp minimum 8ms); <= 10 seconds total. */
  public static byte[] encode(List<Bitmap> frames,int frameDurationMs,int quality)throws IOException{
    if(frames==null||frames.size()<2||frames.size()>60)throw new IOException("Butuh 2 sampai 60 frame");
    if(frameDurationMs<20||(long)frames.size()*frameDurationMs>10000)
      throw new IOException("Durasi animasi tidak valid");
    ByteArrayOutputStream content=new ByteArrayOutputStream();
    ByteArrayOutputStream features=new ByteArrayOutputStream();
    features.write(0x12);u24(features,0);u24(features,SIZE-1);u24(features,SIZE-1);
    chunk(content,"VP8X",features.toByteArray());
    chunk(content,"ANIM",new byte[6]); // transparent background, infinite loop
    for(Bitmap bitmap:frames){
      if(bitmap.getWidth()!=SIZE||bitmap.getHeight()!=SIZE)throw new IOException("Ukuran frame salah");
      byte[] image=encodeImageChunks(bitmap,quality);
      ByteArrayOutputStream frame=new ByteArrayOutputStream();
      u24(frame,0);u24(frame,0);u24(frame,SIZE-1);u24(frame,SIZE-1);
      u24(frame,frameDurationMs);
      frame.write(2); // no blending, do not dispose (full canvas frames)
      frame.write(image,0,image.length);
      chunk(content,"ANMF",frame.toByteArray());
      if(content.size()>MAX_BYTES)throw new IOException("Stiker masih terlalu besar");
    }
    ByteArrayOutputStream result=new ByteArrayOutputStream();
    ascii(result,"RIFF");u32(result,content.size()+4);ascii(result,"WEBP");
    byte[] bytes=content.toByteArray();
    result.write(bytes,0,bytes.length);
    byte[] file=result.toByteArray();
    if(file.length>MAX_BYTES)throw new IOException("Melebihi batas 500 KB WhatsApp");
    return file;
  }
}