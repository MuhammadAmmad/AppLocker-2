package com.eeontheway.android.applocker.lock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.eeontheway.android.applocker.app.AppInfoManager;
import com.eeontheway.android.applocker.app.BaseAppInfo;
import com.eeontheway.android.applocker.utils.ServiceUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 锁定应用的后台服务
 * 该服务一直在后台运行，不断监测前台的运行状态，立即锁定需要锁定的应用
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockService extends Service {
    private LockConfigDao dao;
    private UnlockedList unlockedList;
    private Handler screenUnlockAllHandler;
    private Runnable clearAppLockedRunnable;
    private SettingsManager settingsManager;

    private Thread thread;
    private boolean quit = false;
    private boolean threadWait = false;

    private AppInfoManager appInfoManager;
    private BroadcastReceiver screenLockReceiver;
    private BroadcastReceiver screenUnLockReceiver;
    private BroadcastReceiver appUnlockReceiver;
    private BroadcastReceiver packageRemoveReceiver;
    private BroadcastReceiver packageInstallReceiver;

    private static final String ACTION_NEW_APP_UNLOCKED = "LockService.newAppUnlocked";
    private static final String PARAM_APP_NAME = "packagename";

    private List<BaseAppInfo> appInfoList = new ArrayList<>();
    private List<LockConfigInfo> lockConfigInfoList = new ArrayList<>();

    /**
     * 启动应用锁服务
     */
    public static void startBlockService(Context context) {
        if (ServiceUtils.isServiceRunning(context, LockService.class.getName()) == false) {
            Intent intent = new Intent(context, LockService.class);
            context.startService(intent);
        }
    }

    /**
     * 关闭应用锁服务
     */
    public static void stopBlockService(Context context) {
        if (ServiceUtils.isServiceRunning(context, LockService.class.getName()) == true) {
            Intent intent = new Intent(context, LockService.class);
            context.stopService(intent);
        }
    }

    /**
     * 发广播消息告知服务有新的应用已经解除锁定
     * @param packageName 已经解锁的app包名
     */
    public static void broadcastAppUnlocked (Context context, String packageName) {
        // 纪录应用的包名
        Intent intent = new Intent(ACTION_NEW_APP_UNLOCKED);
        intent.putExtra(PARAM_APP_NAME, packageName);

        // 发送本地广播告知服务，有应用已经解锁
        LocalBroadcastManager lm = LocalBroadcastManager.getInstance(context);
        lm.sendBroadcast(intent);
    }

    /**
     * 注册应用解锁的广播事件监听
     * 当用户输入正确的解锁密码时，将发送广播消息。
     */
    private void registerAppUnlockReceiver() {
        // 应用解锁的广播事件监听器
        appUnlockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 取出解锁的应用包名，添加到临时解锁队列
                String packageName = intent.getStringExtra(PARAM_APP_NAME);
                unlockedList.add(packageName);
            }
        };

        // 注册广播监听器
        IntentFilter intentFilter = new IntentFilter(ACTION_NEW_APP_UNLOCKED);
        LocalBroadcastManager lm = LocalBroadcastManager.getInstance(this);
        lm.registerReceiver(appUnlockReceiver, intentFilter);
    }

    /**
     * 取消应用解锁的插言事件监听
     */
    private void unregisterAppUnlockReceiver () {
        LocalBroadcastManager lm = LocalBroadcastManager.getInstance(this);
        lm.unregisterReceiver(appUnlockReceiver);
    }

    /**
     * 注册锁屏广播事件监听
     * 当锁屏发生时，服务不再需要监听App的锁，减少系统开销
     */
    private void registerScreenLockReceiver() {
        // 锁屏的广播事件监听器
        screenLockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 首先，检查是否要在锁屏时清除所有的应用锁定状态
                int mode = settingsManager.getScreenUnlockLockMode();
                switch (mode) {
                    case SettingsManager.SCREENLOCK_CLEAN_NO_LOCK:
                        // 不需要做任何处理，这样已经锁的和不锁的状态保持不变
                        break;
                    case SettingsManager.SCREENLOCK_CLEAN_ALL_LOCKED:
                        // 清除所有已经解锁应用的锁定状态，
                        unlockedList.clear();
                        break;
                    case SettingsManager.SCREENLOCK_CLEAN_ALL_LOCKED_AFTER_3MIN:
                        // 启动延时操作
                        screenUnlockAllHandler.postDelayed(clearAppLockedRunnable, 3000*60);
                        break;
                }

                // 锁屏后，不需要监听锁，停止监听线程即可
                suspendThread();
            }
        };

        // 注册广播监听器
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenLockReceiver, intentFilter);
    }

    /**
     * 注册屏解锁广播事件监听
     * 当屏锁解开时，需发监听广播事件
     */
    private void registerScreenUnLockReceiver() {
        // 锁屏的广播事件监听器
        screenUnLockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 移除延时清除已解锁的App列表
                screenUnlockAllHandler.removeCallbacks(clearAppLockedRunnable);

                // 开启后，恢复锁屏监听
                resumeWatchThread();
            }
        };

        // 注册广播监听器
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenUnLockReceiver, intentFilter);
    }

    /**
     * 取消屏锁相关的事件监听
     */
    private void unregisterScreenLockReceiver () {
        unregisterReceiver(screenLockReceiver);
        unregisterReceiver(screenUnLockReceiver);
    }

    /**
     * 注册安装包移除的监听器
     */
    private void registerPackageRemoveListener() {
        // 注册安装包移除监听器
        packageRemoveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 有安装包移除，从缓存队列中删除相应的项
                String packageName = intent.getData().getSchemeSpecificPart();
                synchronized (appInfoList) {
                    for (BaseAppInfo appInfo : appInfoList) {
                        if (appInfo.getPackageName().equals(packageName)) {
                            appInfoList.remove(appInfo);
                        }
                        return;
                    }
                }
            }
        };

        // 注册监听器
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addDataScheme("package");
        registerReceiver(packageRemoveReceiver, intentFilter);
    }

    /**
     * 注册安装包安装的监听器
     */
    private void registerPackageInstallListener() {
        // 注册安装包移除监听器
        packageInstallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // 有新包安装，重新加载整个缓存队列
                synchronized (appInfoList) {
                    appInfoList = appInfoManager.queryAllAppInfo(AppInfoManager.AppType.ALL_APP);
                }
            }
        };

        // 注册监听器
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(packageInstallReceiver, intentFilter);
    }

    /**
     * 取消安装包移除的监听器
     */
    private void removePackageRemoveListener() {
        unregisterReceiver(packageRemoveReceiver);
        unregisterReceiver(packageInstallReceiver);
    }

    /**
     * Bind回调,未用
     *
     * @param intent
     * @return 未用
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * Serivce的onCreate回调
     */
    @Override
    public void onCreate() {

        // 获取所有的包信息
        appInfoManager = new AppInfoManager(this);
        appInfoList = appInfoManager.queryAllAppInfo(AppInfoManager.AppType.ALL_APP);

        // 当数据库发生变化时，重新加载所有的配置项
        dao = new LockConfigDao(this);
        dao.registerDataChangeReveiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadAllLockConfigList();
            }
        });
        loadAllLockConfigList();

        settingsManager = SettingsManager.getInstance(this);
        unlockedList = new UnlockedList(this);
        screenUnlockAllHandler = new Handler();
        clearAppLockedRunnable = new Runnable() {
            @Override
            public void run() {
                // 清除所有已经解锁应用的锁定状态
                unlockedList.clear();

                // 简单停止监听线程即可
                suspendThread();
            }
        };

        createWatchThread();
        registerAppUnlockReceiver();
        registerScreenLockReceiver();
        registerScreenUnLockReceiver();
        registerPackageInstallListener();
        registerPackageRemoveListener();
    }

    /**
     * 加载所有的锁定配置列表
     */
    public void loadAllLockConfigList () {
        // 生成自己的锁定信息，即对于自己总是锁定的
//        LockConfigInfo configInfo = new LockConfigInfo();
//        configInfo.setLocked(true);
//        configInfo.setPackageName(getPackageName());

        // 加入到锁定配置对列中
        synchronized (lockConfigInfoList) {
            lockConfigInfoList.clear();
            lockConfigInfoList.addAll(dao.queryAllLockInfo());
            //lockConfigInfoList.add(configInfo);
        }
    }

    /**
     * Service的onStartCommand回调
     *
     * @param intent 未用
     * @param flags 未用
     * @param startId 未用
     * @return 未用
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 启动线程
        startWatchThread();
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /**
     * Serivce的onDestroy回调
     */
    @Override
    public void onDestroy() {
        // 停止监听器和线程
        unregisterAppUnlockReceiver();
        unregisterScreenLockReceiver();
        removePackageRemoveListener();

        stopWatchThread();

        // 如果发现服务被强行中止，则重启服务
        // 重启后可能会由于监听线程在同步对像上，造成整个应用卡死退出
        if (dao.isAnyPackageNeedLock()) {
            startBlockService(this);
            Log.d("AppLocker", "destroy");
        }
        dao.close();

        super.onDestroy();
    }

    /**
     * 启动观察线程
     */
    private void startWatchThread () {
        quit = false;
        thread.start();
    }

    /**
     * 停止观察线程
     */
    private void suspendThread () {
        threadWait = true;
    }

    /**
     * 启动观察线程
     */
    private void resumeWatchThread () {
        threadWait = false;
        synchronized (thread) {
            thread.notifyAll();
        }
    }

    /**
     * 停止观察线程
     */
    private void stopWatchThread () {
        // 设置停止标记，让线程自己退出
        quit = true;

        // 清除可能的延时操作
        screenUnlockAllHandler.removeCallbacks(clearAppLockedRunnable);
    }

    /**
     * 创建应用锁定观察线程
     */
    private void createWatchThread () {
        thread = new Thread(new WatchThreadRunnable());
    }

    /**
     * 锁定队列的观察线程处理器
     */
    class WatchThreadRunnable implements Runnable {
        private String currentPackageName = null;
        private String currentLockedPackageName = null;
        private AppInfoManager infoManager = new AppInfoManager(LockService.this);
        private String topActivityName;
        /**
         * 判断是否需要锁定
         * @return true 需要锁定; false 不需要锁定
         */
        private boolean isNeedLock () {
            // 顶层显示解锁界面时，不需要加锁定
            topActivityName = appInfoManager.queryFirstAppActivityName();
            if (PasswordVerifyActivity.class.getName().equals(topActivityName)
                    || LockLogActivity.class.getName().equals(topActivityName)) {
                return false;
            }
            // 已经解锁过
            if (unlockedList.contains(currentPackageName)) {
                return false;
            }

            // 当前包和最新锁定的包相同，则认为不需要锁定
            // 注：锁定界面的包和被锁应用包的包是不同的
            if (TextUtils.equals(currentLockedPackageName, currentPackageName)) {
                return false;
            }

            // 数据库中配置不需要
            synchronized (lockConfigInfoList) {
                for (LockConfigInfo configInfo : lockConfigInfoList) {
                    if (configInfo.getPackageName().equals(currentPackageName)) {
                        return configInfo.isLocked();
                    }
                }
            }
            return false;
        }

        /**
         * 休眠一会儿
         */
        private void sleep () {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Thread的运行函数
         */
        @Override
        public void run() {
            String mypackageName = getPackageName();
            while (!quit) {
                // 检查是否需要暂停线程，配合锁屏操作
                if (threadWait) {
                    synchronized (thread) {
                        try {
                            Log.d("AppLocker", "Thread wait!");
                            thread.wait();
                            Log.d("AppLocker", "Thread wakeup!");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // 检查是否需要从已解锁队列中清除已经退出的App
                if (settingsManager.isAutoLockOnAppQuit()) {
                    unlockedList.removeExitedApps();
                }

                // 仅当顶层有应用，才检查是否需要锁定
                currentPackageName = infoManager.queryFirstAppPackageName();
                if (currentPackageName != null) {
                    // 如果包名已经发生变化，则立即锁定自己，提高安全度
                    if (!currentPackageName.equals(mypackageName)) {
                        unlockedList.remove(mypackageName);
                    }

                    // 如果启动了一次解锁即解锁全部，且当前应用并不是自己
                    if (settingsManager.isUnlockAnyUnlockAllEnabled() &&
                            !currentPackageName.equals(mypackageName)) {
                        if (unlockedList.isEmpty() == false) {
                            // 小睡一会儿，然后再重新检查锁定
                            sleep();
                            continue;
                        }
                    }

                    if (isNeedLock()) {
                        synchronized (appInfoList) {
                            for (BaseAppInfo appInfo : appInfoList) {
                                if (appInfo.getPackageName().equals(currentPackageName)) {
                                    String packageName = currentPackageName;
                                    String appName = appInfo.getName();

                                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                    Bitmap bitmap = ((BitmapDrawable) appInfo.getIcon()).getBitmap();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

                                    PasswordVerifyActivity.startActivity(LockService.this,
                                            packageName,
                                            appName,
                                            stream.toByteArray(),
                                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    currentLockedPackageName = currentPackageName;
                                    break;
                                }
                            }
                        }
                    } else {
                        currentLockedPackageName = null;
                    }
                }

                // 小睡一会儿，然后再重新检查锁定
                sleep();
            }

            // 线程结束时，释放Activity
            PasswordVerifyActivity.finishActivity();
        }
    }
}

