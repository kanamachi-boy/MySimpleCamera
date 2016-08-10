package com.example.banchan.mysimplecamera;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//SQLite処理Helper
public class DatabaseHelper extends SQLiteOpenHelper{

        private static final int DATABASE_VERSION = 1;
        private static final String DATABASE_FILE_NAME = "MySimpleCamera1_1.db";
        private Context mContext;
        private SQLiteDatabase mDb;

        public DatabaseHelper(Context context) {
                super(context, DATABASE_FILE_NAME, null, DATABASE_VERSION);
                mContext = context;
                mDb = this.getWritableDatabase();
            }     // コンストラクタ

        public void onCreate(SQLiteDatabase db) {
                //
                //  getWritableDatabase();の度にチェックされ
                //  DBが無い時（作成された時）だけ実行される。
                //  DB自体を作成するメソッドは無い！無ければ自動的に作成されるが
                //  それは最初のテーブルをcreteした時。
                //
                db.execSQL(
                        "CREATE TABLE camera_data ("
                                + "_id integer primary key autoincrement,"
                                + "thumbnail blob, "
                                + "date text, "
                                + "latitude real, "
                                + "longitude real, "
                                + "location text, "      //  5
                                + "width integer, "
                                + "height integer, "
                                + "comment text, "
                                + "position integer, "    //  9  追加・削除の度に表示順をセットする
                                + "checked integer, "   //  10
                                + "size integer, "
                                + "filename text"
                                + ")"
                );
            }    // DB生成

        public int insert(Bitmap bitmap, String aFilename){
                /////   新規登録
                try {

                    // bitmapからサムネイルを作成
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Bitmap forTN = Bitmap.createScaledBitmap(bitmap, 92, 92, true);
                    forTN.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] bytesTN = baos.toByteArray();


                    //  日付を作成
                    Date date = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    //  DBへInsert
                    ContentValues cv = new ContentValues();
                    //cv.put("picture",bytes);    //  1     DBに入れるのは重過ぎる？
                    cv.put("thumbnail", bytesTN);   //  2
                    cv.put("date", sdf.format(date));   //  3
                    cv.put("latitude", Constants.getPrefrenceFloat(mContext, Constants.LAST_LATITUDE, 0F)); //  4
                    cv.put("longitude", Constants.getPrefrenceFloat(mContext, Constants.LAST_LONGITUDE, 0F));    //  5
                    cv.put("location", Constants.getPrefrenceString(mContext, Constants.LAST_LOCATION_NAME, "- - -"));   //  6
                    cv.put("width", bitmap.getWidth()); //  7
                    cv.put("height", bitmap.getHeight());   //  8
                    cv.put("comment", ""); //  9   comment
                    //  10  position
                    cv.put("checked", 0);   //  11  checked
                    File file = new File("/data/data/com.example.banchan.mysimplecamera/files/" + aFilename);
                    cv.put("size", file.length());  //  12
                    cv.put("filename", aFilename);  //  13

                    mDb.insert("camera_data", null, cv);

                    bitmap.recycle();
                    forTN.recycle();

                    int lastID = resetDisplayPosition(); //  ここで 10 がセットされる

                    Log.d("■", "" + lastID);
                    return lastID;
                }
                catch(Exception e){
                    Log.d("■", e.getMessage());
                    return -1;
                }
            }

        public int updateComment(String aComment, int aID){
            /////   コメントの更新
            try{
                ContentValues cv = new ContentValues();
                cv.put("comment", aComment);
                return mDb.update("camera_data",cv,"_id=" + aID, null);
            }
            catch(Exception e){
                return -1;
            }
        }

        public ArrayList<Bitmap> getThumbnails (){
            /////   サムネイルを全部取得
            try {
                String sql = "select thumbnail from camera_data order by _id DESC";
                Cursor csr = mDb.rawQuery(sql, null);

                if(csr.getCount() != 0) {

                    ArrayList<Bitmap> rtnTN = new ArrayList<Bitmap>();
                    csr.moveToFirst();
                    do {
                        rtnTN.add
                                (BitmapFactory.decodeByteArray(csr.getBlob(0), 0, csr.getBlob(0).length));

                    } while (csr.moveToNext());
                    csr.close();
                    return rtnTN;
                }
                else{
                    csr.close();
                    return null;
                }
            }
            catch (Exception e){
                Log.d("■", "getThumbnails " + e.getMessage() );
                return null;
            }
        }

        public ArrayList<NewCameraData> getAllData (int Limit){
        /////   メイン画像以外を全部取得
            Limit = Limit == 0 ? 99999 : Limit;

            try {
                String sql = "select * from camera_data order by _id DESC limit " + Limit +";" ;
                Cursor csr = mDb.rawQuery(sql, null);
                ArrayList<NewCameraData> rtnTN = new ArrayList<NewCameraData>();
                Log.d("■", "csr cnt" + csr.getCount());
                csr.moveToFirst();
                do{
                    NewCameraData mData = new NewCameraData();
                    mData._id = csr.getInt(0);
                    mData.thumbnail = csr.getBlob(1);
                    mData.date = csr.getString(2);
                    mData.latitude = csr.getFloat(3);
                    mData.longitude = csr.getFloat(4);
                    mData.location = csr.getString(5);
                    mData.width = csr.getInt(6);
                    mData.height = csr.getInt(7);
                    mData.comment = csr.getString(8);
                    mData.position = csr.getInt(9);
                    mData.checked = csr.getInt(10);
                    mData.size = csr.getInt(11);
                    mData.filename = csr.getString(12);

                    rtnTN.add(mData);

                } while(csr.moveToNext());
                csr.close();
                return rtnTN;
            }
            catch (Exception e){
                return null;
            }
        }

        public NewCameraData getData(int aID){
            String sql = "select * from camera_data where _id = " + aID + ";" ;
            Cursor csr = mDb.rawQuery(sql, null);

            csr.moveToFirst();
            NewCameraData mData = new NewCameraData();
            mData._id = csr.getInt(0);
            mData.thumbnail = csr.getBlob(1);
            mData.date = csr.getString(2);
            mData.latitude = csr.getFloat(3);
            mData.longitude = csr.getFloat(4);
            mData.location = csr.getString(5);
            mData.width = csr.getInt(6);
            mData.height = csr.getInt(7);
            mData.comment = csr.getString(8);
            mData.position = csr.getInt(9);
            mData.checked = csr.getInt(10);
            mData.size = csr.getInt(11);
            mData.filename = csr.getString(12);

            return mData;
        }

        public String getDataByHTML (int aID){
        //  画像以外のデータを文字列に連結して取り出す
        String sql = "select " +
                "_id, date, latitude, longitude, location, width, height, comment, position, checked, size, filename "  +
                "from camera_data where _id = " + aID + ";" ;

        Cursor csr = mDb.rawQuery(sql, null);
        csr.moveToFirst();
        String aDisp="";

        for ( int i = 0; i < csr.getColumnCount(); i++ ){
            aDisp += "<font color=\"blue\">" + csr.getColumnName(i) + " : </font>"
                    + "<font color=\"black\"><small><I>"
                    + csr.getString(i) + "</I></small></font><br>";
        }
        return aDisp;
    }

        public String getABSFilePath(int aID){
        //  選択位置からIDに変換し、ファイル名に物理パスを付けて返す
        return "/data/data/com.example.banchan.mysimplecamera/files/" +
                getData(aID).filename;

    }

        public int delete (Integer id){
            /////   削除

            int result = mDb.delete("camera_data", "_id =" + id, null);

            resetDisplayPosition();

            //Log.d("■", "" + result);
            return result;
        }

        public int position2id(int position){
            String sql = "select _id from camera_data where position = " + position + ";" ;
            Cursor csr = mDb.rawQuery(sql, null);

            if(csr.moveToFirst()){
                return csr.getInt(0);
            }
            else{
                return -1;
            }
        }

        private int resetDisplayPosition(){
            String sql = "select _id from camera_data order by _id DESC;" ;
            Cursor csr = mDb.rawQuery(sql, null);
            int rtnID=0;
            if(csr.moveToFirst()) {
                int newPosition = 0;
                do {
                    if(newPosition == 0){
                        rtnID = csr.getInt(0);  //  最初のID＝最新を返す
                    }
                    ContentValues cv = new ContentValues();
                    cv.put("position", newPosition);
                    mDb.update("camera_data", cv, "_id=" + csr.getInt(0), null);
                    newPosition++;
                } while (csr.moveToNext());
            }
            csr.close();
            return rtnID;
        }   //  insert delete の度にVIEWの表示順をリセットし、最新IDを返す

        public void onUpgrade(SQLiteDatabase db,int oldVersion, int newVersion) {
            //　DATABASE_VERSIONを変更すると呼ばれる
            //checkDataBaseExists();
        } // abstruct

        private boolean checkDataBaseExists() {

            String abPath =
                    mContext.getDatabasePath(DATABASE_FILE_NAME).getAbsolutePath();


            SQLiteDatabase checkDb = null;
            try {
                checkDb = SQLiteDatabase.openDatabase(abPath, null, SQLiteDatabase.OPEN_READONLY);
            } catch (Exception e) {
                // データベースはまだ存在していない
            }

            if (checkDb == null) {
                // データベースはまだ存在していない
                return false;
            }

            int oldVersion = checkDb.getVersion();
            int newVersion = DATABASE_VERSION;

            if (oldVersion == newVersion) {
                // データベースは存在していて最新
                checkDb.close();
                return true;
            }
            // データベースが存在していて最新ではないので削除
            File f = new File(abPath);
            f.delete();
            return false;
        }

        public float howLongFromCreated(int aID){
            // 　ID指定されたレコードが現時刻より何時間前に作られたか返す
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss" );
            try {
                //  文字で時刻を持っているので形式を合わせてparseし、getTimeでミリ秒に変換
                Date date = sdf.parse(getData(aID).date);
                return  (float)(System.currentTimeMillis() - date.getTime()) / (float)( 60 * 60 * 1000);

            } catch (ParseException e) {
                return -1;
            }

        }


}