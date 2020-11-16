package com.example.navigation.ui;

import android.util.Log;

import androidx.annotation.Nullable;

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
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    float x;
    float y;
    int radius;
    public Point(float x, float y,int radius){
        this.x = x;
        this.y = y;
        this.radius = radius;
    }
    public static int distance(float x, float y, float xx, float yy){
        float xiel = (x - xx);
        float yiel = y - yy;
        int res = 0;
        res = (int) Math.sqrt(Math.pow(xiel,2) + Math.pow(yiel,2));
        return res;
    }

    public static Point getClosest(float xx, float yy, ArrayList<Point> array ){
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

    public static boolean checkCollide(ArrayList<Point> array, float px, float py){
        boolean result = false;
        for(Point x: array){
            if(0<distance(x.x,x.y,px,py) && distance(x.x,x.y,px,py) < 2*x.radius - 5){
                result = true;
                return result;
            }
        }
        return result;
    }

    public static boolean checkWithin(ArrayList<Point> array, float px, float py){
        boolean result = false;
        for(Point x: array){
            if(distance(x.x,x.y,px,py) <= x.radius){
                result = true;
                return result;
            }
        }
        return result;
    }

    public static Point getCollide(ArrayList<Point> array, float px, float py){
        Point result = new Point(0,0,20);
        for(Point x: array){
            if(distance(x.x,x.y,px,py) <= 2*x.radius){
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
            if(p.getX() == this.getX() && p.getY() == this.getY()){
                return true;
            }
        }else{
            return false;
        }
        return false;
    }

}
