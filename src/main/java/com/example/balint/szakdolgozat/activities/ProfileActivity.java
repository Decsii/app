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
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.balint.szakdolgozat.R;
import com.example.balint.szakdolgozat.javaclasses.Options;

import io.realm.Realm;
import io.realm.RealmResults;

public class ProfileActivity extends ActionBarActivity {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

    }
    /**
     * A service és az activity közötti kommunikációért felelős osztály.
     */
    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message message) {
            int state = message.arg1;
            Intent intent;
            Toast toast;
            String uz;
            switch (state) {
                case 2:
                    intent = new Intent(ProfileActivity.this, MainActivity.class);
                    startActivity(intent);
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

        TextView b1 = (TextView) findViewById(R.id.Logout);
        b1.setOnClickListener(logoutAction);

        TextView b2 = (TextView) findViewById(R.id.removeMyself);
        b2.setOnClickListener(removeMyselfAct);

    }

    /**
     * Kijelentkezés listener.
     */
    private View.OnClickListener logoutAction = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("ASD","ADS");
            tcps.logOut();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        }
    };

    /**
     * Regisztráció törlése listener.
     */
    private View.OnClickListener removeMyselfAct = new View.OnClickListener() {
        public void onClick(View v) {
            tcps.removeMyself();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        }
    };

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
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
     * Ha csatlakoztunk a servicehez, akkor fut le.
     */
    private void onServiceReady(){
        Intent intent = new Intent(ProfileActivity.this, TCPService.class);
        intent.putExtra("MESSENGER", new Messenger(messageHandler));
        tcps.onBind(intent);

        if(!tcps.isLogedIn()){
            intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);
            return;
        }

        setSwitches();
        swtichInit();

        TextView name = (TextView) findViewById(R.id.profileName);
        name.setText(tcps.getMyName());
    }

    /**
     * Kapcsolók beállitása.
     */
    private void setSwitches(){
        Switch s1 = (Switch) findViewById(R.id.notifS);
        Switch s2 = (Switch) findViewById(R.id.soundS);
        Switch s3 = (Switch) findViewById(R.id.vibrateS);
        Switch s4 = (Switch) findViewById(R.id.sendS);

        Realm realm = Realm.getInstance(this);
        RealmResults<Options> result = realm.where(Options.class)
                .equalTo("key", "notification")
                .findAll();

        if( result.size() != 0 ) {
            if(  result.get(0).getValue().toString().equals("0")) {
                s1.setChecked(false);
            }else{
                s1.setChecked(true);
            }
        }else{
            s1.setChecked(true);
            realm.beginTransaction();
            Options option = realm.createObject(Options.class);
            option.setKey("notification");
            option.setValue("1");
            realm.commitTransaction();
        }

        result = realm.where(Options.class)
                .equalTo("key", "notifsound")
                .findAll();
        Log.d("RESULT", result.size() + result.toString());
        if( result.size() != 0 ) {
            if(result.get(0).getValue().equals("0")) {
                Log.d("RESULT2","FÓÓLSZ");
                s2.setChecked(false);
            }else{
                s2.setChecked(true);
            }
        }else{
            s2.setChecked(true);
            realm.beginTransaction();
            Options option = realm.createObject(Options.class);
            option.setKey("notifsound");
            option.setValue("1");
            realm.commitTransaction();
        }

        result = realm.where(Options.class)
                .equalTo("key", "notifvibrate")
                .findAll();

        if( result.size() != 0 ) {
            if( result.get(0).getValue().toString().equals("0")) {
                s3.setChecked(false);
            }else{
                s3.setChecked(true);
            }
        }else{
            s3.setChecked(true);
            realm.beginTransaction();
            Options option = realm.createObject(Options.class);
            option.setKey("notifvibrate");
            option.setValue("1");
            realm.commitTransaction();
        }

        result = realm.where(Options.class)
                .equalTo("key", "sendwithenter")
                .findAll();

        if( result.size() != 0 ) {
            if(  result.get(0).getValue().toString().equals("0") ) {
                s4.setChecked(false);
            }else{
                s4.setChecked(true);
            }
        }else{
            s4.setChecked(true);
            realm.beginTransaction();
            Options option = realm.createObject(Options.class);
            option.setKey("sendwithenter");
            option.setValue("1");
            realm.commitTransaction();
        }
    }

    /**
     * Listenerek hozzáadása a kapcsolókhoz.
     */
    private void swtichInit(){
        Switch s1 = (Switch) findViewById(R.id.notifS);
        s1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Realm realm = Realm.getInstance(ProfileActivity.this);
                RealmResults<Options> result = realm.where(Options.class)
                        .equalTo("key", "notification")
                        .findAll();
                if (isChecked) {
                    realm.beginTransaction();
                    result.get(0).setValue("1");
                    realm.commitTransaction();
                } else {
                    realm.beginTransaction();
                    result.get(0).setValue("0");
                    realm.commitTransaction();
                }
            }
        });

        Switch s2 = (Switch) findViewById(R.id.soundS);
        s2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Realm realm = Realm.getInstance(ProfileActivity.this);
                RealmResults<Options> result = realm.where(Options.class)
                        .equalTo("key", "notifsound")
                        .findAll();
                if(isChecked){
                    realm.beginTransaction();
                    result.get(0).setValue("1");
                    realm.commitTransaction();
                }else{
                    realm.beginTransaction();
                    result.get(0).setValue("0");
                    realm.commitTransaction();
                }
            }
        });

        Switch s3 = (Switch) findViewById(R.id.vibrateS);
        s3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Realm realm = Realm.getInstance(ProfileActivity.this);
                RealmResults<Options> result = realm.where(Options.class)
                        .equalTo("key", "notifvibrate")
                        .findAll();
                if(isChecked){
                    realm.beginTransaction();
                    result.get(0).setValue("1");
                    realm.commitTransaction();
                }else{
                    realm.beginTransaction();
                    result.get(0).setValue("0");
                    realm.commitTransaction();
                }
            }
        });

        Switch s4 = (Switch) findViewById(R.id.sendS);
        s4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Realm realm = Realm.getInstance(ProfileActivity.this);
                RealmResults<Options> result = realm.where(Options.class)
                        .equalTo("key", "sendwithenter")
                        .findAll();
                if (isChecked) {
                    realm.beginTransaction();
                    result.get(0).setValue("1");
                    realm.commitTransaction();
                } else {
                    realm.beginTransaction();
                    result.get(0).setValue("0");
                    realm.commitTransaction();
                }
            }
        });

    }

}
