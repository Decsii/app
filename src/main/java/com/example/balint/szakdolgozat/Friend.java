package com.example.balint.szakdolgozat;

/**
 * Created by Balint on 2015.08.29..
 */
public class Friend {

    String name;
    int userid;

    public Friend(int userid) {
        this.userid = userid;
    }

    public Friend(int userid, String name) {
        this.name = name;
        this.userid = userid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserid() {
        return userid;
    }
}
