package com.personal.stickerstudio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

/** Offline segmentation of an object using the bundled Silueta ONNX model. */
public final class AiBackgroundRemover {
  private static final String MODEL="silueta.onnx";
  private static final int SIZE=320;
  private static final float[] MEAN={0.485f,0.456f,0.406f};
  private static final float[] STD={0.229f,0.224f,0.225f};
  private AiBackgroundRemover() {}

  private static File modelFile(Context context)throws Exception{
    File folder=new File(context.getFilesDir(),"ai-models");
    if(!folder.exists()&&!folder.mkdirs())throw new IllegalStateException("Gagal membuat folder model AI.");
    File model=new File(folder,MODEL);
    if(model.isFile()&&model.length()>20000000L)return model;
    File temp=new File(folder,"silueta.tmp");
    try(InputStream in=context.getAssets().open(MODEL);FileOutputStream out=new FileOutputStream(temp)){
      byte[] buffer=new byte[65536];int n;
      while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
    }
    if(temp.length()<20000000L||!temp.renameTo(model))
      throw new IllegalStateException("Model AI tidak ditemukan atau rusak.");
    return model;
  }

  public static Bitmap remove(Context context,Bitmap original)throws Exception{
    if(original==null)throw new IllegalArgumentException("Foto kosong");
    File model=modelFile(context);
    Bitmap small=Bitmap.createScaledBitmap(original,SIZE,SIZE,true);
    int[] colors=new int[SIZE*SIZE];
    small.getPixels(colors,0,SIZE,0,0,SIZE,SIZE);
    if(small!=original)small.recycle();

    ByteBuffer bb=ByteBuffer.allocateDirect(3*SIZE*SIZE*4).order(ByteOrder.nativeOrder());
    FloatBuffer input=bb.asFloatBuffer();
    for(int channel=0;channel<3;channel++){
      int shift=(2-channel)*8;
      for(int i=0;i<SIZE*SIZE;i++){
        float value=((colors[i]>>shift)&255)/255f;
        input.put((value-MEAN[channel])/STD[channel]);
      }
    }
    input.rewind();

    float[][] mask;
    OrtEnvironment env=OrtEnvironment.getEnvironment();
    try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
      options.setIntraOpNumThreads(2);
      try(OrtSession session=env.createSession(model.getAbsolutePath(),options);
          OnnxTensor tensor=OnnxTensor.createTensor(env,input,new long[]{1,3,SIZE,SIZE});
          OrtSession.Result results=session.run(Collections.singletonMap(
            session.getInputNames().iterator().next(),tensor))){
        float[][][][] output=(float[][][][])results.get(0).getValue();
        mask=output[0][0];
      }
    }

    float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
    for(float[] row:mask)for(float v:row){
      if(Float.isFinite(v)){min=Math.min(v,min);max=Math.max(v,max);}
    }
    if(!Float.isFinite(max)||max-min<0.00001f)
      throw new IllegalStateException("AI tidak bisa mendeteksi objek utama.");

    int[] alphaPixels=new int[SIZE*SIZE];
    for(int y=0;y<SIZE;y++)for(int x=0;x<SIZE;x++){
      float normalized=(mask[y][x]-min)/(max-min);
      float soft=Math.max(0f,Math.min(1f,(normalized-0.07f)/0.86f));
      int a=Math.min(255,Math.max(0,Math.round(soft*255f)));
      alphaPixels[y*SIZE+x]=Color.argb(255,a,a,a);
    }
    Bitmap maskSmall=Bitmap.createBitmap(alphaPixels,SIZE,SIZE,Bitmap.Config.ARGB_8888);
    Bitmap maskFull=Bitmap.createScaledBitmap(maskSmall,original.getWidth(),original.getHeight(),true);
    if(maskSmall!=maskFull)maskSmall.recycle();

    int w=original.getWidth(),h=original.getHeight();
    int[] source=new int[w*h],alpha=new int[w*h];
    original.getPixels(source,0,w,0,0,w,h);
    maskFull.getPixels(alpha,0,w,0,0,w,h);
    maskFull.recycle();
    for(int i=0;i<source.length;i++){
      int p=source[i];
      int a=(Color.alpha(p)*Color.red(alpha[i])+127)/255;
      source[i]=(p&0x00ffffff)|(a<<24);
    }
    Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    out.setPixels(source,0,w,0,0,w,h);
    return out;
  }
}