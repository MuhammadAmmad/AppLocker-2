package com.eeontheway.android.applocker.applock;

import com.eeontheway.android.applocker.app.AppInfo;

/**
 * App锁定信息
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class AppLockInfo {
    /**
     * App信息
     */
    private AppInfo appInfo;

    /**
     * 获取App信息
     * @return App信息
     */
    public AppInfo getAppInfo() {
        return appInfo;
    }

    /**
     * 设置App信息
     * @param appInfo App信息
     */
    public void setAppInfo(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    /**
     * 是否被锁定
     */
    private boolean locked;

    /**
     * 返回应用是否锁定信息
     * @return 应用是否锁定
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * 设置应用锁定信息
     * @param locked 应用是否锁定
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
