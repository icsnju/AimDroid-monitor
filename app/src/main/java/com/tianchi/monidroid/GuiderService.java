package com.tianchi.monidroid;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by Tianchi on 16/10/1.
 * <p>
 * It is a service connected with server.
 * It will guide the switch of activities.
 */

public class GuiderService extends Service {

    private SharedPreferences mSharePreferences;
    private SharedPreferences.Editor mEditor;

    private IBinder mBinder = new GuiderBinder();

    private final AsyncTask mTask = new AsyncTask() {
        @Override
        protected Object doInBackground(Object[] params) {
            int count=0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    mEditor.putString("count", ""+count++).commit();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSharePreferences = getSharedPreferences("moni", Activity.MODE_WORLD_READABLE);
        mEditor = mSharePreferences.edit();

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Guider Service")
                .setContentText("I am running..")
                .build();
        startForeground(1,notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    class GuiderBinder extends Binder {
        public void startTask() {
            mTask.execute();
        }
    }
}
