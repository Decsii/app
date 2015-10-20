package com.example.balint.szakdolgozat.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.balint.szakdolgozat.javaclasses.FriendListAdapter;
import com.example.balint.szakdolgozat.javaclasses.FriendListItem;
import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.TCPService;

import java.util.List;


public class FriendListActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.example.balint.szakdolgozat.MESSAGE";
    private TCPService tcps;
    private boolean mBound = false;
    private ListView usersListView;
    private ArrayAdapter<String> namesArrayAdapter;
    private List<String> friendList;
    private List<String> sendedList;
    private List<String> requestList;
    private int currentView;
    private Handler messageHandler = new MessageHandler();
    private String[] menuItems;
    private FriendListAdapter fla;

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            Intent intent;
            String uz;
            Toast toast;
            switch (state) {
                case 15:
                    writeFriendList();
                    break;
                case 2:
                    intent = new Intent(FriendListActivity.this, MainActivity.class);
                    uz = "nomsg";
                    intent.putExtra(EXTRA_MESSAGE, uz);
                    startActivity(intent);
                    break;
                case 4:
                    toast = Toast.makeText(FriendListActivity.this, "Felkérés elfogadva", Toast.LENGTH_SHORT);
                    toast.show();
                    writeRequests();
                    break;
                case 5:
                    toast = Toast.makeText(FriendListActivity.this, "Felkérés visszavonva", Toast.LENGTH_SHORT);
                    toast.show();
                    writeSended();
                    break;
                case 6:
                    toast = Toast.makeText(FriendListActivity.this, "Felkérés visszautasitva", Toast.LENGTH_SHORT);
                    toast.show();
                    writeRequests();
                    break;
                case 7:
                    toast = Toast.makeText(FriendListActivity.this, "Barát hozzáadva", Toast.LENGTH_SHORT);
                    toast.show();
                    writeSended();
                    break;
                case 16:
                    writeFriendList();
                    break;
                case 10:
                    writeFriendList();
                    break;
                case 900:
                    intent = new Intent(FriendListActivity.this, MessagingActivity.class);
                    uz = "nomsg";
                    intent.putExtra(EXTRA_MESSAGE, uz);
                    startActivity(intent);
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
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        usersListView = (ListView) findViewById(R.id.usersListView);
        fla = new FriendListAdapter(this);

        Button btn = (Button) findViewById(R.id.frequestB);
        btn.setOnClickListener(requestB);

        Button btn2 = (Button) findViewById(R.id.addFriendB);
        btn2.setOnClickListener(addFriend);

        Button btn4 = (Button) findViewById(R.id.addFriendV);
        btn4.setOnClickListener(showAddFriend);

        Button btn3 = (Button) findViewById(R.id.writeFriendList);
        btn3.setOnClickListener(writeFriendListB);

        //Button btn4 = (Button) findViewById(R.id.writeSended);
        //btn4.setOnClickListener(writeSendedB);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.d("éppen", "most");
        if (v.getId() == R.id.usersListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle("Menu");

            switch (currentView) {
                case 0:
                    menuItems = new String[]{"Törlés"};
                    break;
                case 1:
                    menuItems = new String[]{"Visszautasitás"};
                    break;
                case 2:
                    menuItems = new String[]{""};
                    break;
                default:
                    menuItems = new String[]{""};
                    break;
            }
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int menuItemIndex = item.getItemId();
        String listItemName;
        switch (menuItems[menuItemIndex]) {
            case "Törlés":
                listItemName = friendList.get(info.position);
                tcps.deleteFriend(listItemName);
                break;
            case "Visszautasitás":
                listItemName = requestList.get(info.position);
                tcps.declineRequest(listItemName);
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_friend_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_torles) {
            tcps.removeMyself();
        }

        if (id == R.id.action_kijelentkezes) {
            tcps.logOut();
            Intent intent = new Intent(FriendListActivity.this, MainActivity.class);
            intent.putExtra(EXTRA_MESSAGE, "nomsg");
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener requestB = new View.OnClickListener() {
        public void onClick(View v) {
            writeRequests();
        }
    };

    private View.OnClickListener showAddFriend = new View.OnClickListener() {
        public void onClick(View v) {
            LinearLayout laout = (LinearLayout) findViewById(R.id.addFriendField);
            if (laout.getVisibility() != View.VISIBLE) {
                laout.setVisibility(View.VISIBLE);
            } else {
                laout.setVisibility(View.GONE);
            }
        }
    };

    private View.OnClickListener writeFriendListB = new View.OnClickListener() {
        public void onClick(View v) {
            writeFriendList();
        }
    };

    private View.OnClickListener addFriend = new View.OnClickListener() {
        public void onClick(View v) {
            EditText et = (EditText) findViewById(R.id.addFriendT);
            tcps.addFriend(et.getText().toString());
        }
    };

    private View.OnClickListener writeSendedB = new View.OnClickListener() {
        public void onClick(View v) {
            writeSended();
        }
    };

    private void writeFriendList() {
        currentView = 0;
        friendList = tcps.getFriendsString();
        usersListView.setAdapter(fla);
        //namesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.user_list_item, friendList);
        //usersListView.setAdapter(namesArrayAdapter);
        fla.clearList();
        for (FriendListItem s : tcps.getFriendList()) {
            fla.addFriend(s);
        }

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                Log.d(friendList.get(i), "" + i);
                tcps.startConv(friendList.get(i),fla.getLastActive(i), tcps.getFriendList().get(i).isLoggedin());
            }
        });

        registerForContextMenu(usersListView);

    }

    private void writeRequests() {
        currentView = 1;
        requestList = tcps.getRequestsString();
        namesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.user_list_item, requestList);
        usersListView.setAdapter(namesArrayAdapter);

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                tcps.acceptRequest(requestList.get(i));
                Log.d(requestList.get(i), "" + i);
            }
        });

        registerForContextMenu(usersListView);

    }

    private void writeSended() {
        currentView = 2;
        sendedList = tcps.getSendedString();
        namesArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.user_list_item, sendedList);
        usersListView.setAdapter(namesArrayAdapter);

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int i, long l) {
                tcps.undoRequest(sendedList.get(i));
                Log.d(sendedList.get(i), "" + i);
            }
        });

        registerForContextMenu(usersListView);
    }

    private void onServiceReady() {
        mBound = true;
        Intent intent = new Intent(FriendListActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);
        tcps.setCurrentPartner(-1);
        Log.d("", "FRIEND REQUEST");
        if (!tcps.requestedyet) tcps.requestFriendList();
    }
}