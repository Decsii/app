package com.example.balint.szakdolgozat.activities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.DBMessage;
import com.example.balint.szakdolgozat.javaclasses.Friend;
import com.example.balint.szakdolgozat.javaclasses.FriendListItem;
import com.example.balint.szakdolgozat.javaclasses.Options;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by Balint on 2015.08.20..
 */
public class TCPService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    public final static String EXTRA_MESSAGE = "com.example.balint.szakdolgozat.MESSAGE";

    public Socket socket = null;
    private DataOutputStream dos;
    private DataInputStream dis;

    private boolean logedIn = false;
    public int myId;

    private List<FriendListItem> friendList = new ArrayList<>();
    private List<FriendListItem> friendRequests = new ArrayList<>();
    private List<FriendListItem> sendedRequests = new ArrayList<>();

    private Messenger messageHandler;
    private boolean sendAF = false;

    private boolean sendedR = false;
    private int numOfSended = 0;


    public List<Pair<String, Integer>> messagesList = new ArrayList<>();

    boolean friendS = false;
    int numOfFriends = 0;

    boolean requestS = false;
    int numOfRequests = 0;

    public int currentPartner;

    private String currentPartnerName;
    private String lastActive;


    private int addFId;
    private String addFName;

    private int messagesArrived;

    public boolean requestedyet = false;

    private List<Integer> leftList = new ArrayList<>();

    private Queue<String> msgQ = new LinkedList<>();
    private Queue<Pair<Integer,FriendListItem>> refreshQ = new LinkedList<>();

    private boolean friendListActive = false;

    public class LocalBinder extends Binder {
        public TCPService getService() {
            return TCPService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");
        return mBinder;
    }

    @Override
    public void onDestroy(){
        Log.d("dest","SeERVICE destroy");
        try {
            socket.close();
        }catch(Exception e){
            Log.d("cannot","close socekt");
        }
        super.onDestroy();
    }

    NotificationManager nm;
    Notification notification;
    PendingIntent pending;
    Intent notifIntent;

    @Override
    public void onCreate() {
        startTCP();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return START_NOT_STICKY;
    }

    public void startTCP() {
        Log.d("start", "SERVICE START");
        Thread t1 = new Thread() {
            public void run() {
                while (true) {
                    try {
                        String ip = "79.143.178.118";
                        int port = 6000;
                        InetSocketAddress isa = new InetSocketAddress(ip, port);
                        socket = new Socket();
                        socket.connect(isa);
                        dos = new DataOutputStream(socket.getOutputStream());
                        dis = new DataInputStream(socket.getInputStream());
                        startCommunication();
                        break;
                    } catch (Exception e) {
                        try {
                            Thread.sleep(3000);
                            Log.d("", "újracsatlakozás");
                        } catch (InterruptedException ex) {
                            Log.d("", "váratlan hiba");
                        }
                    }
                }
            }
        };
        t1.start();
    }

    public void startCommunication() {
        Thread t1 = new Thread() {
            public void run() {
                String str;
                Log.d("comm","COMMUNICATION READY");
                while (true) {
                    try {
                        int length = dis.readInt();
                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);
                        str = new String(bytes);
                    } catch (IOException e) {
                        startTCP();
                        break;
                    }
                    Log.d("server : ", str);
                    try {
                        feldolgozas(str);
                    } catch (JSONException e) {
                        //itt azé kéne valami figyelmeztetés hogy elrontódott a dolog
                        Log.d("HIBA: ", "JSONHIBA");
                        Log.d("myapp", Log.getStackTraceString(e));
                    }
                }
            }
        };
        t1.start();
    }

    public void feldolgozas(String jsonString) throws JSONException {
        JSONObject jsonRootObject = new JSONObject(jsonString);
        String type;
        try {
            type = jsonRootObject.get("type").toString();
        } catch (JSONException e) {
            type = "hiba";
        }

        if (!type.equals("hiba")) {
            JSONObject jObj;
            JSONArray jArr;
            switch (jsonRootObject.get("type").toString()) {
                case "0":
                    JSONObject jo3 = jsonRootObject.optJSONObject("userdata");
                    myId = jo3.getInt("id");
                    logedIn = true;
                    sendMessage(0);
                    Log.d("tipus : ", "regisztrációs kerelem");
                    break;
                case "1":
                    logedIn = true;
                    JSONObject jo4 = jsonRootObject.optJSONObject("userdata");
                    myId = jo4.getInt("id");
                    logInHandler();
                    Log.d("tipus : ", "bejelentkezesi kerelem");
                    break;
                case "2":
                    logOut();
                    sendMessage(2);
                case "3":
                    JSONObject jo2 = jsonRootObject.optJSONObject("userdata");

                    if (sendedR) {
                        sendedRequests.get(numOfSended - 1).setName(jo2.getString("username"));
                        if (sendedRequests.size() == numOfSended) {
                            numOfSended = 0;
                            sendedR = false;
                        } else {
                            userInfoByID(sendedRequests.get(numOfSended).getUserid());
                            numOfSended++;
                        }
                    }

                    if (requestS) {
                        friendRequests.get(numOfRequests - 1).setName(jo2.getString("username"));
                        if (friendRequests.size() == numOfRequests) {
                            requestS = false;
                            numOfRequests = 0;
                            sendedRequests();
                        } else {
                            userInfoByID(friendRequests.get(numOfRequests).getUserid());
                            numOfRequests++;
                        }
                    }

                    if (friendS) {
                        requestedyet = true;
                        friendList.get(numOfFriends - 1).setName(jo2.getString("username"));
                        friendList.get(numOfFriends - 1).setLast_login(jo2.getLong("last_login"));
                        if( jsonRootObject.getInt("isloggedin") != 0 ){
                            friendList.get(numOfFriends - 1).setLoggedin(true);
                        }
                        if (friendList.size() == numOfFriends) {
                            friendS = false;
                            numOfFriends = 0;
                            requests();
                            for (FriendListItem f : friendList) {
                                getMsg(f.getUserid(), 10);
                            }
                        } else {
                            userInfoByID(friendList.get(numOfFriends).getUserid());
                            numOfFriends++;
                        }
                    }

                    break;
                case "4":
                    sendMessage(4);
                    break;
                case "5":
                    sendMessage(5);
                    break;
                case "6":
                    sendMessage(6);
                    break;
                case "7":
                    sendedRequests.add( new FriendListItem(addFId,addFName,"","",false,0,2));
                    sendMessage(7);
                    break;
                case "8":
                    sendedHandler(jsonRootObject.optJSONArray("requests"));
                    break;
                case "9":
                    requestListHandler(jsonRootObject.optJSONArray("requests"));
                    break;
                case "10":
                    Log.d("","üzenett jöt");
                    try {
                        Realm realm = Realm.getInstance(this);
                        jArr = jsonRootObject.optJSONArray("messages");
                        if( jArr.length() == 0 ){
                            messagesArrived++;
                            if (messagesArrived == friendList.size()) {
                                messagesArrived = 0;
                                setListItems();
                                sendMessage(10);
                                Log.d("","MINdEN ÜZENET MEGÉRKEZETT");
                            }
                            return;
                        }

                        RealmResults<DBMessage> result = realm.where(DBMessage.class)
                                .beginGroup()
                                    .equalTo("fromid", jArr.getJSONObject(0).getInt("from"))
                                    .equalTo("toid", jArr.getJSONObject(0).getInt("to"))
                                .endGroup()
                                .or()
                                .beginGroup()
                                    .equalTo("fromid", jArr.getJSONObject(0).getInt("to"))
                                    .equalTo("toid", jArr.getJSONObject(0).getInt("from"))
                                .endGroup()
                                .findAll();

                        for( Integer in : leftList ){
                            if( in == jArr.getJSONObject(0).getInt("from") || in == jArr.getJSONObject(0).getInt("to") ){
                                messagesArrived++;
                                for (int i = 0; i < jArr.length(); i++) {
                                    if (jArr.getJSONObject(i).getInt("msgid") > result.max("msgid").intValue()) {
                                        insertMessage(realm, jArr.getJSONObject(0).getInt("from"), jArr.getJSONObject(i).getString("msg"), jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(i).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                                    }
                                }
                                if (messagesArrived == friendList.size()) {
                                    messagesArrived = 0;
                                    setListItems();
                                    sendMessage(10);
                                    Log.d("","MINdEN ÜZENET MEGÉRKEZETT");
                                }
                                return;
                            }
                        }

                        if( jArr.length() == 20 ){
                            messagesArrived++;
                            for (int i = 0; i < jArr.length(); i++) {
                                if (jArr.getJSONObject(i).getInt("msgid") > result.max("msgid").intValue()) {
                                    insertMessage(realm, jArr.getJSONObject(0).getInt("from"), jArr.getJSONObject(i).getString("msg"), jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(i).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                                }
                            }
                        }else {
                            long max = result.max("msgid").longValue();
                            if (max != 0) {
                                if (jArr.getJSONObject(0).getInt("msgid") > max) {
                                    if( jArr.getJSONObject(0).getInt("from") == myId ){
                                        leftList.add(jArr.getJSONObject(0).getInt("to"));
                                        getMsg(jArr.getJSONObject(0).getInt("to"), 20);
                                    }else{
                                        leftList.add(jArr.getJSONObject(0).getInt("from"));
                                        getMsg(jArr.getJSONObject(0).getInt("from"), 20);
                                    }

                                } else {
                                    messagesArrived++;
                                    for (int i = 0; i < jArr.length(); i++) {
                                        if (jArr.getJSONObject(i).getInt("msgid") > result.max("msgid").intValue()) {
                                            insertMessage(realm, jArr.getJSONObject(0).getInt("from"), jArr.getJSONObject(i).getString("msg"), jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(i).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                                        }
                                    }
                                }
                            } else {
                                if( jArr.getJSONObject(0).getInt("from") == myId ){
                                    getMsg(jArr.getJSONObject(0).getInt("to"), 20);
                                    leftList.add(jArr.getJSONObject(0).getInt("to"));
                                }else{
                                    getMsg(jArr.getJSONObject(0).getInt("from"), 20);
                                    leftList.add(jArr.getJSONObject(0).getInt("from"));
                                }
                            }
                        }
                        if (messagesArrived == friendList.size()) {
                            messagesArrived = 0;
                            setListItems();
                            sendMessage(10);
                            Log.d("","MINDEN ÜZENET MEGÉRKEZETT");
                        }
                    } catch (Exception e) {
                        Log.d("", Log.getStackTraceString(e));
                    }
                    break;
                case "13":
                    try {
                        Realm realm = Realm.getInstance(this);
                        jObj = jsonRootObject.optJSONObject("message");
                        insertMessage(realm, jObj.getInt("from"), decrypt(jObj.getString("msg")),jObj.getInt("msgid"), jObj.getDouble("t"), jObj.getInt("to"));
                        setListItems();
                        sendMessage(13);
                    } catch (Exception e) {
                        Log.d("", "13 as error");
                        Log.d("", Log.getStackTraceString(e));
                    }
                    break;
                case "14":
                    JSONObject jo = jsonRootObject.optJSONObject("userdata");
                    if (sendAF) {
                        addFName = jo.getString("username");
                        addFId = jo.getInt("id");
                        addFriend(jo.getInt("id"));
                        sendAF = false;
                    }
                    break;
                case "15":
                    friendListHandler(jsonRootObject.optJSONArray("relationships"));
                    break;
                case "16":
                    sendMessage(16);
                    break;
                case "19" :
                    jArr = jsonRootObject.optJSONArray("messages");
                    Realm realm = Realm.getInstance(this);
                    String message = "";
                    try {
                        message = decrypt(jArr.getJSONObject(0).getString("msg"));
                    }catch(Exception e){
                        Log.d("HIba","decrypt hiba");
                        return;
                    }
                    Log.d("message", message);
                    if( jArr.getJSONObject(0).getInt("from") == currentPartner ) {
                        insertMessage(realm, jArr.getJSONObject(0).getInt("from"), message,jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(0).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                        msgQ.add(message);
                        sendMessage(16);
                    }else{
                        insertMessage(realm, jArr.getJSONObject(0).getInt("from"), message, jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(0).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                        setListItems();
                        nm = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
                        notifIntent = new Intent(this, MessagingActivity.class);
                        notifIntent.putExtra("id", jArr.getJSONObject(0).getInt("from"));
                        pending = PendingIntent.getActivity(this, 0, notifIntent, 0);

                        if( friendListActive ){
                            int i = 0;
                            while ( i < friendList.size() ){
                                if ( friendList.get(i).getUserid() ==  jArr.getJSONObject(0).getInt("from") ){
                                    friendList.get(i).setLstMsg(message);
                                    friendList.get(i).setTime(getStringDateHHmm(jArr.getJSONObject(0).getLong("t")*1000));
                                    refreshQ.add(new Pair<Integer, FriendListItem>(i,friendList.get(i)));
                                    sendMessage(200);
                                    break;
                                }
                                i++;
                            }
                        }
                        //startNotification(realm, jArr, message);

                    }
                    break;
                case "21" :
                    jObj = jsonRootObject.optJSONObject("userdata ");
                    friendRequests.add(new FriendListItem(jObj.getInt("userid"),jObj.getString("username"),"","",jObj.getInt("isloggedin") == 0 ? false : true,0,1));
                    if( friendListActive ) {
                        sendMessage(300);
                    }
                    break;
                case "22":
                    jObj = jsonRootObject.optJSONObject("userdata ");
                    int ind = 0;
                    for ( int i = 0; i < sendedRequests.size(); i++ ){
                        if ( sendedRequests.get(i).getUserid() ==  jObj.getInt("userid") ){
                            ind = i;
                            break;
                        }
                    }
                    sendedRequests.remove(ind);
                    friendList.add(new FriendListItem( jObj.getInt("userid"), jObj.getString("username"),"","",jObj.getInt("isloggedin") == 0 ? false : true  ,0,0));
                    if( friendListActive ) {
                        sendMessage(301);}
                    break;
            }
        } else {
            String errorCode = jsonRootObject.get("code").toString();
            switch (errorCode) {
                case "21":
                    sendMessage(1121);
                    break;
                case "2":
                    sendMessage(112);
                    break;
            }
            Log.d("semi :" + jsonRootObject.get("code").toString() + " ", jsonRootObject.get("error").toString());
        }

    }

    public void startNotification(Realm realm, JSONArray jArr, String message) throws JSONException{
        RealmResults<Options> result = realm.where(Options.class)
                .equalTo("key", "notification")
                .findAll();
        if( result.get(0).getValue().equals("1") ){

            result = realm.where(Options.class)
                    .equalTo("key", "notifsound")
                    .findAll();

            RealmResults<Options> result2 = realm.where(Options.class)
                    .equalTo("key", "notifvibrate")
                    .findAll();

            if( result.get(0).getValue().equals("1") && result2.get(0).getValue().equals("1") ) {
                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                notification = new Notification.Builder(this)
                        .setContentTitle("Chat")
                        .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                        .setContentIntent(pending)
                        .setVibrate(new long[]{0, 1000, 1000, 1000, 1000})
                        .setSound(alarmSound)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .build();
                nm.notify(jArr.getJSONObject(0).getInt("from"), notification);
            } else{
                if ( result.get(0).getValue().equals("1") ){
                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    notification = new Notification.Builder(this)
                            .setContentTitle("Chat")
                            .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                            .setContentIntent(pending)
                            .setSound(alarmSound)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build();
                    nm.notify(jArr.getJSONObject(0).getInt("from"), notification);
                }else if( result2.get(0).getValue().equals("1" )){
                    notification = new Notification.Builder(this)
                            .setContentTitle("Chat")
                            .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                            .setContentIntent(pending)
                            .setVibrate(new long[]{0, 1000, 1000, 1000, 1000})
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build();
                    nm.notify(jArr.getJSONObject(0).getInt("from"), notification);
                }else{
                    notification = new Notification.Builder(this)
                            .setContentTitle("Chat")
                            .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                            .setContentIntent(pending)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build();
                    nm.notify(jArr.getJSONObject(0).getInt("from"), notification);
                }

            }
        }
    }

    public void insertMessage(Realm realm, int fromid, String msg, int msgid, double t, int toid) {
        //Realm realm = Realm.getInstance(this);
        realm.beginTransaction();

        RealmResults<DBMessage> result = realm.where(DBMessage.class)
                .beginGroup()
                    .equalTo("fromid", fromid)
                    .equalTo("toid", toid)
                .endGroup()
                .or()
                .beginGroup()
                    .equalTo("fromid", toid)
                    .equalTo("toid", fromid)
                .endGroup()
                .findAll();

        if (result.size() >= 20) {
            long min = result.min("msgid").longValue();
            for (DBMessage msgObj : result) {
                if (min == msgObj.getMsgid()) {
                    msgObj.removeFromRealm();
                    break;
                }
            }
        }

        DBMessage msgObj = realm.createObject(DBMessage.class);
        msgObj.setMsg(msg);
        msgObj.setFromid(fromid);
        msgObj.setToid(toid);
        msgObj.setMsgid(msgid);
        msgObj.setT(t);

        realm.commitTransaction();
    }

    public void logOut() {
        try {
            requestedyet = false;
            logedIn = false;
            socket.close();
            socket = null;
        } catch (Exception e) {
            Log.d("", "nem tudta bezárni a socketet");
        }
    }

    private void setListItems(){
        Realm realm  = Realm.getInstance(this);
        int i = 0;
        for (FriendListItem fli : friendList){
            RealmResults<DBMessage> result = realm.where(DBMessage.class)
                    .beginGroup()
                    .equalTo("fromid", fli.getUserid())
                    .equalTo("toid", myId)
                    .endGroup()
                    .or()
                    .beginGroup()
                    .equalTo("fromid", myId)
                    .equalTo("toid", fli.getUserid())
                    .endGroup()
                    .findAll();

            long max = result.max("msgid").longValue();

            for (DBMessage dbm : result) {
               if( dbm.getMsgid() == max ){


                   long asd = ((long)Math.floor(dbm.getT() + 0.5d))*1000;


                   fli.setLstMsg(dbm.getMsg());
                   fli.setTime(getStringDateHHmm(asd));

                   //Log.d("time","" + dateFormatted);
                   break;
               }
            }
            i++;
        }
    }

    private String getStringDateHHmm( long asd ){
        Date date = new Date(asd);
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    public void requestListHandler(JSONArray jsonArray) {
        friendRequests.clear();
        if (jsonArray.length() == 0) {
            sendedRequests();
            return;
        }
        try {
            requestS = true;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                friendRequests.add(new FriendListItem(friendObject.getInt("requestor"),"","","",false,0,1));
            }

            userInfoByID(friendRequests.get(numOfRequests).getUserid());
            numOfRequests++;
        } catch (Exception e) {
            Log.d("hiba : ", "requesthandle hiba");
        }
    }

    public void sendedHandler(JSONArray jsonArray) {
        sendedRequests.clear();
        if (jsonArray.length() == 0) {
            return;
        }
        try {
            sendedR = true;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                //sendedRequests.add(new Friend(friendObject.getInt("requested")));
                sendedRequests.add(new FriendListItem(friendObject.getInt("requested"),"","","",false,0,2));
            }

            userInfoByID(sendedRequests.get(numOfSended).getUserid());
            numOfSended++;
        } catch (Exception e) {
            Log.d("hiba : ", "requesthandle hiba");
        }
    }

    public void friendListHandler(JSONArray jsonArray) {
        friendList.clear();
        try {
            if (jsonArray.length() == 0) {
                requests();
                return;
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                if (friendObject.getInt("user1") == myId) {
                    //friendList.add(new Friend(friendObject.getInt("user2")));
                    friendList.add(new FriendListItem(friendObject.getInt("user2"),"","","",false,0,0));
                } else {
                    //friendList.add(new Friend(friendObject.getInt("user1")));
                    friendList.add(new FriendListItem(friendObject.getInt("user1"),"","","",false,0,0));
                }
            }
            friendS = true;
            userInfoByID(friendList.get(numOfFriends).getUserid());
            numOfFriends++;
        } catch (Exception e) {
            Log.d("hiba : ", "friendlisthandler" + myId);
        }
    }

    public void logInHandler() {
        logedIn = true;
        sendMessage(1);
    }

    public List<FriendListItem> getFriendList() {
        return friendList;
    }

    public void logIn(String username, String pass) {
        String msg = "{\"type\":1, \"username\":\"" + username + "\", \"pass\":\"" + pass + "\"}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("hiba", "login küldési hiba ");
        }
    }

    public void registration(String username, String pass) {
        String msg = "{\"type\":0, \"username\":\"" + username + "\", \"pass\":\"" + pass + "\"}";
        Log.d("kuld", msg);
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "regist küldési hiba ");
        }
    }

    public void addFriend(String username) {
        String msg = "{\"type\":14,\"username\":\"" + username + "\"}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
            sendAF = true;
        } catch (Exception e) {
            Log.d("hiba", "addfriend küldési hiba ");
        }
    }

    public void addFriend(int userid) {
        String msg = "{\"type\":7, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "addfriend küldési hiba ");
        }
    }

    public void requestFriendList() {
        String msg = "{\"type\":15}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "requestfriendlist küldési hiba ");
        }
    }

    public void userInfoByID(int userid) {
        String msg = "{\"type\":3, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "userInfoByID küldési hiba ");
        }
    }

    public void requests() {
        String msg = "{\"type\":9}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "requests küldési hiba ");
        }
    }

    public void sendedRequests() {
        String msg = "{\"type\":8}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "sendedreq küldési hiba ");
        }
    }

    public void removeMyself() {
        String msg = "{\"type\":2}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "removeMyself küldési hiba ");
        }
    }

    public void acceptRequest(String userName) {
        int userid = -1;

        int ind = 0;
        while (ind < friendRequests.size()) {
            //Log.d(friendRequests.get(ind).getName(),userName);
            if (friendRequests.get(ind).getName().equals(userName)) {
                userid = friendRequests.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1) {
            Log.d("hiba", "UIhiba");
            return;
        }

        friendList.add(friendRequests.get(ind));

        friendRequests.remove(ind);

        String msg = "{\"type\":4, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "acceptRequest küldési hiba ");
        }
    }

    public void undoRequest(String userName) {
        int userid = -1;

        int ind = 0;
        while (ind < sendedRequests.size()) {
            //Log.d(friendRequests.get(ind).getName(),userName);
            if (sendedRequests.get(ind).getName().equals(userName)) {
                userid = sendedRequests.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1) {
            Log.d("hiba", "UIhiba");
            return;
        }

        sendedRequests.remove(ind);

        String msg = "{\"type\":5, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "undoRequest küldési hiba ");
        }
    }

    public void declineRequest(String userName) {
        int userid = -1;

        int ind = 0;
        while (ind < friendRequests.size()) {
            //Log.d(friendRequests.get(ind).getName(),userName + " " +  friendRequests.size());
            if (friendRequests.get(ind).getName().equals(userName)) {
                userid = friendRequests.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1) {
            Log.d("hiba", "UIhiba");
            return;
        }
        friendRequests.remove(ind);

        String msg = "{\"type\":6, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "declineRequest küldési hiba ");
        }
    }

    public void deleteFriend(String userName) {
        int userid = -1;

        int ind = 0;
        while (ind < friendList.size()) {
            if (friendList.get(ind).getName().equals(userName)) {
                userid = friendList.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1) {
            Log.d("hiba", "UIhiba");
            return;
        }

        friendList.remove(ind);

        String msg = "{\"type\":16, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "deleteFriend küldési hiba ");
        }
    }

    public void sendMessage(int state) {
        Log.d("kuldok", "uzenetet");
        android.os.Message message = android.os.Message.obtain();
        message.arg1 = state;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<String> getRequestsString() {
        List str = new ArrayList<>();
        for (FriendListItem s : friendRequests) {
            str.add(s.getName());
        }
        return str;
    }

    public List<String> getFriendsString() {
        List str = new ArrayList<>();
        for (FriendListItem s : friendList) {
            str.add(s.getName());
        }
        return str;
    }

    public List<String> getSendedString() {
        List str = new ArrayList<>();
        for (FriendListItem s : sendedRequests) {
            str.add(s.getName());
        }
        return str;
    }

    public void setCurrentPartner(int currentPartner) {
        this.currentPartner = currentPartner;
    }

    public void setCurrentPartnerName(String currentPartnerName) {
        this.currentPartnerName = currentPartnerName;
    }

    public void setLastActive(String lastActive) {
        this.lastActive = lastActive;
    }

    public void startConv(String userName, long lastActive, boolean loggedin) {
        int userid = -1;
        int ind = 0;
        while (ind < friendList.size()) {
            if (friendList.get(ind).getName().equals(userName)) {
                userid = friendList.get(ind).getUserid();
                break;
            }
            ind++;
        }

        //ideiglenes
        if (userid == -1) {
            Log.d("hiba", "UIhiba");
            return;
        }
        currentPartner = userid;
        currentPartnerName = userName;

        //Date date = new Date(lastActive);
        if (!loggedin) {
            long currTime = System.currentTimeMillis() / 1000L;
            long time = currTime - lastActive;

            if (time >= 2592000) {
                this.lastActive = "Utoljára bejelentkezve : 30 napnál régebben";
            } else if (time >= 86400) {
                int rounded = (int) Math.floor(time / 86400);
                this.lastActive = "Utoljára bejelentkezve : " + rounded + " napja";
            } else if (time >= 3600) {
                int rounded = (int) Math.floor(time / 3600);
                this.lastActive = "Utoljára bejelentkezve : " + rounded + " órája";
            } else {
                int rounded = (int) Math.floor(time / 60);
                this.lastActive = "Utoljára bejelentkezve : " + rounded + " órája";
            }
        }else{
            this.lastActive = "Éppen aktiv";
        }
        //Log.d("" + lastActive ,"" + currTime);

       // DateFormat formatter = new SimpleDateFormat("HH:mm");
        //String dateFormatted = formatter.format(date);

        sendMessage(900);
    }

    public void receiveMsg(int n) {
        String msg = "{\"type\":10, \"userid\":" + currentPartner + ",\"msgcount\":" + n + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "asdasd receiveMsg küldési hiba ");
        }
    }

    public void getMsg(int userid, int n) {
        String msg = "{\"type\":10, \"userid\":" + userid + ",\"msgcount\":" + n + "}";
        String msg2 = "{\"type\":10, \"userid\":26,\"msgcount\":20}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "getMsg küldési hiba ");
        }
    }

    public void getMoreMsg(int userid, int minmsgid, int n) {
        String msg = "{\"type\":11, \"userid\":" + userid + ",\"msgid\":" + minmsgid + ",\"msgcount\":" + n + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "getMoreMsg küldési hiba ");
        }
    }

    public String encrypt(String msg)  throws Exception{
        DESKeySpec keySpec = new DESKeySpec("12345678".getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);
        // ENCODE plainTextPassword String
        byte[] cleartext = msg.getBytes("UTF8");

        Cipher cipher = Cipher.getInstance("DES"); // cipher is not thread safe
        cipher.init(Cipher.ENCRYPT_MODE, key);
        //String encryptedPwd = base64encoder.encode(cipher.doFinal(cleartext));
        String encryptedPwd = Base64.encodeToString(cipher.doFinal(cleartext), Base64.DEFAULT);

        return encryptedPwd;
    }

    public String decrypt(String msg) throws Exception{
        DESKeySpec keySpec = new DESKeySpec("12345678".getBytes("UTF8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey key = keyFactory.generateSecret(keySpec);

        // DECODE encryptedPwd String
        //byte[] encrypedPwdBytes = base64decoder.decodeBuffer(msg);
        byte[] encrypedPwdBytes = Base64.decode(msg, Base64.DEFAULT);

        Cipher cipher = Cipher.getInstance("DES");// cipher is not thread safe
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainTextPwdBytes = (cipher.doFinal(encrypedPwdBytes));

        return new String(plainTextPwdBytes);
    }

    public void send(String message) {
        try {
            String enc = encrypt(message);
            String text = enc.replace("\n", "").replace("\r", "");
            String msg = "{\"type\":13, \"userid\":" + currentPartner + ",\"msg\":\"" + text + "\"}";
            Log.d("message", msg);
            Log.d("message", enc.getBytes() + "");
            byte[] bytes = msg.getBytes();
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "send hiba hiba ");
        }
    }

    public String getCurrentPartnerName() {
        return currentPartnerName;
    }

    public String getLastActive() {
        return lastActive;
    }

    public Queue<String> getMsgQ() {
        return msgQ;
    }

    public Queue<Pair<Integer, FriendListItem>> getRefreshQ() {
        return refreshQ;
    }

    public boolean isFriendListActive() {
        return friendListActive;
    }

    public void setFriendListActive(boolean friendListActive) {
        this.friendListActive = friendListActive;
    }

    public List<FriendListItem> getFriendRequests() {
        return friendRequests;
    }

    public List<FriendListItem> getSendedRequests() {
        return sendedRequests;
    }

    public void logMe(String str){
        Log.d("logthis",str);
    }

}
