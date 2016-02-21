package com.eeontheway.android.applocker.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

/**
 * 系统信息工具
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class SystemUtils {
    /**
     * 获取当前App安装的时间
     *
     * @return 安装的时间
     */
    public static long getAppInstalledTime(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            return info.lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return 0;
    }
}

