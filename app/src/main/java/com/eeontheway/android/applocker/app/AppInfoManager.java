package com.eeontheway.android.applocker.app;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 进程信息管理器
 * 主要用于获取进程的相关信息
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class AppInfoManager {
    /**
     * APP类型
     */
    public enum AppType {
        ALL_APP, /**
         * 所有App
         */
        USER_APP, /**
         * 用户App
         */
        SYSTEM_APP      /** 系统App */
    }

    private Context context;
    private ActivityManager activityManager;
    private PackageManager packageManager;

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public AppInfoManager(Context context) {
        this.context = context;
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        packageManager = context.getPackageManager();
    }

    /**
     * 通过包名，获取完整的App信息
     * @param packageName 应用的包名
     * @return App信息
     */
    public AppInfo queryAppInfo (String packageName) {
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(
                PackageManager.GET_ACTIVITIES);

        for (PackageInfo info : packageInfos) {
            if (packageName.equals(info.applicationInfo.packageName)) {
                AppInfo appInfo = new AppInfo();
                appInfo.setPackageName(info.applicationInfo.packageName);
                appInfo.setIcon(info.applicationInfo.loadIcon(packageManager));
                appInfo.setName(info.applicationInfo.loadLabel(packageManager).toString());
                if ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    appInfo.setUserApp(true);
                } else {
                    appInfo.setUserApp(false);
                }
                appInfo.setUsedSize(new File(info.applicationInfo.sourceDir).length());
                appInfo.setVersionName(info.versionName);

                appInfo.setVersionCode(info.versionCode);
                appInfo.setInRom((info.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == 0);

                return appInfo;
            }
        }

        return null;
    }

    /**
     * 查看位于前台的App
     * @return App的包名
     */
    public String queryFirstAppPackageName () {
        if (Build.VERSION.SDK_INT < 21) {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
            if (runningTaskInfos.size() > 0) {
                return runningTaskInfos.get(0).topActivity.getPackageName();
            }
        } else {
            try {
                Field processStateTopField = ActivityManager.class.getDeclaredField("PROCESS_STATE_TOP");
                int processStateTop = processStateTopField.getInt(activityManager);

                List<ActivityManager.RunningAppProcessInfo> infoList = activityManager.getRunningAppProcesses();
                for (ActivityManager.RunningAppProcessInfo info : infoList) {
                    Field processStateField = ActivityManager.RunningAppProcessInfo.class.getDeclaredField("processState");
                    int currentState = processStateField.getInt(info);
                    if (currentState == processStateTop) {
                        return info.processName;
                    }
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * 查看位于前台的App
     * @return App的顶层Activity类名
     */
    public String queryFirstAppActivityName () {
        if (Build.VERSION.SDK_INT < 21) {
            List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
            if (runningTaskInfos.size() > 0) {
                return runningTaskInfos.get(0).topActivity.getClassName();
            }
        }

        return null;
    }

    /**
     * 判断指定应用是否正在运行
     * @param packageName 要判断的任务
     * @return true 是; false 否
     */

    public boolean isPackageRunning (String packageName) {
        List<ActivityManager.RunningAppProcessInfo> infoList = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infoList) {
            try {
                if (info.processName.equals(packageName)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * 获取所有的App信息列表
     *
     * @param appType 待获取的App类型
     * @return App信息列表
     */
    public List<BaseAppInfo> queryAllAppInfo(AppType appType) {
        List<BaseAppInfo> appInfoList = new ArrayList<>();
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);

        for (PackageInfo info : packageInfos) {
            AppType type = ((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) ?
                    AppType.USER_APP : AppType.SYSTEM_APP;

            if ((appType == AppType.ALL_APP) || (appType == type)) {
                AppInfo appInfo = new AppInfo();
                appInfo.setPackageName(info.applicationInfo.packageName);
                appInfo.setIcon(info.applicationInfo.loadIcon(packageManager));
                appInfo.setName(info.applicationInfo.loadLabel(packageManager).toString());
                appInfo.setUserApp(type == AppType.USER_APP);
                appInfo.setUsedSize(new File(info.applicationInfo.sourceDir).length());
                appInfo.setVersionName(info.versionName);
                //appInfo.setSignatures(info.signatures[0].toCharsString());

                appInfo.setVersionCode(info.versionCode);
                appInfo.setInRom((info.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == 0);

                appInfoList.add(appInfo);
            }
        }

        return appInfoList;
    }

    /**
     * 查看应用的详细信息
     *
     * @param packageName 待查看应用的包名
     */
    public void viewProcess(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        context.startActivity(intent);
    }

    /**
     * 杀死进程
     */
    public void killProcess(String packageName) {
        activityManager.restartPackage(packageName);
    }

    /**
     * 获取所有的进程信息
     *
     * @param appType 待获取的App类型
     * @return 进程信息列表
     */
    public List<BaseAppInfo> queryAllProcessInfo(AppType appType) {
        List<BaseAppInfo> processInfoList = new ArrayList<>();

        List<ActivityManager.RunningAppProcessInfo> infoList = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infoList) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(info.processName, 0);
                AppType type = ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) ?
                        AppType.USER_APP : AppType.SYSTEM_APP;

                if ((appType == AppType.ALL_APP) || (appType == type)) {
                    ProcessInfo processInfo = new ProcessInfo();

                    Drawable icon = packageInfo.applicationInfo.loadIcon(packageManager);
                    processInfo.setIcon(icon);
                    String name = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                    processInfo.setName(name);
                    processInfo.setUserApp(type == AppType.USER_APP);

                    processInfo.setPid(info.pid);
                    processInfo.setUid(info.uid);
                    processInfo.setPackageName(info.processName);

                    Debug.MemoryInfo[] memInfo = activityManager.getProcessMemoryInfo(new int[]{info.pid});
                    processInfo.setUsedSize(memInfo[0].dalvikPrivateDirty);

                    processInfoList.add(processInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return processInfoList;
    }

}
