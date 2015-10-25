package com.example.balint.szakdolgozat.activities;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.balint.szakdolgozat.javaclasses.DBMessage;
import com.example.balint.szakdolgozat.javaclasses.FriendListItem;
import com.example.balint.szakdolgozat.javaclasses.MessageAdapter;
import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.TCPService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    //msgRecieved();
                    break;
                case 16:
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
        startService(intent);
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
                Spannable sp = getSmiledText(MessagingActivity.this,messageF.getText());
                messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_OUTGOING);
                tcps.messagesList.add(new Pair(messageF.getText().toString(), 1));
                messageF.setText("");
            }
        }
    };

    private void msgRecieved() {
        Spannable sp = getSmiledText(this,tcps.getMsgQ().poll());
        messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_INCOMING);
        //List<String> asd = new ArrayList<>();
        //List<Integer> asd2 = new ArrayList<>();
        //for (DBMessage msg : result) {
        //    asd.add(msg.getMsg());
        //    asd2.add(msg.getFromid());
        //}
        //Log.d("", asd.toString());
        //for (int i = 0; i < asd.size(); i++) {
        //    String message = asd.get(i);
        //    if (tcps.myId == asd2.get(i)) {
        //        messageAdapter.addMessage(message, MessageAdapter.DIRECTION_OUTGOING);
        //    } else {
        //        messageAdapter.addMessage(message, MessageAdapter.DIRECTION_INCOMING);
        //    }
        //}
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
        Log.d("", result2.toString());
        Log.d("test1", tcps.currentPartner + " " + tcps.myId);
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

        Log.d("", result.toString());
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
            Spannable sp = getSmiledText(this,message);
            if (tcps.myId == asd2.get(i)) {
                messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_OUTGOING);
            } else {
                messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_INCOMING);
            }
        }
    }

    private void onServiceReady() {
        Intent intent;
        mBound = true;
        intent = new Intent(MessagingActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);

        intent = getIntent();
        int id = intent.getIntExtra("id",-1);

        if ( id != -1 ){
            //start conv cucait beállitjuk
            List<FriendListItem> friends = tcps.getFriendList();
            for( FriendListItem f : friends ){
                if ( id == f.getUserid() ){
                    tcps.setCurrentPartner(id);
                    tcps.setCurrentPartnerName(f.getName());
                    tcps.setLastActive(f.getTime());
                }
            }
        }

        TextView tw = (TextView) findViewById(R.id.nameV);
        TextView tw2 = (TextView) findViewById(R.id.lastV);
        tw.setText(tcps.getCurrentPartnerName());
        tw2.setText(tcps.getLastActive());
        populateMessageHistory();
    }

    private final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
    private static final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();

    {
        addPattern(emoticons, ":)", R.drawable.smile_emo);
        addPattern(emoticons, ":D", R.drawable.happy_emo);
        addPattern(emoticons, "<3", R.drawable.heart_emo);
        addPattern(emoticons, ":DD", R.drawable.lol_emo);
        addPattern(emoticons, ":(", R.drawable.sad_emo);
    }

    private void addPattern(Map<Pattern, Integer> map, String smile, int resource) {
        map.put(Pattern.compile(Pattern.quote(smile)), resource);
    }

    public boolean addSmiles(Context context, Spannable spannable) {
        boolean hasChanges = false;
        for (Map.Entry<Pattern, Integer> entry : emoticons.entrySet()) {
            Matcher matcher = entry.getKey().matcher(spannable);
            while (matcher.find()) {
                boolean set = true;
                for (ImageSpan span : spannable.getSpans(matcher.start(),
                        matcher.end(), ImageSpan.class))
                    if (spannable.getSpanStart(span) >= matcher.start()
                            && spannable.getSpanEnd(span) <= matcher.end())
                        spannable.removeSpan(span);
                    else {
                        set = false;
                        break;
                    }
                if (set) {
                    hasChanges = true;
                    spannable.setSpan(new ImageSpan( context , entry.getValue()),
                            matcher.start(), matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return hasChanges;
    }

    public Spannable getSmiledText(Context context, CharSequence text) {
        Spannable spannable = spannableFactory.newSpannable(text);
        addSmiles(context, spannable);
        return spannable;
    }

}
