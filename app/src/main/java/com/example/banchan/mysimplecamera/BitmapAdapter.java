package com.example.banchan.mysimplecamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.List;

public class BitmapAdapter extends ArrayAdapter<Bitmap> {
    //  カスタムクラスを作らないと画像を扱えない？
    private int resourceId;
    private Context mContext;

    public BitmapAdapter(Context context, int resource, List<Bitmap> objects) {
        super(context, resource, objects);
        resourceId = resource;
        mContext = context;
    }

    //  getViewをoverrideするのが目的
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ImageView view;

        if (convertView == null) {
            view = new ImageView(getContext());
            int gvSize =
                    Constants.getPrefrenceBoolean(mContext, Constants.THUMBNAIL_ENLARGE, true)
                            ? 138 : 92;
            view.setLayoutParams(new GridView.LayoutParams(gvSize, gvSize));
            view.setScaleType(ImageView.ScaleType.CENTER_CROP); //  大きい場合、センター中心に切り取り
            view.setPadding(6, 6, 6, 6);
        }
        else{
            view =  (ImageView) convertView;
        }

        view.setImageBitmap(getItem(position));
        return view;
    }

    //@Override
    public View getView_old(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(resourceId, null);
        }
        ImageView view = (ImageView) convertView;
        view.setImageBitmap(getItem(position));
        return view;
    }
}

