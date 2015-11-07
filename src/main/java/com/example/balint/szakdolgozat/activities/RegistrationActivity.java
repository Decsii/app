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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.example.balint.szakdolgozat.R;


public class RegistrationActivity extends ActionBarActivity {

    public final static String EXTRA_MESSAGE = "com.example.balint.szakdolgozat.MESSAGE";
    private TCPService tcps;
    private boolean mBound = false;

    public class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            Intent intent;
            String uz;
            switch (state) {
                case 0:
                    intent = new Intent(RegistrationActivity.this, FriendListActivity.class);
                    uz = "nomsg";
                    intent.putExtra(EXTRA_MESSAGE, uz);
                    startActivity(intent);
                    break;
            }
        }
    }

    public Handler messageHandler = new MessageHandler();

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TCPService.LocalBinder binder = (TCPService.LocalBinder) service;
            tcps = binder.getService();
            mBound = true;
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
        setContentView(R.layout.activity_registration);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_registration, menu);
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

    public void registration(View v) {
        EditText userName = (EditText) findViewById(R.id.ruserText);
        EditText pass = (EditText) findViewById(R.id.rpasswordText);
        if (userName.getText().toString() != "" && pass.getText().toString() != "") {
            tcps.registration(userName.getText().toString(), pass.getText().toString());
        }
    }

}
