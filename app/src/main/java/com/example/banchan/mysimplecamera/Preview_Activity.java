package com.example.banchan.mysimplecamera;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Preview_Activity extends Activity {

    ImageView imageView;  //  カスタムImageView
    Bitmap orgbitmap;
    Bitmap embeddedBitmap;
    EditText editText;
    ImageButton imageButton;
    private int mID;
    ActionBar actionBar;
    boolean isImageEmbedded = false;    //  文字の埋め込みをしたらそちらを送る
    private int lineSpace = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_preview);

        actionBar = getActionBar();
        DatabaseHelper DBH = new DatabaseHelper(this);
        mID = Constants.getPrefrenceInt(this, Constants.ID_SELECTED, 0);
        String aPath = DBH.getABSFilePath(mID);
        String mComment = DBH.getData(mID).comment;
        DBH.close();
        String[] aaa = aPath.split("/",0);

        actionBar.setTitle("コメント追記と共有");
        actionBar.setSubtitle(aaa[aaa.length - 1]);
        actionBar.setDisplayHomeAsUpEnabled(true);

        imageView = (ImageView) findViewById(R.id.image_view2);
        //  いきなり大きな画像を読み込むとOOMで落ちるので設定値に縮小したBitmapを作る
        orgbitmap = MediaAvtivity.makeReasonableBitmap
                (aPath, Constants.getPrefrenceInt(this,Constants.DISPLAY_WIDTH, 0));

        imageView.setImageBitmap(orgbitmap);
        editText = (EditText) findViewById(R.id.view_edit);
        if(mComment == ""){
            editText.setHint("コメントを記入");
        }
        else {
            editText.setText(mComment);
        }
        imageButton = (ImageButton)findViewById(R.id.view_btn);
        imageButton.setOnClickListener(new ButtonClickListener());

    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        ////    画面の横幅とボタンの幅からエディットテキストの幅を決める
        //          onCreateでは画面が出来てないのでサイズを取得できない。

        WindowManager wm = getWindowManager();
        Display disp = wm.getDefaultDisplay();
        Point p1 = new Point();
        disp.getSize(p1);
        int imgW = p1.x / 7;
        imageButton.setMaxWidth(imgW);
        imageButton.setMaxHeight(imgW);
        editText.setWidth(p1.x - imgW - 8);
        //Toast.makeText(this, edW.toString() , Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        //■■■■   オプションメニュー表示
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_preview, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.previewEnd:
                //
                finish();
                break;

            case android.R.id.home:
                finish();
                break;

            case R.id.previewEmbedText:
                //  コメントを画像に埋め込む
                embedTextIntoImage(0);  //  保存サイズのまま
                break;

            case R.id.previewEmbedText_plus:
                //  コメントを画像に埋め込む
                embedTextIntoImage(2);  //  文字を+1
                break;

            case R.id.previewEmbedText_minus:
                //  コメントを画像に埋め込む
                embedTextIntoImage(-2);  //  文字を-1
                break;

            case R.id.previewEmbedText_wide:
                //  文字の縦間隔を広げる
                embedTextVerticalSpacing(2);
                break;

            case R.id.previewEmbedText_narrow:
                //  文字の縦間隔を狭める
                embedTextVerticalSpacing(-2);
                break;

            case R.id.previewXferRect:
                //  角丸に変形してPNGで共有させる（テキスト無し）
                xfer2RectPNGAndShare();
                break;

            case R.id.previewShare:
                //  写真とメモなどを共有させる
                sharing();
                break;

            case R.id.previewColorSetting:
                //  テキストの色を変える
                int orgCol = Constants.getPrefrenceInt(this, Constants.EMBED_TEXT_COLOR_SET,0);
                int numCol = Constants.embedTextColorSet.length;   //  色を増やすにはConstantsに追記
                orgCol ++;
                orgCol = orgCol > numCol - 1 ? 0 : orgCol;
                //Log.d("■", "" +orgCol +":"+ orgShd );
                Constants.setPrefrenceInt(this, Constants.EMBED_TEXT_COLOR_SET, orgCol);

                embedTextIntoImage(0);  //  保存サイズのまま

                break;

            default:
        }
        return true;
    }   //  option メニュー

    private void embedTextIntoImage(int reSize){

        //  reSizeで文字の大きさを微調整する
        if (reSize != 0){
            int orgSize = Constants.getPrefrenceInt(this, Constants.EMBED_TEXT_SIZE, 24);
            int newSize = orgSize + reSize;
            newSize = newSize < 12 ? 12 : newSize;
            newSize = newSize > 128 ? 128 : newSize;
            Log.d("■", "newSize " + newSize);
            Constants.setPrefrenceInt(this, Constants.EMBED_TEXT_SIZE, newSize);
        }

        //  コメントの埋め込み
        DatabaseHelper DBH = new DatabaseHelper(Preview_Activity.this);
        String mComment = DBH.getData(mID).comment;
        DBH.close();
        //
        if(mComment != "") {
            embeddedBitmap = addTextToBitmapForComment
                    (Preview_Activity.this, orgbitmap, mComment);
            imageView.setImageBitmap(embeddedBitmap);

            //orgbitmap.recycle();    //  これ以降使わない
            //orgbitmap = null;

            isImageEmbedded = true;
        }

    }

    private void embedTextVerticalSpacing(int aPicth){
        //  aPicthで文字の大きさを微調整する
        if (aPicth != 0 && isImageEmbedded){

            int orgSize = lineSpace;
            int newSize = orgSize + aPicth;

            if(newSize < - orgbitmap.getHeight()){
                lineSpace =  - orgbitmap.getHeight();
            }
            else if(newSize > orgbitmap.getHeight()){
                lineSpace =   orgbitmap.getHeight();
            }
            else{
                lineSpace = newSize;
            }

            embedTextIntoImage(0);
        }

    }

    private void sharing(){
        //  写真とメモなどを共有させる
        if(isImageEmbedded) {   //  埋め込みされた
            //  加工bitmapから送付用ファイル作成
            DatabaseHelper DBH1 = new DatabaseHelper(this);
            String mABSpath1 = DBH1.getABSFilePath(mID);
            NewCameraData NCD = new NewCameraData();
            NCD = DBH1.getData(mID);
            DBH1.close();

            String copyFilename1 = MediaAvtivity.getAbsPathForShareing(mABSpath1);
            final FileOutputStream out1;
            try {
                out1 = new FileOutputStream(copyFilename1);
                embeddedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out1);
                out1.flush();
                out1.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent intent2 = new Intent(Intent.ACTION_SEND);
            intent2.setType("image/jpeg");
            String sendingMsg = "";
            sendingMsg += NCD.date + "\n";
            sendingMsg += NCD.location + "\n";
            sendingMsg += NCD.comment;

            Uri uri2 = Uri.fromFile(new File(copyFilename1));  //  ★uriに変換しないと共有できない！
            intent2.putExtra(Intent.EXTRA_STREAM, uri2);
            intent2.putExtra(Intent.EXTRA_TEXT, sendingMsg);
            startActivity(intent2);
        }
        else{   //  そのまま送る
            MediaAvtivity.shareImageAndMessages(this, mID);
        }


    }

    private void xfer2RectPNGAndShare(){
        //  角丸に変形してPNGで共有させる（テキスト無し）

        Bitmap bbb = CameraActiviity2.transferRoundRectImage(Preview_Activity.this, embeddedBitmap);
        imageView.setImageBitmap(bbb);

        DatabaseHelper DBH = new DatabaseHelper(this);
        String mABSpath = DBH.getABSFilePath(mID);
        DBH.close();

        String copyFilename = MediaAvtivity.getAbsPathForShareing(mABSpath);
        String aaa[] = copyFilename.split("\\.", 0);
        String aaa0 = aaa[0] + ".png";  //  拡張子を変更

        final FileOutputStream out;
        try {
            out = new FileOutputStream(aaa0);
            //Bitmap bbb = CameraActiviity2.transferRoundRectImage(embeddedBitmap);
            bbb.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Intent intent1 = new Intent(Intent.ACTION_SEND);
        intent1.setType("image/png");

        Uri uri1 = Uri.fromFile(new File(aaa0));  //  ★uriに変換しないと共有できない！
        intent1.putExtra(Intent.EXTRA_STREAM, uri1);
        startActivity(intent1);



    }

    private void regComment(){
        //  メモをDBに登録する
        //  先頭の半・全角スペースを削除してから末尾の半・全角スペースも同様に削除。
        String mVal = editText.getText().toString().replaceAll("^[\\s　]*", "").replaceAll("[\\s　]*$", "");
        if(mVal.isEmpty()){
            Toast.makeText(Preview_Activity.this, "空白メモは登録しません。", Toast.LENGTH_SHORT).show();
        }
        else {
            DatabaseHelper DBH = new DatabaseHelper(Preview_Activity.this);
            int result = DBH.updateComment(mVal, mID);
            DBH.close();
            Toast.makeText(Preview_Activity.this, result == 1 ? "コメント登録しました。" : "登録失敗..." , Toast.LENGTH_LONG).show();
        }
    }

    private class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            regComment();
        }
    }

    public Bitmap addTextToBitmapForComment
            (Context context, Bitmap org, String aText ){

        int aTextSize = Constants.getPrefrenceInt(context, Constants.EMBED_TEXT_SIZE,24);   //  def 24
        int aTextColor = Constants.embedTextColorSet[
                Constants.getPrefrenceInt(context, Constants.EMBED_TEXT_COLOR_SET,0)][0];  //  def 黒
        int aShadowColor = Constants.embedTextColorSet[
                Constants.getPrefrenceInt(context, Constants.EMBED_TEXT_COLOR_SET,0)][1];  //  def

        float sp2px = context.getResources().getDisplayMetrics().scaledDensity;

        //  文字(sp)をピクセルに変換して基準文字サイズを得る
        float scaleTextSize = aTextSize * sp2px < 20 ? 20 : aTextSize * sp2px ;
        scaleTextSize = scaleTextSize > org.getWidth() / 3 ? (int) (org.getWidth() / 3 ) : scaleTextSize;

        //  テキストを行単位に分割する
        String[] subLines = aText.split("\n",0);

        //  ベースキャンバスを作る
        Bitmap bitmap_rtn = Bitmap.createBitmap
                (org.getWidth(), org.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap_rtn);
        canvas.drawBitmap(org, 0, 0, null);     //  bitmapを描写
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); // テキスト用paint作成
        paint.setColor(aTextColor);
        float aDiff = 0.5F + (float) (scaleTextSize / 75);
        paint.setShadowLayer(2f, aDiff, aDiff, aShadowColor);
        paint.setTextSize(aTextSize);

        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float fontHeight = fontMetrics.descent - fontMetrics.ascent;    //  一行の高さ（acentはbaseより上だから負）

        //int rowAlign = Constants.getPrefrenceInt(context, Constants.EMBED_TEXT_ROW_ALIGN, 0);

        //  全体の高さは行間調整を足す（マイナスは詰める）
        float blockHeight = fontHeight * subLines.length + lineSpace * (subLines.length - 1);

        for(int j=0; j < subLines.length; j++){

            float textWidth = paint.measureText(subLines[j]);   //  各行の長さ
            int centeringX = ( org.getWidth() - (int) textWidth ) / 2;  //  横方向をセンタリングするX位置

            //  ブロック全体の上部へbeseからはみ出た分を足す（ascentを引く）と一行目のbase
            //  それに一行の高さ+調整分を加算してゆく
            int centeringY =(int)( ( org.getHeight() - blockHeight ) / 2
                    - fontMetrics.ascent +( fontHeight + lineSpace ) * j  );

            canvas.drawText(subLines[j], centeringX, centeringY, paint);
        }
        return bitmap_rtn;  //  Canvasで操作したbitmapを返す
    }   //  画像にコメントを追加

}
