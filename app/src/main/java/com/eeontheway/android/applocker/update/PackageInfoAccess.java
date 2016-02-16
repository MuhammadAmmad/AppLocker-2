package com.eeontheway.android.applocker.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * 包信息访问类
 */
public class PackageInfoAccess {
    private Context context;
    private PackageManager packageManager;

    /**
     * 构造器
     * @param context Activity或其它Context
     */
    public PackageInfoAccess (Context context) {
        this.context = context;

        packageManager = context.getPackageManager();
    }

    /**
     * 获取用户可读的版本名
     * @return 版本名称
     */
    public String getVersionName () {

        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 获取版本号
     * @return 版本号
     */
    public int getVersionCode () {
        int versionCode = 0;

        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionCode;
    }
}
