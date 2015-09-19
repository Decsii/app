package com.example.balint.szakdolgozat;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import io.realm.Realm;

/**
 * Created by Balint on 2015.08.20..
 */
public class TCPService  extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    public final static String EXTRA_MESSAGE = "com.example.balint.szakdolgozat.MESSAGE";

    public Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private boolean logedIn = false;
    private int myId;

    private List<Friend> friendList = new ArrayList<>();
    private List<Friend> friendRequests = new ArrayList<>();

    private Messenger messageHandler;
    private boolean sendAF = false;

    private boolean sendedR = false;
    private int numOfSended = 0;
    private List<Friend> sendedRequests = new ArrayList<>();

    List<Pair<String,Integer>> messagesList = new ArrayList<>();

    boolean friendS = false;
    int numOfFriends = 0;

    boolean requestS = false;
    int numOfRequests = 0;

    int currentPartner;

    private double timeS;

    private int addFId;
    private String addFName;

    private boolean byebye = false;

    String currUz;

    public boolean requestedyet = false;

    public class LocalBinder extends Binder {
        TCPService getService() {
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
        super.onDestroy();
    }

    @Override
    public void onCreate(){
        startTCP();
        /*
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                try {
                    String ip = "79.143.178.118";
                    int port = 6000;
                    InetSocketAddress isa = new InetSocketAddress(ip,port);
                    socket.connect(isa);
                    dos = new DataOutputStream(socket.getOutputStream());
                    dis = new DataInputStream(socket.getInputStream());
                    while(true) {
                        String str;
                        while(true) {
                            int length = dis.readInt();

                            byte[] bytes = new byte[length];
                            dis.read(bytes, 0, length);

                            str = new String(bytes);
                            if ( str != null ) {
                                Log.d("server : ", str);
                                try {
                                    feldolgozas(str);
                                }catch(JSONException e){
                                    Log.d("HIBA: ", "JSONHIBA");
                                    Log.d("myapp", Log.getStackTraceString(e));
                                }
                            }
                        }
                    }
                 }catch(Exception e) {
                    byebye = true;
                    System.out.println("something went wrong");
                    Log.d("myapp", Log.getStackTraceString(e));
                }
            }
        };
        thread.start();
        */
    }

    public void startTCP(){
        Thread t1 = new Thread(){
            public void run() {
                while(true) {
                    try{
                        String ip = "79.143.178.118";
                        int port = 6000;
                        InetSocketAddress isa = new InetSocketAddress(ip,port);
                        socket = new Socket();
                        socket.connect(isa);
                        dos = new DataOutputStream(socket.getOutputStream());
                        dis = new DataInputStream(socket.getInputStream());
                        startCommunication();
                        Thread thread2 = new Thread() {
                            @Override
                            public void run() {
                                while (true){
                                    try {
                                        Thread.sleep(1000);
                                    }catch (Exception e){
                                        Log.d("", "INTERRUPTED");
                                    }
                                    if(logedIn)receiveMsg(1);
                                }
                            }
                        };
                        thread2.start();
                        Log.d("","start tcp leááált");
                        break;
                    }catch(Exception e){
                        try{
                            Thread.sleep(2000);
                            Log.d("","újracsatlakozás");
                        }catch (InterruptedException ex){
                            Log.d("","váratlan hiba");
                        }
                    }
                }
            }
        };
        t1.start();
    }

    public void startCommunication(){
        Thread t1 = new Thread() {
            public void run() {
                String str;
                while(true) {
                    try {
                        int length = dis.readInt();
                        byte[] bytes = new byte[length];
                        dis.read(bytes, 0, length);
                        str = new String(bytes);

                    }catch(IOException e){
                        startTCP();
                        Log.d("","start communication leááált");
                        break;
                    }
                    Log.d("server : ", str);
                    try {
                        feldolgozas(str);
                    }catch(JSONException e){
                        //itt azé kéne valami figyelmeztetés hogy elrontódott a dolog
                        Log.d("HIBA: ", "JSONHIBA");
                        Log.d("myapp", Log.getStackTraceString(e));
                    }
                }
            }
        };
        t1.start();
    }

    public void feldolgozas(String jsonString) throws JSONException{
        JSONObject jsonRootObject = new JSONObject(jsonString);
        String type;
        try {
            type = jsonRootObject.get("type").toString();
        }catch(JSONException e){
            type = "hiba";
        }

        if (!type.equals("hiba")) {
            JSONObject jsonObject = jsonRootObject.optJSONObject("userdata");
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
                    sendMessage(2);
                case "3":
                    JSONObject jo2 = jsonRootObject.optJSONObject("userdata");

                    if ( sendedR ){
                        sendedRequests.get(numOfSended -1).setName( jo2.getString("username") );
                        if ( sendedRequests.size() == numOfSended ){
                            //sendMessage(8);
                            numOfSended = 0;
                            sendedR = false;
                        }else{
                            userInfoByID( sendedRequests.get(numOfSended).getUserid() );
                            numOfSended++;
                        }
                    }

                    if ( requestS ){
                        friendRequests.get(numOfRequests -1).setName( jo2.getString("username") );
                        if ( friendRequests.size() == numOfRequests ){
                            //sendMessage(9);
                            requestS = false;
                            numOfRequests = 0;
                            sendedRequests();
                        }else{
                            userInfoByID( friendRequests.get(numOfRequests).getUserid() );
                            numOfRequests++;
                        }
                    }

                    if ( friendS ){
                        requestedyet = true;
                        friendList.get(numOfFriends - 1).setName( jo2.getString("username") );
                        if ( friendList.size() == numOfFriends ){
                            sendMessage(15);
                            friendS = false;
                            numOfFriends = 0;
                            requests();
                        }else{
                            userInfoByID( friendList.get(numOfFriends).getUserid() );
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
                    sendedRequests.add( new Friend(addFId, addFName) );
                    sendMessage(7);
                    break;
                case "8":
                    sendedHandler(jsonRootObject.optJSONArray("requests"));
                    break;
                case "9":
                    requestListHandler(jsonRootObject.optJSONArray("requests"));
                    break;
                case "10":
                    try {

                        jArr = jsonRootObject.optJSONArray("messages");
                        int kuldo = jArr.getJSONObject(0).getInt("from");
                        if ( kuldo == currentPartner ){
                            if ( timeS != jArr.getJSONObject(0).getDouble("t") ) {
                                timeS = jArr.getJSONObject(0).getDouble("t");
                                currUz = jArr.getJSONObject(0).getString("msg");
                                messagesList.add(new Pair(currUz,0));
                                sendMessage(10);
                            }
                        }
                    }catch(Exception e){}
                    break;
                case "13":
                    sendMessage(13);
                    break;
                case "14":
                    JSONObject jo = jsonRootObject.optJSONObject("userdata");
                    if (sendAF){
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
            }
        }else{
            String errorCode = jsonRootObject.get("code").toString();
            switch(errorCode){
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

    public void logOut(){
        try {
            byebye = true;
            requestedyet = false;
            timeS = 0;
            logedIn = false;
            socket.close();
            socket = null;
        }catch (Exception e){
            Log.d("","nem tudta bezárni a socketet");
        }
    }

    public void requestListHandler(JSONArray jsonArray){
        friendRequests.clear();
        if (jsonArray.length() == 0){
            sendedRequests();
            return;
        }
        try {
            requestS = true;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                friendRequests.add( new Friend( friendObject.getInt("requestor") ) );
            }

            userInfoByID( friendRequests.get(numOfRequests).getUserid() );
            numOfRequests++;
        }catch(Exception e){
            Log.d("hiba : ", "requesthandle hiba"  );
        }
    }

    public void sendedHandler(JSONArray jsonArray){
        sendedRequests.clear();
        if (jsonArray.length() == 0){
            return;
        }
        try {
            sendedR = true;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                sendedRequests.add( new Friend( friendObject.getInt("requested") ) );
            }

            userInfoByID( sendedRequests.get(numOfSended).getUserid() );
            numOfSended++;
        }catch(Exception e){
            Log.d("hiba : ", "requesthandle hiba"  );
        }
    }

    public void friendListHandler(JSONArray jsonArray){
        friendList.clear();
        try {
            if (jsonArray.length() == 0){
                requests();
                return;
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject friendObject = jsonArray.getJSONObject(i);
                if (friendObject.getInt("user1") == myId) {
                    friendList.add( new Friend( friendObject.getInt("user2") ));
                } else {
                    friendList.add( new Friend( friendObject.getInt("user1") ));
                }
            }
            friendS = true;
            userInfoByID( friendList.get(numOfFriends).getUserid() );
            numOfFriends++;
        }catch(Exception e){
            Log.d("hiba : ", "friendlisthandler" + myId  );
        }
    }

    public void logInHandler(){
        logedIn = true;
        sendMessage(1);
    }

    public void logIn(String username, String pass){
        String msg = "{\"type\":1, \"username\":\""+ username  +"\", \"pass\":\""+ pass + "\"}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            e.printStackTrace();
            Log.d("hiba","login küldési hiba ");
        }
    }

    public void registration(String username, String pass){
        String msg = "{\"type\":0, \"username\":\""+ username  +"\", \"pass\":\""+ pass + "\"}";
        Log.d("kuld", msg);
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","regist küldési hiba ");
        }
    }

    public void addFriend(String username){
        String msg = "{\"type\":14,\"username\":\""+ username  +"\"}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
            sendAF = true;
        }catch (Exception e) {
            Log.d("hiba","addfriend küldési hiba ");
        }
    }

    public void addFriend(int userid){
        String msg = "{\"type\":7, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","addfriend küldési hiba ");
        }
    }

    public void requestFriendList(){
        String msg = "{\"type\":15}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","requestfriendlist küldési hiba ");
        }
    }

    public void userInfoByID(int userid){
        String msg = "{\"type\":3, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","userInfoByID küldési hiba ");
        }
    }

    public void requests(){
        String msg = "{\"type\":9}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","requests küldési hiba ");
        }
    }

    public void sendedRequests(){
        String msg = "{\"type\":8}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","sendedreq küldési hiba ");
        }
    }

    public void removeMyself(){
        String msg = "{\"type\":2}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","removeMyself küldési hiba ");
        }
    }

    public void acceptRequest(String userName){
        int userid = -1;

        int ind = 0;
        while ( ind < friendRequests.size() ){
            //Log.d(friendRequests.get(ind).getName(),userName);
            if ( friendRequests.get(ind).getName().equals(userName) ){
                userid = friendRequests.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1){
            Log.d("hiba", "UIhiba");
            return;
        }

        friendList.add( friendRequests.get(ind) );

        friendRequests.remove(ind);

        String msg = "{\"type\":4, \"userid\":" + userid + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","acceptRequest küldési hiba ");
        }
    }

    public void undoRequest(String userName){
        int userid = -1;

        int ind = 0;
        while ( ind < sendedRequests.size() ){
            //Log.d(friendRequests.get(ind).getName(),userName);
            if ( sendedRequests.get(ind).getName().equals(userName) ){
                userid = sendedRequests.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1){
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
        }catch (Exception e) {
            Log.d("hiba","undoRequest küldési hiba ");
        }
    }


    public void declineRequest(String userName){
        int userid = -1;

        int ind = 0;
        while ( ind < friendRequests.size() ){
            //Log.d(friendRequests.get(ind).getName(),userName + " " +  friendRequests.size());
            if ( friendRequests.get(ind).getName().equals(userName) ){
                userid = friendRequests.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1){
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
        }catch (Exception e) {
            Log.d("hiba","declineRequest küldési hiba ");
        }
    }

    public void deleteFriend(String userName){
        int userid = -1;

        int ind = 0;
        while ( ind < friendList.size() ){
            if ( friendList.get(ind).getName().equals(userName) ){
                userid = friendList.get(ind).getUserid();
                break;
            }
            ind++;
        }
        //ideiglenes
        if (userid == -1){
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
        }catch (Exception e) {
            Log.d("hiba","deleteFriend küldési hiba ");
        }
    }


    public void sendMessage(int state) {
        Log.d("kuldok", "uzenetet");
        Message message = Message.obtain();
        message.arg1 = state;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public List<String> getRequestsString(){
        List str = new ArrayList<>();
        for ( Friend s : friendRequests ){
            str.add(s.getName());
        }
        return str;
    }

    public List<String> getFriendsString(){
        List str = new ArrayList<>();
        for ( Friend s : friendList ){
            str.add(s.getName());
        }
        return str;
    }

    public List<String> getSendedString(){
        List str = new ArrayList<>();
        for ( Friend s : sendedRequests ){
            str.add(s.getName());
        }
        return str;
    }

    public void startConv( String userName ){
        int userid = -1;

        int ind = 0;
        while ( ind < friendList.size() ){
            if ( friendList.get(ind).getName().equals(userName) ){
                userid = friendList.get(ind).getUserid();
                break;
            }
            ind++;
        }

        //ideiglenes
        if (userid == -1){
            Log.d("hiba", "UIhiba");
            return;
        }
        currentPartner = userid;
        sendMessage(900);

    }

    public void receiveMsg(int n){
        String msg = "{\"type\":10, \"userid\":" + currentPartner +",\"msgcount\":" + n + "}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","receiveMsg küldési hiba ");
        }
    }

    public void send(String message){
        //{"type":13, "userid":17,"msg":"Szia! Ez egy tesztüzenet."}

        String msg = "{\"type\":13, \"userid\":" + currentPartner + ",\"msg\":\"" + message +"\"}";
        byte[] bytes = msg.getBytes();
        try {
            dos.writeInt(bytes.length);
            dos.write(bytes);
            dos.flush();
        }catch (Exception e) {
            Log.d("hiba","send hiba hiba ");
        }
    }

}
