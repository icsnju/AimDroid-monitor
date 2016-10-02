package com.tianchi.monidroid;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

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

    public final static String MY_PACKAGE="com.tianchi.monidroid";
    public final static String SHARE_NAME="moni";
    public final static String PACKAGE_NAME_KEY="pkgname";
    public final static String BLOCK_KEY="block";
    public final static String TARGET_KEY="target";
    public final static String INTENT_KEY="intent";

    public final static String LOG = "Monitor_Log";

    private boolean isStarting = true;


    private XSharedPreferences pre = new XSharedPreferences(MY_PACKAGE, SHARE_NAME);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        //pre.reload();
        //String pkgName = pre.getString(PACKAGE_NAME_KEY, "");
        String pkgName="com.chuanwg.chuanwugong";
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

                    pre.reload();
                    if (pre.getBoolean(BLOCK_KEY, false)) {
                        //This activity is blocked
                        Log.i(LOG, "#start#" + in.getComponent().getShortClassName() + "#" + IntentJsonConverter.intentToJson(in) + "#");
                        param.setResult(null);
                    }
                    isStarting = true;
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
                    Activity a = (Activity) objs[0];
                    Log.v(LOG, "#create#" + a.getLocalClassName() + "#");
                }
                isStarting = false;
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object[] objs = param.args;
                if (objs.length != 2) return;

                //If it is starting another activity, do noting
                if (isStarting)
                    return;

                pre.reload();
                String targetName = pre.getString(TARGET_KEY, "");

                //It is the first time to start this app
                if (targetName.length() <= 0)
                    return;

                //It is blocked, I will do noting
                Boolean isBlock=pre.getBoolean(BLOCK_KEY,false);
                if(isBlock)
                    return;

                //It has a target activity
                if (objs[0] instanceof Activity) {
                    Activity a = (Activity) objs[0];

                    if (!targetName.equals(a.getLocalClassName())) {
                        String intentContent = pre.getString(INTENT_KEY, "");
                        if (intentContent.length() > 0) {
                            Intent targetIntent = IntentJsonConverter.jsonToIntent(intentContent);
                            a.startActivity(targetIntent);
                        } else {
                            Log.v(LOG, "error intent length.");
                        }
                    }
                }
            }

        });

        //hook finish
        hook_methods("android.app.Activity", "finish", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.v(LOG, "#finish#");
                pre.reload();
                if (pre.getBoolean(BLOCK_KEY, false))
                    param.setResult(null);
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
}
