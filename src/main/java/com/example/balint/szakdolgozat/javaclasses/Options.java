package com.example.balint.szakdolgozat.javaclasses;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import io.realm.RealmObject;

/**
 * Created by Balint on 2015.10.25..
 */
public class Options  extends RealmObject {

    private String key;
    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
