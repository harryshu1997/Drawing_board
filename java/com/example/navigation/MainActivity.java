package com.example.navigation;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ImageReader;
import android.media.audiofx.DynamicsProcessing;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.navigation.ui.Forb;
import com.example.navigation.ui.Point;
import com.example.navigation.ui.home.HandWrite;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.shawnlin.numberpicker.NumberPicker;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.Format;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 1001;
    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.CALL_PHONE,Manifest.permission.READ_EXTERNAL_STORAGE};
    List<String> permissionsList = new ArrayList<>();
    private static final int IMAGE_REQUEST_CODE = 1;
    private final String TAG = this.getClass().getName();
    private AppBarConfiguration mAppBarConfiguration;
    public static Bitmap newBitmap;
    public static ArrayList<Point> newPoints;
    public static ArrayList<Forb> newForbs;

    HttpCallBackListener listener;
    String path = null;
    String pasteString = null;
    TextView URL;
    TextView scaleRatio;
    ArrayList<Forb> decodeForbs = new ArrayList<Forb>();
    ArrayList<Point> decodePoints = new ArrayList<Point>();

    //communicate with fragment
    private MyViewModel mViewModel;

    private final static Pattern IMG_URL = Pattern
            .compile(".*?(gif|jpeg|png|jpg|bmp)");

// this part is for loading OpenCV lib
//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    Log.i("OpenCV", "OpenCV loaded successfully");
//
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //communicate with fragment
        mViewModel = new ViewModelProvider(this).get(MyViewModel.class);
        initPermissions();
        listener = new HttpCallBackListener() {
            @Override
            public void onFinish(Bitmap bitmap) {
                newBitmap = bitmap;
                if(newBitmap == null){
                    Toast.makeText(getApplicationContext(),"get new bitmap failed!",Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(Exception e) {
            }
        };
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //不熄屏
        setContentView(R.layout.activity_main);


        scaleRatio = findViewById(R.id.scaleRatio);
        mViewModel.getScaleRatio().observe(this, new Observer<Float>() {
            @Override
            public void onChanged(Float aFloat) {
                String s = String.format("%.2f",aFloat);
                scaleRatio.setText("Scale: " + s + "x");
            }
        });

        final NumberPicker np = findViewById(R.id.number_picker);
        np.setOnValueChangedListener(npListener);

        final NumberPicker npGap = findViewById(R.id.number_picker_gap);
        npGap.setOnValueChangedListener(npGapListener);

        final NumberPicker npSize = findViewById(R.id.number_picker_radius);
        npSize.setOnValueChangedListener(npSizeListener);

        mViewModel.getIsSize().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    npSize.setVisibility(View.VISIBLE);
                }else{
                    npSize.setVisibility(View.GONE);
                }
            }
        });

        mViewModel.getIsGap().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    npGap.setVisibility(View.VISIBLE);
                }else{
                    npGap.setVisibility(View.GONE);
                }
            }
        });

        mViewModel.getIsForb().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if(aBoolean){
                    np.setVisibility(View.VISIBLE);
                }else{
                    np.setVisibility(View.GONE);
                }
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(fabListener);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each  menu ID as a setof Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery)
                .setDrawerLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //------------------------------------------------------------------ Uncomment this part to show navigation view
        //Toolbar toolbar = findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        //-------------------------------------------------------------------
        NavigationUI.setupWithNavController(navigationView, navController);

        Fragment hand = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        HandWrite.height = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        HandWrite.width = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        HandWrite.width = hand.getActivity().getWindowManager().getDefaultDisplay().getWidth() - 200;
        HandWrite.height = hand.getActivity().getWindowManager().getDefaultDisplay().getHeight() - 200;
        Log.i(TAG,"测量的宽-200 " +HandWrite.width + "测量的高-200 " + HandWrite.height);

        //go to this url to select an image currently unavailable
        ImageButton robotrak = findViewById(R.id.Robotrak);
        robotrak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToUrl("http://192.168.1.120:61815/Robotrak_new/index.html");
            }
        });

        //copy image url to change the background image
        URL = findViewById(R.id.URL);
        URL.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                URL.setText("");
                return false;
            }
        });
        URL.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i == EditorInfo.IME_ACTION_DONE){
                    path = textView.getText().toString();
                    if(isTrueURL(path) && isImgUrl(path)){
                        Toast.makeText(getApplicationContext(),"Get URL successful!" + path,Toast.LENGTH_SHORT).show();
                        Bitmap bitmap = null;
                        try{
                             getImage(path,listener);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(),"not a valid URL!",Toast.LENGTH_SHORT).show();
                    }
                    finish();
                    startActivity(getIntent());
                }
                return false;
            }
        });
    }

    private FloatingActionButton.OnClickListener fabListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                //在这里跳转到手机系统相册里面
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, IMAGE_REQUEST_CODE);
        }
    };


    private NumberPicker.OnValueChangeListener npListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mViewModel.setForbSize(newVal);
        }
    };

    private NumberPicker.OnValueChangeListener npGapListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mViewModel.setPGap(newVal);
        }
    };

    private NumberPicker.OnValueChangeListener npSizeListener = new NumberPicker.OnValueChangeListener() {
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mViewModel.setPSize(newVal);
        }
    };




    /**
     * 判断一个url是否为图片url
     *
     * @param url
     * @return
     */
    public static boolean isImgUrl(String url) {
        if (url == null || url.trim().length() == 0)
            return false;
        return IMG_URL.matcher(url).matches();
    }
    public static boolean isTrueURL(String url){
        String regex = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]" ;
        Pattern patt = Pattern. compile(regex );
        Matcher matcher = patt.matcher(url);
        return matcher.matches();
    }

    /**
     * go to defined url
     * @param url
     */
    private void goToUrl (String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    /**
     * 请求权限
     */
    private void initPermissions() {
        permissionsList.clear();
        //判断哪些权限未授予
        for(String permission : permissions){
            if(ActivityCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED){
                permissionsList.add(permission);
            }
        }
        //请求权限
        if(!permissionsList.isEmpty()){
            String[] permissions = permissionsList.toArray(new String[permissionsList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSION_REQUEST);
        }
    }

    /**
     * 权限回调,
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_REQUEST:
                //ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_PERMISSION);
                break;
            default:
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //在相册里面选择好相片之后调回到现在的这个activity中
        switch (requestCode) {
            case IMAGE_REQUEST_CODE://这里的requestCode是我自己设置的，就是确定返回到那个Activity的标志
                if (resultCode == RESULT_OK) {//resultcode是setResult里面设置的code值
                    try {
                        Uri selectedImage = data.getData(); //获取系统返回的照片的Uri
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        Cursor cursor = getContentResolver().query(selectedImage,
                                filePathColumn, null, null, null);//从系统表中查询指定Uri对应的照片
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        path = cursor.getString(columnIndex);  //获取照片路径
                        cursor.close();
                        Bitmap bitmap = null;
                        InputStream is = null;
                        List<Bitmap> images = new ArrayList<>();
                        DataInputStream ds = null;
                        int pointsNum = 0;
                        int forbsNum = 0;
                        try{
                            is = new FileInputStream(path);
                            ds = new DataInputStream(is);
                            images.add(BitmapFactory.decodeStream(is));
                            bitmap = BitmapFactory.decodeStream(is).copy(Bitmap.Config.ARGB_8888, true);
                            pointsNum = ds.readInt();
                            forbsNum = ds.readInt();
                            System.out.println("pints num !!!!!!!!!!!!!!!  " + pointsNum);
                            System.out.println("forbs num !!!!!!!!!!!!!!!  " + forbsNum);
                            for(int i=0; i<pointsNum; i++){
                                float px = ds.readFloat();
                                float py = ds.readFloat();
                                int pr = ds.readInt();
                                Point p = new Point(px,py,pr);
                                p.setOriginalX(px);
                                p.setOriginalY(py);
                                p.setOriginalRadius(pr);
                                decodePoints.add(p);
                            }
                            for(int i = 0; i < forbsNum; i++){
                                float fx = ds.readFloat();
                                float fy = ds.readFloat();
                                int fr = ds.readInt();
                                Forb f = new Forb(fx, fy, fr);
                                f.setOriginalX(fx);
                                f.setOriginalY(fy);
                                f.setOriginalRadius(fr);
                                decodeForbs.add(f);
                            }
                            System.out.println("p size = " + decodePoints.size());
                            System.out.println("f size: " + decodeForbs.size());
                            newForbs = decodeForbs;
                            newPoints = decodePoints;
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        ds.close();
                        is.close();
                        newBitmap = bitmap;
                        if(newBitmap == null){
                            Toast.makeText(getApplicationContext(),"Get Bitmap From URL Failed",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getApplicationContext(),"Get Bitmap From URL Success",Toast.LENGTH_SHORT).show();
                        }
                        finish();
                        startActivity(getIntent());
                    } catch (Exception e) {
                        // TODO Auto-generatedcatch block
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    /**
     *  helper function to convert URI to byte array
     * @param inputStream
     * @return
     * @throws IOException
     */
    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPasteString();
        //---------------------------------------------------uncomment this part to load OpenCV lib
//        if (!OpenCVLoader.initDebug()) {
//            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mLoaderCallback);
//        } else {
//            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // 从黏贴板获取数据
    private void getPasteString()
    {
        // 获取并保存粘贴板里的内容
        try {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                    if(!clipboard.hasPrimaryClip()){
                        Log.i(TAG,"no primary clip !!!!!!!!!!!!!!!");
                        return;
                    }
                    ClipData clipData = clipboard.getPrimaryClip();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        CharSequence text = clipData.getItemAt(0).getText();
                        pasteString = text.toString();
                        Log.d(TAG, "getFromClipboard text=" + pasteString);
                        Toast.makeText(getApplicationContext(),"getFromClipboard text= " + pasteString,Toast.LENGTH_SHORT).show();
                    }else{
                        Log.i(TAG,"failed");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getFromClipboard error");
            e.printStackTrace();
        }
    }

    /**
     * bitmap转换 get image from website
     * @param
     * @return
     */
    public void getImage(final String path, final HttpCallBackListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL imageUrl = null;
                try {
                    imageUrl = new URL(path);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                try {
                    HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    Bitmap bitmap1= createBitmapThumbnail(bitmap,false);
                    if (listener != null) {
                        listener.onFinish(bitmap1);
                    }
                    is.close();
                } catch (IOException e) {
                    if (listener != null) {
                        listener.onError(e);
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * crate image thumbnail
     * @param bitmap
     * @param needRecycler
     * @return
     */
    public Bitmap createBitmapThumbnail(Bitmap bitmap,boolean needRecycler){
        int w=bitmap.getWidth();
        int h=bitmap.getHeight();
        int newWidth=HandWrite.width;
        int newHeight=HandWrite.height;
        float scaleWidth=((float)newWidth)/w;
        float scaleHeight=((float)newHeight)/h;
        Matrix matrix=new Matrix();
        matrix.postScale(scaleWidth,scaleHeight);
        Bitmap newBitMap=Bitmap.createBitmap(bitmap,0,0,w,h,matrix,true);
        if(needRecycler)bitmap.recycle();
        return newBitMap;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        try {
            super.onConfigurationChanged(newConfig);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Log.v("Himi", "onConfigurationChanged_ORIENTATION_LANDSCAPE");
            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Log.v("Himi", "onConfigurationChanged_ORIENTATION_PORTRAIT");
            }
        } catch (Exception ex) {
        }
    }
}