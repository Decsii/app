package com.example.balint.szakdolgozat.javaclasses;

import android.widget.BaseAdapter;

/**
 * Created by Balint on 2015.09.22..
 */
public class FriendListItem {

    //private String img;
    private String name;
    private String lstMsg;
    private String time;
    private int userid;
    private final int type = 0;

    public FriendListItem(int userid, String name, String lstMsg, String time) {
        this.name = name;
        this.lstMsg = lstMsg;
        this.time = time;
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLstMsg() {
        return lstMsg;
    }

    public void setLstMsg(String lstMsg) {
        this.lstMsg = lstMsg;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getType() {
        return type;
    }
}
