package com.example.balint.szakdolgozat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;


public class MessagingActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.example.balint.szakdolgozat.MESSAGE";
    public EditText et;

    private TCPService tcps;
    private boolean mBound = false;
    private MessageAdapter messageAdapter;
    private ListView messagesList;
    private List<String> messageList = new ArrayList<>();
    private EditText messageF;
    private Button sendB;
    private Handler messageHandler = new MessageHandler();

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message message) {
            int state = message.arg1;
            Intent intent;
            String uz;
            switch (state) {
                case 13:
                    Log.d("service", "kézbesitbe");
                    break;
                case 10:
                    msgRecieved();
                    break;
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TCPService.LocalBinder binder = (TCPService.LocalBinder) service;
            tcps = binder.getService();
            onServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));

        // Bind to LocalService
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        messageF = (EditText) findViewById(R.id.messageF);

        sendB = (Button) findViewById(R.id.sendB);
        sendB.setOnClickListener(sendBclick);

        messagesList = (ListView) findViewById(R.id.listMessages);
        messageAdapter = new MessageAdapter(this);
        messagesList.setAdapter(messageAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_messaging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener sendBclick = new View.OnClickListener() {
        public void onClick(View v) {
            if (messageF.getText().toString() != "") {
                tcps.send(messageF.getText().toString());
                messageList.add(messageF.getText().toString());
                messageAdapter.addMessage(messageF.getText().toString(), MessageAdapter.DIRECTION_OUTGOING);
                tcps.messagesList.add(new Pair(messageF.getText().toString(), 1));
            }
        }
    };

    private void msgRecieved() {
        Realm realm = Realm.getInstance(this);
        //RealmResults<DBMessage> result = realm.where(DBMessage.class)
        //        .equalTo("fromid", tcps.currentPartner)
        //        .or()
        //        .equalTo("fromid", tcps.myId)
        //        .findAll();

        RealmResults<DBMessage> result = realm.where(DBMessage.class)
                .beginGroup()
                .equalTo("fromid", tcps.currentPartner)
                .equalTo("toid", tcps.myId)
                .endGroup()
                .or()
                .beginGroup()
                .equalTo("fromid", tcps.myId)
                .equalTo("toid", tcps.currentPartner)
                .endGroup()
                .findAll();

        List<String> asd = new ArrayList<>();
        List<Integer> asd2 = new ArrayList<>();
        for (DBMessage msg : result) {
            asd.add(msg.getMsg());
            asd2.add(msg.getFromid());
        }
        Log.d("", asd.toString());
        for (int i = 0; i < asd.size(); i++) {
            String message = asd.get(i);
            if (tcps.myId == asd2.get(i)) {
                messageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING);
            } else {
                messageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING);
            }
        }
        //messageAdapter.addMessage(tcps.currUz, MessageAdapter.DIRECTION_INCOMING);
    }

    private void populateMessageHistory() {

        Realm realm = Realm.getInstance(this);

        //RealmResults<DBMessage> result = realm.where(DBMessage.class)
        //        .equalTo("fromid", tcps.currentPartner)
        //        .or()
        //        .equalTo("fromid", tcps.myId)
        //        .findAll();

        RealmResults<DBMessage> result2 = realm.where(DBMessage.class).findAll();
        Log.d("",result2.toString());
        Log.d("test1",tcps.currentPartner + " " + tcps.myId);
        RealmResults<DBMessage> result = realm.where(DBMessage.class)
                .beginGroup()
                    .equalTo("fromid", tcps.currentPartner)
                    .equalTo("toid", tcps.myId)
                .endGroup()
                .or()
                .beginGroup()
                    .equalTo("fromid", tcps.myId)
                    .equalTo("toid", tcps.currentPartner)
                .endGroup()
                .findAll();

        Log.d("",result.toString());
        result.sort("msgid");
        List<String> asd = new ArrayList<>();
        List<Integer> asd2 = new ArrayList<>();
        for (DBMessage msg : result) {
            asd.add(msg.getMsg());
            asd2.add(msg.getFromid());
        }
        Log.d("", asd.toString());
        for (int i = 0; i < asd.size(); i++) {
            String message = asd.get(i);
            if (tcps.myId == asd2.get(i)) {
                messageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING);
            } else {
                messageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING);
            }
        }
    }

    private void onServiceReady() {
        mBound = true;
        Intent intent = new Intent(MessagingActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);
        populateMessageHistory();
    }

}
