package com.example.banchan.mysimplecamera;

import java.security.SecureRandom;

/**
 * Created by Banchan on 2016/05/08.
 */
public class NewCameraData {
    public int _id; //  0
    public byte[] thumbnail;
    public String date;
    public float latitude;
    public float longitude;
    public String location; //  5
    public int width;
    public int height;
    public String comment;
    public int position;
    public int checked;  //  10
    public int size;
    public String filename;

    NewCameraData(){
        checked = 0;
        comment = "";
    }

}
