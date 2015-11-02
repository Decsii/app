package com.example.balint.szakdolgozat.activities;

import android.app.Notification;
import android.app.NotificationManager;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.balint.szakdolgozat.javaclasses.DBMessage;
import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.TCPService;

import io.realm.Realm;
import io.realm.RealmResults;


public class MainActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.example.balint.szakdolgozat.MESSAGE";
    private TCPService tcps;
    private boolean mBound = false;
    private Handler messageHandler = new MessageHandler();


    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message message) {
            int state = message.arg1;
            Intent intent;
            Toast toast;
            String uz;
            switch (state) {
                case 0:
                    intent = new Intent(MainActivity.this, FriendListActivity.class);
                    uz = "nomsg";
                    intent.putExtra(EXTRA_MESSAGE, uz);
                    startActivity(intent);
                    break;
                case 1:
                    intent = new Intent(MainActivity.this, FriendListActivity.class);
                    uz = "nomsg";
                    intent.putExtra(EXTRA_MESSAGE, uz);
                    startActivity(intent);
                    break;
                case 1121:
                    toast = Toast.makeText(MainActivity.this, "Hibás felhasználónév/jelszó", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case 112:
                    toast = Toast.makeText(MainActivity.this, "Már be vagy jelentkezve", Toast.LENGTH_SHORT);
                    toast.show();
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
            mBound = true;
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

        Realm realm = Realm.getInstance(this);
        RealmResults<DBMessage> result = realm.where(DBMessage.class)
                .findAll();
        //RealmResults<DBMessage> result = realm.where(DBMessage.class)
        //        .beginGroup()
        //        .equalTo("fromid", 26)
        //        .equalTo("toid", 28)
        //        .endGroup()
        //        .or()
        //        .beginGroup()
        //        .equalTo("fromid", 28)
        //        .equalTo("toid", 26)
        //        .endGroup()
        //        .findAll();
        //Log.d("result", result.toString());
        //realm.beginTransaction();
        //result.clear();
        //realm.commitTransaction();
        //RealmResults<Messages> result2 = realm.where(Messages.class)
        //        .findAll();
        //int asdf = result2.max("msgid").intValue();
        //Log.d("resultMAX","" + asdf );
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
        setContentView(R.layout.activity_main);

        Button btn = (Button) findViewById(R.id.signInB);
        btn.setOnClickListener(signInA);

        Button btn2 = (Button) findViewById(R.id.regB);
        btn2.setOnClickListener(regA);
    }

    private View.OnClickListener signInA = new View.OnClickListener() {
        public void onClick(View v) {
            logIn();
        }
    };

    private View.OnClickListener regA = new View.OnClickListener() {
        public void onClick(View v) {
            toRegistration();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void logIn() {
        EditText userName = (EditText) findViewById(R.id.usernameText);
        EditText pass = (EditText) findViewById(R.id.passwordText);
        if (userName.getText().toString() != "" && pass.getText().toString() != "") {
            tcps.logIn(userName.getText().toString(), pass.getText().toString());
        }
    }

    public void toRegistration() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        String message = "nomsg";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private void onServiceReady() {
        Intent intent = new Intent(MainActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);
        EditText userName = (EditText) findViewById(R.id.usernameText);
        userName.setText(tcps.asd);
        proba();
    }

    public void proba() {

    }

}
