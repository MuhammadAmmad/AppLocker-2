package com.eeontheway.android.applocker.lock;


import java.io.Serializable;

/**
 * App锁定日志信息
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockLogInfo implements Serializable {
    public static final String PHOTO_PATH_PREFIX = "AppLockerPhoto";

    /**
     * 内部ID
     */
    private long id;

    /**
     * 获取内部ID
     * @return 内部ID
     */
    public long getId() {
        return id;
    }

    /**
     * 设置内部ID
     * @param id 内部ID
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * 应用的包名
     */
    private String packageName;

    /**
     * 获取应用的包名
     * @return 应用的包名
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * 设置应用的包名
     * @return 应用的包名
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * App名称，非包名
     */
    private String appName;

    /**
     * 获取App名称
     * @return App名称
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 设置App名称
     * @param appName App名称
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    /**
     * 日志纪录的时间
     */
    private String time;

    /**
     * 获取日志纪录时间
     * @return 日志纪录时间
     */
    public String getTime() {
        return time;
    }

    /**
     * 设置日志纪录时间
     * @param time 日志纪录时间
     */
    public void setTime(String time) {
        this.time = time;
    }

    /**
     * 密码次数
     */
    private int passwordErrorCount;

    /**
     * 获取密码次数
     * @return 密码次数
     */
    public int getPasswordErrorCount() {
        return passwordErrorCount;
    }

    /**
     * 设置密码次数
     * @param passwordErrorCount 密码次数
     */
    public void setPasswordErrorCount(int passwordErrorCount) {
        this.passwordErrorCount = passwordErrorCount;
    }

    /**
     * 拍摄的照片
     */
    private String photoPath;

    /**
     * 获取拍摄的照片路径
     * @return 照片路径
     */
    public String getPhotoPath() {
        return photoPath;
    }

    /**
     * 设置拍摄的照片路径
     * @param photoPath 照片路径
     */
    public void setPhotoPath (String photoPath) {
        this.photoPath = photoPath;
    }

    /**
     * 图像是否保存在内部
     * @return true 是; false 否
     */
    public boolean isPhotoInInternal () {
        return photoPath.startsWith("/");
    }
}
