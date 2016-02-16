package com.eeontheway.android.applocker.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * 系统信息工具
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class SystemInfoUtils {
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

