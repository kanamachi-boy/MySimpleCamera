package com.example.banchan.mysimplecamera;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActiviity2 extends Activity implements
        SensorEventListener, LocationListener  {
    private Camera camera;
    int mRoll;
    //boolean isReverse;
    private String presentAddr ="";
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    ActionBar actionBar;

    boolean enableShutter;
    boolean enableLocSensor;
    boolean enableRollSensor;

    private ActionBarCountDownTimer aCDT;
    private int mMinExposure;   //  露出系
    private int mMaxExposure;
    private int mExposureValue;
    private Button mBtnExposure;

    TextView tv1;   //  撮影サイズ
    TextView tv2;   //  使用カメラ
    TextView tv3;   //
    TextView tv4;   //  現在地
    TextView tv5;   //
    TextView tv6;
    TextView tv7;   //  エラーメッセージ

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);

        SurfaceView mySurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(mSurfaceListener);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        actionBar = getActionBar();
        actionBar.setSubtitle("画面をタップで撮影");
        //  露出調整ボタン
        mBtnExposure = (Button)findViewById(R.id.expButton);
        mBtnExposure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setExposure();
            }
        });


        tv1 = (TextView)findViewById(R.id.cmTxt1);
        tv2 = (TextView)findViewById(R.id.cmTxt2);
        tv3 = (TextView)findViewById(R.id.cmTxt3);
        tv4 = (TextView)findViewById(R.id.cmTxt4);
        tv5 = (TextView)findViewById(R.id.cmTxt5);
        tv6 = (TextView)findViewById(R.id.cmTxt6);
        tv7 = (TextView)findViewById(R.id.cmTxt7);
    }   //  OnCreate

    @Override
    protected void onResume() {
        super.onResume();

        //  センサーの許可
        enableLocSensor = Constants.getPrefrenceBoolean(this,Constants.IS_LOCATION_SEARCH,true);
        mRoll = enableLocSensor ? 90 : mRoll;   //  センサー無しは縦（90°）固定
        enableRollSensor = Constants.getPrefrenceBoolean(this,Constants.IS_ROLLING_SEARCH,true);

        try {

            //  デバイス傾きListenerの登録
            if (enableRollSensor) {
                mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
                if (sensors.size() > 0) {
                    Sensor s = sensors.get(0);
                    mSensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_UI);
                }
            }

            //  位置センサーのセット（falseは有効時限内なら検知しない）
            if (enableLocSensor) {
                setLocationMgr(false);
            }
        }catch (Exception e){
            Toast.makeText(this, "onResume", Toast.LENGTH_SHORT).show();
        }


    }   //  Createはfinishしてないと呼ばれないのでここに置く

    @Override
    protected void onPause() {
        super.onPause();
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        if(mLocationManager != null) {
            mLocationManager.removeUpdates(this);
            mLocationManager = null;
        }
        if(aCDT != null){
            aCDT.cancel();
            aCDT = null;
        }
    }    // ▼重要：Listenerの登録解除 画面を離れると必ず呼ばれる？
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //■■■■   オプションメニュー表示
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_camera, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        //  位置センサータイマーが起動中なら手動検知メニューを無効にする。
        if(aCDT == null){
            menu.findItem(R.id.locationUpdate).setEnabled(true);
        }
        else {
            menu.findItem(R.id.locationUpdate).setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        //■■■■   オプションメニューのイベント処理
        switch (item.getItemId()) {
            case R.id.setPhotoSize:
                //  カメラに関する設定
                makeCameraSettingDialog(this);
                break;

            case R.id.setOtherSetting:
                //  その他の設定
                makeOtherSettingDialog(this);
                break;

            case R.id.locationUpdate:
                //  位置情報の手動更新
                setLocationMgr(true);
                break;

            case R.id.goToGallery2:
                //  ↓へ流れる
            case R.id.goToGallery:
                //  写真管理へ
                Intent intent2 = new Intent(this, MediaAvtivity.class);
                startActivity(intent2);
                finish();
                break;

            case R.id.appFinish:
                //  終了
                finish();
                break;
            default:
        }
        return true;
    }    //  main menu

    private SurfaceHolder.Callback mSurfaceListener =
            new SurfaceHolder.Callback() {
                public void surfaceCreated(SurfaceHolder holder) {
                    try{
                    //  Camera数を取得し画面の反対側Cameraをデフォルトで開く
                    int numberOfCameras = Camera.getNumberOfCameras();
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    int defaultCameraId = -1;
                    int currentCameraID = 0;
                    if(numberOfCameras > 1) {
                        for (int i = 0; i < numberOfCameras; i++) {
                            Camera.getCameraInfo(i, cameraInfo);
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                //  機器の初期メインカメラIDを記録
                                Constants.setPrefrenceInt(CameraActiviity2.this,Constants.MAIN_CAMERA_ID, i);
                                defaultCameraId = i;
                            }
                            else{   //  3つ以上あると大きい方がサブカメラ
                                Constants.setPrefrenceInt(CameraActiviity2.this,Constants.SUB_CAMERA_ID, i);
                            }
                        }
                        //  設定されているカメラでOPEN。未設定なら機種のメインカメラ。
                        currentCameraID = Constants.getPrefrenceInt
                                (CameraActiviity2.this, Constants.CURRENT_CAMERA_ID, defaultCameraId);
                        camera = Camera.open(currentCameraID);
                    }
                    else{
                        //
                        camera = Camera.open();
                    }
                    tv2.setText((currentCameraID==0) ? "Mainカメラ" : "Subカメラ");

                    //  カメラのMax撮影サイズが設定されていない場合
                    int mW = Constants.getPrefrenceInt(CameraActiviity2.this,Constants.CAMERA_MAX_WIDTH,0);
                    int mH = Constants.getPrefrenceInt(CameraActiviity2.this,Constants.CAMERA_MAX_HEIGHT,0);
                    if (mW ==0 || mH == 0){
                        setCameraMaxSize(camera);
                    }
                        tv1.setText(getPresentPictureSize());   //  ここにしないと初期インストールでエラーになる！
                    //Toast.makeText(CameraActiviity2.this, String.format("%d %d",mW,mH), Toast.LENGTH_SHORT).show();
                    //camera.setDisplayOrientation(90);  // 縦に固定

                        camera.setPreviewDisplay(holder);
                        enableShutter = true;
                    } catch (Exception e) {
                        Toast.makeText(CameraActiviity2.this, "surfaceCreated", Toast.LENGTH_SHORT).show();
                        tv7.setText(e.getMessage());
                    }
                }

                public void surfaceDestroyed(SurfaceHolder holder) {
                    camera.release();
                    camera = null;
                }

                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    try {
                        // 画面サイズから適正プレビューサイズをセット
                        Camera.Parameters parameters = camera.getParameters();
                        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
                        /*
                        //String pSZs = "PreviewSizes\n";
                        for (int i = 0; i < previewSizes.size(); i++) {
                            //pSZs += String.format("%d x %d", previewSizes.get(i).width, previewSizes.get(i).height);
                            if (i < previewSizes.size() - 1) {
                                //pSZs += "\n";
                            }
                        }
                        //tv5.setText(pSZs);
                        */
                        Camera.Size optimalSize = getOptimalPreviewSize(previewSizes, width, height);
                        int pW = optimalSize.width;
                        int pH = optimalSize.height;

                        parameters.setPreviewSize(pW, pH);
                        //tv3.setText(String.format("param %d : %d x %d", format,  width, height));  //  90°回転させているので
                        //tv4.setText(String.format("Preview Set %d x %d", pH, pW));
                        camera.setParameters(parameters);
                        camera.setDisplayOrientation(90);  // 縦に固定
                        camera.startPreview();

                        // 露出の値を確認
                        mMinExposure = camera.getParameters().getMinExposureCompensation();
                        mMaxExposure = camera.getParameters().getMaxExposureCompensation();
                        mExposureValue = camera.getParameters().getExposureCompensation();
                        mBtnExposure.setText("露出 : " + mExposureValue);
                    }catch(Exception e){
                        Toast.makeText(CameraActiviity2.this, "surfaceChanged", Toast.LENGTH_SHORT).show();
                    }


                }
            };  //  ★★★画面に関するイベントのコールバックリスナー

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {

        int shortLength = Math.min(w, h);  //  短い方を基準
        int maxLength =0;
        Camera.Size rtn = null;
        //String msg ="パラメータ:" + shortLength + "\n";
        for(int i=0; i < sizes.size(); i++){
            //  短い方を取り出し
            int listLength = Math.min(sizes.get(i).width,sizes.get(i).height);
            //msg += "#" + i + " リスト短辺:"+listLength + "\n";

            if(shortLength >= listLength){    //  リストより短いもの
                if(maxLength < listLength){
                    rtn = sizes.get(i);
                    maxLength = listLength;
                    //msg +="候補の短辺:"+maxLength + "\n";
                }
            }
        }
        //tv7.setText(msg);
       return rtn;


    }

    private Camera.ShutterCallback mShutterListener =
            new Camera.ShutterCallback() {
                public void onShutter() {

                }
            };   //  ▲autofocusしない時

    private Camera.PictureCallback mPictureListener =
            new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {
                    asyncResizeAndSave(data);
                }
            };   //  ▲autofocusしない時 SDカードにJPEGデータを保存する

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (camera != null && enableShutter ) {
                enableShutter = false;
                if(aCDT != null){   //  サブタイトルの表示タイマーをキャンセル
                    aCDT.cancel();
                }
                //SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                //Integer isAF = sp.getInt("isAF", 1);

                if  (Constants.getPrefrenceBoolean(this,Constants.IS_USE_AF,true)) {
                    Log.d("■", "onTouchEvent --> AF");
                    camera.autoFocus(autoFocusCallback);
                }
                else {
                    camera.takePicture(mShutterListener, null, mPictureListener);
                    Log.d("■", "onTouchEvent --> non AF");
                }
            }
        }

        return true;
    }   //  ***タップで撮影

    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            camera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                }
            }, null, new TakePictureCallback());
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        //  センサーを実装すると必須
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION){

            int oRoll = (int)event.values[2];
            if (oRoll >= -30 && oRoll <=30) {
                mRoll = 90;
            }
            else if(oRoll < -30 &&  oRoll >=-120) {
                mRoll = 180;
            }
            else if(oRoll >30 && oRoll <=120) {
                mRoll = 0;
            }
            else {
                mRoll = 90;
            }
        }
    }    //  回転センサー

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {




        setLastLocation(location);

        Toast.makeText(this, "更新完了" , Toast.LENGTH_SHORT).show();
        tv4.setText(presentAddr);


        aCDT.cancel();
        aCDT = null;  //  subTitleのガイダンスを中止
        mLocationManager.removeUpdates(this);

    }    //  GPS・NETWORK

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    class ActionBarCountDownTimer extends CountDownTimer{
        private int mCnt = 0;
        //  ActionBarのサブタイトルにガイドやGPS情報等を標示する
        public ActionBarCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);

        }
        @Override
        public void onFinish() {

            Toast.makeText(CameraActiviity2.this, "位置情報取得に失敗しました。", Toast.LENGTH_SHORT).show();
            tv4.setText("");
            presentAddr = "";
            mLocationManager.removeUpdates(CameraActiviity2.this);
        }
        @Override
        public void onTick(long millisUntilFinished){   //  残時間
            mCnt ++;
            String[] keisen = {"位置更新中",""};    //
            tv4.setText(keisen[mCnt % 2]);

        }

    }   //  タイマー

    class TakePictureCallback implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            //Toast.makeText(getContext(), "TakePictureCallback", Toast.LENGTH_SHORT).show();
            //resizeAndSavePicture(data);
            asyncResizeAndSave(data);
        }
    }

    private void asyncResizeAndSave(byte[] data){
        // ■■■■■■■■　　■■■■■■■
        //　無名クラスで非同期処理実装
        new AsyncTask<byte[] , String, Integer>(){

            private ProgressDialog progressDialog = null;
            //Integer mScaleID;
            Integer[] mScale;
            String AttachName;
            Boolean isKiritori;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(CameraActiviity2.this);
                progressDialog.setTitle("画像を保存しています...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setMessage("");
                progressDialog.show();

                //  設定値に長辺を合わせて縮小
                CameraSizesFromResource cR = new CameraSizesFromResource(CameraActiviity2.this);
                mScale = cR.getSizeByID(Constants.getPrefrenceInt
                        (CameraActiviity2.this,Constants.PHOTOSIZE_ID, 2));

                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd_HHmmss");
                AttachName = sdf.format(date) + ".jpg"; //  保存ファイル名を先にGETしておく
                isKiritori = Constants.getPrefrenceBoolean
                        (CameraActiviity2.this,Constants.IS_KIRITORI, false);   //  保存値を調べて設定

            }

            //  非同期処理
            @Override
            protected Integer doInBackground(byte[]... params) {
                //Long startTime = System.currentTimeMillis();
                //Long rtnTime = (long) 0;
                Integer lastID=-1;
                if (params[0] != null) {
                    try {
                        Bitmap bitmap_1 = BitmapFactory.decodeByteArray(params[0], 0, params[0].length);
                        //Bitmap bitmap_2 = null;

                        //if (mScaleID > 0) {  //  原画のままでない
                            publishProgress("圧縮サイズを計算...");
                            //  収縮及び擬似ズームをする
                            //int aRoll = isReverse ? mRoll +180 : mRoll;
                        Bitmap bitmap_2 = getResizedBitmap(bitmap_1, mScale[0], mScale[1], mRoll, isKiritori);
                        /*
                        } else {   //  縮小なし
                            Matrix matrix = new Matrix();
                            matrix.postRotate(mRoll);  //  向きを調整
                            bitmap_2 = Bitmap.createBitmap
                                    (bitmap_1, 0, 0, bitmap_1.getWidth(), bitmap_1.getHeight(), matrix, true);
                        }
                        */

                        Bitmap bitmap_2_a;
                        //  現在地と時刻を画像に入れる
                        Boolean mDateLabel = Constants.getPrefrenceBoolean(CameraActiviity2.this,Constants.IS_DATE_LABEL ,true);
                        Boolean mLocLabel = Constants.getPrefrenceBoolean(CameraActiviity2.this, Constants.IS_LOCATION_LABEL, true);
                        if(!mDateLabel && !mLocLabel){  //  追加なし
                            bitmap_2_a = bitmap_2;  //  そのまま
                        }
                        else{
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                            String mThisTime = (mDateLabel) ? sdf.format(calendar.getTime()) : "";
                            presentAddr = (mLocLabel) ? presentAddr : "";
                            //String mID = String.format("%d",MediaAvtivity.getLastContentID(getApplicationContext()) + 1);
                            //bitmap_2_a = addTextToBitmap(bitmap_2, mID);
                            bitmap_2_a = addTextToBitmap(bitmap_2, presentAddr + " " + mThisTime);
                        }

                        publishProgress("保存中...");
                        final FileOutputStream out = openFileOutput(AttachName, Context.MODE_PRIVATE);
                        bitmap_2_a.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.close();
                        DatabaseHelper DH = new DatabaseHelper(getApplicationContext());
                        publishProgress("画像保存完了...");
                        lastID = DH.insert(bitmap_2_a, AttachName);  //  Databaseに登録
                        DH.close();
                        publishProgress("DB登録完了...");
                        //rtnTime = System.currentTimeMillis() - startTime;
                    }catch (NullPointerException e) {

                       lastID = -1;
                    }catch (Exception e) {

                       lastID = -1;
                    }

                }
                return lastID;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                // 進捗をメッセージで表示
                progressDialog.setMessage(values[0]);
            }

            @Override
            protected void onPostExecute(Integer mlastID){
                //  メインactivityのstaticメソッドでcontextproviderに登録
                if (mlastID >= 0) {   //  失敗すると　-1を返すので共用画面を呼ばない
                    // ダイレクト共有モード！
                    if(Constants.getPrefrenceBoolean(CameraActiviity2.this, Constants.IS_DIRECT_SHARE, false)) {
                        Constants.setPrefrenceInt(CameraActiviity2.this, Constants.ID_SELECTED, mlastID);
                        Intent intent = new Intent(CameraActiviity2.this, Preview_Activity.class);
                        startActivity(intent);
                        finish();
                    }

                }
                else{
                    Toast.makeText(CameraActiviity2.this, "処理できませんでした。\n保存サイズを小さくしてみてください。",
                            Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
                enableShutter = true;   //  撮影可能にする


            }
        }.execute(data);
    }   //  ★非同期で画像縮小・保存

    static Bitmap getResizedBitmap(Bitmap orgBMP,int targetW,int targetH,int aRoll,Boolean Kiritori){
        //  ターゲットのサイズに収縮したbitmapを返す
        int x_1 = orgBMP.getWidth();    //  元の幅
        int y_1 = orgBMP.getHeight();   //  元の高さ
        int x_2 = targetW;    //  ターゲット幅
        int y_2 = targetH;    //  ターゲット高さ
        Matrix matrix = new Matrix();

        Log.d("■orgnal ","x_1="+ x_1 +":y_1="+ y_1);
        Log.d("■target ", "x_2=" + x_2 + ":y_2=" + y_2);

        //  元の画像から切り出す部分（収縮の元になる）
        int x_3=x_1,y_3=y_1,startX=0, startY=0;
        float mRatio;   //  収縮率
        Bitmap bitmap_2 = null  ; // ターゲット画像

        Kiritori = false;   //  ▼当面使わない
        if(Kiritori){
            if(x_1 > x_2 && y_1 > y_2){
                x_3 = x_2;
                y_3 = y_2;
                startX = (x_1 - x_2) / 2;
                startY = (y_1 - y_2) / 2;
                mRatio = 1;
            }
            else{
                //  残念なお知らせを画像を作成
                Bitmap bitmap_2_0 = Bitmap.createBitmap
                        (640, 480, Bitmap.Config.ARGB_8888);    //  ベースキャンバスを作る
                Canvas canvas = new Canvas(bitmap_2_0);
                canvas.drawBitmap(orgBMP, 0, 0, null);     //  bitmapを描写
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // テキスト用paint作成
                paint.setColor(Color.RED);

                paint.setTextSize(40);
                canvas.drawText("画像作成に失敗しました。", 100, 150, paint);   //  テキスト描写
                canvas.drawText("保存サイズを、",100,200,paint);
                canvas.drawText("Device Maxより",100,250,paint);
                canvas.drawText("縦・横とも小さく",100,300,paint);
                canvas.drawText("設定してください。",100,350,paint);

                return bitmap_2_0;  //  Canvasで操作したbitmapを返す
            }
        }
        else {
            if (y_1 / x_1 < y_2 / x_2) {  //  ターゲットの方が縦長の場合
                y_3 = y_1;
                x_3 = (int) (y_1 * ((float) x_2 / y_2));
                startX = (x_1 - x_3) / 2;
                startY = 0;
                mRatio = (float) y_2 / y_1;
            } else {                               //  ターゲットの方が横長の場合
                x_3 = x_1;
                y_3 = (int) (x_1 * ((float) y_2 / x_2));
                startX = 0;
                startY = (y_1 - y_3) / 2;
                mRatio = (float) x_2 / x_1;
            }
        }

        Log.d("■arg ", "x_3=" + x_3 + ":y_3=" + y_3);
        Log.d("■arg ", "startX=" + startX + ":startY=" + startY);
        Log.d("■arg ", "mRatio=" + mRatio);

        matrix.postScale(mRatio, mRatio);
        matrix.postRotate(aRoll);  //  向きを調整
        bitmap_2 = Bitmap.createBitmap(orgBMP, startX, startY, x_3, y_3, matrix, true);

        return bitmap_2;
    }

    private void setCameraMaxSize(Camera camera){
        //  カメラのMAX撮影サイズを保存しておく（通常はインストール時のみ）
        Integer[] _mSizes = new Integer[2];
        Camera.Parameters prams = camera.getParameters();
        List<Camera.Size> sizes = prams.getSupportedPictureSizes();
        long sizeMax = 0;
        for (int i = 0; i < sizes.size(); i++) {
            long mMax = sizes.get(i).width * sizes.get(i).height;

            if (mMax > sizeMax) {
                _mSizes[0] = sizes.get(i).width;
                _mSizes[1] = sizes.get(i).height;
                sizeMax = mMax;
            }
        }
        Constants.setPrefrenceInt(this, Constants.CAMERA_MAX_WIDTH, _mSizes[0]);
        Constants.setPrefrenceInt(this, Constants.CAMERA_MAX_HEIGHT, _mSizes[1]);
    }

    public Bitmap addTextToBitmap (Bitmap org, String aText){

        int aTextSize =(int) (org.getWidth() / 25 );
        Bitmap bitmap_rtn = Bitmap.createBitmap
                (org.getWidth(), org.getHeight(), Bitmap.Config.ARGB_8888);    //  ベースキャンバスを作る

        Canvas canvas = new Canvas(bitmap_rtn);
        canvas.drawBitmap(org, 0, 0, null);     //  bitmapを描写
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // テキスト用paint作成
        paint.setColor(Color.BLACK);

        float aDiff = 0.5F +
                ( aTextSize * CameraActiviity2.this.getResources().getDisplayMetrics().scaledDensity ) /75;

        paint.setShadowLayer(2f, aDiff, aDiff, Color.rgb(192,192,192));
        paint.setTextSize(aTextSize);
        int padding = (int) aTextSize /3;
        canvas.drawText(aText, padding, org.getHeight() - padding, paint);   //  テキスト描写

        return bitmap_rtn;  //  Canvasで操作したbitmapを返す
    }   //  画像にテキスト追加

    static  Bitmap transferRoundRectImage(Context context, Bitmap org){

         // 画像サイズ取得
        int width  = org.getWidth();
        int height = org.getHeight();

        // リサイズ後サイズ
        int w = width;
        int h = height;

        double mRadiusRatio =
                Constants.getPrefrenceBoolean(context, Constants.IS_RECT_RADIUS_BIGGER, true)
                ? 0.2 : 0.1;

        int mR = (int) (w * mRadiusRatio);   //

         // 切り取り領域となるbitmap生成
        Bitmap clipArea = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

         // 角丸矩形を描写（切り取り用）
         // RectFで角丸描写、arg3、arg4は半径、ANTI_ALIASでふちをぼかして滑らかにする
        Canvas c = new Canvas(clipArea);
        c.drawRoundRect(new RectF(0, 0, w, h), mR, mR, new Paint(Paint.ANTI_ALIAS_FLAG));

        // 角丸画像となるbitmap生成
        Bitmap newImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        // 切り取り領域を描写
        Canvas canvas = new Canvas(newImage);
        Paint paint = new Paint();
        canvas.drawBitmap(clipArea, 0, 0, paint);

        // 切り取り領域内にオリジナルの画像を描写
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(org, new Rect(0, 0, width, height), new Rect(0, 0, w, h), paint);

         //
        return  newImage;

    }   //  角丸に加工する【新】

    private void setLocationMgr(boolean mMode) { //  trueにすると強制実行
        tv4.setText("");
        tv7.setText("");
        String provider;

        try {
            //  ①LocationManagerを生成。失敗したら何もしない。
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //  ②getBestProvider （方位、速度、高度はfalseにしても良い？）
            final Criteria criteria = new Criteria();
            criteria.setBearingRequired(false);    // 方位不要
            criteria.setSpeedRequired(false);    // 速度不要
            criteria.setAltitudeRequired(false);    // 高度不要
            provider = mLocationManager.getBestProvider(criteria, true);
            if (provider == null) {
                throw new Exception();
            }
        }catch(Exception e){
            tv7.setText("provider取得に失敗");
            return;
        }

            //  ③最後に取得した情報をGET
        try {
            int timeAgo;
            final Location lastKnownLocation = mLocationManager.getLastKnownLocation(provider);
            if(lastKnownLocation != null){
                timeAgo = (int) ((System.currentTimeMillis() - lastKnownLocation.getTime()) / (60 * 1000));
            }
            else {
                timeAgo = 9999;
            }

            int avlTime = Constants.locationAvailableMNT[
                    Constants.getPrefrenceInt(getApplicationContext(),
                    Constants.LOCATION_AVAILABLE_MINUTES,0)];
            //Toast.makeText(this, "" + avlTime , Toast.LENGTH_SHORT).show();
            if (!mMode && timeAgo < avlTime  ) {
                //  設定時間内なら再取得しない
                setLastLocation(lastKnownLocation);
                tv4.setText(timeAgo + "分前 : " + presentAddr);
                //Toast.makeText(this,timeAgo + "分前に取得した\n" + presentAddr + " を採用します。" , Toast.LENGTH_LONG).show();
                return;
            } else {

                Toast.makeText(this, provider + "を使用して位置情報を更新します。", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            tv7.setText("前回情報取得に失敗");
            return;
        }

        try {
            //  ④情報取得できない場合
            mLocationManager.requestLocationUpdates(provider, 0, 0, this);

            if (aCDT != null) {
                aCDT = null;
            }
            aCDT = new ActionBarCountDownTimer(60000, 1000);    //  期限を60秒にセット
            aCDT.start();
        }catch(Exception e){
            tv7.setText("タイマー設定に失敗。");
        }

    }    //  Location Providerを決めて更新を監視スタート。同時にタイマーで期限設定する。

    public void makeCameraSettingDialog(Context context){

        //try {
            //  撮影サイズを設定
            LinearLayout alertLayout0 = new LinearLayout(context);
            alertLayout0.setOrientation(LinearLayout.VERTICAL);
            alertLayout0.setPadding(15, 15, 15, 15);
            alertLayout0.setBackgroundColor(Color.argb(32, 0, 0, 255)); //  第一パラメータを0にすると透明

            LinearLayout LL1 = new LinearLayout(context);
            LL1.setOrientation(LinearLayout.HORIZONTAL);

            TextView textView0 = new TextView(context);
            textView0.setTextSize(16);
            textView0.setPadding(0,8,0,0);
            textView0.setText("写真の保存サイズ");
            textView0.setTextColor(Color.argb(255, 0, 0, 255));
            //alertLayout0.addView(textView0);
            //  チェックboxを作成してレイアウトに追加
            final RadioGroup radioGroup = new RadioGroup(context);
            //   リソースファイルからGET（一番目はH/Wmaxに予約）
            CameraSizesFromResource cR = new CameraSizesFromResource(context);
            ArrayList<String> mLables = cR.getSizeLabels();
            final RadioButton[] radioButtons = new RadioButton[mLables.size()];

            for (int i = 0; i < mLables.size(); i++) {
                radioButtons[i] = new RadioButton(context);
                String radioTxt = mLables.get(i);
                radioButtons[i].setText(radioTxt);
                radioButtons[i].setTextSize(12);
                radioGroup.addView(radioButtons[i]);
            }

            int hozonSize = Constants.getPrefrenceInt(context,Constants.PHOTOSIZE_ID, 2);
            radioButtons[hozonSize].setChecked(true);

            LL1.addView(textView0);
            LL1.addView(radioGroup);

            alertLayout0.addView(LL1);

            //alertLayout0.addView(radioGroup);

            int currentCameraID  = Constants.getPrefrenceInt
                    (context, Constants.CURRENT_CAMERA_ID, 0);   //  使用するカメラ
            final Switch sw = new Switch(context);
            sw.setText("メインカメラを使用");
            sw.setTextSize(16);
            sw.setTextColor(Color.argb(255, 0, 0, 255));
            sw.setChecked(currentCameraID==0 ? true : false);
            alertLayout0.addView(sw);
                /*
            //  切り取りズーム
            Boolean isKiritori = Constants.getPrefrenceBoolean(context,Constants.IS_KIRITORI, false);
            final CheckBox checkBox = new CheckBox(this);
            CharSequence cSq = Html.fromHtml("<font color=\"blue\">切取りZoom</font><br>"
                    + "<font color=\"black\"><small>映像中央を縮小せずに切り取ってズームします。</small></font>");
            checkBox.setText(cSq);
            checkBox.setTextColor(Color.argb(255, 0, 128, 128));
            checkBox.setChecked(isKiritori);
            alertLayout0.addView(checkBox);
            */

            //  AFを使うか？
            final CheckBox checkBox4 = new CheckBox(context);
            checkBox4.setText("オートフォーカスを使う");
            checkBox4.setTextSize(16);
            checkBox4.setTextColor(Color.argb(255, 0, 0, 255));
            Boolean is_Use_AF = Constants.getPrefrenceBoolean(context,Constants.IS_USE_AF, true);
            checkBox4.setChecked(is_Use_AF);

            alertLayout0.addView(checkBox4);

            AlertDialog.Builder builder0 = new AlertDialog.Builder(context);
            //  作成したレイアウトをセット
            builder0.setView(alertLayout0)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (int i=0; i < radioButtons.length; i++) {
                                        if(radioButtons[i].isChecked()){
                                            Constants.setPrefrenceInt(getApplicationContext(),Constants.PHOTOSIZE_ID, i);
                                            tv1.setText(getPresentPictureSize());
                                        }
                                    }
                                    Constants.setPrefrenceInt
                                            (getApplicationContext(),Constants.CURRENT_CAMERA_ID, sw.isChecked() ? 0 : 1);

                                    /*
                                   //   切り取りズームを使う
                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(),Constants.IS_KIRITORI, checkBox.isChecked());
                                            */

                                    //  AFを使う？
                                    Constants.setPrefrenceBoolean
                                                (getApplicationContext(),Constants.IS_USE_AF, checkBox4.isChecked());
                                }
                            })
                    .setNegativeButton("キャンセル",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .show();
        //}catch (Exception e){
        //   Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        //}

    }   //  カメラ設定

    public void makeOtherSettingDialog(Context context){

        try {
            //  撮影サイズを設定
            LinearLayout alertLayout0 = new LinearLayout(context);
            alertLayout0.setOrientation(LinearLayout.VERTICAL);
            alertLayout0.setPadding(15, 15, 15, 15);
            alertLayout0.setBackgroundColor(Color.argb(32, 0, 0, 255)); //  第一パラメータを0にすると透明

            Boolean IsDateLabel = Constants.getPrefrenceBoolean
                    (context, Constants.IS_DATE_LABEL, true);   //  写真に日付を付ける？
            final CheckBox checkBox = new CheckBox(this);
            checkBox.setText("写真に撮影日を付ける");
            checkBox.setTextSize(16);
            checkBox.setTextColor(Color.argb(255, 0, 0, 255));
            checkBox.setChecked(IsDateLabel);
            alertLayout0.addView(checkBox);

            Boolean IsLocLabel = Constants.getPrefrenceBoolean
                    (context,Constants.IS_LOCATION_LABEL, true);   //  写真に現在地を付ける？
            final CheckBox checkBox1 = new CheckBox(this);
            checkBox1.setText("写真に現在地を付ける");
            checkBox1.setTextSize(16);
            checkBox1.setTextColor(Color.argb(255, 0, 0, 255));
            checkBox1.setChecked(IsLocLabel);
            alertLayout0.addView(checkBox1);

            LinearLayout LL1 = new LinearLayout(context);
            LL1.setOrientation(LinearLayout.HORIZONTAL);
            TextView textView0 = new TextView(context);
            textView0.setText("位置情報有効期限（分）");
            textView0.setTextSize(16);
            textView0.setTextColor(Color.argb(255, 0, 0, 255));
            LL1.addView(textView0);

            final Spinner spinner = new Spinner(context);
            spinner.setMinimumHeight(12);
            ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(context,
                    android.R.layout.simple_list_item_single_choice,Constants.locationAvailableMNT );
            spinner.setAdapter(adapter);

            int locationMode = Constants.getPrefrenceInt
                    (context, Constants.LOCATION_AVAILABLE_MINUTES, 0);   //  前回場所有効時間
            spinner.setSelection(locationMode);
            LL1.addView(spinner);
            alertLayout0.addView(LL1);

            Boolean IsLocSearchl = Constants.getPrefrenceBoolean
                    (context,Constants.IS_LOCATION_SEARCH, true);   //   位置情報調査をする？
            final CheckBox checkBox2 = new CheckBox(this);
            checkBox2.setText("位置情報検出をする");
            checkBox2.setTextSize(16);
            checkBox2.setTextColor(Color.argb(255, 0, 0, 255));
            checkBox2.setChecked(IsLocSearchl);
            alertLayout0.addView(checkBox2);

            Boolean IsRollSearchl = Constants.getPrefrenceBoolean
                    (context,Constants.IS_ROLLING_SEARCH, true);   //   傾き検知をする？
            final CheckBox checkBox3 = new CheckBox(this);
            checkBox3.setText("傾き検知をする");
            checkBox3.setTextSize(16);
            checkBox3.setTextColor(Color.argb(255, 0, 0, 255));
            checkBox3.setChecked(IsRollSearchl);
            alertLayout0.addView(checkBox3);

            Boolean IsDirectShare = Constants.getPrefrenceBoolean
                    (context,Constants.IS_DIRECT_SHARE, false);   //   ダイレクト共有
            final CheckBox checkBox4 = new CheckBox(this);
            checkBox4.setText("ダイレクトに共有画面へ");
            checkBox4.setTextSize(16);
            checkBox4.setTextColor(Color.argb(255, 0, 0, 255));
            checkBox4.setChecked(IsDirectShare);
            alertLayout0.addView(checkBox4);

            AlertDialog.Builder builder0 = new AlertDialog.Builder(context);
            //  作成したレイアウトをセット
            builder0.setView(alertLayout0)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Constants.setPrefrenceInt
                                            (getApplicationContext(),Constants.LOCATION_AVAILABLE_MINUTES, spinner.getSelectedItemPosition());

                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(), Constants.IS_DATE_LABEL, checkBox.isChecked());

                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(), Constants.IS_LOCATION_LABEL, checkBox1.isChecked());
                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(), Constants.IS_LOCATION_SEARCH, checkBox2.isChecked());
                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(), Constants.IS_ROLLING_SEARCH, checkBox3.isChecked());
                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(), Constants.IS_DIRECT_SHARE, checkBox4.isChecked());
                                }
                            })
                    .setNegativeButton("キャンセル",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    .show();
        }catch (Exception e){
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }   //  その他の設定

    public boolean setLastLocation(Location location){
        //  locationを記録しておく
        try {
            float thisLat = (float)location.getLatitude();
            float thisLng = (float)location.getLongitude();

            Constants.setPrefrenceFloat(getApplicationContext(), Constants.LAST_LATITUDE, thisLat);
            Constants.setPrefrenceFloat(getApplicationContext(), Constants.LAST_LONGITUDE, thisLng);

            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.JAPAN);
            //List<Address> list_address = null;

            List<Address> list_address = geocoder.getFromLocation(thisLat, thisLng, 2);
            if(list_address.size() != 0) {
                presentAddr = list_address.get(1).getLocality();
                presentAddr += list_address.get(1).getSubLocality() == null ? "" : list_address.get(1).getSubLocality();
                    //  list_address.get(1).getAdminArea() //   県名は長くなるのでカット
            }
            else{
                presentAddr = " - - - ";
            }
            Constants.setPrefrenceString(getApplicationContext(), Constants.LAST_LOCATION_NAME, presentAddr);
            return true;

        } catch (Exception e) {
            tv7.setText("地名変換に失敗");
            presentAddr = " - - - ";
            return false;
        }
    }

    private String getPresentPictureSize(){
        CameraSizesFromResource cR = new CameraSizesFromResource(CameraActiviity2.this);
        Integer[] mScale;
        mScale = cR.getSizeByID(Constants.getPrefrenceInt
                (CameraActiviity2.this,Constants.PHOTOSIZE_ID, 2));

        return String.format("%d x %d", mScale[0], mScale[1]);

    }

    private void setExposure() {
        if (camera != null) {
            mExposureValue++;
            if (mExposureValue > mMaxExposure) {
                mExposureValue = mMinExposure;
            }
            Camera.Parameters params = camera.getParameters();
            params.setExposureCompensation(mExposureValue);
            camera.setParameters(params);
            mBtnExposure.setText("露出 :"  + mExposureValue);
        }
    }
}