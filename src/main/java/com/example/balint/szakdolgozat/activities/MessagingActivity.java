package com.example.balint.szakdolgozat.activities;

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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.balint.szakdolgozat.javaclasses.DBMessage;
import com.example.balint.szakdolgozat.javaclasses.FriendListItem;
import com.example.balint.szakdolgozat.javaclasses.MessageAdapter;
import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.Options;
import com.example.balint.szakdolgozat.javaclasses.TCPService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmResults;


public class MessagingActivity extends ActionBarActivity{
    /**
     * Service
     */
    private TCPService tcps;
    /**
     * Az activity hozzá van-e kötve a servicehez.
     */
    private boolean mBound = false;
    /**
     * Adapter üzenetekhez.
     */
    private MessageAdapter messageAdapter;
    /**
     * Listview üzenetek megjelenitésére.
     */
    private ListView messagesList;
    /**
     * Üzeneteket tároló lista.
     */
    private List<String> messageList = new ArrayList<>();
    /**
     * Üzenet küldés input mező.
     */
    private EditText messageF;
    /**
     * Görgetés kikapcsolása a listviewn.
     */
    private boolean scrollD = false;
    /**
     * A service és az activity közötti kommunikáció.
     */
    private Handler messageHandler = new MessageHandler();

    /**
     * A service és az activity közötti kommunikációért felelős osztály.
     */
    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message message) {
            int state = message.arg1;
            Intent intent;
            String uz;
            switch (state) {
                case 2:
                    intent = new Intent(MessagingActivity.this, LoginActivity.class);
                    startActivity(intent);
                    break;
                case 11:
                    loadMoreMessages();
                    break;
                case 16:
                    msgRecieved();
                    break;
                case 30:
                    Log.d("vissze","alitottam");
                    scrollD = false;
                    break;
            }
        }
    }

    /**
     * Az activity csatlakozása a servicehez.
     */
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
            tcps.setCurrentPartner(-1);
            unbindService(mConnection);
            mBound = false;
        }
    }

    private boolean swe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        Realm realm = Realm.getInstance(this);
        RealmResults<Options> result = realm.where(Options.class)
                .equalTo("key", "sendwithenter")
                .findAll();
        if( result.get(0).getValue().toString().equals("0")  ){
            swe = false;
        }else{
            swe = true;
        }

        messageF = (EditText) findViewById(R.id.messageF);

        Button sendB = (Button) findViewById(R.id.sendB);
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

    /**
     * Üzenet elküldése
     */
    private View.OnClickListener sendBclick = new View.OnClickListener() {
        public void onClick(View v) {
            tcps.send(messageF.getText().toString());
            messageList.add(messageF.getText().toString());
            Spannable sp = getSmiledText(MessagingActivity.this, messageF.getText());
            messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_OUTGOING);
            messageF.setText("");
        }
    };

    /**
     * Listview görgetés listener.
     */
    AbsListView.OnScrollListener lwScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (listIsAtTop()){
                if (scrollD){
                    Log.d("ititit","ititi");
                    return;
                }
                scrollD = true;
                tcps.getMoreMsg(tcps.getCurrentPartner(),tcps.getCurrentFirstMsgid(),10);
            }
        }
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    };

    /**
     * Üzenetet kaptunk. Jelenitsük meg.
     */
    private void msgRecieved() {
        Spannable sp = getSmiledText(this, tcps.getMsgQ().poll());
        messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_INCOMING);
    }

    /**
     * Üzenetek megjelenitése.
     */
    private void writeMessageHistory() {
        Realm realm = Realm.getInstance(this);

        RealmResults<DBMessage> result2 = realm.where(DBMessage.class).findAll();
        //Log.d("", result2.toString());
        //Log.d("test1", tcps.currentPartner + " " + tcps.getMyId());
        RealmResults<DBMessage> result = realm.where(DBMessage.class)
                .beginGroup()
                .equalTo("fromid", tcps.getCurrentPartner())
                .equalTo("toid", tcps.getMyId())
                .endGroup()
                .or()
                .beginGroup()
                .equalTo("fromid", tcps.getMyId())
                .equalTo("toid", tcps.getCurrentPartner())
                .endGroup()
                .findAll();

        //Log.d("", result.toString());
        result.sort("msgid");
        List<String> asd = new ArrayList<>();
        List<Integer> asd2 = new ArrayList<>();
        if (result.size() != 0)tcps.setCurrentFirstMsgid(result.get(0).getMsgid());
        for (DBMessage msg : result) {
            asd.add(msg.getMsg());
            asd2.add(msg.getFromid());
        }

        //Log.d("", asd.toString());
        for (int i = 0; i < asd.size(); i++) {
            String message = asd.get(i);
            Spannable sp = getSmiledText(this, message);
            if (tcps.getMyId() == asd2.get(i)) {
                messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_OUTGOING);
            } else {
                messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_INCOMING);
            }
        }
        messagesList.setOnScrollListener(lwScrollListener);
    }

    /**
     * Listviewt teljesen felgörgettük-e.
     */
    private boolean listIsAtTop() {
        if (messagesList.getChildCount() == 0) return true;
        return messagesList.getChildAt(0).getTop() == 0;
    }

    /**
     * Ha csatlakoztunk a servicehez, akkor fut le.
     */
    private void onServiceReady() {
        Intent intent;
        mBound = true;
        intent = new Intent(MessagingActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);

        if(!tcps.isLogedIn()){
            intent = new Intent(MessagingActivity.this, LoginActivity.class);
            startActivity(intent);
            return;
        }

        intent = getIntent();
        int id = intent.getIntExtra("id", -1);

        if (id != -1) {
            //start conv cucait beállitjuk
            List<FriendListItem> friends = tcps.getFriendList();
            for (FriendListItem f : friends) {
                if (id == f.getUserid()) {
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
        writeMessageHistory();
    }

    /**
     * Több üzenet betöltése, ha elértük a listview tetejét..
     */
    private void loadMoreMessages() {
        List<Pair<Integer, String>> list = tcps.getMoreMessages();
        final int size = list.size();
        for (int i = list.size() - 1; i >= 0; i--) {
            String message = list.get(i).second;
            Spannable sp = getSmiledText(this, message);
            if (tcps.getMyId() == list.get(i).first) {
                messageAdapter.addMessageFirst(sp, MessageAdapter.DIRECTION_OUTGOING);
            } else {
                messageAdapter.addMessageFirst(sp, MessageAdapter.DIRECTION_INCOMING);
            }
        }
        messagesList.post(new Runnable() {
            @Override
            public void run() {
                scrollD = true;
                messagesList.setSelection(size);
                Thread t2 = new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        scrollD = false;
                    }
                };
                t2.start();
            }
        });
    }

    private final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
    /**
     * Smiley tároló map.
     */
    private final Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();
    {
        addPattern(emoticons, ":)", R.drawable.smile_emo);
        addPattern(emoticons, ":D", R.drawable.happy_emo);
        addPattern(emoticons, "<3", R.drawable.heart_emo);
        addPattern(emoticons, ":DD", R.drawable.lol_emo);
        addPattern(emoticons, ":(", R.drawable.sad_emo);
    }

    /**
     * Smiley hozzáadása.
     */
    private void addPattern(Map<Pattern, Integer> map, String smile, int resource) {
        map.put(Pattern.compile(Pattern.quote(smile)), resource);
    }

    /**
     * Üzenet tartalmaz-e smileyt.
     */
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
                    spannable.setSpan(new ImageSpan(context, entry.getValue()),
                            matcher.start(), matcher.end(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return hasChanges;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_UP) {
            if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER && swe) {
                tcps.send(messageF.getText().toString());
                messageList.add(messageF.getText().toString());
                Spannable sp = getSmiledText(MessagingActivity.this, messageF.getText());
                messageAdapter.addMessage(sp, MessageAdapter.DIRECTION_OUTGOING);
                messageF.setText("");
                return false;
            }
        }
        return super.dispatchKeyEvent(e);
    };

    /**
     * @return Üzenet a smileyval.
     */
    public Spannable getSmiledText(Context context, CharSequence text) {
        Spannable spannable = spannableFactory.newSpannable(text);
        addSmiles(context, spannable);
        return spannable;
    }

}
