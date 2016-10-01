package com.tianchi.monidroid;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
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
    public static String LOG = "Monitor_Log";

    private String pkgName = null;
    private Intent targetIntent = null;
    private String targetName = null;
    private boolean isBlock = false;
    private boolean isStarting = true;

    private static final String pkgFilePath = "/sdcard/pkg.txt";
    private static final String targetFilePath = "/sdcard/target.txt";

    XSharedPreferences pre = new XSharedPreferences("com.tianchi.monidroid", "moni");


    private String getPkgName() {
        String pkgName = null;
        File pkgFile = new File(pkgFilePath);
        if (!pkgFile.exists()) return null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(pkgFile));
            for (String line; (line = br.readLine()) != null; ) {
                if (line.length() > 0) {
                    pkgName = line;
                    break;
                }
            }
            br.close();
        } catch (IOException e) {
            //Log.v(LOG, e.toString());
        }
        return pkgName;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (pkgName == null)
            pkgName = getPkgName();

        //find the target package
        if (pkgName != null && loadPackageParam.packageName.equals(pkgName)) {
            pre.reload();
            Log.v(LOG,"pre:"+pre.getString("count",""));

            final File targetFile = new File(targetFilePath);
            if (!targetFile.exists()) {
                targetName = null;
                targetIntent = null;
            } else {
                BufferedReader br = new BufferedReader(new FileReader(targetFile));
                int i = 0;
                for (String line; (line = br.readLine()) != null; ) {

                    if (line.length() > 0 && i == 0) {
                        targetName = line;
                    } else if (line.length() > 0 && i == 1) {
                        targetIntent = IntentJsonConverter.jsonToIntent(line);
                    }
                    i++;
                }
            }

            isBlock = false;
            isStarting = true;

            //hook startActivity
            hook_methods("android.app.Activity", "startActivityForResult", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object[] objs = param.args;
                    if (objs.length != 3) return;
                    if (objs[0] instanceof Intent) {
                        Intent in = (Intent) objs[0];

                        Log.i(LOG, "#start#" + in.getComponent().getShortClassName() + "#" + IntentJsonConverter.intentToJson(in) + "#");
                        if (isBlock) {
                            param.setResult(null);
                        }
                        isStarting = true;
                    }
                    pre.reload();
                    Log.v(LOG,"pre:"+pre.getString("count",""));
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
                    if (objs[0] instanceof Activity) {
                        Activity a = (Activity) objs[0];
                        if (targetName != null && !targetName.equals(a.getLocalClassName()) && !isStarting) {
                            a.startActivity(targetIntent);
                        } else if (targetName == null && !isStarting) {
                            targetName = a.getLocalClassName();
                        }

                        if (targetName != null && targetName.equals(a.getLocalClassName())) {
                            isBlock = true;
                        }
                    }
                }

            });

            //hook finish
            hook_methods("android.app.Activity", "finish", new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.v(LOG, "#finish#");
                    if (isBlock)
                        param.setResult(null);
                }
            });

        }
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
