package com.tianchi.monidroid;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    String LOG = "Monitor_Log";
    GuiderService.GuiderBinder yourBinder=null;

    //Create a ServiceConnection for starting a thread
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            yourBinder=(GuiderService.GuiderBinder)service;
            yourBinder.startTask();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start the guider service
        Intent bindIntent = new Intent(this, GuiderService.class);
        bindService(bindIntent,serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy(){
        Log.v(Monitor.LOG, "MainActivity is Distory!");
        super.onDestroy();
        if(yourBinder!=null){
            yourBinder.stopTask();
        }
        unbindService(serviceConnection);
    }
}
