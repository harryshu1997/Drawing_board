package com.example.navigation.ui.home;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.navigation.R;
import com.example.navigation.ui.Point;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private HomeViewModel homeViewModel;
    private HandWrite handwrite = null;
   // private HandWrite prehand = null;
    private ImageButton clear = null;
    private ImageButton point = null;
    private ImageButton mouse = null;
    private ImageButton line_circle = null;
    private ImageButton eraser = null;
    private ImageButton group = null;
    private ImageButton capture = null;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);


        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        handwrite = root.findViewById(R.id.handwriteview);

        clear = root.findViewById(R.id.cancel);
        point = root.findViewById(R.id.dot);
        mouse = root.findViewById(R.id.mouse);
        eraser = root.findViewById(R.id.eraser);
        line_circle = root.findViewById(R.id.lineCircle);
        group = root.findViewById(R.id.group_circle);
        capture = root.findViewById(R.id.capture);
        handwrite.setStyle(5);
        handwrite.setColor(Color.GREEN);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handwrite.clear();
                clear.setBackgroundColor(Color.GREEN);
                point.setBackgroundColor(Color.TRANSPARENT);
                mouse.setBackgroundColor(Color.TRANSPARENT);
                eraser.setBackgroundColor(Color.TRANSPARENT);
                line_circle.setBackgroundColor(Color.TRANSPARENT);
                group.setBackgroundColor(Color.TRANSPARENT);

            }
        });

        group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HandWrite.constant = Constant.group_circle;
                clear.setBackgroundColor(Color.TRANSPARENT);
                point.setBackgroundColor(Color.TRANSPARENT);
                mouse.setBackgroundColor(Color.TRANSPARENT);
                eraser.setBackgroundColor(Color.TRANSPARENT);
                line_circle.setBackgroundColor(Color.TRANSPARENT);
                group.setBackgroundColor(Color.GREEN);
            }
        });
        point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // handwrite.draw();
                HandWrite.constant = Constant.draw_circle;
                clear.setBackgroundColor(Color.TRANSPARENT);
                point.setBackgroundColor(Color.GREEN);
                mouse.setBackgroundColor(Color.TRANSPARENT);
                eraser.setBackgroundColor(Color.TRANSPARENT);
                line_circle.setBackgroundColor(Color.TRANSPARENT);
                group.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        mouse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              //  handwrite.draw();
                HandWrite.constant = Constant.draw_line;
                clear.setBackgroundColor(Color.TRANSPARENT);
                point.setBackgroundColor(Color.TRANSPARENT);
                mouse.setBackgroundColor(Color.GREEN);
                eraser.setBackgroundColor(Color.TRANSPARENT);
                line_circle.setBackgroundColor(Color.TRANSPARENT);
                group.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        line_circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // handwrite.draw();
                HandWrite.constant = Constant.draw_lineCircle;
                clear.setBackgroundColor(Color.TRANSPARENT);
                point.setBackgroundColor(Color.TRANSPARENT);
                mouse.setBackgroundColor(Color.TRANSPARENT);
                eraser.setBackgroundColor(Color.TRANSPARENT);
                line_circle.setBackgroundColor(Color.GREEN);
                group.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // handwrite.draw();
                HandWrite.constant = Constant.eraser;
                clear.setBackgroundColor(Color.TRANSPARENT);
                point.setBackgroundColor(Color.TRANSPARENT);
                mouse.setBackgroundColor(Color.TRANSPARENT);
                eraser.setBackgroundColor(Color.GREEN);
                line_circle.setBackgroundColor(Color.TRANSPARENT);
                group.setBackgroundColor(Color.TRANSPARENT);
            }
        });
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testViewSnapshot(handwrite);
                outPutFile();
            }
        });
        return root;
    }


    /**
     * 输出打点坐标
     */
    private void outPutFile(){
        String state = Environment.getExternalStorageState();
        //获取外bai部du设备zhi状态
        // 检测外部设备是否可用
        if(!state.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getContext(), "外部设备不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        //创建文件
        File sdCard = Environment.getExternalStorageDirectory();
        //获取外部设备的目录dao
        File file = new File(sdCard,"打点数据.txt");
        //文件位置
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            //打开文件输出流
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            //写入到缓存流
            writer.write("打点的数据：\n");
            writer.write("图片width= " + HandWrite.width + " height= " + HandWrite.height + "\n");
            for(Point p: handwrite.getPoints()){
                String str = "";
                str = "x: " + p.getX() + "      y: " + p.getY() + "       radius: " + p.getRadius() + "\n";
                writer.write(str);
            }
            
            //从从缓存流写入
            writer.close();
            //关闭流
            Toast.makeText(getContext(), "输出成功", Toast.LENGTH_SHORT).show();
        } catch(Exception exception) {
            Toast.makeText(getContext(), "输出失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 对View进行截图
     */
    private void testViewSnapshot(View view) {
        //使控件可以进行缓存
        view.setDrawingCacheEnabled(true);
        //获取缓存的 Bitmap
        Bitmap drawingCache = view.getDrawingCache();
        //复制获取的 Bitmap
        drawingCache = Bitmap.createBitmap(drawingCache);
        drawingCache = scaleImage(drawingCache,3888,2592);
        //关闭视图的缓存
        view.setDrawingCacheEnabled(false);

        String storePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "draw";
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }

        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        if (drawingCache != null) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                //通过io流的方式来压缩保存图片
                boolean isSuccess = drawingCache.compress(Bitmap.CompressFormat.PNG, 80, fos);
                fos.flush();
                fos.close();

                //保存图片后发送广播通知更新数据库
                Uri uri = Uri.fromFile(file);
                getActivity().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                if (isSuccess) {
                    Toast.makeText(getContext(), "成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "失败", Toast.LENGTH_SHORT).show();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            Toast.makeText(getContext(), "失败2", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 按新的宽高缩放图片
     *
     * @param bm
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static Bitmap scaleImage(Bitmap bm, int newWidth, int newHeight)
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
        if (bm != null & !bm.isRecycled())
        {
            bm.recycle();
            bm = null;
        }
        return newbm;
    }
    @Override
    public void onResume() {
        super.onResume();

    }
}