package com.example.navigation.ui.home;

/**
 * 项目名： Navigation
 * 文件名： HandWrite
 *
 * @Author: Zhihao Shu(Harry)
 * @email: 919900130@qq.com
 * 创建日期：2020 2020/7/22 9:34
 * 描述：
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.navigation.HttpCallBackListener;
import com.example.navigation.MainActivity;
import com.example.navigation.R;
import com.example.navigation.ui.Point;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static android.content.ContentValues.TAG;
import static android.os.FileUtils.copy;


public class HandWrite extends View
{
    Paint paint = null;             //定义画笔
    Bitmap originalBitmap = null;   //存放原始图像
    Bitmap new1_Bitmap = null;      //存放从原始图像复制的位图图像
    Bitmap handwrite_Bitmap = null;      //存放书写
    Bitmap new2_Bitmap = null;      //存放处理后的图像
    float startX = 0,startY = 0;    //画线的起点坐标
    float clickX = 0,clickY = 0;    //画线的终点坐标
    int radius = 20;            //画圈的半径
    boolean isMove = true;      //设置是否画线的标记
    boolean isClick = true;     //是否只是click
    boolean isClear = false;        //设置是否清除涂鸦的标记
    boolean isEraser = false;
    public static int constant = Constant.draw_line; //初始化画图为写line


    public static int width; //图片宽度
    public static int height; //图片高度

    public ArrayList<Point> getPoints() {
        return points;
    }

    ArrayList<Point> points;


    public void setColor(int color) {
        this.color = color;
    }

    int color = Color.GREEN;        //设置画笔的颜色（绿色）
    int penColor = Color.RED;
    float strokeWidth = 2.0f;       //设置画笔的宽度


    public HandWrite(Context context, AttributeSet attrs)
    {
        super(context, attrs);

       // originalBitmap = GetLocalOrNetBitmap("https://blog.foreverlove.us/girl2.png"); //used for 导入图片 Deprecated!

        points = new ArrayList<Point>();
        if(MainActivity.newBitmap != null){
            originalBitmap = null;
            originalBitmap = MainActivity.newBitmap;
            new1_Bitmap = Bitmap.createBitmap(originalBitmap);
            handwrite_Bitmap = Bitmap.createBitmap(originalBitmap);
        }else{
            originalBitmap = null;
            originalBitmap = BitmapFactory
                    .decodeResource(getResources(), R.mipmap.test)
                    .copy(Bitmap.Config.ARGB_8888,true);
            new1_Bitmap = Bitmap.createBitmap(originalBitmap);
            handwrite_Bitmap = Bitmap.createBitmap(originalBitmap);
        }

    }

    public void draw(){
        isClear = false;
    }



    public void clear(){
        isClear = true;
        originalBitmap = scaleBmp(originalBitmap,width,height);
        new2_Bitmap = Bitmap.createBitmap(originalBitmap);
        handwrite_Bitmap = Bitmap.createBitmap(originalBitmap);
        points.clear();
        invalidate();
    }
    public void setStyle(float strokeWidth){
        this.strokeWidth = strokeWidth;
    }
    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        new1_Bitmap = scaleBmp(new1_Bitmap,width,height);

        switch (constant){
            case Constant.draw_line:{
                canvas.drawBitmap(HandWriting(new1_Bitmap), 0, 0,null);
                break;
            }
            case Constant.draw_circle:{
                canvas.drawBitmap(DrawCircle(new1_Bitmap), 0, 0,null);
                break;
            }
            case Constant.draw_lineCircle:{
                canvas.drawBitmap(LineCircle(new1_Bitmap), 0, 0,null);
                break;
            }
            case Constant.eraser:{
                canvas.drawBitmap(Eraser(new1_Bitmap),0,0,null);
                break;
            }
            case Constant.group_circle:{
                canvas.drawBitmap(groupCircle(new1_Bitmap),0,0,null);
            }
        }

    }

    public Bitmap DrawCircle(Bitmap mMap){
        Canvas canvas = null;
        if(isClear) {
            canvas = new Canvas(new2_Bitmap);
        }
        else{
            canvas = new Canvas(mMap);
        }
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        if(isClick)
        {
            if(!Point.checkCollide(points,clickX,clickY)){
                canvas.drawCircle(clickX, clickY,radius, paint);
                points.add(new Point(clickX,clickY,radius));
            }
        }

        if(isClear)
        {

            return new2_Bitmap;
        }
        return mMap;
    }


    public Bitmap LineCircle(Bitmap o_Bitmap)
    {
        Canvas canvas = null;
        if(isClear) {
            canvas = new Canvas(new2_Bitmap);
        }
        else{
            canvas = new Canvas(o_Bitmap);
        }
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);

        if(isMove || isClick)
        {
           if((Point.distance(startX,startY,clickX,clickY) >= 2*radius)){
                Path path = new Path();
                path.moveTo(startX,startY);
                float[] pos = new float[2];
                float[] tan = new float[2];
                Log.d(TAG,clickX + " + " + clickY);
                Log.d(TAG,"start " + startX + " " + startY);
                Log.d(TAG,"dist: " + Point.distance(startX,startY,clickX,clickY));
                path.lineTo(clickX,clickY);
                PathMeasure measure = new PathMeasure();
                measure.setPath(path,false);
                measure.getPosTan(2*radius,pos,tan);
                if((!Point.checkCollide(points,pos[0],pos[1]))){
                    canvas.drawCircle(pos[0], pos[1],radius, paint);
                    points.add(new Point(pos[0],pos[1],radius));
                    startX = pos[0];
                    startY = pos[1];
                    path.moveTo(startX,startY);
                    }
                }
            }

        if(isClear)
        {
            return new2_Bitmap;
        }
        return o_Bitmap;
    }

    public Bitmap Eraser(Bitmap o_Bitmap)
    {
        Canvas canvas = null;
        if(isClear) {
            canvas = new Canvas(new2_Bitmap);
        }
        else{
            canvas = new Canvas(o_Bitmap);
        }
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.TRANSPARENT);
        paint.setStrokeWidth(strokeWidth);
        if(isMove || isClick)
        {
            if(Point.checkWithin(points,clickX,clickY)){
                Point collision = Point.getCollide(points,clickX,clickY);
                //canvas.drawCircle(collision.getX(), collision.getY(),radius, paint);
                points.remove(collision);
            }
        }
        startX = clickX;
        startY = clickY;

        if(isClear)
        {
           // return new2_Bitmap;
            originalBitmap = scaleBmp(originalBitmap,width,height);
            Bitmap map = Bitmap.createBitmap(originalBitmap);
            paint.setColor(color);
            canvas = new Canvas(map);
            for(Point p : points){
                canvas.drawCircle(p.getX(), p.getY(),radius, paint);
            }
            new2_Bitmap = map;
            return map;
        }
        if(true){
            originalBitmap = scaleBmp(originalBitmap,width,height-10);
            Bitmap map = Bitmap.createBitmap(originalBitmap);
            paint.setColor(color);
            canvas = new Canvas(map);
            for(Point p : points){
                canvas.drawCircle(p.getX(), p.getY(),radius, paint);
            }
            o_Bitmap = map;
            return  o_Bitmap;
        }

        return o_Bitmap;
    }


    /**
     * get a group of circles according to click point and start point
     * @param o_Bitmap
     * @return
     */
    public Bitmap groupCircle(Bitmap o_Bitmap)
    {
        Canvas canvas = null;
        if(isClear) {
            canvas = new Canvas(new2_Bitmap);
        }
        else{
            canvas = new Canvas(o_Bitmap);
        }
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        if(isMove || isClick)
        {
            if((Point.distance(startX,startY,clickX,clickY) >= 2*radius)) {
                Path path = new Path();
                path.moveTo(startX, startY);
                float[] pos = new float[2];
                float[] tan = new float[2];
                path.lineTo(clickX, clickY);
                PathMeasure measure = new PathMeasure();
                measure.setPath(path, false);
                measure.getPosTan(2 * radius, pos, tan);
                if ((!Point.checkCollide(points, pos[0], pos[1]))) {
                    double degree = Math.atan2(tan[0],tan[1])*180/Math.PI;
                    if(degree < 0){
                        degree += 360;
                    }
                    Log.i(TAG,"degree:  " + degree);
                    canvas.drawCircle(pos[0], pos[1], radius, paint);
                    points.add(new Point(pos[0], pos[1], radius));
                    path.moveTo(startX, startY);
                    Point high = calcHigh(startX,startY,pos[0],pos[1],radius);
                    Point low = calcLow(startX,startY,pos[0],pos[1],radius);
                    if(!Point.checkCollide(points,high.getX(),high.getY())) {
                        canvas.drawCircle(high.getX(), high.getY(), radius, paint);
                        points.add(high);
                    }
                    if(!Point.checkCollide(points,low.getX(),low.getY())){
                        canvas.drawCircle(low.getX(),low.getY(),radius,paint);  //-----------------------------------------------------
                        points.add(low);
                    }
                    startX = pos[0];
                    startY = pos[1];
                }
            }
        }
        if(isClear)
        {
            return new2_Bitmap;
        }
        return o_Bitmap;
    }


    private Point calcLow(float startX, float startY, float endX, float endY, int radius){
        int distance = 2*radius;
        int len = 2*radius;
        float dx = endX - startX;
        float dy = endY - startY;
        float perpx = -dy;
        float perpy = dx;
        float newX = endX - perpx;
        float newY = endY - perpy;
        return new Point(newX,newY,radius);
    }

    private Point calcHigh(float startX, float startY, float endX, float endY, int radius){
        int distance = 2*radius;
        int len = 2*radius;
        float dx = endX - startX;
        float dy = endY - startY;
        float perpx = -dy;
        float perpy = dx;
        float newX = endX + perpx;
        float newY = endY + perpy;
        return new Point(newX,newY,radius);
    }

    public Bitmap HandWriting(Bitmap o_Bitmap)
    {
        Canvas canvas = null;
        if(isClear) {
            canvas = new Canvas(new2_Bitmap);
        }
        else{
            canvas = new Canvas(o_Bitmap);
        }
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(penColor);
        paint.setStrokeWidth(strokeWidth);
        if(isMove)
        {
            canvas.drawLine(startX, startY, clickX, clickY, paint);
        }
        startX = clickX;
        startY = clickY;
        if(isClear)
        {
            return new2_Bitmap;
        }
        return o_Bitmap;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        clickX = event.getX();
        clickY = event.getY();
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            isMove = false;
            isClick = true;
            isEraser = true;
            startX = clickX;
            startY = clickY;
            invalidate();
            return true;
        }
        else if(event.getAction() == MotionEvent.ACTION_MOVE)
        {
            isMove = true;
            isClick = false;
            isEraser = true;
            invalidate();
            return true;
        }else if(event.getAction() == MotionEvent.ACTION_UP){
            isClick = false;
            isMove = false;
            isEraser = false;
        }
        return super.onTouchEvent(event);
    }


    /**
     * 按新的宽高缩放图片
     *
     * @param bm
     * @param newWidth
     * @param newHeight
     * @return
     */
    private Bitmap scaleBmp(Bitmap bm, int newWidth, int newHeight)
    {
        if (bm == null)
        {
            return null;
        }
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        if (bm != null & !bm.isRecycled() && bm != originalBitmap)
        {
            bm.recycle();
            bm = null;
        }
        return newbm;
    }

    /**
     * 得到本地或者网络上的bitmap url - 网络或者本地图片的绝对路径,比如:
     *
     * A.网络路径: url=&quot;http://blog.foreverlove.us/girl2.png&quot; ;
     *
     * B.本地路径:url=&quot;file://mnt/sdcard/photo/image.png&quot;;
     *
     * C.支持的图片格式 ,png, jpg,bmp,gif等等
     *
     * @param url
     * @return
     */
    public static Bitmap GetLocalOrNetBitmap(String url)

    {
        Bitmap bitmap = null;
        InputStream in = null;
        BufferedOutputStream out = null;
        try
        {
            in = new BufferedInputStream(new URL(url).openStream(), Constant.IO_BUFFER_SIZE);
            final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            out = new BufferedOutputStream(dataStream, Constant.IO_BUFFER_SIZE);
            copy(in, out);
            out.flush();
            byte[] data = dataStream.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            data = null;
            return bitmap;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

}
