package com.eeontheway.android.applocker.lock;

import android.content.Context;
import android.content.SharedPreferences;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.app.AppInfo;
import com.eeontheway.android.applocker.locate.LocationService;
import com.eeontheway.android.applocker.locate.Position;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * 锁定配置管理器
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockConfigManager {
    private static final String lastModeIdKey = "lastModeIdKey";
    private static final String LOCK_CONFIG_FILE = "lock_config_pref";

    private Context context;
    private ConditionDatabase conditionDatabase;

    private static LockConfigManager instance;
    private static int instanceCount;

    private LockModeInfo currentLockModeInfo;
    private List<LockModeInfo> lockModeInfoList = new ArrayList<>();
    private List<AppLockInfo> appLockInfoList = new ArrayList<>();
    private List<BaseLockCondition> lockConditionList = new ArrayList<>();
    private List<AccessLog> accessLogList = new ArrayList<>();

    private ConfigObservable observable;
    private boolean observableEnable = true;

    /**
     * 获取初始实例
     *
     * @param context 上下文
     */
    public static LockConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new LockConfigManager(context);
            instance.conditionDatabase.open();
            instance.loadModeList();
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
                instance.conditionDatabase.close();
                instance.observable.deleteObservers();
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
        conditionDatabase = new ConditionDatabase(context);
        observable = new ConfigObservable();
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
     * 取消数据变化监听器
     * @param observer 监听器
     */
    public void unregisterObserver (Observer observer) {
        observable.deleteObserver(observer);
    }

    /**
     * 配置是否使能监听器
     * @param enable 是否使能
     */
    public void setObserverEnable (boolean enable) {
        observableEnable = enable;

        // 手动触发一次
        if (enable) {
            observable.notifyObservers();
        }
    }

    /**
     * 初始化模式列表
     */
    private void loadModeList() {
        // 获取所有的模式列表
        lockModeInfoList = conditionDatabase.queryModeList();
        if (lockModeInfoList.size() == 0) {
            // 如果列表为空，则创建一个缺省项，再切换过去
            currentLockModeInfo = createDefaultMode();
            switchModeInfo(0);
        } else {
            // 如果列表不为空，则检查上一次使能的模式id
            // 如果找到，则切换过去，否则，切换至第0个
            SharedPreferences sp = context.getSharedPreferences(LOCK_CONFIG_FILE, Context.MODE_PRIVATE);
            boolean exist = sp.contains(lastModeIdKey);
            if (exist) {
                // 找到相应id的模式，如果找到，则切换过去
                int id = sp.getInt(lastModeIdKey, 0);
                for (LockModeInfo modeInfo : lockModeInfoList) {
                    if (id == modeInfo.getId()) {
                        switchModeInfo(lockModeInfoList.indexOf(modeInfo));
                        return;
                    }
                }

            }

            // 找不到相应id的项，或者没有相应配置，则切换至模式0
            switchModeInfo(0);
        }
    }

    /**
     * 创建缺省的模式
     */
    private LockModeInfo createDefaultMode () {
        LockModeInfo modeInfo = addModeInfo(context.getString(R.string.default_mode));
        if (modeInfo == null) {
            throw new IllegalStateException("Add default mode failed");
        }
        modeInfo.setEnabled(true);
        return modeInfo;
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
        LockModeInfo modeInfo = conditionDatabase.addModeInfo(modeName);
        if (modeInfo != null) {
            lockModeInfoList.add(modeInfo);
            observable.notifyObservers();
        }
        return modeInfo;
    }

    /**
     * 删除一个模式
     * @param modeInfo 待删除的模式
     * @return 是否删除成功
     */
    public boolean deleteModeInfo(LockModeInfo modeInfo) {
        if (lockModeInfoList.size() > 1) {
            conditionDatabase.deleteModeInfo(modeInfo);
            lockModeInfoList.remove(modeInfo);

            // 删除后，需要切换模式
            switchModeInfo(0);
            return true;
        }

        return false;
    }

    /**
     * 更新模式信息
     *
     * @param modeInfo 待更新的模式信息
     * @return 是否成功 true/false
     */
    public boolean updateModeInfo(LockModeInfo modeInfo) {
        boolean ok = conditionDatabase.updateModeInfo(modeInfo);
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
        // 如果是自己，则不必再切换
        // 否则，完成切换
        LockModeInfo nextMode = lockModeInfoList.get(index);
        if (nextMode == currentLockModeInfo) {
            return;
        }

        // 获取指定模式
        if (currentLockModeInfo != null) {
            currentLockModeInfo.setEnabled(false);
        }

        currentLockModeInfo = lockModeInfoList.get(index);
        currentLockModeInfo.setEnabled(true);

        // 重新加载各项配置
        loadAppInfoList();
        loadLockConditionList();

        // 保存当前模式
        SharedPreferences sp = context.getSharedPreferences(LOCK_CONFIG_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(lastModeIdKey, currentLockModeInfo.getId());
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
     * 选中所有的App
     * @param selected 是否全选所有的App
     */
    public void selectAllApp (boolean selected) {
        for (AppLockInfo lockInfo : appLockInfoList) {
            lockInfo.setSelected(selected);
        }

        observable.notifyObservers();
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
     * @param modeInfo 指定的模式
     */
    public void loadAppInfoList (LockModeInfo modeInfo) {
        appLockInfoList = conditionDatabase.getAppInfoByMode(modeInfo);
        observable.notifyObservers();
    }

    public void loadAppInfoList() {
        loadAppInfoList(currentLockModeInfo);
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
        boolean ok = conditionDatabase.addAppInfoToMode(appLockInfo, lockModeInfo);
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

        ok = conditionDatabase.updateAppInfo(appLockInfo, enable);
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
            boolean ok = conditionDatabase.deleteAppInfo(lockInfo);
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
     * 删除指定的App信息
     * @param positon 待删除的App信息序号
     * @return 成功/失败
     */
    public boolean deleteAppInfo (int positon) {
        AppLockInfo appLockInfo = appLockInfoList.get(positon);
        boolean ok = conditionDatabase.deleteAppInfo(appLockInfo);
        if (ok) {
            appLockInfoList.remove(appLockInfo);
            observable.notifyObservers();
        }

        return ok;
    }

    /**
     * 加载锁定时机信息列表
     */
    public void loadLockConditionList() {
        loadLockConditionList(currentLockModeInfo);
    }

    public void loadLockConditionList(LockModeInfo modeInfo) {
        lockConditionList = conditionDatabase.queryLockCondition(modeInfo);
        observable.notifyObservers();
    }

    /**
     * 删除指定的锁定条件
     * @param positon 待删除的锁定条件序号
     * @return 成功/失败
     */
    public boolean deleteLockCondition (int positon) {
        BaseLockCondition condition = lockConditionList.get(positon);
        boolean ok = conditionDatabase.deleteLockCondition(condition);
        if (ok) {
            lockConditionList.remove(condition);
            observable.notifyObservers();
        }

        return ok;
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
            boolean ok = conditionDatabase.deleteLockCondition(config);
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
     * 选中所有的条件
     */
    public void selectAllCondition (boolean selected) {
        for (BaseLockCondition condition : lockConditionList) {
            condition.setSelected(selected);
        }

        observable.notifyObservers();
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
        boolean ok = conditionDatabase.updateLockCondition(config);
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
        boolean ok = conditionDatabase.addLockConditionIntoMode(config, lockModeInfo);
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
            if (observableEnable) {
                setChanged();
                super.notifyObservers();
            }
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
        boolean ok = conditionDatabase.addAccessInfo(accessLog);
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
            boolean ok = conditionDatabase.deleteAccessLog(log);
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
        List<AccessLog> accessLogs = conditionDatabase.queryAccessLog(accessLogList.size(), moreCount);
        if (accessLogs.size() > 0) {
            accessLogs.addAll(accessLogs);
            observable.notifyObservers();
        }
        return accessLogs.size();
    }

    /**
     * 检查指定的包是否需要锁定
     * @param packageName 待检查的包
     * @param calendar 当前日期
     * @param locateServiceIsOk 定位服务是否正常工作
     * @param position 当前地址
     * @return true/false
     */
    public boolean isPackageNeedLock (String packageName, Calendar calendar,
                                        boolean locateServiceIsOk, Position position) {
        // 遍历App列表，查找其是否在其中
        for (AppLockInfo appLockInfo : appLockInfoList) {
            // 如果找到相应的App信息，则进一步判断是否符合锁定条件
            AppInfo appInfo = appLockInfo.getAppInfo();
            if (appInfo.getPackageName().equals(packageName)) {
                // 如果未启动，则不需要锁定
                if (!appLockInfo.isEnable()) {
                    return false;
                }

                // 遍历锁定条件队列，判断是否满足所有锁定条件
                for (BaseLockCondition condition : lockConditionList) {
                    // 未使能，略过
                    if (!condition.isEnable()) {
                        continue;
                    }

                    if (condition instanceof TimeLockCondition) {
                        // 判断时间上是否匹配
                        if (!((TimeLockCondition)condition).isMatch(calendar)) {
                            return false;
                        }
                    } else if (condition instanceof PositionLockCondition) {
                        // 判断地理位置是否符合
                        if (!((PositionLockCondition)condition).isMatch(position)) {
                            return false;
                        }
                    }
                }

                // 未满足任何条件，视为锁定
                return true;
            }
        }

        return false;
    }
}
