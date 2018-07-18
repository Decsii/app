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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.balint.szakdolgozat.javaclasses.DBMessage;
import com.example.balint.szakdolgozat.javaclasses.FriendListAdapter;
import com.example.balint.szakdolgozat.javaclasses.FriendListItem;
import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.RequestListAdapter;
import com.example.balint.szakdolgozat.javaclasses.TCPService;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * @author      Decsi Bálint
 */

public class FriendListActivity extends ActionBarActivity {
    /**
     * Serviceeeeeeee
	 nemtom
>>>>>>> test_2
     */
    private TCPService tcps;
    /**
     * Az activity hozzá van-e kötve a servicehez.
     */
    private boolean mBound = false;
    /**
     * Listview a barátok és felkérések megjelenitéséhez.
     */
    private ListView usersListView;
    /**
     * Barátok nevének tárolása.
     */
    private List<String> friendList;
    /**
     * Jelenleg mit jelenitünk meg. (barátlista, felkérés lista)
     */
    private int currentView;
    /**
     * A service és az activity közötti kommunikáció.
     */
    private Handler messageHandler = new MessageHandler();
    /**
     * Hosszú kattintásnál felugró menü opciói.
     */
    private String[] menuItems;
    /**
     * Adatpter barátlistához.
     */
    private FriendListAdapter fla;
    /**
     * Adapted request listához.
     */
    private RequestListAdapter rla;

    /**
     * A service és az activity közötti kommunikációért felelős osztály.
     */
    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            Intent intent;
            Toast toast;
            switch (state) {
                case 15:
                    writeFriendList();
                    break;
                case 2:
                    intent = new Intent(FriendListActivity.this, LoginActivity.class);
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
                    writeRequests();
                    break;
                case 6:
                    toast = Toast.makeText(FriendListActivity.this, "Felkérés visszautasitva", Toast.LENGTH_SHORT);
                    toast.show();
                    writeRequests();
                    break;
                case 7:
                    toast = Toast.makeText(FriendListActivity.this, "Barát felkérés elküldve", Toast.LENGTH_SHORT);
                    toast.show();
                    writeRequests();
                    break;
                case 12:
                    toast = Toast.makeText(FriendListActivity.this, "Üzenetek törölve", Toast.LENGTH_SHORT);
                    toast.show();
                    refreshFriendItem();
                    break;
                case 16:
                    writeFriendList();
                    break;
                case 10:
                    writeFriendList();
                    break;
                case 200:
                    refreshFriendItem();
                    break;
                case 300:
                    toast = Toast.makeText(FriendListActivity.this, "Új barát felkrés érkezett", Toast.LENGTH_SHORT);
                    toast.show();
                    if (currentView == 1) {
                        writeRequests();
                    }
                    break;
                case 301:
                    toast = Toast.makeText(FriendListActivity.this, "Barát felkérésedet elfogadták", Toast.LENGTH_SHORT);
                    toast.show();
                    if (currentView == 1) {
                        writeRequests();
                    } else if (currentView == 0) {
                        writeFriendList();
                    }
                    break;
                case 900:
                    intent = new Intent(FriendListActivity.this, MessagingActivity.class);
                    startActivity(intent);
                    break;
                case 9111:
                    toast = Toast.makeText(FriendListActivity.this, "Nincs ilyen felhasználó", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case 9116:
                    toast = Toast.makeText(FriendListActivity.this, "A felhasználó már szerepel a barátlistán", Toast.LENGTH_SHORT);
                    toast.show();
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
        //Csatlakozunk a servicehez.
        Intent intent = new Intent(this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        startService(intent);
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Lecsatlakozunk a serviceről.
        if (mBound) {
            tcps.setFriendListActive(false);
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
        btn.setOnClickListener(requestA);

        Button btn2 = (Button) findViewById(R.id.addFriendB);
        btn2.setOnClickListener(addFriend);

        Button btn4 = (Button) findViewById(R.id.addFriendV);
        btn4.setOnClickListener(showAddFriend);

        Button btn3 = (Button) findViewById(R.id.writeFriendList);
        btn3.setOnClickListener(writeFriendListB);

        Button btn5 = (Button) findViewById(R.id.profileB);
        btn5.setOnClickListener(profileA);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.usersListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle("Menü");
            switch (currentView) {
                case 0:
                    menuItems = new String[]{"Barát törlése","Üzenetek törlése"};
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
            case "Barát törlése":
                listItemName = friendList.get(info.position);
                tcps.deleteFriend(listItemName);
                break;
            case "Üzenetek törlése":
                Realm realm = Realm.getInstance(this);
                RealmResults<DBMessage> result = realm.where(DBMessage.class)
                        .beginGroup()
                        .equalTo("fromid", tcps.getMyId())
                        .equalTo("toid", tcps.getFriendList().get(info.position).getUserid())
                        .endGroup()
                        .or()
                        .beginGroup()
                        .equalTo("fromid", tcps.getFriendList().get(info.position).getUserid())
                        .equalTo("toid", tcps.getMyId())
                        .endGroup()
                        .findAll();
                int maxmsgid = result.max("msgid").intValue();
                tcps.deleteMessages(tcps.getFriendList().get(info.position).getUserid(),maxmsgid, info.position);
                realm.beginTransaction();
                result.clear();
                realm.commitTransaction();
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
        return super.onOptionsItemSelected(item);
    }

    /**
     * Felkérés lista gomb listener.
     */
    private View.OnClickListener requestA = new View.OnClickListener() {
        public void onClick(View v) {
            writeRequests();
        }
    };

    /**
     * Profile gomb listener.
     */
    private View.OnClickListener profileA = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(FriendListActivity.this, ProfileActivity.class);
            startActivity(intent);
        }
    };

    /**
     * Add friend gomb listener.( Az input mező megjelenitése. )
     */
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

    /**
     * Friendlist gomb listener.
     */
    private View.OnClickListener writeFriendListB = new View.OnClickListener() {
        public void onClick(View v) {
            writeFriendList();
        }
    };

    /**
     * Add friend gomb listener.
     */
    private View.OnClickListener addFriend = new View.OnClickListener() {
        public void onClick(View v) {
            EditText et = (EditText) findViewById(R.id.addFriendT);
            tcps.addFriend(et.getText().toString());
        }
    };

    /**
     * Barátok listázása a listviewba.
     */
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
                //Log.d(friendList.get(i), "" + i);
                tcps.startConv(friendList.get(i), fla.getLastActive(i), tcps.getFriendList().get(i).isLoggedin());
            }
        });

        registerForContextMenu(usersListView);

    }

    /**
     * Felkérések listázása a listviewba.
     */
    private void writeRequests() {
        currentView = 1;

        usersListView.setAdapter(rla);
        rla.clearList();

        if (tcps.getFriendRequests().size() != 0) {
            rla.addFriend(new FriendListItem(-1, "", "", "", false, 0, 3));
        }
        for (FriendListItem s : tcps.getFriendRequests()) {
            rla.addFriend(s);
        }
        if (tcps.getSendedRequests().size() != 0) {
            rla.addFriend(new FriendListItem(-1, "", "", "", false, 0, 4));
        }
        for (FriendListItem s : tcps.getSendedRequests()) {
            rla.addFriend(s);
        }

        registerForContextMenu(usersListView);

    }
    /**
     * Egy adapter elem frissitése.
     */
    private void refreshFriendItem() {
        fla.refreshFriend(tcps.getRefreshQ().poll());
    }

    /**
     * Ha csatlakoztunk a servicehez, akkor fut le.
     */
    private void onServiceReady() {
        mBound = true;
        Intent intent = new Intent(FriendListActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);
        if(!tcps.isLogedIn()){
            intent = new Intent(FriendListActivity.this, LoginActivity.class);
            startActivity(intent);
            return;
        }
        rla = new RequestListAdapter(this, tcps);
        tcps.setCurrentPartner(-1);
        tcps.setFriendListActive(true);
        //Log.d("", "FRIEND REQUEST");
		//semmi
        if (!tcps.isRequestedyet()) {
            tcps.requestFriendList();
        } else {
            writeFriendList();
        }
    }
}
//asd-1-1
//asd-1-2
//asd-2-1
//asd-2-2
//asd 3-3