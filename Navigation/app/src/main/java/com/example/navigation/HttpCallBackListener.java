package com.example.navigation;

import android.graphics.Bitmap;

/**
 * 项目名： Navigation
 * 文件名： HttpCallBackListener
 *
 * @Author: Zhihao Shu(Harry)
 * @email: 919900130@qq.com
 * 创建日期：2020 2020/7/30 9:23
 * 描述：
 */

//自定义一个接口

public interface HttpCallBackListener {

    void onFinish(Bitmap bitmap);

    void onError(Exception e);

}

