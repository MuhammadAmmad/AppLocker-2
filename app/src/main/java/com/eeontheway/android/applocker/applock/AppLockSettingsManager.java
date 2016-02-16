package com.eeontheway.android.applocker.applock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * App锁配置操作类showLockAlert
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class AppLockSettingsManager {
    public static final int SCREENLOCK_CLEAN_NO_LOCK = 0;
    public static final int SCREENLOCK_CLEAN_ALL_LOCKED = 1;
    public static final int SCREENLOCK_CLEAN_ALL_LOCKED_AFTER_3MIN = 2;

    // 各个配置项的Key
    // 注意要与pref_applocker_config.xml中的保持一致
    public static final String applock_password_key = "applock_password";
    public static final String unlock_failed_capture_enable_key = "unlock_failed_capture_enable";
    public static final String unlock_failed_capture_errcount_key = "unlock_failed_capture_errcount";
    public static final String unlock_failed_capture_location_key = "unlock_failed_capture_location";
    public static final String screen_lock_mode_key = "screen_lock_mode";
    public static final String autolock_on_app_quit_key = "autolock_on_app_quit";
    public static final String one_password_unlock_all_key = "one_password_unlock_all";
    public static final String alert_lock_unlock_key = "alert_lock_unlock";
    public static final String add_tag_to_photo_key = "add_tag_to_photo";

    // 各个配置项
    private String applock_password;
    private boolean unlock_failed_capture_enable;
    private int unlock_failed_capture_errcount;
    private boolean unlock_failed_capture_location;
    private int screen_lock_mode;
    private boolean autolock_on_app_quit;
    private boolean one_password_unlock_all;
    private boolean alert_lock_unlock;
    private boolean add_tag_to_photo;

    private Context context;
    private static AppLockSettingsManager instance;

    /**
     * 获取实例：单例模式
     */
    public static AppLockSettingsManager getInstance (Context context) {
        if (instance == null) {
            instance = new AppLockSettingsManager();
            instance.context = context;

            // 加载所有配置
            instance.reLoadAllPreferences();
        }

        return instance;
    }

    protected AppLockSettingsManager () {}

    /**
     * 加载所有的配置项
     */
    public void reLoadAllPreferences () {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(instance.context);

        instance.applock_password = sharedPref.getString(applock_password_key, "");
        instance.unlock_failed_capture_enable = sharedPref.getBoolean(unlock_failed_capture_enable_key, false);
        instance.unlock_failed_capture_errcount = Integer.parseInt(sharedPref.getString(unlock_failed_capture_errcount_key, "1"));
        instance.unlock_failed_capture_location = sharedPref.getBoolean(unlock_failed_capture_location_key, false);
        instance.screen_lock_mode = Integer.parseInt(sharedPref.getString(screen_lock_mode_key, "0"));
        instance.autolock_on_app_quit = sharedPref.getBoolean(autolock_on_app_quit_key, false);
        instance.one_password_unlock_all = sharedPref.getBoolean(one_password_unlock_all_key, false);
        instance.alert_lock_unlock = sharedPref.getBoolean(alert_lock_unlock_key, false);
        instance.add_tag_to_photo = sharedPref.getBoolean(add_tag_to_photo_key, false);
    }

    /**
     * 保存密码
     * @param password
     */
    public void savePassword (String password) {
        applock_password = password;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(instance.context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(applock_password_key, password);
        editor.commit();
    }

    /**
     * 更新配置
     * @param key 待更新的配置项键值
     * @param preferences 配置项数据
     */
    public void updateSetting (String key, SharedPreferences preferences) {
        if (key.equals(applock_password_key)) {
            instance.applock_password = preferences.getString(applock_password_key, "");
        } else if (key.equals(unlock_failed_capture_enable_key)) {
            instance.unlock_failed_capture_enable = preferences.getBoolean(unlock_failed_capture_enable_key, true);
        } else if (key.equals(unlock_failed_capture_errcount_key)) {
            instance.unlock_failed_capture_errcount = Integer.parseInt(preferences.getString(unlock_failed_capture_errcount_key, "5"));
        } else if (key.equals(unlock_failed_capture_location_key)) {
            instance.unlock_failed_capture_location = preferences.getBoolean(unlock_failed_capture_location_key, false);
        } else if (key.equals(screen_lock_mode_key)) {
            instance.screen_lock_mode = Integer.parseInt(preferences.getString(screen_lock_mode_key, "1"));
        } else if (key.equals(autolock_on_app_quit_key)) {
            instance.autolock_on_app_quit = preferences.getBoolean(autolock_on_app_quit_key, true);
        } else if (key.equals(one_password_unlock_all_key)) {
            instance.one_password_unlock_all = preferences.getBoolean(one_password_unlock_all_key, false);
        } else if (key.equals(alert_lock_unlock_key)) {
            instance.alert_lock_unlock = preferences.getBoolean(alert_lock_unlock_key, true);
        } else if (key.equals(add_tag_to_photo_key)) {
            instance.add_tag_to_photo = preferences.getBoolean(add_tag_to_photo_key, true);
        }
    }

    /**
     * 获取安全密码
     * @return 安全密码，也即解锁密码
     */
    public String getPassword() {
        return applock_password;
    }

    /**
     * 检查是否在解锁失败时，抓拍
     * @return true 开启; false 关闭
     */
    public boolean isCaptureOnFailEnable() {
        return unlock_failed_capture_enable;
    }

    /**
     * 获取得应当抓拍时解锁失败的次数
     * @return 解锁失败次数
     */
    public int getCaptureOnFailCount () {
        return unlock_failed_capture_errcount;
    }

    /**
     * 获取抓拍图片的存储图径
     * @return true 存储的相册路径中; false 存储照片到内部
     */
    public boolean isCaptureOnFailPhotoInGallray() {
        return unlock_failed_capture_location;
    }

    /**
     * 获取锁屏时的锁定模式
     * @return 锁定模式
     */
    public int getScreenUnlockLockMode() {
        return screen_lock_mode;
    }

    /**
     * 是否在在应用退出时重新锁定
     * @return true 开启; false 关闭
     */
    public boolean isAutoLockOnAppQuit() {
        return autolock_on_app_quit;
    }

    /**
     * 是否一次解锁后，解锁所有的应用
     * @return true 开启; false 关闭
     */
    public boolean isUnlockAnyUnlockAllEnabled () {
        return one_password_unlock_all;
    }

    /**
     * 是否显示锁定信息提示
     * @return true 显示; false 不显示
     */
    public boolean isAlertLockUnlockEnabled () {
        return alert_lock_unlock;
    }

    /**
     * 是否添加拍摄标记到照片
     * @return true 开启; false 关闭
     */
    public boolean isAddTagToPhoto () {
        return add_tag_to_photo;
    }
}


