package com.tianchi.monidroid;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Attr;

import java.util.Iterator;
import java.util.Set;

/**
 * Created by Tianchi on 16/9/11.
 */
public class IntentJsonConverter {

    private static final String ATTR_ACTION = "action";
    private static final String TAG_CATEGORIES = "categories";
    private static final String ATTR_CATEGORY = "category";
    private static final String TAG_EXTRA = "extra";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_FLAGS = "flags";


    public static String intentToJson(Intent intent) {
        if (intent == null) {
            return null;
        }
        JSONObject json = new JSONObject();
        try {
            if (intent.getAction() != null) {
                json.put(ATTR_ACTION, intent.getAction());
            }

            if (intent.getData() != null) {
                json.put(ATTR_DATA, intent.getData().toString());
            }

            if (intent.getType() != null) {
                json.put(ATTR_TYPE, intent.getType());
            }

            if (intent.getComponent() != null) {
                json.put(ATTR_COMPONENT, intent.getComponent().flattenToShortString());
            }

            json.put(ATTR_FLAGS, Integer.toHexString(intent.getFlags()));

            if (intent.getCategories() != null) {
                JSONArray categories=new JSONArray();
                Set<String> set=intent.getCategories();
                Iterator<String> it=set.iterator();
                while(it.hasNext()){
                    categories.put(it.next());
                }
                json.put(TAG_CATEGORIES, categories);
            }
        } catch (JSONException e) {
            Log.v(Monitor.LOG, "JSON err:\n" + e.toString());
            return null;
        }

        return json.toString();
    }

    public static Intent jsonToIntent(String content){
        if(content==null)
            return null;
        try {
            JSONObject json=new JSONObject(content);
            Intent in=new Intent();
            if(json.has(ATTR_ACTION)){
                in.setAction(json.getString(ATTR_ACTION));
            }

            if(json.has(ATTR_DATA)){
                in.setData(Uri.parse(json.getString(ATTR_DATA)));
            }

            if(json.has(ATTR_TYPE)){
                in.setType(json.getString(ATTR_TYPE));
            }

            if(json.has(ATTR_COMPONENT)){
                in.setComponent(ComponentName.unflattenFromString(json.getString(ATTR_COMPONENT)));
            }

            in.setFlags(Integer.valueOf(json.getString(ATTR_FLAGS),16));

            if(json.has(TAG_CATEGORIES)){
                JSONArray array=json.getJSONArray(TAG_CATEGORIES);
                for(int i=0;i<array.length();i++){
                    in.addCategory(array.getString(i));
                }
            }
            return in;
        } catch (JSONException e) {
            Log.v(Monitor.LOG, "JSON err:\n" + e.toString());
        }
        return null;
    }
}
