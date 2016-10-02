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
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Tianchi on 16/10/1.
 * <p>
 * It is a service connected with server.
 * It will guide the switch of activities.
 */

public class GuiderService extends Service {

    private SharedPreferences mSharePreferences;
    private SharedPreferences.Editor mEditor;

    private final static int PORT = 1909;

    private IBinder mBinder = new GuiderBinder();

    private final AsyncTask mTask = new AsyncTask() {
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                ServerSocket server = new ServerSocket(PORT);

                //Get a client
                Socket socket = server.accept();

                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (true) {
                    String guide = input.readLine();
                    if(guide==null){
                        Log.v("Monitor.LOG", "Client closed socket");
                        return null;
                    }
                    if (guide.length() > 0) {
                        String[] items = guide.split("#");
                        if (items.length != 2 && items.length != 1) {
                            Log.v(Monitor.LOG, "error read line " + guide + " " + items.length);
                            continue;
                        }
                        String second = "";
                        if (items.length == 2)
                            second = items[1];

                        Log.v("Monitor.LOG", "read line: " + guide+" "+items[0]);
                        switch (items[0]) {
                            case Monitor.BLOCK_KEY:
                                if (items[1].equals("true"))
                                    mEditor.putBoolean(Monitor.BLOCK_KEY, true);
                                else
                                    mEditor.putBoolean(Monitor.BLOCK_KEY, false);
                                break;
                            case Monitor.INTENT_KEY:
                                mEditor.putString(Monitor.INTENT_KEY, second);
                                break;
                            case Monitor.PACKAGE_NAME_KEY:
                                mEditor.putString(Monitor.PACKAGE_NAME_KEY, second);
                                break;
                            case Monitor.TARGET_KEY:
                                mEditor.putString(Monitor.TARGET_KEY, second);
                                break;
                            default:
                                Log.v(Monitor.LOG, "error guide");
                        }
                        mEditor.commit();
                    }
                }

            } catch (IOException e) {
                Log.v(Monitor.LOG, e.toString());
            }

            return null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSharePreferences = getSharedPreferences(Monitor.SHARE_NAME, Activity.MODE_WORLD_READABLE);
        mEditor = mSharePreferences.edit();

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Guider Service")
                .setContentText("I am running..")
                .build();
        startForeground(1, notification);
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
