package com.example.navigation.ui.home;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.example.navigation.MyViewModel;
import com.example.navigation.R;
import com.example.navigation.ui.Forb;
import com.example.navigation.ui.Point;
import com.example.navigation.ui.Utils;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private HomeViewModel homeViewModel;
    private HandWrite handwrite = null;
    private ImageButton clear = null;
    private ImageButton point = null;
    private ImageButton path = null;
    private ImageButton eraser = null;
    private ImageButton group = null;
    private ImageButton ring_circle = null;
    private ImageButton capture = null;
    private ImageButton forb = null;
    private ImageButton scale = null;

    private RelativeLayout layout = null;

    //get data from outside class
    private MyViewModel viewModel;
    private ScaleRatioVR scaleRatioVR = Constant.scaleRatioVR;

    //create forb list
    ArrayList<Forb> forbs = new ArrayList<Forb>();


    private Dialog mDialog;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    Utils.closeDialog(mDialog);
                    break;
            }
        }
    };

    public View onCreateView(@NonNull final LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        //get permissions
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }

        //find the image view
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        handwrite = root.findViewById(R.id.handwriteview);

        //find the buttons
        clear = root.findViewById(R.id.cancel);
        point = root.findViewById(R.id.dot);
        eraser = root.findViewById(R.id.eraser);
        path = root.findViewById(R.id.path);
        group = root.findViewById(R.id.group_circle);
        ring_circle = root.findViewById(R.id.ring_circle);
        capture = root.findViewById(R.id.capture);
        forb = root.findViewById(R.id.forb);
        scale = root.findViewById(R.id.scale);
        handwrite.setStyle(5);
        handwrite.setColor(Color.GREEN);

        //find layout
        layout = root.findViewById(R.id.layout);

        //set signals
        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);

        scaleRatioVR.setListener(new ScaleRatioVR.ChangeListener() {
            @Override
            public void onChange() {
                float newValue = scaleRatioVR.getScale();
                Log.i(TAG,"new scale value: " + newValue);
                viewModel.setScaleRatio(newValue);
            }
        });

        viewModel.getForbSize().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Constant.setForbSize(integer);
            }
        });

        viewModel.getPGap().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Constant.setGap(integer);
            }
        });

        viewModel.getPSize().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                Constant.setPRadius(integer);
            }
        });

        //define buttons' behaviors
        ring_circle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandWrite.constant = Constant.ring_circle;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle_c);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(true);
                viewModel.setIsGap(true);
                viewModel.setIsSize(true);
            }
        });

        scale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandWrite.constant = Constant.draw_scale;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.image_scale_green_50);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(false);
                viewModel.setIsGap(false);
                viewModel.setIsSize(false);
            }
        });

        forb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HandWrite.constant = Constant.draw_forb;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb_c);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(true);
                viewModel.setIsGap(false);
                viewModel.setIsSize(false);
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handwrite.clear();
                HandWrite.constant = Constant.delete;
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(false);
                viewModel.setIsGap(false);
                viewModel.setIsSize(false);
            }
        });

        group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HandWrite.constant = Constant.group_circle;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult_c);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(false);
                viewModel.setIsGap(true);
                viewModel.setIsSize(true);
            }
        });

        point.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handwrite.draw();
                HandWrite.constant = Constant.draw_circle;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single_c);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(false);
                viewModel.setIsGap(false);
                viewModel.setIsSize(true);
            }
        });

        path.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handwrite.draw();
                HandWrite.constant = Constant.draw_path;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path_c);
                eraser.setImageResource(R.drawable.ic_eraser);
                viewModel.setForb(false);
                viewModel.setIsGap(true);
                viewModel.setIsSize(true);
            }
        });

        eraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // handwrite.draw();
                HandWrite.constant = Constant.eraser;
                //change image button source make it distinguishable
                ring_circle.setImageResource(R.drawable.ic_circle);;
                scale.setImageResource(R.drawable.ic_image_scale);
                forb.setImageResource(R.drawable.ic_forb);
                group.setImageResource(R.drawable.ic_mult);
                point.setImageResource(R.drawable.ic_single);
                path.setImageResource(R.drawable.ic_path);
                eraser.setImageResource(R.drawable.ic_eraser_c);
                viewModel.setForb(false);
                viewModel.setIsGap(false);
                viewModel.setIsSize(false);
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewModel.setForb(false);
                viewModel.setIsGap(false);
                viewModel.setIsSize(false);
                mDialog = Utils.createLoadingDialog(getContext(), "加载中...");
                mHandler.sendEmptyMessageDelayed(1, 5000);
                new Thread(new Runnable() {
                        public void run() {
                            testViewSnapshot(handwrite);
                            outPutFile();
                        }
                    }).start();
                Log.i(TAG,"CLICK PICTURE !!!!!!!!!!!");
            }
        });
        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * 输出打点坐标
     */
    private void outPutFile(){
        String state = Environment.getExternalStorageState();
        //获取外部设备状态
        // 检测外部设备是否可用
        if(!state.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getContext(), "外部设备不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        //创建文件
        String sdCard = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            sdCard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        }else{
            Log.i(TAG,"fail to write!!!!!!!!!!!!");
        }

        //获取外部设备的目录dao
        File file = new File(sdCard);
        String currentDate = new SimpleDateFormat("ss_dd_MM_yyyy", Locale.getDefault()).format(new Date());
        String filename = currentDate + ".txt";
        File newFile = new File(file, filename);
        Log.i(TAG,file.getAbsolutePath() + "  " + filename);
        //文件位置
        try {
            FileOutputStream outputStream = new FileOutputStream(newFile);
            //打开文件输出流
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            //写入到缓存流
            writer.write("打点的数据 (在原图的基础上)：\n");
            writer.write("屏幕显示的 width= " + HandWrite.width + " height= " + HandWrite.height + "\n");
            for(Point p: handwrite.getPoints()){
                String str = "";
                str = "x: " + p.getOriginalX() + "      y: " + p.getOriginalY() + "       radius: " + p.getOriginalRadius() + "\n";
                writer.write(str);
            }

            //从从缓存流写入
            writer.close();
            //关闭流
            outputStream.close();
            Log.i(TAG,"输出成功");
        } catch(Exception exception) {
            exception.printStackTrace();
            Log.i(TAG,"输出失败");
        }
    }

    /**
     * 把两个位图覆盖合成为一个位图，以底层位图的长宽为基准 unused!!
     * @param backBitmap 在底部的位图
     * @param frontBitmap 盖在上面的位图
     * @return
     */
    public Bitmap mergeBitmap(Bitmap backBitmap, Bitmap frontBitmap) {

        if (backBitmap == null || backBitmap.isRecycled()
                || frontBitmap == null || frontBitmap.isRecycled()) {
            Log.e(TAG, "backBitmap=" + backBitmap + ";frontBitmap=" + frontBitmap);
            return null;
        }
        Bitmap bitmap = backBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Rect baseRect  = new Rect(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
        Rect frontRect = new Rect(0, 0, frontBitmap.getWidth(), frontBitmap.getHeight());
        canvas.drawBitmap(frontBitmap, frontRect, baseRect, null);
        return bitmap;
    }

    /**
     * 将图片存在相册中
     * @param view
     */
    private void testViewSnapshot(View view) {
        //使控件可以进行缓存
        view.setDrawingCacheEnabled(true);
        //获取缓存的 Bitmap 和 点坐标
        Bitmap drawingCache = handwrite.getNew1_Bitmap();/*view.getDrawingCache();*/
        Bitmap drawingCacheOriginal = handwrite.getOriginalBitmap();
        ArrayList<Forb> forbs = handwrite.getForbs();
        ArrayList<Point> points = handwrite.getPoints();
        //复制获取的 Bitmap
        drawingCache = Bitmap.createBitmap(drawingCache);
        drawingCacheOriginal = Bitmap.createBitmap(drawingCacheOriginal);
        //关闭视图的缓存
        view.setDrawingCacheEnabled(false);
        String storePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + "draw";
        File appDir = new File(storePath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        Log.i(TAG,appDir.getAbsolutePath() + "  " + fileName);
        if (drawingCache != null) {
            try {
                FileOutputStream fos = new FileOutputStream(file);
                DataOutputStream dof = new DataOutputStream(fos);
                //通过io流的方式来压缩保存图片
                boolean isSuccess = drawingCache.compress(Bitmap.CompressFormat.PNG, 80, fos);
                boolean isSuccess2 = drawingCacheOriginal.compress(Bitmap.CompressFormat.PNG, 80, fos);
                dof.writeInt(points.size());
                dof.writeInt(forbs.size());
                for(Point p: points){
                    dof.writeFloat(p.getOriginalX());
                    dof.writeFloat(p.getOriginalY());
                    dof.writeInt(p.getOriginalRadius());
                }
                for(Forb f : forbs){
                    dof.writeFloat(f.getOriginalX());
                    dof.writeFloat(f.getOriginalY());
                    dof.writeInt(f.getOriginalRadius());
                }
                //fos.write("/#".getBytes("UTF-8"));
                dof.flush();
                dof.close();
                fos.flush();
                fos.close();

                //保存图片后发送广播通知更新数据库
                Uri uri = Uri.fromFile(file);
                getActivity().getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

                if ( isSuccess && isSuccess2) {
                    //Toast.makeText(getContext(), "成功", Toast.LENGTH_SHORT).show();
                    Log.i(TAG,"保存成功");
                } else {
                    //Toast.makeText(getContext(), "失败", Toast.LENGTH_SHORT).show();
                    Log.i(TAG,"保存失败");
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            //Toast.makeText(getContext(), "失败2", Toast.LENGTH_SHORT).show();
            Log.i(TAG,"保存失败2");
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

}