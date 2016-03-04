package com.eeontheway.android.applocker.lock;

import android.content.Context;
import android.content.SharedPreferences;

import com.eeontheway.android.applocker.app.AppInfo;
import com.eeontheway.android.applocker.locate.LocationService;
import com.eeontheway.android.applocker.locate.Position;
import com.eeontheway.android.applocker.main.StartupApplcation;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * 应用锁定信息存储的数据库
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockConfigManager {
    private static final String lastModeIdKey = "lastModeIdKey";
    private static final String LOCK_CONFIG_FILE = "lock_config_pref";

    private Context context;
    private LockConfigDatabase lockConfigDatabase;

    private static LockConfigManager instance;
    private static int instanceCount;

    private LockModeInfo currentLockModeInfo;
    private List<LockModeInfo> lockModeInfoList = new ArrayList<>();
    private List<AppLockInfo> appLockInfoList = new ArrayList<>();
    private List<BaseLockCondition> lockConditionList = new ArrayList<>();
    private List<AccessLog> accessLogList = new ArrayList<>();

    private ConfigObservable observable;

    /**
     * 获取初始实例
     *
     * @param context 上下文
     */
    public static LockConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new LockConfigManager(context);
        }
        instanceCount++;
        return instance;
    }

    /**
     * 释放实例
     */
    public static void freeInstance() {
        if (instance != null) {
            if (--instanceCount == 0) {
                instanceCount = 0;
                instance.lockConfigDatabase.close();
                instance = null;
            }
        }
    }

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    private LockConfigManager(Context context) {
        this.context = context;

        lockConfigDatabase = new LockConfigDatabase(context);
        lockConfigDatabase.open();
        observable = new ConfigObservable();

        loadModeList();
    }

    /**
     * 注册数据变化的监听器
     *
     * @param observer
     */
    public void registerObserver(Observer observer) {
        observable.addObserver(observer);
    }

    /**
     * 初始化模式列表
     */
    private void loadModeList() {
        lockModeInfoList = lockConfigDatabase.queryModeList();

        SharedPreferences sp = context.getSharedPreferences(LOCK_CONFIG_FILE, Context.MODE_PRIVATE);

        boolean exist = sp.contains(lastModeIdKey);
        if (exist) {
            int id = sp.getInt(lastModeIdKey, 0);
            for (LockModeInfo modeInfo : lockModeInfoList) {
                if (id == modeInfo.getId()) {
                    switchModeInfo(lockModeInfoList.indexOf(modeInfo));
                    return;
                }
            }
        }
    }

    /**
     * 获取模式信息列表
     *
     * @return 模式信息列表
     */
    public int getLockModeCount() {
        return lockModeInfoList.size();
    }

    /**
     * 获取指定位置的模式信息
     *
     * @param index 指定位置
     * @return 模式信息
     */
    public LockModeInfo getLockModeInfo(int index) {
        if (index < lockModeInfoList.size()) {
            return lockModeInfoList.get(index);
        }

        return null;
    }

    /**
     * 当前的模式信息
     *
     * @return 当前模式信息
     */
    public LockModeInfo getCurrentLockModeInfo() {
        return currentLockModeInfo;
    }

    /**
     * 添加一个新模式
     *
     * @param modeName 新模式名
     * @return 新模式信息
     */
    public LockModeInfo addModeInfo(String modeName) {
        LockModeInfo modeInfo = lockConfigDatabase.addModeInfo(modeName);
        if (modeInfo != null) {
            lockModeInfoList.add(modeInfo);
            observable.notifyObservers();
        }
        return modeInfo;
    }

    /**
     * 删除一个模式
     *
     * @param modeInfo 待删除的模式
     */
    public void deleteModeInfo(LockModeInfo modeInfo) {
        lockConfigDatabase.deleteModeInfo(modeInfo);
        lockModeInfoList.remove(modeInfo);
        observable.notifyObservers();
    }

    /**
     * 更新模式信息
     *
     * @param modeInfo 待更新的模式信息
     * @return 是否成功 true/false
     */
    public boolean updateModeInfo(LockModeInfo modeInfo) {
        boolean ok = lockConfigDatabase.updateModeInfo(modeInfo);
        if (ok) {
            observable.notifyObservers();
        }
        return ok;
    }

    /**
     * 切换当前模式
     *
     * @param index 新模式的序号
     */
    public void switchModeInfo(int index) {
        // 检查模式
        LockModeInfo modeInfo = lockModeInfoList.get(index);
        if (currentLockModeInfo == null) {
            currentLockModeInfo = modeInfo;

            // 重新加载各项配置
            loadAppInfoList();
            loadLockConditionList();
        } else if (currentLockModeInfo != modeInfo) {
            currentLockModeInfo.setEnabled(false);
            currentLockModeInfo = modeInfo;

            // 重新加载各项配置
            loadAppInfoList();
            loadLockConditionList();
        }
        currentLockModeInfo.setEnabled(true);

        // 保存当前模式
        SharedPreferences sp = context.getSharedPreferences(LOCK_CONFIG_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (modeInfo == null) {
            editor.clear();
        } else {
            editor.putInt(lastModeIdKey, modeInfo.getId());
        }
        editor.commit();

        observable.notifyObservers();
    }

    /**
     * 获取当前模式下加入锁定配置的App数量
     *
     * @return App数量
     */
    public int getAppListCount() {
        return appLockInfoList.size();
    }

    /**
     * 检查被选中的App数量
     *
     * @return 被选中的APP数量
     */
    public int selectedAppCount() {
        int count = 0;

        for (AppLockInfo lockInfo : appLockInfoList) {
            if (lockInfo.isSelected()) {
                count++;
            }
        }

        return count;
    }

    /**
     * 获得指定位置的App锁定信息
     *
     * @param index 指定位置
     * @return App锁定信息
     */
    public AppLockInfo getAppLockInfo(int index) {
        return appLockInfoList.get(index);
    }

    /**
     * 加载所有的APp列表
     */
    public void loadAppInfoList() {
        loadAppInfoList(currentLockModeInfo);
    }

    public void loadAppInfoList (LockModeInfo modeInfo) {
        appLockInfoList = lockConfigDatabase.getAppInfoByMode(modeInfo);
        observable.notifyObservers();
    }

    /**
     * 将指定的App信息添加到指定模式下边
     *
     * @param appLockInfo  指定的App锁定信息
     * @param lockModeInfo 指定的App信息
     * @return 是否添加成功; true/false
     */
    public boolean addAppInfoToMode(AppLockInfo appLockInfo, LockModeInfo lockModeInfo) {
        for (AppLockInfo lockInfo : appLockInfoList) {
            // 如果已经在列表中，则无需插入
            if (lockInfo.getPackageName().equals(appLockInfo.getPackageName())) {
                return true;
            }
        }

        // 插入数据库中
        boolean ok = lockConfigDatabase.addAppInfoToMode(appLockInfo, lockModeInfo);
        if (ok) {
            appLockInfoList.add(appLockInfo);
            observable.notifyObservers();
        }
        return ok;
    }

    public boolean addAppInfoToMode(AppLockInfo appLockInfo) {
        return addAppInfoToMode(appLockInfo, currentLockModeInfo);
    }

    /**
     * 切换App的锁定状态
     *
     * @param appLockInfo 待设置的App信息
     * @param enable      是否锁定
     * @return 操作是否成功
     */
    public boolean setAppLockEnable(AppLockInfo appLockInfo, boolean enable) {
        boolean ok;

        ok = lockConfigDatabase.updateAppInfo(appLockInfo, enable);
        if (ok) {
            appLockInfo.setEnable(enable);
            observable.notifyObservers();
        }
        return ok;
    }

    /**
     * 判断指定包名的App是否在App锁定列表中
     *
     * @param packageName
     * @return true/false
     */
    public boolean isPackageInAppLockList(String packageName) {
        for (AppLockInfo info : appLockInfoList) {
            if (info.getPackageName().equals(packageName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 将App从锁定配置列表中移除
     *
     * @return 移除的数量
     */
    public int deleteSelectedAppInfo() {
        int count = 0;
        List<AppLockInfo> removeList = new ArrayList<>();

        // 扫描需要移除的App信息
        for (AppLockInfo lockInfo : appLockInfoList) {
            if (lockInfo.isSelected()) {
                removeList.add(lockInfo);
            }
        }

        // 开始移除操作
        for (AppLockInfo lockInfo : removeList) {
            // 从数据库中移除
            boolean ok = lockConfigDatabase.deleteAppInfo(lockInfo);
            if (ok) {
                // 再从缓存队列中移除
                appLockInfoList.remove(lockInfo);

                count++;
            }
        }

        // 通知外界数据发生变化
        if (count > 0) {
            observable.notifyObservers();
        }

        return count;
    }

    /**
     * 加载锁定时机信息列表
     */
    public void loadLockConditionList() {
        loadLockConditionList(currentLockModeInfo);
    }

    public void loadLockConditionList(LockModeInfo modeInfo) {
        lockConditionList = lockConfigDatabase.queryLockCondition(modeInfo);
        observable.notifyObservers();
    }

    /**
     * 删除选中的锁定配置
     * @return 移除的数量
     */
    public int deleteSelectedLockCondition() {
        int count = 0;
        List<BaseLockCondition> removeList = new ArrayList<>();

        // 扫描需要移除的App信息
        for (BaseLockCondition config : lockConditionList) {
            if (config.isSelected()) {
                removeList.add(config);
            }
        }

        // 开始移除操作
        for (BaseLockCondition config : removeList) {
            // 从数据库中移除
            boolean ok = lockConfigDatabase.deleteLockCondition(config);
            if (ok) {
                // 再从缓存队列中移除
                lockConditionList.remove(config);

                count++;
            }
        }

        // 通知外界数据发生变化
        if (count > 0) {
            observable.notifyObservers();
        }

        return count;
    }

    /**
     * 检查被选中的App数量
     *
     * @return 被选中的APP数量
     */
    public int selectedLockCondition() {
        int count = 0;

        for (BaseLockCondition config : lockConditionList) {
            if (config.isSelected()) {
                count++;
            }
        }

        return count;
    }

    /**
     * 获取指定位置的锁定配置
     *
     * @param index 指定位置
     * @return 锁定配置信息
     */
    public BaseLockCondition getLockCondition(int index) {
        return lockConditionList.get(index);
    }

    /**
     * 获取当前模式下的锁定配置的数量
     *
     * @return 锁定配置的数量
     */
    public int getLockConditionCount() {
        return lockConditionList.size();
    }

    /**
     * 更新锁定时机配置
     * @param config 待更新的配置
     * @return 是否成功 true/false
     */
    public boolean updateLockCondition(BaseLockCondition config) {
        boolean ok = lockConfigDatabase.updateLockCondition(config);
        if (ok) {
            // 如果数据库写成功了，则更新缓存
            for (BaseLockCondition condition : lockConditionList) {
                if (condition.getId() == config.getId()) {
                    condition.copy(config);
                    break;
                }
            }
            observable.notifyObservers();
        }
        return ok;
    }

    /**
     * 将指定的锁定配置添加到指定模式下边
     *
     * @param config  指定的App锁定信息
     * @param lockModeInfo 指定的App信息
     * @return 是否添加成功; true/false
     */
    public boolean addLockConditionIntoMode(BaseLockCondition config, LockModeInfo lockModeInfo) {
        boolean ok = lockConfigDatabase.addLockConditionIntoMode(config, lockModeInfo);
        if (ok) {
            lockConditionList.add(config);
            observable.notifyObservers();
        }
        return ok;
    }

    public boolean addLockConditionIntoMode(BaseLockCondition config) {
        return addLockConditionIntoMode(config, currentLockModeInfo);
    }

    /**
     * 数据变化通知器
     */
    class ConfigObservable extends Observable {
        @Override
        public void notifyObservers() {
            setChanged();
            super.notifyObservers();
        }
    }

    /**
     * 获取日志条目数
     * @return 日志条目数
     */
    public int getAcessLogsCount () {
        return accessLogList.size();
    }

    /**
     * 获取指定位置的日志信息
     * @param index 指定位置
     * @return 日志信息
     */
    public AccessLog getAccessLog (int index) {
        if (index < accessLogList.size()) {
            return accessLogList.get(index);
        }

        return null;
    }

    /**
     * 获取指定包名的最新锁定日志
     * @param packageName 查找是使用的包名
     * @return 日志信息
     */
    public AccessLog getAccessLog (String packageName) {
        for (AccessLog accessLog : accessLogList) {
            if (accessLog.getPackageName().equals(packageName)) {
                return accessLog;
            }
        }

        return null;
    }

    /**
     * 检查被选中的日志条目数量
     *
     * @return 被选中的日志条目数
     */
    public int selectedAccessLogs () {
        int count = 0;

        for (AccessLog log : accessLogList) {
            if (log.isSelected()) {
                count++;
            }
        }

        return count;
    }

    /**
     * 选中的所有日志条目数量
     */
    public void selectAllAccessLogs (boolean selecte) {
        for (AccessLog log : accessLogList) {
            log.setSelected(selecte);
        }

        if (accessLogList.size() > 0) {
            observable.notifyObservers();
        }
    }

    /**
     * 将指定的日志信息添加到数据库中保存
     * @param accessLog  待保存的日志信息
     * @return 是否添加成功; true/false
     */
    public boolean addAccessLog (AccessLog accessLog) {
        boolean ok = lockConfigDatabase.addAccessInfo(accessLog);
        if (ok) {
            // 新生成的日志，添加到头部
            accessLogList.add(0, accessLog);
            observable.notifyObservers();
        }
        return ok;
    }

    /**
     * 删除选中的锁定配置
     * @return 移除的数量
     */
    public int deleteSelectedAccessLogs () {
        int count = 0;
        List<AccessLog> removeList = new ArrayList<>();

        // 扫描需要移除的App信息
        for (AccessLog log : accessLogList) {
            if (log.isSelected()) {
                removeList.add(log);
            }
        }

        // 开始移除操作
        for (AccessLog log : removeList) {
            // 从数据库中移除
            boolean ok = lockConfigDatabase.deleteAccessLog(log);
            if (ok) {
                // 只删除内部的照片，存储在相册中的不删除
                if (log.getPhotoPath() != null) {
                    if (log.isPhotoInInternal()) {
                        String path = log.getPhotoPath();
                        new File(path).delete();
                    }
                }

                // 再从缓存队列中移除
                accessLogList.remove(log);

                count++;
            }
        }

        // 通知外界数据发生变化
        if (count > 0) {
            observable.notifyObservers();
        }

        return count;
    }

    /**
     * 获取更多的日志信息，用于延迟加载
     * @param moreCount 期望获取的更多数量
     * @return 实际获取的数量
     */
    public int loadAccessLogsMore (int moreCount) {
        List<AccessLog> accessLogs = lockConfigDatabase.queryAccessLog(accessLogList.size(), moreCount);
        if (accessLogs.size() > 0) {
            accessLogs.addAll(accessLogs);
            observable.notifyObservers();
        }
        return accessLogs.size();
    }

    /**
     * 检查指定的包是否需要锁定
     * @param packageName 待检查的包
     * @return true/false
     */
    public boolean isPackageNeedLock (String packageName) {
        // 获取当前时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(0, 0, 0);

        // 获取当前位置
        LocationService locationService = LocationService.getInstance(context);
        Position position = locationService.getLastPosition().clone();

        // 遍历App列表，查找其是否在其中
        for (AppLockInfo appLockInfo : appLockInfoList) {
            // 如果找到相应的App信息，则进一步判断是否符合锁定条件
            AppInfo appInfo = appLockInfo.getAppInfo();
            if (appInfo.getPackageName().equals(packageName)) {
                // 如果未启动，则不需要锁定
                if (!appLockInfo.isEnable()) {
                    return false;
                }

                // 遍历锁定条件队列，判断是否满足任意锁定条件
                for (BaseLockCondition condition : lockConditionList) {
                    // 未使能，略过
                    if (!condition.isEnable()) {
                        continue;
                    }

                    if (condition instanceof TimeLockCondition) {
                        // 判断时间上是否匹配
                        if (((TimeLockCondition)condition).isMatch(calendar)) {
                            return true;
                        }
                    } else if (condition instanceof GpsLockCondition) {
                        // 判断地理位置是否符合
                        if (((GpsLockCondition)condition).isMatch(position)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
