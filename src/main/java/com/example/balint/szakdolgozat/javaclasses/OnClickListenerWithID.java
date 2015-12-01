package com.example.balint.szakdolgozat.javaclasses;

import android.view.View;

/**
 * Created by Balint on 2015.11.08..
 */
public class OnClickListenerWithID implements View.OnClickListener {

    private int userid;
    private String username;

    public OnClickListenerWithID(int userid, String username) {
        this.userid = userid;
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    /*public void setUsername(String username) {
        this.username = username;
    }*/

    @Override
    public void onClick(View v) {

    }
}
