package com.example.navigation.ui.home;

/**
 * 项目名： Navigation
 * 文件名： HandWrite
 *
 * @Author: Zhihao Shu(Harry)
 * @email: 919900130@qq.com / shu@brightview-tech.com
 * 创建日期：2020 2020/7/22 9:34
 * 描述：修改于 2021/08/04
 */
import android.content.Context;
import android.content.SyncStats;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.RegionIterator;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.example.navigation.MainActivity;
import com.example.navigation.MyViewModel;
import com.example.navigation.R;
import com.example.navigation.ui.Forb;
import com.example.navigation.ui.Point;


import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import java.net.URL;
import java.util.ArrayList;


import static android.content.ContentValues.TAG;
import static android.os.FileUtils.copy;
public class HandWrite extends View {
    private Paint paint = null;                                         //定义画笔
    private Bitmap originalBitmap = null;                               //存放原始图像
    private Bitmap new1_Bitmap = null;                                  //存放从原始图像复制的位图图像
    private Bitmap new2_Bitmap = null;                                  //存放处理后的图像
    float startX = 0, startY = 0, startPathX = 0, startPathY;           //画线的起点坐标
    float clickX = 0, clickY = 0;                                       //画线的终点坐标
    int radius = 10;                                                    //画圈的半径
    int gap = 5;                                                        //圆圈的间隔
    int ring_radius = 100;                                              //ring 的半径
    int fRadius;                                                        //Forbs Radius
    boolean isMove = true;                                              //设置是否画线的标记
    boolean isClick = true;                                             //是否只是click
    boolean isClear = false;                                            //设置是否清除涂鸦的标记
    boolean isUp = false;                                               //判断手指是否抬起
    boolean isPress = false;                                            //判断是否按压 当前小新平板不支持3d touch
    int color = Color.GREEN;                                            //设置画笔的颜色（绿色）
    int ring_color = Color.BLUE;                                        //设置圆圈预测位置 （蓝色）
    int path_color = Color.RED;                                         //设置path的颜色 （红色）
    float strokeWidth = 2.0f;                                           //设置画笔的宽度
    public static int width;                                            //screen 宽度
    public static int height;                                           //screen 高度
    private int pic_width;                                              //picture height
    private int pic_height;                                             //picture width
    public static int constant = Constant.draw_line;                    //初始化画图为写line

    //Forb values
    Drawable aforb;
    Bitmap BMforb;
    int actualSzieOnScreen = 0;
    Resources r;

    //for path drawing
    private Path pathDraw;
    private Path totalPath;
    private int firstClick = 0;
    private double slop = 0;
    ArrayList<PointF> pathPoints = new ArrayList<PointF>();

    //for image scaling and moving
    private boolean firstCenter = true;
    private int fingers = 0;
    private float[] firstFinger = new float[2];
    private float[] secondFinger = new float[2];
    private float scale_ratio = 1.0f;
    private float temp_scale_ratio = 1.0f;
    private int initialDist = 0;
    private float[] scale_center = new float[2];
    private int pic_locationX;
    private int pic_locationY;
    private float moveDx;
    private float moveDy;
    private float[] startMove = new float[2];
    private int scaleWidth;
    private int scaleHeight;
    private ScaleRatioVR scaleRatioVR = Constant.scaleRatioVR;

    //Define forb list and points list
    private ArrayList<Point> points;
    private ArrayList<Forb> forbs;

    //getter and setter functions
    public Bitmap getOriginalBitmap() {
        return originalBitmap;
    }
    public Bitmap getNew1_Bitmap() {
        return new1_Bitmap;
    }
    public ArrayList<Forb> getForbs() {
        return forbs;
    }
    public void setPoints(ArrayList<Point> points) {
        this.points = points;
    }
    public void setForbs(ArrayList<Forb> forbs) {
        this.forbs = forbs;
    }
    public ArrayList<Point> getPoints() {
        return points;
    }
    public void setColor(int color) {
        this.color = color;
    }
    public void setStyle(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public HandWrite(Context context, AttributeSet attrs) {
        super(context, attrs);
        points = new ArrayList<Point>();
        forbs = new ArrayList<Forb>();
        if ( MainActivity.newBitmap != null) {
            Log.i(TAG,"Get new bitmap from gallery success!");
            originalBitmap = null;
            originalBitmap = MainActivity.newBitmap;
            new1_Bitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            if(MainActivity.newPoints != null){
                this.points.clear();
                for(Point p : MainActivity.newPoints){
                   this.points.add(p);
                }
                Log.i(TAG, "Add points from gallery!  " + points.size());
            }
            if(MainActivity.newForbs != null){
                this.forbs.clear();
                for(Forb f : MainActivity.newForbs){
                    this.forbs.add(f);
                }
                Log.i(TAG, "Add forbs from gallery!  " + forbs.size());
            }
            new1_Bitmap = newRedraw();
        } else {
            originalBitmap = null;
            originalBitmap = BitmapFactory
                    .decodeResource(getResources(), R.mipmap.test)
                    .copy(Bitmap.Config.ARGB_8888, true);
            new1_Bitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
            Log.i(TAG, "Load default image");
        }

        //initialize height and width
        pic_height = originalBitmap.getHeight();
        pic_width = originalBitmap.getWidth();
        scaleWidth = originalBitmap.getWidth();
        scaleHeight = originalBitmap.getHeight();

        //set forb size to new forb size
        this.fRadius = Constant.getForbSize();
        this.gap = Constant.getGap();
        this.radius = Constant.getPRadius();

        //get Forb image from drawable
        aforb = getResources().getDrawable(R.drawable.ic_forb_c);
        BMforb = drawableToBitmap(aforb);
        Log.i(TAG,"intrinsic width  " + BMforb.getHeight() + " xxx " + fRadius + "  " + BMforb.getHeight() / fRadius);
        r = getResources();
        int px = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, fRadius, r.getDisplayMetrics()));
        int ratio = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()));
        actualSzieOnScreen = px*ratio;
        BMforb = scaleBmp(BMforb, actualSzieOnScreen,actualSzieOnScreen);
        Log.i(TAG, "pxxx:  " + px + "  px*ratio" + px*ratio);

        //for path drawing
        pathDraw = new Path();
        totalPath = new Path();
    }

    public void draw() {
        isClear = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        pic_locationX = (int) (moveDx);
        pic_locationY = (int) (moveDy);
        //Log.i(TAG,"click " + clickX + "   " + clickY + " scale: " + scale_ratio);
        //System.out.println("width picwidth*scale movedx  " + width + " " + pic_width*scale_ratio + "  " + moveDx  + " " + pic_locationX);
        //System.out.println("height picheight*scale movedy  " + height + " " + pic_height*scale_ratio + "  " + moveDy  + " " + pic_locationY);
        switch (constant) {
            case Constant.draw_scale: {
                canvas.drawBitmap(scale(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.draw_circle: {
                this.radius = (int) (Constant.getPRadius() * scale_ratio);
                canvas.drawBitmap(DrawCircle(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.draw_path: {
                if(firstClick == 0){
                    new2_Bitmap = new1_Bitmap.copy(new1_Bitmap.getConfig(),true);
                }
                this.gap = (int) (Constant.getGap() * scale_ratio);
                this.radius = (int) (Constant.getPRadius() * scale_ratio);
                canvas.drawBitmap(path(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.eraser: {
                updateBMForb();
                canvas.drawBitmap(Eraser(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.group_circle: {
                this.gap = (int) (Constant.getGap() * scale_ratio);
                this.radius = (int) (Constant.getPRadius() * scale_ratio);
                canvas.drawBitmap(groupCircle(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.ring_circle: {
                this.gap = (int) (Constant.getGap() * scale_ratio);
                this.radius = (int) (Constant.getPRadius() * scale_ratio);
                this.ring_radius = (int) (Constant.getForbSize() * scale_ratio);
                canvas.drawBitmap(ring(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.draw_forb: {
                updateBMForb();
                canvas.drawBitmap(forb(), pic_locationX, pic_locationY, null);
                break;
            }
            case Constant.delete: {
                canvas.drawBitmap(delete(), pic_locationX, pic_locationY, null);
                break;
            }
            default: {
                canvas.drawBitmap(new1_Bitmap,pic_locationX,pic_locationY,null);
                Log.e(TAG, "Error when handle events!");
                break;
            }
        }
    }


    /**
     * clear the current screen including points and (forbs)
     */
    public void clear() {
        isClear = true;
        points.clear();
        forbs.clear();
        //reset path
        pathDraw = new Path();
        totalPath = new Path();
        invalidate();
    }

    /**
     * update Frob Bitmap size
     *
     */
    private void updateBMForb(){
        this.fRadius = (int) (Constant.getForbSize() * scale_ratio);
        aforb = getResources().getDrawable(R.drawable.ic_forb_c);
        BMforb = drawableToBitmap(aforb);
        int px = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, fRadius, r.getDisplayMetrics()));
        int ratio = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()));
        actualSzieOnScreen = (int) (px*ratio*scale_ratio);
        BMforb = scaleBmp(BMforb, actualSzieOnScreen,actualSzieOnScreen);
        Log.i(TAG, "change forb size ==  " + fRadius + "  px*ratio" + px*ratio);
    }

    /**
     * clear the current screen
     *
     * @return
     */
    private Bitmap delete()
    {
        new1_Bitmap.recycle();
        new1_Bitmap = originalBitmap.copy(originalBitmap.getConfig(), true);
        if (new2_Bitmap != null){
            new2_Bitmap = originalBitmap.copy(originalBitmap.getConfig(),true);
        }
            new1_Bitmap = scaleBmp(new1_Bitmap, (int) (originalBitmap.getWidth() * scale_ratio), (int) (originalBitmap.getHeight() * scale_ratio));
            new2_Bitmap = scaleBmp(new2_Bitmap, (int) (originalBitmap.getWidth() * scale_ratio), (int) (originalBitmap.getWidth() * scale_ratio));
        return newRedraw();
    }

    /**
     *  update the new1 bitmap according to points and forbs
     * @return updated Bitmap
     */
    private Bitmap redraw(){
        new1_Bitmap.recycle();
        new1_Bitmap = originalBitmap.copy(originalBitmap.getConfig(),true);
        new1_Bitmap = scaleBmp(new1_Bitmap, (int) (pic_width * scale_ratio), (int) (pic_height * scale_ratio));
        Canvas canvas = null;
        canvas = new Canvas(new1_Bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        aforb = getResources().getDrawable(R.drawable.ic_forb_c);
        BMforb = drawableToBitmap(aforb);
        for(Point p : points){
            canvas.drawCircle(p.getX() ,p.getY() ,p.getRadius() ,paint);
        }
        //remove overlap forbs
        ArrayList<Forb> tempForbs = (ArrayList<Forb>) forbs.clone();
        for(Forb f : tempForbs){
            if(f.checkWithin(forbs,f.getX(),f.getY(),f.getRadius())){
                if(!f.equals(Forb.getCollide(forbs, f.getX(),f.getY()))) {
                    forbs.remove(Forb.getCollide(forbs, f.getX(), f.getY()));
                }
            }
        }
        for(Forb ff: forbs){
            int px = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, ff.getRadius(), r.getDisplayMetrics()));
            int ratio = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()));
            actualSzieOnScreen = (int) (px*ratio);
            BMforb = scaleBmp(BMforb, actualSzieOnScreen,actualSzieOnScreen);
          canvas.drawBitmap(BMforb,ff.getX()-actualSzieOnScreen/2.0f, ff.getY()-actualSzieOnScreen/2.0f,null);
        }
        return new1_Bitmap;
    }

    /**
     * redraw the bitmap from the gallery
     * @return
     */
    private Bitmap newRedraw(){
        r = getResources();
        Canvas canvas = null;
        canvas = new Canvas(new1_Bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        aforb = getResources().getDrawable(R.drawable.ic_forb_c);
        BMforb = drawableToBitmap(aforb);
        for(Point p : points){
            canvas.drawCircle(p.getX() ,p.getY() ,p.getRadius() ,paint);
        }
        //remove overlap forbs
        ArrayList<Forb> tempForbs = (ArrayList<Forb>) forbs.clone();
        for(Forb f : tempForbs){
            if(f.checkWithin(forbs,f.getX(),f.getY(),f.getRadius())){
                if(!f.equals(Forb.getCollide(forbs, f.getX(),f.getY()))) {
                    forbs.remove(Forb.getCollide(forbs, f.getX(), f.getY()));
                }
            }
        }
        for(Forb ff: forbs){
            int px = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, ff.getRadius(), r.getDisplayMetrics()));
            int ratio = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 1, r.getDisplayMetrics()));
            actualSzieOnScreen = (int) (px*ratio);
            BMforb = scaleBmp(BMforb, actualSzieOnScreen,actualSzieOnScreen);
            canvas.drawBitmap(BMforb,ff.getX()-actualSzieOnScreen/2.0f, ff.getY()-actualSzieOnScreen/2.0f,null);
        }
        return new1_Bitmap;
    }

    /**
     * draw forbs on the bitmap
     *
     * @return drawn bitmap
     */
    private Bitmap forb() {
        Forb f;
        float actualX = clickX - moveDx;
        float actualY = clickY - moveDy;
        if(isClick ){
            if(forbs == null){
                f = new Forb(actualX, actualY, fRadius);
                f.setOriginalX(actualX / scale_ratio);
                f.setOriginalY(actualY / scale_ratio);
                f.setOriginalRadius((int) (fRadius / scale_ratio));
                forbs.add(f);
            }
            else if (!Forb.checkCollideF(forbs, actualX, actualY, fRadius)) {
                f = new Forb(actualX,actualY,fRadius);
                f.setOriginalX(actualX / scale_ratio);
                f.setOriginalY(actualY / scale_ratio);
                f.setOriginalRadius((int) (fRadius / scale_ratio));
                forbs.add(f);
            }
            if(Forb.checkWithin(forbs, actualX, actualY, 0)){
                f = Forb.getCollide(forbs, actualX, actualY);
                f.setSelected(true);
            }
        }
        else if(isUp){
            setFF(); //reset all forbs to not selected
            for(Forb fbs: forbs){
               if(Forb.checkCollidePF(points, fbs.getX(), fbs.getY(), fbs.getRadius())){
                   //remove those points that collides with forbs
               }
            }
        }else{
                f = Forb.getCollide(forbs, actualX, actualY);
                if(f != null && f.isSelected()){
                    forbs.remove(f);
                    f = new Forb(actualX, actualY, fRadius);
                    f.setSelected(true);
                    f.setOriginalX(actualX / scale_ratio);
                    f.setOriginalY(actualY / scale_ratio);
                    f.setOriginalRadius((int) (fRadius / scale_ratio));
                    forbs.add(f);
                }
        }
        return redraw();
    }

    /**
     * set forbs selected all to false;
     */
    public void setFF(){
        for(Forb f: forbs){
            f.setSelected(false);
        }
    }

    /**
     * set forbs clicked to 0
     */
    public void resetClick(){
        for(Forb f: forbs){
            f.setClicked(0);
        }
    }

    /**
     * draw the bitmap by openCV currently unused !!!
     *
     * @param res
     * @param clickx
     * @param clicky
     * @param radius
     * @return
     */
    private Bitmap cv_DrawCircle(Bitmap res, float clickx, float clicky, int radius) {
        Mat source = new Mat();
        Utils.bitmapToMat(res, source);
        org.opencv.core.Point p = new org.opencv.core.Point(clickx, clicky);
        Scalar cv_color = new Scalar(0, 255, 0, 128); //circle color --green
        Imgproc.circle(source, p, radius, cv_color, (int) strokeWidth, Imgproc.LINE_AA, 4);
        Utils.matToBitmap(source, res);
        return res;
    }

    /**
     * draw a single circle on the bitmap
     *
     * @return drawn bitmap
     */
    public Bitmap DrawCircle() {
        new2_Bitmap = new1_Bitmap.copy(new1_Bitmap.getConfig(),true);
        Canvas canvas2 = null;
        canvas2 = new Canvas(new2_Bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(ring_color);
        paint.setStrokeWidth(strokeWidth);
        float actualX = clickX - moveDx;
        float actualY = clickY - moveDy;
        if(isClick || isMove){
            canvas2.drawLine(actualX,actualY,actualX + 80, actualY, paint);
            canvas2.drawLine(actualX,actualY,actualX - 80, actualY, paint);
            canvas2.drawLine(actualX,actualY,actualX, actualY + 80, paint);
            canvas2.drawLine(actualX,actualY,actualX , actualY - 80, paint);
            return new2_Bitmap;
        }
        else if(isUp) {
            Canvas canvas = null;
            canvas = new Canvas(new1_Bitmap);
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(color);
            paint.setStrokeWidth(strokeWidth);
            if (!Point.checkCollide(forbs, points, actualX, actualY, radius, radius)) {
                canvas.drawCircle(actualX, actualY, radius, paint);
                //cv_DrawCircle(mMap,clickX,clickY,radius);
                Point p = new Point(actualX, actualY ,radius);
                p.setOriginalX(actualX/scale_ratio);
                p.setOriginalY(actualY/scale_ratio);
                p.setOriginalRadius((int) (radius / scale_ratio));
                points.add(p);
            }
        }
        return new1_Bitmap;
    }

    /**
     * helper function for draw a group of circle in a defined region /currently unused
     *
     * @return
     */
    public float[] controlPoint(float x, float y, float endx, float endy){
        float[] res = new float[2];
        float dx = Math.abs(endx - x);
        float dy = Math.abs(endy - y);
       int p = calcPhase(x,y,endx,endy);
       int ratio = 4;
       switch(p){
           case 1:
               res[0] = endx + ratio * dx;
               res[1] = endy - ratio * dy;
               break;
           case 2:
               res[0] = endx - ratio * dx;
               res[1] = endy - ratio * dy;
               break;
           case 3:
               res[0] = endx - ratio * dx;
               res[1] = endy + ratio * dy;
               break;
           case 4:
               res[0] = endx + ratio * dx;
               res[1] = endy + ratio * dy;
               break;
       }
        return res;
    }

    /**
     *  helper function to calc relative phase
     * @param startx
     * @param starty
     * @param endx
     * @param endy
     * @return
     */
    public int calcPhase(float startx, float starty, float endx, float  endy){
        if(endx >= startx && endy <= starty){
            return 1;
        }else if(endx < startx && endy <= starty){
            return 2;
        }else if(endx < startx && endy >= starty){
            return 3;
        }else{
            return 4;
        }
    }

    /**
     * helper function to calc the slope of two points
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public float calcSlope(float x1, float y1,
                       float x2, float y2)
    {
        return (y2 - y1) / (x2 - x1);
    }

    /**
     * draw a group of points within a region
     * @return drawn bitmap
     */
    private Bitmap path() {
        Canvas canvas = null;
        canvas = new Canvas(new2_Bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(path_color);
        paint.setStrokeWidth(strokeWidth);
        Path path = new Path();
        if(isClick){
            firstClick++;
        }
        if(firstClick == 1) { //first click set start point
            pathPoints.add(new PointF(startPathX  - moveDx, startPathY -moveDy));
            path.moveTo(startX - moveDx ,startY - moveDy);
        }
        if(isClick || isMove){
            firstClick++;
            pathPoints.add(new PointF(clickX - moveDx,clickY - moveDy));
            path.moveTo(startX -moveDx , startY - moveDy);
            path.lineTo(clickX - moveDx ,clickY - moveDy );
            canvas.drawPath(path, paint);
            startX = clickX;
            startY = clickY;
        }
        if(isUp){
            firstClick = 0;
            pathPoints.add(new PointF(clickX - moveDx,clickY - moveDy));
            for(int i = 0; i < pathPoints.size() - 1; i++){
                pathDraw.moveTo(pathPoints.get(i).x,pathPoints.get(i).y);
                pathDraw.lineTo(pathPoints.get(i+1).x,pathPoints.get(i+1).y);
                totalPath.addPath(pathDraw);
            }
            double lastX = pathPoints.get(pathPoints.size()-1).x;
            double lastY = pathPoints.get(pathPoints.size()-1).y;
            pathDraw.moveTo((float) lastX,(float) lastY);
            pathDraw.lineTo(pathPoints.get(0).x, pathPoints.get(0).y);
            totalPath.addPath(pathDraw);
            totalPath.close();
            //add points inside the polygon
            RectF rect = new RectF();
            totalPath.computeBounds(rect,true);
            canvas.drawPath(totalPath,paint);
            canvas.drawRect(rect,paint);
            path_addPoint(canvas, paint, rect);
            pathPoints.clear();
            pathDraw.reset();
            totalPath.reset();
            new2_Bitmap.recycle();
            return redraw();
        }
        return new2_Bitmap;
    }

    /**
     * add points into the defined area
     * @param canvas
     * @param paint
     * @param rect
     */
    private void path_addPoint(Canvas canvas, Paint paint, RectF rect){
       float leftX = rect.left;
       float leftY = rect.top;
       float rightX = rect.right;
       float rightY = rect.bottom;
       int n_row = (int) ((rightX - leftX) / 2 / (radius + gap));
       int n_col = (int) ((rightY - leftY) / 2 / (radius + gap));

       PointF[] pointFs = new PointF[pathPoints.size()];
       for(int i = 0; i < pathPoints.size(); i++){
           pointFs[i] = pathPoints.get(i);
       }
       for(int i = 1; i < n_row + 1; i++){
           for(int j = 1; j < n_col + 1; j++){
               float px = leftX + i * 2 * (radius + gap);
               float py = leftY + j * 2 * (radius + gap);
               PointF point = new PointF(px, py);
               if(isInPolygon(point, pointFs, pathPoints.size()) && !Point.checkCollide(forbs,points,px,py,radius, radius)){
                   Point pp = new Point(point.x, point.y,radius);
                   pp.setOriginalX(point.x / scale_ratio);
                   pp.setOriginalY(point.y / scale_ratio);
                   pp.setOriginalRadius((int) (radius / scale_ratio));
                   points.add(pp);
                   Point p = new Point(px, py, radius);
                   p.setOriginalX(px / scale_ratio);
                   p.setOriginalY(py / scale_ratio);
                   p.setOriginalRadius((int) (radius / scale_ratio));
                   points.add(p);
                   canvas.drawCircle(px, py, radius, paint);
               }
           }
       }
    }

    /**
     * helper function to calc a point if it's inside a polygon
     * @param point
     * @param points
     * @param n
     * @return
     */
    public boolean isInPolygon(PointF point, PointF[] points, int n) {
        int nCross = 0;
        for (int i = 0; i < n; i++) {
            PointF p1 = points[i];
            PointF p2 = points[(i + 1) % n];
            // 求解 y=p.y 与 p1 p2 的交点
            // p1p2 与 y=p0.y平行
            if (p1.y == p2.y)
                continue;
            // 交点在p1p2延长线上
            if (point.y < Math.min(p1.y, p2.y))
                continue;
            // 交点在p1p2延长线上
            if (point.y >= Math.max(p1.y, p2.y))
                continue;
            // 求交点的 X 坐标
            double x = (double) (point.y - p1.y) * (double) (p2.x - p1.x)
                    / (double) (p2.y - p1.y) + p1.x;
            // 只统计单边交点
            if (x > point.x)
                nCross++;
        }
        return (nCross % 2 == 1);
    }


    /**
     * scaling image
     *
     * @return Scaled Image
     */
    private Bitmap scale() {
        if(isClick && fingers == 2 && firstCenter){
            initialDist = distance(firstFinger[0],firstFinger[1],secondFinger[0],secondFinger[1]);
            float centerX = firstFinger[0] + (secondFinger[0] - firstFinger[0])/2;
            float centerY = firstFinger[1] + (secondFinger[1] - firstFinger[1])/2;
            scale_center[0] = centerX;
            scale_center[1] = centerY;
            firstCenter = false;
        }else if((isClick || isMove) && fingers == 1 && firstClick == 0){
            firstClick++;
            startMove[0] = clickX;
            startMove[1] = clickY;
        }
        if(isMove && fingers == 2){
            firstCenter = false;
            int offDist = distance(firstFinger[0],firstFinger[1],secondFinger[0],secondFinger[1]);
            int diff = offDist - initialDist;
            if(diff >= 20 && temp_scale_ratio <= 2.0f){
                temp_scale_ratio += 0.05f;
                initialDist = offDist;
            }else if(diff <= -20 && temp_scale_ratio > 0.25f){
                temp_scale_ratio -= 0.05f;
                initialDist = offDist;
            }
            if(temp_scale_ratio >= 0.80f && temp_scale_ratio <= 1.20f){
                scale_ratio = 1.0f;
            }else{
                scale_ratio = temp_scale_ratio;
            }
            scaleRatioVR.setScale(scale_ratio);

            //change previous points and forbs location
            for(Point p : points){
                p.setX(p.getOriginalX() * scale_ratio);
                p.setY(p.getOriginalY() * scale_ratio);
                p.setRadius((int) (p.getOriginalRadius() * scale_ratio));
            }
            for(Forb f : forbs){
                f.setX(f.getOriginalX() * scale_ratio);
                f.setY(f.getOriginalY() * scale_ratio);
                f.setRadius((int) (f.getOriginalRadius() * scale_ratio));
            }
        }else if(isMove && fingers == 1 && firstClick != 0){
            //move pic as desired
            if((pic_locationX > (-1.5 * width)) && (pic_locationX < 1.0 * width)){
                float diff = (clickX - startMove[0]);
                    moveDx += diff;
            }else if(pic_locationX <= (-1.5 * width)){
                moveDx += Math.abs((clickX - startMove[0])/2);
            }else if(pic_locationX >= 1.0 * width){
                moveDx -= Math.abs((clickX - startMove[0])/2);
            }

            if((pic_locationY > (-1.5 * height)) && (pic_locationY < 1.0 * height)){
                float diff = (clickY - startMove[1]);
                    moveDy += diff;
            }
            else if(pic_locationY <= (-1.5 * height)){
                moveDy += Math.abs((clickY - startMove[1])/2);
            }else if(pic_locationY >= 1.0 * height){
                moveDy -= Math.abs((clickY - startMove[1])/2);
            }
            float dx = clickX - startMove[0];
            float dy = clickY - startMove[1];
            double angle = Math.atan2(dy, dx) * 180 / Math.PI;
            if(angle < 0){
                angle += 360;
            }
            startMove[0] = clickX;
            startMove[1] = clickY;
        }
        if(isUp){
            firstClick = 0;
            firstCenter = true;
        }
        return redraw();
    }

    /**
     *  helper function to calculate two points' distance
     * @param x
     * @param y
     * @param xx
     * @param yy
     * @return
     */
    public int distance(float x, float y, float xx, float yy){
        float xiel = (x - xx);
        float yiel = y - yy;
        int res = 0;
        res = (int) Math.sqrt(Math.pow(xiel,2) + Math.pow(yiel,2));
        return res;
    }


    /**
     * clear points and forbs according to its encounter location on the bitmap
     *
     * @return
     */
    public Bitmap Eraser() {
        int size = 5;
        if (isMove || isClick) {
            if (Point.checkWithin_new(points, clickX - moveDx, clickY - moveDy, size)) {
                Log.i(TAG,"remove points ClickX ClickY : " + (clickX - moveDx) + " " + (clickY - moveDy));
            }
        }
        else if(isUp){
            if(Forb.checkWithin(forbs,clickX - moveDx,clickY - moveDy,15)){
                Forb f = Forb.getCollide(forbs,clickX - moveDx,clickY - moveDy);
                f.addClick();
                Log.i(TAG,"add Click  " + f.getX() + " | " + f.getY() + "   " + f.getClicked());
                if(f.getClicked() >= 2){
                    forbs.remove(f);
                    Log.i(TAG,"remove forbs : " + f.getX() + " " + f.getY());
                }
            }else{
                resetClick();
            }
        }
        startX = clickX;
        startY = clickY;
        return redraw();
    }


    /**
     * get a ring shape of circles
     *
     * @return drawn bitmap
     */
    private Bitmap ring() {
        new2_Bitmap = new1_Bitmap.copy(new1_Bitmap.getConfig(),true);
        Canvas canvas2 = null;
        canvas2 = new Canvas(new2_Bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(ring_color);
        paint.setStrokeWidth(strokeWidth);
        if(isClick || isMove){
            canvas2.drawCircle(clickX - moveDx, clickY - moveDy, ring_radius,paint);
            return new2_Bitmap;
        }
        else if(isUp){
            Canvas canvas = null;
            canvas = new Canvas(new1_Bitmap);
            paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setStrokeWidth(strokeWidth);
            float actualX = clickX - moveDx;
            float actualY = clickY - moveDy;
            Path path = new Path();
            path.moveTo(actualX,actualY);

            float ringL = (float) (2 * Math.PI * ring_radius);
            float tempL = 2 * radius + gap;
            float tempR = 2 * ring_radius;
            float tempRingRadius = ring_radius;
            float tempRingL = (float) (2 * Math.PI * tempRingRadius);
            float L = (float) (2 * ring_radius * Math.asin(tempL / tempR)) ;
            while( L != 0 && tempRingL % L >= 0.1){
                tempRingRadius += 0.01;
                tempRingL = (float) (2 * Math.PI * tempRingRadius);
                //System.out.println("ring !!!!!!!!!!! " + L + "  " + tempRingL + "  " + tempRingL%L);
            }
            int Ltotal = 0;
            int n = 0;
            float tempLength = L;
            path.addCircle(actualX, actualY, tempRingRadius, Path.Direction.CW);
            PathMeasure measure = new PathMeasure();
            measure.setPath(path, true);
            Log.i(TAG,tempRingL +  tempRingL % tempLength  + " ------------ " + tempLength);
            while(Ltotal < ringL){
                float[] pos = new float[2];
                float[] tan = new float[2];
                measure.getPosTan(n*(tempLength), pos, tan);
                if ((!Point.checkCollide(forbs, points, pos[0], pos[1],radius,radius))) {
                    canvas.drawCircle(pos[0], pos[1], radius, paint);
                    Point pp = new Point(pos[0], pos[1], radius);
                    pp.setOriginalX(pos[0] / scale_ratio);
                    pp.setOriginalY(pos[1] / scale_ratio);
                    pp.setOriginalRadius((int) (radius / scale_ratio));
                    points.add(pp);
                }
                Ltotal += tempLength;
                n++;
            }
        }
        return new1_Bitmap;
    }

    /**
     * helper function to check if a number is a prime number
     * currently unused
     * @param n
     * @return
     */
    public boolean isPrime(int n){
        if(n < 2) {
            return false;
        }
        int x = 2;
        while(x < n){
            if(n % x == 0){
                return false;
            }
            x++;
        }
        return true;
    }

    /**
     * get a group of circles according to click point and start point
     *
     * @return drawn bitmap
     */
    public Bitmap groupCircle() {
        Canvas canvas = null;
        canvas = new Canvas(new1_Bitmap);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        float actualX = clickX - moveDx;
        float actualY = clickY - moveDy;
        float actualStartX = startX - moveDx;
        float actualStartY = startY - moveDy;
        if (isMove || isClick) {
            if (Point.distance(actualStartX, actualStartY, actualX, actualY) >= 0 /*4.0f  * radius + 2.0f * gap*/) {
                Path path = new Path();
                path.moveTo(actualStartX, actualStartY);
                float[] pos = new float[2];
                float[] tan = new float[2];
                path.lineTo(actualX, actualY);
                PathMeasure measure = new PathMeasure();
                measure.setPath(path, false);
                measure.getPosTan((float) (Math.sqrt(3) * (2.0f  * radius + gap)), pos, tan);
                double degree = calcDegree(actualStartX, actualStartY, pos);
                Log.i(TAG, "degree:  " + degree);
                if((degree > 85 && degree < 95) || (degree > 265 && degree < 275)){
                    pos = calcCenter( (float) (Math.sqrt(3) * (2.0f  * radius + gap)), degree, pos);
                }else{
                    measure.getPosTan(4.0f  * radius + 2.0f * gap, pos, tan);
                    pos = calcCenter( 4.0f  * radius + 2.0f * gap, degree, pos);
                }
                if (!Point.checkCollide(forbs, points, pos[0], pos[1],radius,radius)) {
                    canvas.drawCircle(pos[0], pos[1] , radius, paint);
                    Point pp = new Point(pos[0],pos[1] ,radius);
                    pp.setOriginalX((pos[0] ) / scale_ratio);
                    pp.setOriginalY((pos[1] )/ scale_ratio);
                    pp.setOriginalRadius((int) (radius / scale_ratio));
                    points.add(pp);
                    Point[] sides = calc_pos_6(pos);
                    for(int i = 0; i < 6; i++){
                        if(!Point.checkCollide(forbs,points, sides[i].getX(), sides[i].getY(), radius, radius)){
                            canvas.drawCircle(sides[i].getX(),sides[i].getY(), radius, paint);
                            sides[i].setOriginalX((sides[i].getX()) / scale_ratio);
                            sides[i].setOriginalY((sides[i].getY()) / scale_ratio);
                            sides[i].setOriginalRadius((int) (radius / scale_ratio));
                            points.add(sides[i]);
                        }
                    }
                    path.moveTo(actualStartX , actualStartY );
                    startX = pos[0] + moveDx;
                    startY = pos[1] + moveDy;
                }
            }
        }
        return new1_Bitmap;
    }

    /**
     * helper function to calc the other 6 points near the center it is a regular hexagon
     * @param center
     * @return
     */
    private Point[] calc_pos_6(float[] center){
        Point[] sides = new Point[6];
        float r = 2.0f * radius + gap;
        Point point1 = new Point(center[0] - r, center[1], radius); //left
        Point point2 = new Point(center[0] - (r / 2), (float) (center[1] - r * Math.sqrt(3)/2), radius); //top left
        Point point3 = new Point(center[0] + (r / 2), (float) (center[1] - r * Math.sqrt(3)/2), radius); //top right
        Point point4 = new Point(center[0] + r, center[1], radius); //right
        Point point5 = new Point(center[0] + (r / 2), (float) (center[1] + r * Math.sqrt(3)/2), radius); //bottom right
        Point point6 = new Point(center[0] - (r / 2), (float) (center[1] + r * Math.sqrt(3)/2), radius); //bottom left
        sides[0] = point1;
        sides[1] = point2;
        sides[2] = point3;
        sides[3] = point4;
        sides[4] = point5;
        sides[5] = point6;
        return sides;
    }

    /**
     *  helper function to calculate the degree between two points
     * @param centerX
     * @param centerY
     * @param endPos
     * @return
     */
    private double calcDegree(float centerX, float centerY, float[] endPos){
        double rotation = 0.0;
        float xInView = endPos[0];
        float yInView = endPos[1];
        double tmpDegree = Math.atan(Math.abs(endPos[1] - centerY) / Math.abs(endPos[0] - centerX)) * 180.0 / Math.PI;
        if (xInView > centerX && yInView < centerY) {  //第一象限
            rotation = tmpDegree;
        } else if (xInView > centerX && yInView > centerY) //第四象限
        {
            rotation = 360.0 - tmpDegree;
        } else if (xInView < centerX && yInView > centerY) { //第三象限
            rotation = 180.0 + tmpDegree;
        } else if (xInView < centerX && yInView < centerY) { //第二象限
            rotation = 180.0 - tmpDegree;
        } else if (xInView == centerX && yInView < centerY) {
            rotation = 90.0;
        } else if (xInView == centerX && yInView > centerY) {
            rotation = 270.0;
        }else if (xInView < centerX && yInView == centerY) {
            rotation = 180.0;
        }else if (xInView >= centerX && yInView == centerY) {
            rotation = 0.0;
        }else{
        }
        return rotation;
    }

    /**
     * helper function to draw the center point in 8 directions
     * @param degree
     * @param pos end position
     * @return
     */
    private float[] calcCenter(float length, double degree, float[] pos){
        Path helpP = new Path();
        PathMeasure helpMeasure = new PathMeasure();
        float actualStartX = startX - moveDx;
        float actualStartY = startY - moveDy;
        if(degree < 45 || degree > 315){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX + 1.0f * length, actualStartY);
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree >= 45 && degree <= 85){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX + 1.0f * length, (float) (actualStartY - Math.sqrt(3) * 1.0f * length));
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree > 85 && degree < 95){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX, actualStartY - 1.0f * length);
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree >= 95 && degree <= 135){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX - 1.0f * length, (float) (actualStartY - Math.sqrt(3) * 1.0f * length));
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree > 135 && degree < 225){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX - 1.0f * length, actualStartY);
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree >= 225 && degree <= 265){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX - 1.0f * length, (float) (actualStartY + Math.sqrt(3) * 1.0f * length));
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree > 265 && degree < 275){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX, actualStartY + 1.0f * length);
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else if(degree >= 275 && degree <= 315){
            float[] helpPos = new float[2];
            float[] helpTan = new float[2];
            helpP.moveTo(actualStartX,actualStartY);
            helpP.lineTo(actualStartX + 1.0f * length, (float) (actualStartY + Math.sqrt(3) * 1.0f * length));
            helpMeasure.setPath(helpP,false);
            helpMeasure.getPosTan(length,helpPos,helpTan);
            pos = helpPos;
        }else{
            //should be none
            Log.e(TAG,"degree should within 0-360");
        }

        return pos;
    }

    /**
     * helper function for group circles
     * currently unused
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param radius
     * @return
     * @function groupCircle
     */
    private Point calcLow(float startX, float startY, float endX, float endY, int radius, int gap) {
        float dx = endX - startX + gap;
        float dy = endY - startY + gap;
        float perpx = -dy;
        float perpy = dx;
        float newX = endX - perpx;
        float newY = endY - perpy;
        return new Point(newX, newY, radius);
    }

    /**
     * helper function for group circles
     * currently unused
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param radius
     * @return
     * @function groupCircle
     */
    private Point calcHigh(float startX, float startY, float endX, float endY, int radius, int gap) {
        float dx = endX - startX + gap;
        float dy = endY - startY + gap;
        float perpx = -dy;
        float perpy = dx;
        float newX = endX + perpx;
        float newY = endY + perpy;
        return new Point(newX, newY, radius);
    }

    /**
     * convert a drawable to bitmap
     * @param drawable
     * @return bitmap
     */
    public static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        clickX = event.getX();
        clickY = event.getY();
        fingers = event.getPointerCount();
        if(fingers == 1){
            firstFinger[0] = clickX;
            firstFinger[1] = clickY;
        }else if(fingers == 2){
            MotionEvent.PointerCoords corrd2 = new MotionEvent.PointerCoords();
            event.getPointerCoords(1,corrd2);
            MotionEvent.PointerCoords corrd1 = new MotionEvent.PointerCoords();
            event.getPointerCoords(0,corrd1);
            firstFinger[0] = corrd1.x;
            firstFinger[1] = corrd1.y;
            secondFinger[0] = corrd2.x;
            secondFinger[1] = corrd2.y;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                isMove = false;
                isClick = true;
                startPathX = clickX;
                startPathY = clickY;
                startX = clickX;
                startY = clickY;
                isUp = false;
                if(event.getPressure() > 1){
                    isPress = true;
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                isMove = true;
                isClick = false;
                isUp = false;
                if(event.getPressure() > 1){
                    isPress = true;
                }else{
                    isPress = false;
                }
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP: {
                isClick = false;
                isMove = false;
                isClear = false;
                isUp = true;
                isPress = false;
                break;
            }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                copy(in, out);
            }
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
