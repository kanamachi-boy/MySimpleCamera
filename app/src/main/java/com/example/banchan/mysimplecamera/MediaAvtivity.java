package com.example.banchan.mysimplecamera;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MediaAvtivity extends FragmentActivity {

    GridView gv1;
    BitmapAdapter myBMADP2;
    //boolean asyncWorking = false;
    //ArrayList<NewCameraData> TNandDATA;
    //ArrayList<String> thumbnailDATA;      //  画像データ
    //  BitmapAdapter myBMADP2;          //  表示用アダプター
    //CheckedImageArrayAdaptor myBMADP2;  //  checkbox付き対応
    //ArrayList<Bitmap> thumbnails;        //    サムネイル
    //String absPath = "";
    //int tnLimit = 0;
    //boolean tnSize =true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        //ActionBar actionBar = getActionBar();

        gv1 =(GridView) findViewById(R.id.GV_1);

        //  ★画像一覧の表示
        getThumbnails2();

        //  【１】thumbnailのどれかが選択された時 ⇒ previewを表示する
        gv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //  pathから画像を呼び出す。
                //Toast.makeText(MediaAvtivity.this, "*"+position , Toast.LENGTH_SHORT).show();
                //absPath = TNandDATA.get(position)._data;
                DatabaseHelper DBH = new DatabaseHelper(MediaAvtivity.this);
                Constants.setPrefrenceInt
                        (MediaAvtivity.this,Constants.ID_SELECTED,DBH.position2id(position));
                DBH.close();
                Intent intent = new Intent(MediaAvtivity.this, Preview_Activity.class);
                startActivity(intent);
            }
        });

        //  【２】thumbnailの無い場所を押された時 ⇒ ×fragmentが表示されていたら消す。
        gv1.setOnTouchListener(new GridView.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent evt) {
                return false; // trueにすると下層にイベントが伝わらない。
            }
        });

        registerForContextMenu(gv1); // コンテキストメニューの呼び出し元登録

        /////   他のアプリからデータ共有された場合
        /*
        Intent intentGallery;
        intentGallery = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intentGallery.setType("image/jpeg");
        Intent chooserIntent = Intent.createChooser(intentGallery, "Pickup");
        startActivityForResult(chooserIntent, 10008);
        */

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //■■■■ registerForContextMenu()で登録したViewが長押しされると、
        // onCreateContextMenu()が呼ばれる。ここでメニューを作成する。
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //  adaptorから画像indexを調べて絶対pathを取得
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        //absPath = TNandDATA.get(info.position)._data;

        DatabaseHelper DBH = new DatabaseHelper(this);
        final int selectedID = DBH.position2id( info.position);
        final String mABSpath = DBH.getABSFilePath(selectedID);
        DBH.close();

        Log.d("■", "info.position =" + info.position + " : _id = " + selectedID);
        //NCD = DBH.getData(selectedID);

        //byte[] bt = NCD.thumbnail;
        //Bitmap aThumbNail = BitmapFactory.decodeByteArray(bt, 0, bt.length);

        Bitmap aThumbNail = myBMADP2.getItem(info.position);    //  adapterから画像を取り出す

        //final File file = new File(absPath);
        //final ContentResolver resolver = getContentResolver();
        //final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        //final String where = MediaStore.Images.Media.DATA + "=?";

        //final String[] arg = {absPath};

        switch (item.getItemId()){

            case R.id.menu_detail:
                ////   画像情報の表示
                DBH = new DatabaseHelper(this);
                showDetailDialog(DBH.getDataByHTML(selectedID), aThumbNail);
                DBH.close();
                break;

            case R.id.menu_delete:
                ////  画像の削除  確認dialogを表示
                LinearLayout alertLayout = new LinearLayout(this);
                alertLayout.setOrientation(LinearLayout.VERTICAL);
                alertLayout.setPadding(15, 15, 15, 15);
                alertLayout.setBackgroundColor(Color.argb(32, 255, 0, 0)); //  第一パラメータを0にすると透明
                TextView textTitle = new TextView(this);
                String aaa[] = mABSpath.split("/",0);
                textTitle.setText
                        (aaa[aaa.length -1 ] + "\nを削除してよろしいですか？\n");
                textTitle.setTextColor(Color.BLUE);
                textTitle.setTextSize(12);
                alertLayout.addView(textTitle);
                ImageView alertImg = new ImageView(this);
                alertImg.setImageBitmap(aThumbNail);
                alertLayout.addView(alertImg);

                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setView(alertLayout)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        File file = new File(mABSpath);
                                        final DatabaseHelper DBH = new DatabaseHelper(MediaAvtivity.this);
                                        int delCnt = DBH.delete(selectedID);
                                        DBH.close();

                                        if (delCnt  == 1 && file.delete()) {
                                            Toast.makeText(MediaAvtivity.this, " 削除しました。",
                                                    Toast.LENGTH_SHORT).show();
                                            //  bitmapリスト、データリスト、bitmapのアダプタを更新
                                            getThumbnails2();
                                        } else {
                                            Toast.makeText(MediaAvtivity.this, "※画像削除に失敗しました...", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                })
                        .setNegativeButton("キャンセル",
                                new DialogInterface.OnClickListener(){
                                    @Override
                                    public  void onClick(DialogInterface dialog, int which){
                                    }
                                })
                        .show();


                break;
            case R.id.menu_share:
                //  データ共有
                shareImageAndMessages(this, selectedID);
                break;

                }
        DBH.close();
        return true;
        }   //  ■コンテキストMenu

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //■■■■   オプションメニュー表示
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_media, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //  保存された圧縮値をメニューにセット
        // 画面の幅を記録しておく
        WindowManager wm = getWindowManager();
        Display disp = wm.getDefaultDisplay();
        Point p1 = new Point();
        disp.getSize(p1);
        Constants.setPrefrenceInt(this, Constants.DISPLAY_WIDTH, p1.x);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.syncroBMP:
                //  サムネイルの再表示
                getThumbnails2();
                break;

            case R.id.Del24:
                //  新しい画像を一括削除
                deletePhotoDayBrefore
                        (Constants.deleteHoursBefore
                                [Constants.getPrefrenceInt(this, Constants.DELETE_HOURS_ID, 2)]
                        );
                break;

            case R.id.setGallerySetting:
                //  Gallery設定
                makeGallerySettingDialog(this);
                break;

            case R.id.deletePhotoForShare:
                //  転送用に作成したファイルの削除　→　自動化する？
                try {
                    String SAVE_DIR = "/ForShare/";
                    File mDir = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
                    File[] mList = mDir.listFiles();
                    String mStr = "";
                    int delCnt = 0;
                    if(mList.length == 0){  //  ファイルが無い場合エラー扱いとする
                        throw new Exception();
                    }
                    for (int i = 0; i < mList.length; i++) {
                        File delFile = new File(mList[i].toString());
                        int a = delFile.delete() ? delCnt++ : 0;
                    }
                    Toast.makeText(this, ( mList.length == delCnt ) ?
                            delCnt + "ファイル削除しました。" : "削除失敗...", Toast.LENGTH_SHORT).show();
                }catch(Exception e){
                    Toast.makeText(this, "削除するファイルはありません。", Toast.LENGTH_SHORT).show();
                }


                break;

            case R.id.appFinish:
                //  アプリの終了
                finish();
                break;

            case R.id.launchCamera2:
                //  カメラの起動
                //  ↓へ流れる
            case R.id.launchCamera:
                Intent intent2 = new Intent(this, CameraActiviity2.class);
                startActivity(intent2);
                finish();
                break;

            default:
        }
        return true;
    }   //  ■オプションMenu

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == 31) {
            //  カメラAPIから
            // contentproviderの変更を、 bitmapリスト・データリスト・bitmapのアダプタに反映して再表示を通達
            getThumbnails2();

        }
    }

    public boolean deletePhotoDayBrefore(long aHours){
        //  ■■■aDays日以内を削除■■■
        //  現在時刻（ミリ秒）からaDays分引く

        final DatabaseHelper DBH = new DatabaseHelper(this);
        ArrayList<NewCameraData> NCD = new ArrayList<NewCameraData>();
        NCD = DBH.getAllData(0);
        if(NCD == null){
            Toast.makeText(this, "該当するファイルがありません。", Toast.LENGTH_SHORT).show();
            DBH.close();
            return false;
        }
        ArrayList<Bitmap> delBMP = new ArrayList<Bitmap>();
        final Map<Integer, String> delList = new HashMap<Integer, String>();
        for (int i=0; i < NCD.size(); i++){
            //
            if (aHours > DBH.howLongFromCreated(NCD.get(i)._id)){
                delBMP.add(
                        BitmapFactory.decodeByteArray(NCD.get(i).thumbnail, 0, NCD.get(i).thumbnail.length));
                delList.put(NCD.get(i)._id, DBH.getABSFilePath(NCD.get(i)._id));
            }
        }
        DBH.close();


        //  レイアウトを作成
        LinearLayout alertLayout = new LinearLayout(this);
        alertLayout.setOrientation(LinearLayout.VERTICAL);
        alertLayout.setPadding(15, 15, 15, 15);
        alertLayout.setBackgroundColor(Color.argb(32, 255, 0, 0)); //  第一パラメータを0にすると透明
        //  テキストを作成してレイアウトに追加
        TextView textView = new TextView(this);
        textView.setText(aHours + " 時間以内に作成された画像をまとめて削除します。");
        alertLayout.addView(textView);
        //  グリッドを作成、削除リストからADPを生成してグリッドにセットし、レイアウトに追加
        GridView gridView = new GridView(this);

        int dW = Constants.getPrefrenceInt(getApplicationContext(), Constants.DISPLAY_WIDTH, 0);
        int bmW =
        Constants.getPrefrenceBoolean(getApplicationContext(),Constants.THUMBNAIL_ENLARGE,true)
                ? 138 : 92;
        int mnCol = (int) ((dW *0.9 ) / (bmW + 6));   //  画面幅よりやや狭いので　*0.9

        gridView.setNumColumns(mnCol);
        gridView.setPadding(2,2,2,2);
        gridView.setVerticalScrollBarEnabled(true);
            //  カスタムADPにBMPをセットして
        BitmapAdapter delADP = new BitmapAdapter(this,R.layout.list_item,delBMP);
        gridView.setAdapter(delADP);
        alertLayout.addView(gridView);

        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        //  作成したレイアウトをセット
        builder2.setView(alertLayout)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //  まとめて削除→非同期処理化

                                asyncDeleting(delList);

                                getThumbnails2();
                            }
                        })
                .setNegativeButton("キャンセル",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .show();
        return true;
    }   //  xx日前まで削除

    private long asyncDeleting (final Map<Integer, String> aDelList){
        //　非同期処理
        Long startTime = System.currentTimeMillis();

        new AsyncTask<String, Integer, Integer>(){

            private ProgressDialog progressDialog = null;
            //boolean mSizeBln;
            //int mLimit;
            Context mContext;
            Map<Integer, String> delList = aDelList;
            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(MediaAvtivity.this);
                progressDialog.setTitle("画像を削除しています...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(delList.size());
                progressDialog.show();
                mContext = MediaAvtivity.this;
            }

            //  非同期処理
            @Override
            protected Integer doInBackground(String... params) {
                final DatabaseHelper DBH = new DatabaseHelper(mContext);
                int delCnt=0,resCnt=0;
                for( Map.Entry<Integer, String> e : delList.entrySet()){
                    File file = new File(e.getValue());
                    if(file.delete()){  //  まずファイルを削除
                        delCnt++;
                        resCnt += DBH.delete(e.getKey());   //  レコード削除
                        publishProgress(resCnt);
                    }
                }
                DBH.close();
                //  削除リスト数・ファイル削除数・レコード削除数が一致すれば成功
                if((delList.size() == delCnt) && (delCnt== resCnt)){
                    return delCnt;
                }
                else{
                    return -1;
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                // 進捗
                progressDialog.setProgress(values[0].intValue());
            }

            @Override
            protected void onPostExecute(Integer result){

                if(result == -1 ){
                    Toast.makeText(MediaAvtivity.this, "※画像削除に失敗しました...",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MediaAvtivity.this, String.format("%d 枚削除しました。",result),
                            Toast.LENGTH_SHORT).show();
                }
                progressDialog.dismiss();
            }
        }.execute();
        return System.currentTimeMillis() - startTime;
    }   //  ◆【非同期】　まとめて削除

    private long getThumbnails2 (){
        //　非同期処理
        Long startTime = System.currentTimeMillis();

        new AsyncTask<String, Integer, ArrayList<Bitmap>>(){

            private ProgressDialog progressDialog = null;
            //boolean mSizeBln;
            //int mLimit;
            Context mContext;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(MediaAvtivity.this);
                progressDialog.setTitle("画像を準備しています...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
                //mSizeBln = Constants.getPrefrenceBoolean(getApplicationContext(),Constants.THUMBNAIL_ENLARGE,false);
                //mLimit = Constants.displayQty
                //        [Constants.getPrefrenceInt(getApplicationContext(),Constants.DISPLAY_QTY,2)];
                mContext = MediaAvtivity.this;
            }

            //  非同期処理
            @Override
            protected ArrayList<Bitmap> doInBackground(String... params) {
                DatabaseHelper DBH = new DatabaseHelper(mContext);
                ArrayList<Bitmap> rtn = DBH.getThumbnails();
                DBH.close();
                return rtn;

            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                // 進捗
                //if(!is1stTime) {
                 //   progressDialog.setProgress(values[0].intValue());
                  //  progressDialog.setMax(values[1].intValue());
                    //Log.d("■Async ","cnt=" + values[0].intValue());
                //}
            }

            @Override
            protected void onPostExecute(ArrayList<Bitmap> result){
                //thumbnailDATA = getImgDATA();     //  ★データと同期させる。注意！
                //TNandDATA = result;
                if(result == null){

                    gv1.setAdapter(null);   //  最後の1枚を削除したときに残ってしまう対策

                    //  次にここから始動すると真っ暗な画面になってしまうのでユーザーに カメラ or 終了を促がす
                    LinearLayout alertLayout = new LinearLayout(MediaAvtivity.this);
                    alertLayout.setOrientation(LinearLayout.VERTICAL);
                    alertLayout.setPadding(15, 15, 15, 15);
                    alertLayout.setBackgroundColor(Color.argb(32, 0, 255, 0)); //  第一パラメータを0にすると透明
                    ImageView alertImg = new ImageView(MediaAvtivity.this);
                    alertImg.setImageResource(R.drawable.icon_6m_32);
                    alertLayout.addView(alertImg);

                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MediaAvtivity.this);
                    builder2.setTitle("写真ホルダー");
                    builder2.setMessage("表示できる写真がありません...");
                    builder2.setView(alertLayout)
                            .setPositiveButton("カメラを起動",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent2 = new Intent(MediaAvtivity.this, CameraActiviity2.class);
                                            startActivity(intent2);
                                            finish();
                                        }
                                    })
                            .setNegativeButton("アプリを終了",
                                    new DialogInterface.OnClickListener(){
                                        @Override
                                        public  void onClick(DialogInterface dialog, int which){
                                            finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();

                }
                else {
                    try {
                        if(myBMADP2 != null){
                            myBMADP2 = null;
                        }
                        myBMADP2 = new BitmapAdapter(mContext, R.layout.list_item, result);

                        //
                        int dW = Constants.getPrefrenceInt(getApplicationContext(), Constants.DISPLAY_WIDTH, 0);
                        //int bmW = aaa.get(0).getWidth();
                        int bmW =
                        Constants.getPrefrenceBoolean(getApplicationContext(), Constants.THUMBNAIL_ENLARGE, true)
                                ? 136 : 92;
                        //
                        int mnCol = (int) (dW / (bmW + 5));   //  画像横幅+5が何枚入るか？で列数を決める

                        gv1.setNumColumns(mnCol);

                        gv1.setAdapter(myBMADP2);
                        Log.d("■", "setAdapter");
                        //myBMADP2.clear();
                        //myBMADP2=null;

                    } catch (Exception e) {
                        Toast.makeText(MediaAvtivity.this, "写真の表示に失敗しました。", Toast.LENGTH_SHORT).show();
                    }
                }
                progressDialog.dismiss();
            }
        }.execute();
        return System.currentTimeMillis() - startTime;
    }   //  ◆【非同期】　サムネイルの表示

    static long getLastContentID(Context context) throws IOException {
        // 最後のIDをGET
        long rtn;

        ContentResolver contentResolver = context.getContentResolver();
        String fields[] = {MediaStore.Images.Media._ID};
        Cursor cr =contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                fields,
                null,
                null,
                MediaStore.Images.Media._ID + " DESC"
        );
        cr.moveToFirst();
        rtn = cr.getLong(0);
        cr.close();

        return rtn;
    }

    static Bitmap makeReasonableBitmap(String absPath, int wLimit) {
        //  デジカメ写真などをそのまま読み込むと機種によってはOOMで落ちるので
        //  原画のパスと、縮尺基準（縦横長い方のLimit）を渡し、それに合わせたBitmapを返す。
        //  ××圧縮しない場合はファイルを重複して作成しないようnullを返す・
        Bitmap rtnBMP;
        BitmapFactory.Options options = new BitmapFactory.Options();    //  optionクラス作成

        //File file = new File(absPath);
        //Log.d("■", "file.length=" + file.length() );

        if (wLimit == 0) {
            //  非圧縮でそのまま返す
            options.inJustDecodeBounds = false;
            rtnBMP = BitmapFactory.decodeFile(absPath, options);
            Log.d("■", "no limit");
        } else {

            options.inJustDecodeBounds = true;  //  まず、画像情報だけ得る
            BitmapFactory.decodeFile(absPath, options);
            int orgHeight = options.outHeight;  //  縦・横サイズなどGET
            int orgWidht = options.outWidth;
            Log.d("■orgSize ", String.format("%d x %d", orgWidht, orgHeight));
            String imageType = options.outMimeType;
            int aSampleSize = 1;    //  デフォルトは仮縮小なし
            //  縦横とも設定値より大きい場合は「仮」縮小値を求める

            if (orgWidht >= wLimit && orgHeight >= wLimit) {
                if (orgWidht >= orgHeight) {    //  横長は縦（短い方）で判定
                    aSampleSize = (int) Math.round((float) orgHeight / ((float) wLimit * 0.75));
                } else {
                    aSampleSize = Math.round((float) orgWidht / (float) wLimit);
                }
                Log.d("■1st ratio ", String.format("%.3f", 1 / (float)aSampleSize));
                //  ★仮画像をGET
                options.inSampleSize = aSampleSize; //  2だったら1/2のサイズ
                options.inJustDecodeBounds = false;
                Bitmap tmp_bitmap = BitmapFactory.decodeFile(absPath, options);
                //
                int tmp_width = tmp_bitmap.getWidth();  //  仮画像のサイズを調べる
                int tmp_height = tmp_bitmap.getHeight();
                Matrix matrix = new Matrix();
                //  設定値に長辺を合わせて縮小
                float mRatio;
                if (tmp_width > tmp_height) {
                    mRatio = wLimit / (float) tmp_width;
                } else {
                    mRatio = wLimit / (float) tmp_height;
                }
                matrix.postScale(mRatio, mRatio);
                Log.d("■2nd ratio ", String.format("%.3f", mRatio));
                // ★縮小した画像を作成
                rtnBMP = Bitmap.createBitmap(tmp_bitmap, 0, 0, tmp_width, tmp_height, matrix, true);
            }
            else {
                options.inJustDecodeBounds = false;
                rtnBMP = BitmapFactory.decodeFile(absPath, options);

                Log.d("■ ", "small enough");
            }
        }
        return rtnBMP;
    }

    public void showDetailDialog(String aDisp, Bitmap aThumbNail){
        LinearLayout alertLayout0 = new LinearLayout(this);
        alertLayout0.setOrientation(LinearLayout.VERTICAL);
        alertLayout0.setPadding(15, 15, 15, 15);
        alertLayout0.setBackgroundColor(Color.argb(32,0,255,0)); //  第一パラメータを0にすると透明
        TextView textTitle0 = new TextView(this);
        CharSequence cSq = Html.fromHtml(aDisp);
        textTitle0.setText(cSq);
        //textTitle0.setTextSize(12);
        alertLayout0.addView(textTitle0);
        ImageView alertImg0 = new ImageView(this);
        alertImg0.setImageBitmap(aThumbNail);
        alertLayout0.addView(alertImg0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(alertLayout0);
        builder.show();

    }

    public void makeGallerySettingDialog(Context context){

        //try {
            //
            LinearLayout alertLayout0 = new LinearLayout(context);
            alertLayout0.setOrientation(LinearLayout.VERTICAL);
            alertLayout0.setPadding(15, 15, 15, 15);
            alertLayout0.setBackgroundColor(Color.argb(32, 0, 0, 255)); //  第一パラメータを0にすると透明
            //  テキスト
            /*
            TextView textView0 = new TextView(context);
            textView0.setText("写真ホルダーの設定");
            textView0.setTextColor(Color.argb(255, 0, 0, 255));
            alertLayout0.addView(textView0);
            */
            /*
            final Spinner spinner = new Spinner(context);
            ArrayList<String> QtyWithText = new ArrayList<String>();
            for (int i =0; i < Constants.displayQty.length; i++){
                QtyWithText.add("表示枚数 " + String.valueOf(Constants.displayQty[i]));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_list_item_single_choice, QtyWithText );
            spinner.setAdapter(adapter);

            spinner.setSelection(Constants.getPrefrenceInt
                    (context, Constants.DISPLAY_QTY, 0));   //  表示枚数（0は制限なし）
            alertLayout0.addView(spinner);
            */

            boolean thumbNailSize  = Constants.getPrefrenceBoolean
                    (context, Constants.THUMBNAIL_ENLARGE, true);   //  サムネイルの大きさ
            final Switch sw = new Switch(this);
            sw.setText("サムネイルの拡大");
            sw.setTextSize(16);
            sw.setTextColor(Color.argb(255, 0, 0, 255));
            sw.setChecked(thumbNailSize);
            alertLayout0.addView(sw);

            LinearLayout LL1 = new LinearLayout(context);
            LL1.setOrientation(LinearLayout.HORIZONTAL);

            TextView textView0 = new TextView(context);
            textView0.setTextSize(16);
            textView0.setPadding(0,8,0,0);
            textView0.setText("削除する範囲");
            textView0.setTextColor(Color.argb(255, 0, 0, 255));
            //alertLayout0.addView(textView0);
            //  チェックboxを作成してレイアウトに追加
            final RadioGroup radioGroup = new RadioGroup(context);
            //   リソースファイルからGET
            final Integer[] DHL =  Constants.deleteHoursBefore;
            final RadioButton[] radioButtons = new RadioButton[DHL.length];

            for (int i = 0; i < DHL.length; i++) {
                radioButtons[i] = new RadioButton(context);
                String dLabel = "";
                switch (DHL[i]){
                    case 24:
                        dLabel = "1日前";
                        break;
                    case 168:
                        dLabel = "1週間前";
                        break;
                    case 720:
                        dLabel = "1ヶ月前";
                        break;
                    default:
                        dLabel = DHL[i] + "h前";
                }
                radioButtons[i].setText(dLabel);
                radioButtons[i].setTextSize(12);
                radioGroup.addView(radioButtons[i]);
            }
            int hozonSize = Constants.getPrefrenceInt(context,Constants.DELETE_HOURS_ID, 2);
        Log.d("■", "" +hozonSize );
            radioButtons[hozonSize].setChecked(true);


            LL1.addView(textView0);
            LL1.addView(radioGroup);

            alertLayout0.addView(LL1);

            AlertDialog.Builder builder0 = new AlertDialog.Builder(context);
            //  作成したレイアウトをセット
            builder0.setView(alertLayout0)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Constants.setPrefrenceInt(getApplicationContext(), Constants.DISPLAY_QTY, spinner.getSelectedItemPosition());

                                    Constants.setPrefrenceBoolean
                                            (getApplicationContext(), Constants.THUMBNAIL_ENLARGE, sw.isChecked());
                                    for (int i=0; i < radioButtons.length; i++) {
                                        if(radioButtons[i].isChecked()){
                                            Constants.setPrefrenceInt(getApplicationContext(), Constants.DELETE_HOURS_ID, i);
                                        }
                                    }
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

    }   //  その他の設定

    static String copyFileForSharing(String privatePath){

        //  共有用画像の外部保存パスを作成する
        String filename = getAbsPathForShareing(privatePath);

        FileInputStream input= null;
        FileOutputStream output= null;
        try {
            input = new FileInputStream(privatePath);
            output = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            return e.getMessage();
        }

        byte buf[]=new byte[256];
        int len;
        try {
            while((len=input.read(buf))!=-1){
                output.write(buf,0,len);
            }
            output.flush();
            output.close();
            input.close();

        } catch (IOException e) {
            return e.getMessage();
        }

        return filename;
    }

    static void shareImageAndMessages(Context activity, int aID){
        //  ■■■■　データ共有　■■■■　 共有が可能なアプリ一覧を開く
        DatabaseHelper DBH = new DatabaseHelper(activity);
        String mABSpath = DBH.getABSFilePath(aID);
        NewCameraData NCD = new NewCameraData();
        NCD = DBH.getData(aID);
        DBH.close();

        //  オリジナルをそのまま共有
        String copyFilename = copyFileForSharing(mABSpath);

        Intent intent1 = new Intent(Intent.ACTION_SEND);
        intent1.setType("image/jpeg");
        String sendingMsg = "";
        sendingMsg += NCD.date + "\n";
        sendingMsg += NCD.location + "\n";
        sendingMsg += NCD.comment;

        Uri uri1 = Uri.fromFile(new File(copyFilename));  //  ★uriに変換しないと共有できない！
        intent1.putExtra(Intent.EXTRA_STREAM, uri1);
        intent1.putExtra(Intent.EXTRA_TEXT, sendingMsg);
        activity.startActivity(intent1);

    }

    static String getAbsPathForShareing(String privatePath){
        //  共有用画像の外部保存パスを作成する
        final String SAVE_DIR = "/ForShare/";
        File file = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
        try{
            if(!file.exists()){
                file.mkdir();
            }
        }catch(SecurityException e){
            return e.getMessage();
        }
        String[] aaa = privatePath.split("/",0);
        String filename =  file.getAbsolutePath() + "/" + aaa[aaa.length - 1];

        return filename;
    }

}

