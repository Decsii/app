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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.balint.szakdolgozat.R;


public class RegistrationActivity extends ActionBarActivity {

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

        Button b1 = (Button) findViewById(R.id.registB);
        b1.setOnClickListener(registrationA);

        Button b2 = (Button) findViewById(R.id.rbackB);
        b2.setOnClickListener(backA);

    }

    private View.OnClickListener registrationA = new View.OnClickListener() {
        public void onClick(View v) {
            registration();
        }
    };

    private View.OnClickListener backA = new View.OnClickListener() {
        public void onClick(View v) {
            startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
        }
    };

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

    private void registration() {
        EditText userName = (EditText) findViewById(R.id.ruserText);
        EditText pass = (EditText) findViewById(R.id.rpasswordText);
        EditText pass2 = (EditText) findViewById(R.id.rrpasswordText);
        if( userName.getText().toString().equals("") ){
            Toast.makeText(RegistrationActivity.this, "Töltse ki a felhasználónév mezőt", Toast.LENGTH_SHORT).show();
        }else if( pass.getText().toString().equals("")  || pass2.getText().toString().equals("") ){
            Toast.makeText(RegistrationActivity.this, "Töltse ki a jelszómezőt", Toast.LENGTH_SHORT).show();
        }
        if( pass.getText().toString().equals(pass2.getText().toString()) ){
            tcps.registration(userName.getText().toString(), pass.getText().toString());
        }else{
            Toast.makeText(RegistrationActivity.this, "Két jelsző nem egyezik", Toast.LENGTH_SHORT).show();
        }
    }

}
