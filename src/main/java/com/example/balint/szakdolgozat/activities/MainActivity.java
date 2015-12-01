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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.Options;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * @author      Decsi Bálint
 * @version     1.0
 */
public class MainActivity extends ActionBarActivity {
    /**
     * Service
     */
    private TCPService tcps;
    /**
     * Az activity hozzá van-e kötve a servicehez.
     */
    private boolean mBound = false;
    /**
     * A service és az activity közötti kommunikáció.
     */
    private Handler messageHandler = new MessageHandler();

    /**
     * Az activity csatlakozása a servicehez.
     */
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

    /**
     * A service és az activity közötti kommunikációért felelős osztály.
     */
    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message message) {
            int state = message.arg1;
            Intent intent;
            Toast toast;
            switch (state) {
                case 0:
                    intent = new Intent(MainActivity.this, FriendListActivity.class);
                    startActivity(intent);
                    break;
                case 1:
                    intent = new Intent(MainActivity.this, FriendListActivity.class);
                    startActivity(intent);
                    break;
                case 1121:
                    toast = Toast.makeText(MainActivity.this, "Hibás felhasználónév vagy jelszó", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
                case 112:
                    toast = Toast.makeText(MainActivity.this, "Már be vagy jelentkezve", Toast.LENGTH_SHORT);
                    toast.show();
                    break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Csazlakozunk a servicehez.
        Intent intent = new Intent(this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));

        startService(intent);
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Lecsatlakozunk a serviceről.
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

    /**
     * Bejelentkezés gomb listener.
     */
    private View.OnClickListener signInA = new View.OnClickListener() {
        public void onClick(View v) {
            logIn();
        }
    };

    /**
     * Regisztrálás gomb listener.
     */
    private View.OnClickListener regA = new View.OnClickListener() {
        public void onClick(View v) {
            toRegistration();
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Bejelentkezés
     */
    private void logIn() {
        EditText userName = (EditText) findViewById(R.id.usernameText);
        EditText pass = (EditText) findViewById(R.id.passwordText);
        if (userName.getText().toString() != "" && pass.getText().toString() != "") {
            tcps.logIn(userName.getText().toString(), pass.getText().toString());
        }
    }

    /**
     * Regisztráció
     */
    private void toRegistration() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
    }

    /**
     * Ha csatlakoztunk a servicehez, akkor fut le.
     */
    private void onServiceReady() {
        Intent intent = new Intent(MainActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);

        if ( tcps.isLogedIn() ){
            intent = new Intent(MainActivity.this, FriendListActivity.class);
            startActivity(intent);
        }else {
            Realm realm = Realm.getInstance(this);
            RealmResults<Options> result = realm.where(Options.class)
                    .equalTo("key", "sid")
                    .findAll();
            if( result.size() == 1 ){
                tcps.sendSID(result.get(0).getValue());
            }
        }
    }

}
