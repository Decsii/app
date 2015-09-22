package com.example.balint.szakdolgozat;

import io.realm.RealmObject;

/**
 * Created by Balint on 2015.09.19..
 */
public class Messages extends RealmObject {

    private int msgid;
    private String from;
    private int fromid;
    private String msg;
    private double t;

    public int getMsgid() {
        return msgid;
    }

    public void setMsgid(int msgid) {
        this.msgid = msgid;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public int getFromid() {
        return fromid;
    }

    public void setFromid(int fromid) {
        this.fromid = fromid;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public double getT() {
        return t;
    }

    public void setT(double t) {
        this.t = t;
    }
}
