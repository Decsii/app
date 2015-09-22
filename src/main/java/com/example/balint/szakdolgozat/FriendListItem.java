package com.example.balint.szakdolgozat;

import android.widget.BaseAdapter;

/**
 * Created by Balint on 2015.09.22..
 */
public class FriendListItem{

    String img;
    String name;
    String lstMsg;
    String time;
    final int type = 0;

    public FriendListItem(String img, String name, String lstMsg, String time) {
        this.img = img;
        this.name = name;
        this.lstMsg = lstMsg;
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public String getImg() {
        return img;
    }

    public String getName() {
        return name;
    }

    public String getLstMsg() {
        return lstMsg;
    }

    public String getTime() {
        return time;
    }
}
