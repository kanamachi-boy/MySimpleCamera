package com.example.banchan.mysimplecamera;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Banchan on 2016/04/21.
 */
public class CameraSizesFromResource {

    private ArrayList<Integer[]> mSize = new ArrayList<Integer[]>();

    CameraSizesFromResource(Context context){
        //  デバイスの最大サイズを得て短い辺を基準に設定
        Integer[] _mSizes = new Integer[2];
        _mSizes[0] = Constants.getPrefrenceInt(context, Constants.CAMERA_MAX_WIDTH,0);
        _mSizes[1]  = Constants.getPrefrenceInt(context, Constants.CAMERA_MAX_HEIGHT,0);
        int _size_guideLine = Math.min(_mSizes[0],_mSizes[1]);
        //  リソースファイルからデータを得る
        String[] size_str = context.getResources().getStringArray(R.array.camera_sizes);

        for (int i = 0; i < size_str.length; i++){
            String[] a1 =  size_str[i].split(",",0);
            Integer[] sub = {Integer.parseInt(a1[0]),Integer.parseInt(a1[1])};
            //  基準より両辺とも短いセットを採用
            if(sub[0] <= _size_guideLine && sub[1] <= _size_guideLine){
                mSize.add(sub);
            }

        }
    }

    public ArrayList<Integer[]> getSises(){
        return  mSize;
    }

    public Integer[] getSizeByID(int mId){
        return mSize.get(mId);
    }

    public ArrayList<String> getSizeLabels() {
        ArrayList<String> rtn = new ArrayList<String>();
        for(int i=0; i < mSize.size(); i++){
            float mX = mSize.get(i)[0];
            float mY = mSize.get(i)[1];
            String aTxt = String.format("%d X %d", (int)mX, (int)mY);
            /*
            if (i == 0 && isResetedBySmallerThanArgs){
                aTxt += " Device Max";
            }
            if(mX/mY == (float)16/9){
                aTxt += " wide";
            }
            else if(mX==mY){
                aTxt += " square";
            }
            */
            rtn.add(aTxt);
        }

        return  rtn;
    }
}
