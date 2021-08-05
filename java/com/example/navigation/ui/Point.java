package com.example.navigation.ui;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.example.navigation.MyViewModel;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * 项目名： Navigation
 * 文件名： Point
 *
 * @Author: Zhihao Shu(Harry)
 * @email: 919900130@qq.com
 * 创建日期：2020 2020/7/23 11:24
 * 描述：
 */
public class Point {
    float x;
    float y;
    float originalX;
    float originalY;
    int radius;
    int originalRadius;
    boolean isCollide = false;


    public float getOriginalX() {
        return originalX;
    }

    public void setOriginalX(float originalX) {
        this.originalX = originalX;
    }

    public float getOriginalY() {
        return originalY;
    }

    public void setOriginalY(float originalY) {
        this.originalY = originalY;
    }

    public int getOriginalRadius() {
        return originalRadius;
    }

    public void setOriginalRadius(int originalRadius) {
        this.originalRadius = originalRadius;
    }

    public boolean isCollide() {
        return isCollide;
    }

    public void setCollide(boolean collide) {
        isCollide = collide;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public int getRadius() {
        return radius;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public Point(float x, float y, int radius){
        this.x = x;
        this.y = y;
        this.radius = radius;

    }

    /**
     * helper function to calc two points' distance
     * @param x
     * @param y
     * @param xx
     * @param yy
     * @return
     */
    public static int distance(float x, float y, float xx, float yy){
        float xiel = (x - xx);
        float yiel = y - yy;
        int res = 0;
        res = (int) Math.sqrt(Math.pow(xiel,2) + Math.pow(yiel,2));
        return res;
    }


    /**
     *  get the closest point of a point
     * @param xx
     * @param yy
     * @param array
     * @return
     */
    public static Point getClosest(float xx, float yy, ArrayList<Point> array){
        if(array.size() != 0){
            Point result = array.get(0);
            for(Point p: array){
                if(distance(result.getX(),result.getY(),xx,yy) > distance(p.getX(),p.getY(),xx,yy)){
                    result = p;
                }
            }
            return result;
        }
        return null;
    }

    /**
     *  check if a location is colliding with any forbs or points
     * @param farray
     * @param array
     * @param px
     * @param py
     * @param Pbuff
     * @param Fbuff
     * @return
     */
    public static boolean checkCollide(ArrayList<Forb> farray, ArrayList<Point> array, float px, float py, int Pbuff, int Fbuff){
        boolean result = false;
        if(array == null || array.size() == 0){
            return false;
        }
        for(Point x: array){
            if(0 < distance(x.x,x.y,px,py) && distance(x.x,x.y,px,py) < x.radius + Pbuff){
                result = true;
                return result;
            }
        }
        if(farray != null){
            for(Forb f: farray){
                if(0 < distance(f.getX(),f.getX(),px,py) && distance(f.getX(),f.getY(),px,py) < f.getRadius() + Fbuff){
                     result = true;
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * check if a point is within any other points
     * @param array
     * @param px
     * @param py
     * @param buff gap between two points
     * @return
     */
    public static boolean checkWithin(ArrayList<Point> array, float px, float py, float buff){
        boolean result = false;
        if(array == null || array.size() == 0){
            return false;
        }
        for(Point x: array){
            if(distance(x.x,x.y,px,py) <= 2*x.radius + buff){
                result = true;
                return result;
            }
        }
        return result;
    }

    /**
     *  erase encountered points
     * @param array
     * @param px
     * @param py
     * @param buff
     * @return
     */
    public static boolean checkWithin_new(ArrayList<Point> array, float px, float py, float buff){
        if(array == null || array.size() == 0){
            return false;
        }
        ArrayList<Point> temp = new ArrayList<Point>();
        temp = (ArrayList<Point>) array.clone();
        for(Point x: temp){
            if(distance(x.x,x.y,px,py) <= 2*x.radius + buff){
               array.remove(x);
            }
        }
        return true;
    }

    /**
     * get collided point
     * @param array
     * @param px
     * @param py
     * @return
     */
    public static Point getCollide(ArrayList<Point> array, float px, float py){
        Point result = new Point(0,0,20);
        if(array == null || array.size() == 0){
            Log.i(TAG,"array is empty get point collide failed");
            return null;
        }
        for(Point x: array){
            if(distance(x.x,x.y,px,py) <= x.radius){
                result = x;
                return result;
            }
        }
        Log.d(TAG,"no collision point");
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj == null){
            return false;
        }
        if(obj instanceof Point){
            Point p = (Point) obj;
            if(p.getX() == this.x && p.getY() == this.y){
                return true;
            }
        }else{
            return false;
        }
        return false;
    }

}
