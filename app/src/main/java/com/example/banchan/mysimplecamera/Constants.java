package com.example.banchan.mysimplecamera;

////    カスタム定数定義クラス

        import android.content.Context;
        import android.content.SharedPreferences;
        import android.graphics.Color;
        import android.preference.PreferenceManager;

        import java.util.Map;

public class Constants {

    public static final String CAMERA_MAX_WIDTH ="CameraMaxWidth_";
    public static final String CAMERA_MAX_HEIGHT ="CameraMaxHeight_";
    public static final String MAIN_CAMERA_ID ="MainCameraID_";
    public static final String SUB_CAMERA_ID ="SubCameraID_";
    public static final String CURRENT_CAMERA_ID ="currentCameraID_";

    public static final String LAST_LATITUDE ="LastLat_";
    public static final String LAST_LONGITUDE ="LastLng_";
    public static final String LAST_LOCATION_NAME ="LastLocationName_";
    public static final String LAST_LATITUDE_DBL ="LastLat_dbl_";
    public static final String LAST_LONGITUDE_DBL ="LastLng_dbl_";

    public static final String PHOTOSIZE_ID ="PhoteSizeID2_";
    public static final String PREVIEW_SIZE ="previewSize_";
    public static final String IS_KIRITORI ="IS_Kiritori_";
    public static final String IS_USE_AF ="IS_AF_";

    public static final String LOCATION_AVAILABLE_MINUTES ="locationAvailableMunites2_";
    public static final Integer[] locationAvailableMNT = { 5, 15, 60, 120, 240};

    public static final String IS_DATE_LABEL ="isDateLabel_";
    public static final String IS_LOCATION_LABEL ="isLocLabel_";
    public static final String IS_LOCATION_SEARCH ="isLocSearch_";
    public static final String IS_ROLLING_SEARCH ="isRollingSearch_";

    public static final String ID_SELECTED ="db_ID_selected_";
    public static final String IS_DIRECT_SHARE ="is_direct_share";

    public static final String DISPLAY_QTY ="DISP_qty_";
    public static final Integer[] displayQty = { 30, 50, 100, 300, 9999};

    public static final String THUMBNAIL_ENLARGE ="thumbnail_enlarge_";
    public static final String DISPLAY_WIDTH ="DISP_WIDTH_";
    public static final String DELETE_HOURS_ID ="delete_hours_ID2_0";
    public static final Integer[] deleteHoursBefore = { 1, 3, 24, 168, 720};

    public static final String EMBED_TEXT_SIZE ="embed_text_size";
    public static final String EMBED_TEXT_COLOR ="embed_text_color";
    public static final Integer[] embedTextColor =
            {Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA};

    public static final String EMBED_TEXT_COLOR_SET ="embed_text_color_set";
    public static final Integer[][] embedTextColorSet = {
            {Color.BLACK, Color.rgb(192,192,192)},  //  ,silver
            {Color.BLUE,Color.rgb(0,255,255)},    //  ,aqua
            {Color.rgb(0,128,0),Color.rgb(0,255,0)},   // green ,lime
            {Color.RED,Color.rgb(255,144,36)},   //  ,fuchsia
            {Color.rgb(128,0,0),Color.CYAN},   //  purple,maroon
            {Color.GRAY, Color.WHITE}   //
    };

    public static final String EMBED_TEXT_SHADE ="embed_text_shade";
    public static final Integer[] embedTextShade =
            {Color.WHITE, Color.YELLOW, Color.LTGRAY, Color.CYAN, Color.RED};

    public static final String EMBED_TEXT_START_X ="embed_text_start_x";
    public static final String EMBED_TEXT_START_Y ="embed_text_start_y";
    public static final String EMBED_TEXT_ROW_ALIGN ="embed_text_row_align";
    public static final String IS_RECT_RADIUS_BIGGER ="isrectradiusbigger";

    static void setPrefrenceInt(Context context, String mKey, Integer mVal){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = data.edit();
        editor.putInt(mKey, mVal);
        editor.apply();
    }

    static void setPrefrenceBoolean(Context context, String mKey, Boolean mVal){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = data.edit();
        editor.putBoolean(mKey, mVal);
        editor.apply();
    }

    static void setPrefrenceString(Context context, String mKey, String mVal){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = data.edit();
        editor.putString(mKey, mVal);
        editor.apply();
    }

    static void setPrefrenceFloat(Context context, String mKey, Float mVal){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = data.edit();
        editor.putFloat(mKey, mVal);
        editor.apply();
    }


    static Integer getPrefrenceInt(Context context, String mKey, Integer mDefault){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        return data.getInt(mKey,mDefault);
    }

    static Boolean getPrefrenceBoolean(Context context, String mKey, Boolean mDefault){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        return data.getBoolean(mKey, mDefault);
    }

    static String getPrefrenceString(Context context, String mKey, String mDefault){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        return data.getString(mKey, mDefault);
    }

    static Float getPrefrenceFloat(Context context, String mKey, Float mDefault){
        SharedPreferences data = PreferenceManager.getDefaultSharedPreferences(context);
        return data.getFloat(mKey, mDefault);
    }
}
