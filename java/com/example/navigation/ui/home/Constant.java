package com.example.navigation.ui.home;

import com.example.navigation.ui.Forb;
import com.example.navigation.ui.Point;

import java.util.ArrayList;

/**
 * 项目名： Navigation
 * 文件名： Constant
 *
 * @Author: Zhihao Shu(Harry)
 * @email: 919900130@qq.com
 * 创建日期：2020 2020/7/22 10:14
 * 描述：Constant VR for transferring data
 */
public class Constant {
    public static final int IO_BUFFER_SIZE = 1024;
    public static final int draw_line = 0;
    public static final int draw_circle = 1;
    public static final int draw_path = 2;
    public static final int eraser = 3;
    public static final int group_circle = 4;
    public static final int draw_forb = 5;
    public static final int draw_scale = 6;
    public static final int ring_circle = 7;
    public static final int delete = 100;
    public static int forbSize = 100;
    public static int gap = 5;
    public static float scale = 1.0f;
    public static int PRadius = 10;
    public static ScaleRatioVR scaleRatioVR = new ScaleRatioVR();


    public static float getScale() {
        return scale;
    }

    public static void setScale(float scale) {
        Constant.scale = scale;
    }

    public static int getIoBufferSize() {
        return IO_BUFFER_SIZE;
    }

    public static int getPRadius() {
        return PRadius;
    }

    public static void setPRadius(int PRadius) {
        Constant.PRadius = PRadius;
    }

    public static int getGap() {
        return gap;
    }

    public static void setGap(int gap) {
        Constant.gap = gap;
    }

    public static int getForbSize() {
        return forbSize;
    }

    public static void setForbSize(int a){
        forbSize = a;
    }
}
