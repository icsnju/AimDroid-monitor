package com.tianchi.monidroid;

import android.app.Activity;
import android.content.Intent;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    public final static String CHILD_KEY = "child";
    public final static String INTENT_KEY = "intent";

    public final static String LOG = "Monitor_Log";

    private boolean isStarting = true;

    private XSharedPreferences pre = new XSharedPreferences(MY_PACKAGE, SHARE_NAME);
    static String pkgName = "";
    public final static String PACK_CONF = "/data/package.txt";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        //find the target package
        if (pkgName.length() <= 0) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(PACK_CONF));
                String line = br.readLine();
                if (line.length() > 0) {
                    pkgName = line;
                }
                br.close();
            } catch (IOException e) {
                Log.i(LOG, e.toString());
            }
        }

        if (pkgName.length() <= 0 || !loadPackageParam.packageName.equals(pkgName))
            return;

        //Instrumentation
        hook_methods("android.app.Instrumentation", "execStartActivity", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Log.i(LOG, "I am in execStartActivity");
                Object[] objs = param.args;
                Intent in = null;
                if (objs.length > 5 && objs[4] instanceof Intent) {
                    in = (Intent) objs[4];
                }

                if (in != null) {
                    //Monkey
//                    if (in.getComponent() != null && in.getComponent().getShortClassName() != null) {
//                        Log.i(LOG, "@start@" + in.getComponent().getShortClassName() + "@" + in.toUri(0) + "@");
//                    }

                    isStarting = true;

                    pre.reload();
                    boolean isBlock = pre.getBoolean(BLOCK_KEY, false);
                    String targetName = pre.getString(TARGET_KEY, "");
                    String childName = pre.getString(CHILD_KEY, "");
                    if (isBlock) {
                        if (in.getComponent() != null && in.getComponent().getShortClassName() != null) {
                            String className = in.getComponent().getShortClassName();
                            //This activity is blocked
                            Log.i(LOG, "@start@" + className + "@" + in.toUri(0) + "@");
                            if (className.equals(targetName) || className.equals(childName)) {
                                Log.i(LOG, "Start it: " + className);
                                return;
                            } else {
                                param.setResult(null);
                            }
                        } else {
                            //Log.i(LOG, "@start@" + in.getAction() + "@" + in.toUri(0) + "@");
                            Log.i(LOG, "Intent out: " + in.toUri(0));
                        }
//                        if (className!=null&&className.equals(target)) {
//                            Log.i(LOG, "It is target activity. Don't stop it.");
//                            return;
//                        }
                    } else {
                        Log.i(LOG, "It is not block. Let's start " + in.getComponent().getShortClassName());
                    }
                }

            }
        });

        //hook startActivity
//        hook_methods("android.app.Activity", "startActivityForResult", new XC_MethodHook() {
//
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                Log.i(LOG, "I am in");
//                Object[] objs = param.args;
//                if (objs.length < 3) return;
//
//                Intent in = null;
//                if (objs[0] instanceof Intent) {
//                    in = (Intent) objs[0];
//                } else if (objs[1] instanceof Intent) {
//                    in = (Intent) objs[1];
//                }
//
//                if (in != null) {
//                    //Monkey
//                    if (in.getComponent() != null && in.getComponent().getShortClassName() != null) {
//                        Log.i(LOG, "@start@" + in.getComponent().getShortClassName() + "@" + in.toUri(0) + "@");
//                    }
//
//                    isStarting = true;
//
//                    pre.reload();
//                    boolean isBlock = pre.getBoolean(BLOCK_KEY, false);
//                    if (isBlock) {
//                        if (in.getComponent() != null && in.getComponent().getShortClassName() != null) {
//                            String className = in.getComponent().getShortClassName();
//                            //This activity is blocked
//                            Log.i(LOG, "@start@" + className + "@" + in.toUri(0) + "@");
//                            param.setResult(null);
//                        } else {
//                            //Log.i(LOG, "@start@" + in.getAction() + "@" + in.toUri(0) + "@");
//                            Log.i(LOG, "Intent out: " + in.toUri(0));
//                        }
////                        if (className!=null&&className.equals(target)) {
////                            Log.i(LOG, "It is target activity. Don't stop it.");
////                            return;
////                        }
//                    } else {
//                        Log.i(LOG, "It is not block. Let's start " + in.getComponent().getShortClassName());
//                    }
//                }
//            }
//        });


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
                Log.i(LOG, "I am in callActivityOnCreate");

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
                if (targetName.length() <= 0 && a != null) {
                    Log.v(LOG, "It is the first time to start this app " + a.getLocalClassName());
                    return;
                }

                //It is blocked, I will do noting
                Boolean isBlock = pre.getBoolean(BLOCK_KEY, false);
                if (isBlock) {
                    if (a != null) {
                        Intent in = a.getIntent();
                        Log.i(LOG, "@start@" + a.getLocalClassName() + "@" + in.toUri(0) + "@");
                    }
                    return;
                }

                //It has a target activity
                if (a != null && !targetName.equals(a.getLocalClassName())) {
                    String intentContent = pre.getString(INTENT_KEY, "");
                    if (intentContent.length() > 0) {
                        Intent targetIntent = Intent.parseUri(intentContent, 0);
                        //Intent targetIntent = Intent.getIntentOld(intentContent);
                        Log.v(LOG, "I will start the target activity: " + targetIntent.toString());
                        a.startActivity(targetIntent);
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
//                    Activity activity = (Activity) param.thisObject;
//                    //If you want to stop target activity, no way.
//                    if (activity.getLocalClassName().equals(target))
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
}
