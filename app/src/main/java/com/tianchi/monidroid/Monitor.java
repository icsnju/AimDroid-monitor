package com.tianchi.monidroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Tianchi on 16/8/1.
 */
public class Monitor implements IXposedHookLoadPackage {

    public final static String MY_PACKAGE = "com.tianchi.monidroid";
    public final static String SHARE_NAME = "moni";
    public final static String PACKAGE_NAME_KEY = "pkgname";
    public final static String BLOCK_KEY = "block";
    public final static String TARGET_KEY = "target";
    public final static String INTENT_KEY = "intent";

    public final static String LOG = "Monitor_Log";

    private boolean isStarting = true;

    private XSharedPreferences pre = new XSharedPreferences(MY_PACKAGE, SHARE_NAME);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        //pre.reload();
        //String pkgName = pre.getString(PACKAGE_NAME_KEY, "");
        String pkgName = "com.chuanwg.chuanwugong";
        //find the target package
        if (pkgName.length() <= 0 || !loadPackageParam.packageName.equals(pkgName))
            return;

        //hook startActivity
        hook_methods("android.app.Activity", "startActivityForResult", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object[] objs = param.args;
                if (objs.length != 3) return;
                if (objs[0] instanceof Intent) {
                    Intent in = (Intent) objs[0];
                    int requestCode = (int) objs[1];

                    isStarting = true;

                    pre.reload();
                    boolean isBlock = pre.getBoolean(BLOCK_KEY, false);
                    String target = pre.getString(TARGET_KEY, "");
                    if (isBlock) {
                        String className=null;
                        if(in.getComponent()!=null&&in.getComponent().getShortClassName()!=null){
                            className=in.getComponent().getShortClassName();
                        }else{
                            Log.i(LOG, "Intent err: "+in.toUri(0));
                        }

//                        if (className!=null&&className.equals(target)) {
//                            Log.i(LOG, "It is target activity. Don't stop it.");
//                            return;
//                        }

                        //This activity is blocked
                        Log.i(LOG, "@start@" + className + "@" + in.toUri(0) + "@" + requestCode);
                        param.setResult(null);
                    } else {
                        Log.i(LOG, "It is not block. Let's start " + in.getComponent().getShortClassName());
                    }
                }
            }
        });


        //hook create
        hook_methods("android.app.Instrumentation", "callActivityOnCreate", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Object[] objs = param.args;
                if (objs.length != 2) return;
                if (objs[0] instanceof Activity) {
                    Activity activity = (Activity) objs[0];
                    //Log.v(LOG, "@create@" + activity.getLocalClassName() + "@");
                }
                isStarting = false;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object[] objs = param.args;
                if (objs.length != 2) return;

                Activity a = null;
                if (objs[0] instanceof Activity) {
                    a = (Activity) objs[0];
                }

                //If it is starting another activity, do noting
                if (isStarting)
                    return;

                pre.reload();
                String targetName = pre.getString(TARGET_KEY, "");

                //It is the first time to start this app
                if (targetName.length() <= 0) {
                    Log.v(LOG, "It is the first time to start this app");
                    return;
                }

                //It is blocked, I will do noting
                Boolean isBlock = pre.getBoolean(BLOCK_KEY, false);
                if (isBlock) {
                    Log.v(LOG, "It is blocked, I will do noting");
                    return;
                }

                //It has a target activity
                if (a != null && !targetName.equals(a.getLocalClassName())) {
                    String intentContent = pre.getString(INTENT_KEY, "");
                    if (intentContent.length() > 0) {
                        Intent targetIntent = Intent.parseUri(intentContent, 0);
                        //Intent targetIntent = Intent.getIntentOld(intentContent);
                        Log.v(LOG, "I will start the target activity: " + targetIntent.toString());
                        if (targetIntent != null) {
                            a.startActivity(targetIntent);
                        } else {
                            Log.v(LOG, "Intent is null");
                        }
                    } else {
                        Log.v(LOG, "error intent length.");
                    }
                }
            }
        });

        //hook finish
        hook_methods("android.app.Activity", "finish", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.v(LOG, "@finish@");
                pre.reload();

                if (pre.getBoolean(BLOCK_KEY, false)) {
                    String target = pre.getString(TARGET_KEY, "");
                    Activity activity = (Activity) param.thisObject;
                    //If you want to stop target activity, no way.
                    if (activity.getLocalClassName().equals(target))
                        param.setResult(null);
                }
            }
        });

    }

    /**
     * Redefine hook method
     */
    private void hook_method(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void hook_method(String className, ClassLoader classLoader, String methodName,
                             Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void hook_methods(String className, String methodName, XC_MethodHook xmh) {
        try {
            Class<?> clazz = Class.forName(className);

            for (Method method : clazz.getDeclaredMethods())
                if (method.getName().equals(methodName)
                        && !Modifier.isAbstract(method.getModifiers())
                        && Modifier.isPublic(method.getModifiers())) {
                    XposedBridge.hookMethod(method, xmh);
                }
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void collectTapEvent(View node) {
        if (node == null)
            return;

        if (node.isClickable()) {
            Rect rect = new Rect();
            node.getHitRect(rect);
            Log.v(LOG, "@tap@" + rect.exactCenterX() + "," + rect.exactCenterY() + "@");
        }

        if (node instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) node;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child != null && child.getVisibility() == View.VISIBLE) {
                    collectTapEvent(child);
                }
            }
        }
    }

}
