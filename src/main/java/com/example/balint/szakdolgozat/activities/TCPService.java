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

    // Binder
    private final IBinder mBinder = new LocalBinder();
    /**
     * Socket
     */
    private Socket socket = null;
    /**
     * Socket outputstream.
     */
    private DataOutputStream dos;
    /**
     * Socket inputstream.
     */
    private DataInputStream dis;
    /**
     * Bevan-e jelentkezve.
     */
    private boolean logedIn = false;
    /**
     * A felhasználó ID-ja.
     */
    private int myId;
    /**
     * A felhasználó neve.
     */
    private String myName;
    /**
     * Barátlista.
     */
    private List<FriendListItem> friendList = new ArrayList<>();
    /**
     * Bejövő felkérés lista.
     */
    private List<FriendListItem> friendRequests = new ArrayList<>();
    /**
     * Kimenő felkérés lista
     */
    private List<FriendListItem> sendedRequests = new ArrayList<>();
    /**
     * Ha a lista tetejére görgetetta  felhasználó ebbe a listába töltjük be az újakat.
     */
    private List<Pair<Integer,String>> moreMessages = new ArrayList<>();
    /**
     * Üzenetküldés az activitynek.
     */
    private Messenger messageHandler;
    /**
     * Barátfelkérés küldése folyamatban.
     */
    private boolean sendAF = false;
    /**
     * Kimenő felkérések fogadása folyamatban.
     */
    private boolean sendedR = false;
    /**
     * Kimenő felkérések száma.
     */
    private int numOfSended;
    /**
     * Barátlista fogadása folyamatban.
     */
    boolean friendS = false;
    /**
     * Barátok száma.
     */
    private int numOfFriends = 0;
    /**
     * Bejövő felkérések fogadása folyamatban.
     */
    private boolean requestS = false;
    /**
     * Bejövő felkérések száma.
     */
    private int numOfRequests = 0;
    /**
     * A jelenlegi beszélgető partner ID-ja.
     */
    private int currentPartner;
    /**
     * A jelenlegi beszélgető partner neve.
     */
    private String currentPartnerName;
    /**
     * A jelenlegi beszélgetőpartner utolsó bejelentkezésének ideje.
     */
    private String lastActive;
    /**
     * A hozzáadott barát ID-ja.
     */
    private int addFId;
    /**
     * A hozzáadott barát neve.
     */
    private String addFName;
    /**
     * Megjött-e az összes üzenet.
     */
    private int messagesArrived;
    /**
     * Lekértük-e már a barátokat, felkéréseket.
     */
    private boolean requestedyet = false;
    /**
     * Az üzenetek lekéréséhez használt lista, msgid-kat tárol.
     */
    private List<Integer> leftList = new ArrayList<>();
    /**
     * Az éppen beérkezett üzeneteket tároló sor.
     */
    private Queue<String> msgQ = new LinkedList<>();
    /**
     * Frissiteni kell ezeknek a barátoknak az adatait UI-en.
     */
    private Queue<Pair<Integer, FriendListItem>> refreshQ = new LinkedList<>();
    /**
     * A barátlista van-e megnyitva az UI-en.
     */
    private boolean friendListActive = false;
    /**
     * Folyamatos frissités megy-e.
     */
    private boolean refresh = false;
    /**
     * Az aktuális beszélgetőpartner legkisebb msgid-ja amit eltároltunk.
     */
    private int currentFirstMsgid;
    /**
     * Folyamatos frissités leállitása.
     */
    private boolean stopRefresh = false;
    /**
     * Értesitések.
     */
    private NotificationManager nm;
    private Notification notification;
    private PendingIntent pending;
    private Intent notifIntent;


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
    public void onDestroy() {
        Log.d("dest", "SeERVICE destroy");
        try {
            socket.close();
        } catch (Exception e) {
            Log.d("cannot", "close socekt");
        }
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        startTCP();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        return START_NOT_STICKY;
    }

    /**
     * Csatlakozás a szerverhez.
     */
    private void startTCP() {
        Log.d("start", "SERVICE START");
        //Egy szálat inditunk, ami folyamatosan próbál csatlakozni a szerverhez. Ha sikerül leáll.
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

    /**
     * Kommunikáció a szerverrel.
     */
    private void startCommunication() {
        /**
         * A kommunikációnak új szálat inditunk, ami fogadja a szervertől érkező üzeneteket.
         */
        Thread t1 = new Thread() {
            public void run() {
                String str;
                Log.d("comm", "COMMUNICATION READY");
                while (true) {
                    try {
                        int length = dis.readInt();
                        byte[] bytes = new byte[length];
                        dis.readFully(bytes);
                        str = new String(bytes);
                    } catch (IOException e) {
                        logOut();
                        startTCP();
                        stopRefresh = true;
                        logedIn = false;
                        break;
                    }
                    Log.d("server : ", str);
                    try {
                        processServerMsg(str);
                    } catch (JSONException e) {
                        //itt azé kéne valami figyelmeztetés hogy elrontódott a dolog
                        Log.d("HIBA: ", "JSONHIBA");
                        Log.d("myapp", Log.getStackTraceString(e));
                        stopRefresh = true;
                        logedIn = false;
                        break;
                    }
                }
            }
        };
        t1.start();
        /**
         * Folyamatos frissités inditása.
         */
        Thread t2 = new Thread() {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (true) {
                    if (stopRefresh) {
                        stopRefresh = false;
                        break;
                    }
                    if (requestedyet) {
                        Log.d("sztart", "yet");
                        friendS = true;
                        userInfoByID(friendList.get(0).getUserid());
                        refresh = true;
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    } else {
                        try {
                            //Log.d("not", "yet");
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            }
        };
        t2.start();
    }

    /**
     * SeassionID beállitása.
     * @param sid SessionID amit beállitunk
     */
    private void setupSID(String sid) throws JSONException{
        Realm rea = Realm.getInstance(this);
        RealmResults<Options> res = rea.where(Options.class)
                .equalTo("key", "sid")
                .findAll();
        rea.beginTransaction();
        if( res.size() == 1 ){
            res.get(0).setValue(sid);
        }else{
            Options msgObj = rea.createObject(Options.class);
            msgObj.setKey("sid");
            msgObj.setValue(sid);
        }
        rea.commitTransaction();
    }

    /**
     * Bejelentkezési kérelem kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void loginHandler(JSONObject jObj, String sid) throws JSONException{
        logedIn = true;
        myId = jObj.getInt("id");
        myName = jObj.getString("username");
        setupSID(sid);
        sendMessage(1);
    }

    /**
     * Regisztrációs kérelem kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void regHandler(JSONObject jObj, String sid) throws JSONException{
        //JSONObject jo3 = jsonRootObject.optJSONObject("userdata");
        myId = jObj.getInt("id");
        myName = jObj.getString("username");
        logedIn = true;
        setupSID(sid);
        sendMessage(0);
    }

    /**
     * Kijelentkezési kérelem kezelése.
     */
    public void logOut() {
        if ( socket == null || logedIn==false ) return;
        Realm realm  = Realm.getInstance(this);
        RealmResults<Options> result = realm.where(Options.class)
                .equalTo("key", "sid")
                .findAll();
        realm.beginTransaction();
        result.remove(0);
        realm.commitTransaction();
        try {
            refresh = false;
            requestedyet = false;
            logedIn = false;
            socket.close();
            socket = null;
        } catch (Exception e) {
            Log.d("", "nem tudta bezárni a socketet");
        }
        sendMessage(2);
    }

    /**
     * Egy felhasználó adatának kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     * @param loggedin A felhasználó bevan-e jelentkezve.
     */
    private void userDataRequestHandler(JSONObject jObj, int loggedin) throws JSONException{

        if (sendedR) {
            sendedRequests.get(numOfSended - 1).setName(jObj.getString("username"));
            if (sendedRequests.size() == numOfSended) {
                numOfSended = 0;
                sendedR = false;
            } else {
                userInfoByID(sendedRequests.get(numOfSended).getUserid());
                numOfSended++;
            }
        }

        if (requestS) {
            friendRequests.get(numOfRequests - 1).setName(jObj.getString("username"));
            if (friendRequests.size() == numOfRequests) {
                requestS = false;
                numOfRequests = 0;
                outgoingRequests();
            } else {
                userInfoByID(friendRequests.get(numOfRequests).getUserid());
                numOfRequests++;
            }
        }

        if (friendS) {
            if (refresh) {
                friendList.get(numOfFriends - 1).setLast_login(jObj.getLong("last_login"));
                if (loggedin != 0) {
                    friendList.get(numOfFriends - 1).setLoggedin(true);
                }else{
                    friendList.get(numOfFriends - 1).setLoggedin(false);
                }
                refreshQ.add(new Pair<Integer, FriendListItem>(numOfFriends - 1, friendList.get(numOfFriends - 1)));
                friendList.get(numOfFriends - 1).setName(jObj.getString("username"));
                sendMessage(200);
            }else{
                friendList.get(numOfFriends - 1).setLast_login(jObj.getLong("last_login"));
                if (loggedin != 0) {
                    friendList.get(numOfFriends - 1).setLoggedin(true);
                }
                friendList.get(numOfFriends - 1).setName(jObj.getString("username"));
            }

            if (friendList.size() == numOfFriends) {
                friendS = false;
                numOfFriends = 1;
                if (!refresh) {
                    requests();
                    for (FriendListItem f : friendList) {
                        getMsg(f.getUserid(), 10);
                    }
                }
                requestedyet = true;
            } else {
                userInfoByID(friendList.get(numOfFriends).getUserid());
                numOfFriends++;
            }
        }
    }

    /**
     * Az eddig váltott üzenetek lekérdezésének kezelése.
     * @param jArr Szerverüzenetet tartalmazó JSON tömb.
     */
    private void getMessageHandler(JSONArray jArr){
        try {
            Realm realm = Realm.getInstance(this);
            if (jArr.length() == 0) {
                messagesArrived++;
                if (messagesArrived == friendList.size()) {
                    messagesArrived = 0;
                    setListItems();
                    sendMessage(10);
                    Log.d("", "MINdEN ÜZENET MEGÉRKEZETT");
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

            for (Integer in : leftList) {
                if (in == jArr.getJSONObject(0).getInt("from") || in == jArr.getJSONObject(0).getInt("to")) {
                    messagesArrived++;
                    for (int i = 0; i < jArr.length(); i++) {
                        if (jArr.getJSONObject(i).getInt("msgid") > result.max("msgid").intValue()) {
                            insertMessage(realm, jArr.getJSONObject(0).getInt("from"), decrypt(jArr.getJSONObject(i).getString("msg")), jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(i).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                        }
                    }
                    if (messagesArrived == friendList.size()) {
                        messagesArrived = 0;
                        setListItems();
                        sendMessage(10);
                        Log.d("", "MINdEN ÜZENET MEGÉRKEZETT");
                    }
                    return;
                }
            }

            if (jArr.length() == 20) {
                messagesArrived++;
                for (int i = 0; i < jArr.length(); i++) {
                    if (jArr.getJSONObject(i).getInt("msgid") > result.max("msgid").intValue()) {
                        insertMessage(realm, jArr.getJSONObject(0).getInt("from"), decrypt(jArr.getJSONObject(i).getString("msg")), jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(i).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                    }
                }
            } else {
                long max = result.max("msgid").longValue();
                if (max != 0) {
                    if (jArr.getJSONObject(0).getInt("msgid") > max) {
                        if (jArr.getJSONObject(0).getInt("from") == myId) {
                            leftList.add(jArr.getJSONObject(0).getInt("to"));
                            getMsg(jArr.getJSONObject(0).getInt("to"), 20);
                        } else {
                            leftList.add(jArr.getJSONObject(0).getInt("from"));
                            getMsg(jArr.getJSONObject(0).getInt("from"), 20);
                        }

                    } else {
                        messagesArrived++;
                        for (int i = 0; i < jArr.length(); i++) {
                            if (jArr.getJSONObject(i).getInt("msgid") > result.max("msgid").intValue()) {
                                insertMessage(realm, jArr.getJSONObject(0).getInt("from"), decrypt(jArr.getJSONObject(i).getString("msg")), jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(i).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
                            }
                        }
                    }
                } else {
                    if (jArr.getJSONObject(0).getInt("from") == myId) {
                        getMsg(jArr.getJSONObject(0).getInt("to"), 20);
                        leftList.add(jArr.getJSONObject(0).getInt("to"));
                    } else {
                        getMsg(jArr.getJSONObject(0).getInt("from"), 20);
                        leftList.add(jArr.getJSONObject(0).getInt("from"));
                    }
                }
            }
            if (messagesArrived == friendList.size()) {
                messagesArrived = 0;
                setListItems();
                sendMessage(10);
                Log.d("", "MINDEN ÜZENET MEGÉRKEZETT");
            }
        } catch (Exception e) {
            Log.d("", Log.getStackTraceString(e));
        }
    }

    /**
     * Több üzenet lekérdezésének kezelése.
     * @param jArr Szerverüzenetet tartalmazó JSON tömb.
     */
    private void getMoreMessageHandler(JSONArray jArr) throws JSONException{
        moreMessages.clear();
        if ( jArr.length() == 0 ) return;
        currentFirstMsgid = jArr.getJSONObject(0).getInt("msgid");
        for ( int i = 0; i < jArr.length(); i++ ){
            JSONObject jo = jArr.getJSONObject(i);
            try {
                moreMessages.add(new Pair<Integer, String>(jo.getInt("from"), decrypt(jo.getString("msg"))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sendMessage(11);
    }

    /**
     * Üzenet küldés egy felhasználónak kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void sendMessageHandler(JSONObject jObj){
        try {
            Realm realm = Realm.getInstance(this);
            insertMessage(realm, jObj.getInt("from"), decrypt(jObj.getString("msg")), jObj.getInt("msgid"), jObj.getDouble("t"), jObj.getInt("to"));
            setListItems();
            sendMessage(13);
        } catch (Exception e) {
            Log.d("", "13 as error");
            Log.d("", Log.getStackTraceString(e));
        }
    }

    /**
     * Egy felhasználó adatának kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void getUserDataByNameHandler(JSONObject jObj) throws JSONException{
        if (sendAF) {
            addFName = jObj.getString("username");
            addFId = jObj.getInt("id");
            addFriend(jObj.getInt("id"));
            sendAF = false;
        }
    }

    /**
     * Bejelendkezés sid alapján kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void logInWithSIDHandler(JSONObject jObj) throws JSONException{
        myId = jObj.getInt("id");
        myName = jObj.getString("username");
        logedIn = true;
        sendMessage(1);
    }

    /**
     * Üzenet beérkezésének kezelése.
     * @param jArr Szerverüzenetet tartalmazó JSON tömb.
     */
    private void autoMessageRecieveHandler(JSONArray jArr) throws JSONException{
        Realm realm = Realm.getInstance(this);
        String message = "";
        try {
            message = decrypt(jArr.getJSONObject(0).getString("msg"));
        } catch (Exception e) {
            Log.d("HIba", "decrypt hiba");
            return;
        }
        Log.d("message", message);
        if (jArr.getJSONObject(0).getInt("from") == currentPartner) {
            insertMessage(realm, jArr.getJSONObject(0).getInt("from"), message, jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(0).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
            msgQ.add(message);
            sendMessage(16);
        } else {
            insertMessage(realm, jArr.getJSONObject(0).getInt("from"), message, jArr.getJSONObject(0).getInt("msgid"), jArr.getJSONObject(0).getDouble("t"), jArr.getJSONObject(0).getInt("to"));
            setListItems();
            nm = (NotificationManager) getSystemService(this.NOTIFICATION_SERVICE);
            notifIntent = new Intent(this, MessagingActivity.class);
            notifIntent.putExtra("id", jArr.getJSONObject(0).getInt("from"));
            pending = PendingIntent.getActivity(this, 0, notifIntent, 0);

            if (friendListActive) {
                int i = 0;
                while (i < friendList.size()) {
                    if (friendList.get(i).getUserid() == jArr.getJSONObject(0).getInt("from")) {
                        friendList.get(i).setLstMsg(message);
                        friendList.get(i).setTime(getStringDateHHmm(jArr.getJSONObject(0).getLong("t") * 1000));
                        refreshQ.add(new Pair<Integer, FriendListItem>(i, friendList.get(i)));
                        sendMessage(200);
                        break;
                    }
                    i++;
                }
            }
            startNotification(realm, jArr.getJSONObject(0).getInt("from"), message);
        }
    }

    /**
     * Barátfelkérés beérkezésének kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void autoFriendRequestHandler(JSONObject jObj) throws JSONException{
        friendRequests.add(new FriendListItem(jObj.getInt("id"), jObj.getString("username"), "", "", true, 0, 1));
        if (friendListActive) {
            sendMessage(300);
        }
    }

    /**
     * Barátfelkérés elfogadásának kezelése.
     * @param jObj Szerverüzenetet tartalmazó JSON objektum.
     */
    private void autoAcceptRequestHandler(JSONObject jObj) throws JSONException{
        int ind = 0;
        for (int i = 0; i < sendedRequests.size(); i++) {
            if (sendedRequests.get(i).getUserid() == jObj.getInt("id")) {
                ind = i;
                break;
            }
        }
        sendedRequests.remove(ind);
        friendList.add(new FriendListItem(jObj.getInt("id"), jObj.getString("username"), "", "", true, 0, 0));
        if (friendListActive) {
            sendMessage(301);
        }
    }

    /**
     * Szerver üzenetek feldolgozása.
     * @param jsonString Szervertől olvasott adat.
     */
    private void processServerMsg(String jsonString) throws JSONException {
        JSONObject jsonRootObject = new JSONObject(jsonString);
        String type;
        try {
            type = jsonRootObject.get("type").toString();
        } catch (JSONException e) {
            type = "hiba";
        }
        if (!type.equals("hiba")) {
            switch (jsonRootObject.get("type").toString()) {
                case "0":
                    regHandler(jsonRootObject.optJSONObject("userdata"), jsonRootObject.getString("sid"));
                    break;
                case "1":
                    loginHandler(jsonRootObject.optJSONObject("userdata"), jsonRootObject.getString("sid"));
                    break;
                case "2":
                    logOut();
                case "3":
                    userDataRequestHandler(jsonRootObject.optJSONObject("userdata"), jsonRootObject.getInt("isloggedin"));
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
                    sendedRequests.add(new FriendListItem(addFId, addFName, "", "", false, 0, 2));
                    sendMessage(7);
                    break;
                case "8":
                    sendedHandler(jsonRootObject.optJSONArray("requests"));
                    break;
                case "9":
                    requestListHandler(jsonRootObject.optJSONArray("requests"));
                    break;
                case "10":
                    Log.d("", "üzenett jöt");
                    getMessageHandler(jsonRootObject.optJSONArray("messages"));
                    break;
                case "11":
                    getMoreMessageHandler(jsonRootObject.optJSONArray("messages"));
                    sendMessage(30);
                    break;
                case "12":
                    sendMessage(12);
                    break;
                case "13":
                    sendMessageHandler(jsonRootObject.optJSONObject("message"));
                    break;
                case "14":
                    getUserDataByNameHandler(jsonRootObject.optJSONObject("userdata"));
                    break;
                case "15":
                    friendListHandler(jsonRootObject.optJSONArray("relationships"));
                    break;
                case "16":
                    sendMessage(16);
                    break;
                case "18":
                    logInWithSIDHandler(jsonRootObject.optJSONObject("userdata"));
                    break;
                case "19":
                    autoMessageRecieveHandler(jsonRootObject.optJSONArray("messages"));
                    break;
                case "21":
                    autoFriendRequestHandler(jsonRootObject.optJSONObject("userdata"));
                    break;
                case "22":
                    autoAcceptRequestHandler(jsonRootObject.optJSONObject("userdata"));
                    break;
            }
        } else {
            String errorCode = jsonRootObject.get("code").toString();
            switch (errorCode) {
                case "1":
                    sendMessage(9111);
                    break;
                case "6":
                    sendMessage(9116);
                    break;
                case "21":
                    sendMessage(1121);
                    break;
                case "2":
                    sendMessage(112);
                    break;
                case "20":
                    Log.d("asdasdsa","10202020s");
                    sendMessage(20);
                    break;
                case "26":
                    sendMessage(26);
                    break;
                case "27":
                    sendMessage(27);
                    break;
                case "24" :
                    Realm realm  = Realm.getInstance(this);
                    RealmResults<Options> result = realm.where(Options.class)
                            .equalTo("key","sid")
                            .findAll();
                    realm.beginTransaction();
                    result.remove(0);
                    realm.commitTransaction();
                    break;
            }
        }

    }

    /**
     * Értesités küldése
     * @param realm Adatbázis ami az értesitésekre vonatkozó beállitásokat is tartalmazza.
     * @param id Melyik id-jú felhasználtótól kaptuk az üzenetet.
     * @param message Kapott üzenet, amit notifikációba megjelenitünk.
     */
    private void startNotification(Realm realm, int id, String message) throws JSONException {
        RealmResults<Options> result = realm.where(Options.class)
                .equalTo("key", "notification")
                .findAll();
        if (result.get(0).getValue().equals("1")) {

            result = realm.where(Options.class)
                    .equalTo("key", "notifsound")
                    .findAll();

            RealmResults<Options> result2 = realm.where(Options.class)
                    .equalTo("key", "notifvibrate")
                    .findAll();

            if (result.get(0).getValue().equals("1") && result2.get(0).getValue().equals("1")) {
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
                nm.notify(id, notification);
            } else {
                if (result.get(0).getValue().equals("1")) {
                    Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    notification = new Notification.Builder(this)
                            .setContentTitle("Chat")
                            .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                            .setContentIntent(pending)
                            .setSound(alarmSound)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build();
                    nm.notify(id, notification);
                } else if (result2.get(0).getValue().equals("1")) {
                    notification = new Notification.Builder(this)
                            .setContentTitle("Chat")
                            .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                            .setContentIntent(pending)
                            .setVibrate(new long[]{0, 1000, 1000, 1000, 1000})
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build();
                    nm.notify(id, notification);
                } else {
                    notification = new Notification.Builder(this)
                            .setContentTitle("Chat")
                            .setContentText(message).setSmallIcon(R.drawable.ic_launcher)
                            .setContentIntent(pending)
                            .setWhen(System.currentTimeMillis())
                            .setAutoCancel(true)
                            .build();
                    nm.notify(id, notification);
                }

            }
        }
    }

    /**
     * Üzenet hozzáadása az adatbázishoz
     * @param realm Üzeneteket tartalmazó adatbázis.
     * @param fromid Melyik id-jú felhasználtótól kaptuk az üzenetet.
     * @param msg Kapott üzenet, amit notifikációba megjelenitünk.
     * @param msgid Az üzenet id-ja.
     * @param t Az üzenet timestampje.
     * @param toid Akinek küldték az üzenetet.
     */
    private void insertMessage(Realm realm, int fromid, String msg, int msgid, double t, int toid) {
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

    /**
     * Az adatábizis és a barátokat tartalmazó lista szinkronizálása.
     */
    private void setListItems() {
        Realm realm = Realm.getInstance(this);
        int i = 0;
        for (FriendListItem fli : friendList) {
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
                if (dbm.getMsgid() == max) {
                    long asd = ((long) Math.floor(dbm.getT() + 0.5d)) * 1000;
                    fli.setLstMsg(dbm.getMsg());
                    fli.setTime(getStringDateHHmm(asd));
                    break;
                }
            }
            i++;
        }
    }

    /**
     * Az adatábizis és a barátokat tartalmazó lista szinkronizálása.
     * @param timestamp Timestamp.
     * @return String. Szöveggé formázott timestamp.
     */
    private String getStringDateHHmm(long timestamp) {
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }

    /**
     * Bejövő felkérések kezelése.
     * @param jsonArray Szerverüzenetet tartalmazó JSON tömb..
     */
    private void requestListHandler(JSONArray jsonArray) {
        friendRequests.clear();
        if (jsonArray.length() == 0) {
            outgoingRequests();
            return;
        }
        try {
            requestS = true;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                friendRequests.add(new FriendListItem(friendObject.getInt("requestor"), "", "", "", false, 0, 1));
            }

            userInfoByID(friendRequests.get(numOfRequests).getUserid());
            numOfRequests++;
        } catch (Exception e) {
            Log.d("hiba : ", "requesthandle hiba");
        }
    }

    /**
     * Kimenő felkérések kezelése.
     * @param jsonArray Szerverüzenetet tartalmazó JSON tömb..
     */
    private void sendedHandler(JSONArray jsonArray) {
        sendedRequests.clear();
        if (jsonArray.length() == 0) {
            return;
        }
        try {
            sendedR = true;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                sendedRequests.add(new FriendListItem(friendObject.getInt("requested"), "", "", "", false, 0, 2));
            }

            userInfoByID(sendedRequests.get(numOfSended).getUserid());
            numOfSended++;
        } catch (Exception e) {
            Log.d("hiba : ", "requesthandle hiba");
        }
    }

    /**
     * Barátlista kezelése.
     * @param jsonArray Szerverüzenetet tartalmazó JSON tömb..
     */
    private void friendListHandler(JSONArray jsonArray) {
        friendList.clear();
        try {
            if (jsonArray.length() == 0) {
                requests();
                return;
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                if (friendObject.getInt("user1") == myId) {
                    friendList.add(new FriendListItem(friendObject.getInt("user2"), "", "", "", false, 0, 0));
                } else {
                    friendList.add(new FriendListItem(friendObject.getInt("user1"), "", "", "", false, 0, 0));
                }
            }
            friendS = true;
            userInfoByID(friendList.get(numOfFriends).getUserid());
            numOfFriends++;
        } catch (Exception e) {
            Log.d("hiba : ", "friendlisthandler" + myId);
        }
    }


    public List<FriendListItem> getFriendList() {
        return friendList;
    }

    /**
     * Bejelentkezési kérelem küldése a szervernek.
     * @param username Felhasználónév
     * @param pass  Jelszó
     */
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

    /**
     *  Regisztrációs kérelem küldése a szervernek
     * @param username Felhasználónév
     * @param pass Jelszó
     */
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

    /**
     *  Hozzáadni kivánt barát adatainak lekérése név alapján.
     * @param username Hozzáadni kivánt barát felhasználóneve.
     */
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

    /**
     *  Barát felkérés küldése a szervernek.
     * @param userid A hozzáadni kivánt barát ID-ja.
     */
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

    /**
     *  Barátlista lekérése a szervertől.
     */
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

    /**
     *  Egy felhasználó adatának lekérése a szervertől.
     * @param userid
     */
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

    /**
     * Beérkező felkérések lekérdezése a szerverről.
     */
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

    /**
     *  Kimenő felkérések lekérése a szervertől.
     */
    public void outgoingRequests() {
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

    /**
     *  Felhasználói fiók törlésének kérelme a szervertől.
     */
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

    /**
     * Egy felkérés elfogadásának küldése a szervernek.
     * @param userName A felhasználó neve akinek a felkérését elfogadtuk.
     */
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

    /**
     * Egy elkülött felkérésnek visszavonása.
     * @param userName A felhasználó neve
     */
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

    /**
     * Egy barátfelkérés visszautasitásának küldése a szervernek.
     * @param userName A barát neve.
     */
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

    /**
     * Egy barát törlésének küldése a szervernek.
     * @param userName A barát neve
     */
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

    /**
     * Üzenet küldése az activitynek.
     * @param state Az üzenet kódja.
     */
    public void sendMessage(int state) {
        //Log.d("kuldok", "uzenetet");
        android.os.Message message = android.os.Message.obtain();
        message.arg1 = state;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFriendsString() {
        List str = new ArrayList<>();
        for (FriendListItem s : friendList) {
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

    /**
     * Session ID küldése a szervernek.
     * @param sid Session ID.
     */
    public void sendSID(String sid){
        String msg = "{\"type\":18,\"sid\":\"" + sid + "\"}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "sid küldés ");
        }
    }

    /**
     * Egy beszélgetés kezdése egy felhasználóval.
     * @param userName A barát neve.
     * @param lastActive Az utolsó bejelentkezésének ideje.
     * @param loggedin Bevan-e jelentkezve.
     */
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
        } else {
            this.lastActive = "Éppen aktiv";
        }
        //Log.d("" + lastActive ,"" + currTime);

        // DateFormat formatter = new SimpleDateFormat("HH:mm");
        //String dateFormatted = formatter.format(date);

        sendMessage(900);
    }

    /**
     * Üzenetek lekérdezése a szerverről.
     * @param userid A felhasználó ID-ja.
     * @param n Lekérni kivánt üzenetek száma.
     */
    public void getMsg(int userid, int n) {
        String msg = "{\"type\":10, \"userid\":" + userid + ",\"msgcount\":" + n + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "getMsg küldési hiba ");
        }
    }

    /**
     * Több üzenet lekérése
     * @param userid A felhasználó ID-ja
     * @param minmsgid Melyik üzenettől visszamenőleg szeretnénk lekérni.
     * @param n Lekérni kivánt üzenetek száma.
     */
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

    /**
     * Egy üzenet kódolása.
     * @param msg Üzenet.
     * @return Kódolt üzenet.
     * @throws Exception
     */
    private String encrypt(String msg) throws Exception {
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

    /**
     * Egy üzenet visszafejtése.
     * @param msg A kódolt üzenet.
     * @return A visszafejtett üzenet.
     * @throws Exception
     */
    private String decrypt(String msg) throws Exception {
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

    /**
     * EGy üzenet küldése.
     * @param message Üzenet.
     */
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

    public void deleteMessages(int userid,int msgid, int ind) {
        String msg = "{\"type\":12, \"userid\":" + userid + ",\"msgid\":" + msgid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        } catch (Exception e) {
            Log.d("hiba", "deleteMessages küldési hiba ");
        }
        refreshQ.add(new Pair<Integer, FriendListItem>(ind, friendList.get(ind)));
        friendList.get(ind).setLstMsg("");
        friendList.get(ind).setTime("");
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

    public void setFriendListActive(boolean friendListActive) {
        this.friendListActive = friendListActive;
    }

    public List<FriendListItem> getFriendRequests() {
        return friendRequests;
    }

    public List<FriendListItem> getSendedRequests() {
        return sendedRequests;
    }

    public boolean isLogedIn() {
        return logedIn;
    }

    public String getMyName() {
        return myName;
    }

    public int getMyId() {
        return myId;
    }

    public List<Pair<Integer, String>> getMoreMessages() {
        return moreMessages;
    }

    public int getCurrentFirstMsgid() {
        return currentFirstMsgid;
    }

    public void setCurrentFirstMsgid(int currentFirstMsgid) {
        this.currentFirstMsgid = currentFirstMsgid;
    }

    public int getCurrentPartner() {
        return currentPartner;
    }

    public boolean isRequestedyet() {
        return requestedyet;
    }
}
