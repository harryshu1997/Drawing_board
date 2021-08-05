package com.example.navigation.ui;

import android.util.Log;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * 项目名： Navigation
 * 文件名： Forb
 *
 *   @Author: Zhihao Shu(Harry)
 *   @email: 919900130@qq.com
 *
 */
public class Forb {
    private float x;
    private float y;
    private float originalX;
    private float originalY;
    private int radius;
    private int originalRadius;

    public int getOriginalRadius() {
        return originalRadius;
    }

    public void setOriginalRadius(int originalRadius) {
        this.originalRadius = originalRadius;
    }

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

    private boolean isSelected = false;
    private int clicked = 0;

    public int getClicked() {
        return clicked;
    }

    public void setClicked(int clicked) {
        this.clicked = clicked;
    }

    public void addClick(){
        this.clicked++;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Forb (float xx, float yy, int r){
        this.x = xx;
        this.y = yy;
        this.radius = r;
    }

    /**
     * check if current position is colliding with any forbs
     * @param array
     * @param px
     * @param py
     * @param radius
     * @return
     */
    public static boolean checkCollideF(ArrayList<Forb> array, float px, float py, int radius){
        boolean result = false;
        if(array == null || array.size() == 0){
            return false;
        }
        for(Forb x: array){
            if(0<distance(x.x,x.y,px,py) && distance(x.x,x.y,px,py) < x.radius + radius){
                result = true;
                return result;
            }
        }
        return result;
    }

    /**
     * check if current position is colliding with any forbs (for a point)
     * @param array
     * @param px
     * @param py
     * @param radius
     * @return
     */
    public static boolean checkCollideP(ArrayList<Point> array, float px, float py, int radius){
        boolean result = false;
        if(array == null || array.size() == 0){
            return false;
        }
        for(Point x: array){
            if(0<distance(x.x,x.y,px,py) && distance(x.x,x.y,px,py) < x.radius + radius){
                result = true;
                return result;
            }
        }
        return result;
    }

    /**
     *  check if any points are collide with a forb and delete those points
     * @param array
     * @param px
     * @param py
     * @param radius
     * @return
     */
    public static boolean checkCollidePF(ArrayList<Point> array, float px, float py, int radius){

        if(array == null || array.size() == 0){
            return false;
        }
        ArrayList<Point> temp = new ArrayList<Point>();
        temp = (ArrayList<Point>) array.clone();
        for(Point x: temp){
            if(0<distance(x.x,x.y,px,py) && distance(x.x,x.y,px,py) < x.radius + radius){
                array.remove(x);
            }
        }
        return true;
    }

    /**
     * check if a point is within any forbs
     * @param array
     * @param px
     * @param py
     * @param buff
     * @return
     */
    public static boolean checkWithin(ArrayList<Forb> array, float px, float py, float buff){
        boolean result = false;
        if(array == null || array.size() == 0){
            return false;
        }
        for(Forb x: array){
            if(distance(x.x,x.y,px,py) <= x.radius + buff){
                result = true;
                return result;
            }
        }
        return result;
    }

    /**
     *  get the collided forb
     * @param array
     * @param px
     * @param py
     * @return
     */
    public static Forb getCollide(ArrayList<Forb> array, float px, float py){
        Forb result = new Forb(0,0,20);
        if(array == null || array.size() == 0){
            Log.i(TAG,"array is empty getCollide failed");
            return null;
        }
        for(Forb x: array){
            if(distance(x.x,x.y,px,py) <= x.radius){
                result = x;
                return result;
            }
        }
        Log.d(TAG,"no collision point");
        return result;
    }

    /**
     * helper function to calc distance between two points
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Forb other = (Forb ) obj;
        if(other.getRadius() == this.radius && other.getX() == this.x && other.getY() == this.y) return true;
        return false;
    }


}
