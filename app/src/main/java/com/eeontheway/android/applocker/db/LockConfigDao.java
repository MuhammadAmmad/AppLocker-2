package com.eeontheway.android.applocker.db;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import com.eeontheway.android.applocker.app.AppInfo;
import com.eeontheway.android.applocker.lock.LockConfigInfo;
import com.eeontheway.android.applocker.lock.LockInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用锁定信息存储的数据库
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockConfigDao {
    private LocalBroadcastManager lm;

    private static final String BroadcastAction = "LockConfigDao.Changed";

    private LockDatabase lockListDatabase;
    private SQLiteDatabase db;

    /**
     * 构造函数
     * @param context 上下文
     */
    public LockConfigDao(Context context) {
        lm = LocalBroadcastManager.getInstance(context);
        lockListDatabase = new LockDatabase(context);
        db = lockListDatabase.getReadableDatabase();
    }

    /**
     * 数据库是否被打开
     */
    public boolean isOpened () {
        return db.isOpen();
    }

    /**
     * 关闭数据库访问器
     */
    public void close () {
        db.close();
    }

    /**
     * 通知外界数据发生变性
     */
    private void notifyDataChanged () {
        Intent intent = new Intent(BroadcastAction);
        lm.sendBroadcast(intent);
    }

    /**
     * 注册一个数据监听器
     * @param receiver 数据监听器
     */
    public void registerDataChangeReveiver (BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter(BroadcastAction);
        lm.registerReceiver(receiver, filter);
    }


    /**
     * 将数据库中的配置列表与实际系统中的应用列表进行同步，删除数据库中无效的项目
     * 注意，不要单独将用户应用或系统应用同步，否则会在同步某种类型的应用配置时删除另一种类型应用配置
     * @return 实际各应用的锁定列表
     */
    public void syncWithAppInfoList (List<LockInfo> lockInfoList) {
        List<LockConfigInfo> configInfoList = queryAllLockInfo();

        // 扫描所有锁定配置信息
        for (LockConfigInfo configInfo : configInfoList) {
            boolean found = false;

            // 以其中各项为依据，在已有的软件列表中查找
            for (LockInfo lockInfo : lockInfoList) {
                AppInfo appInfo = lockInfo.getAppInfo();
                if (configInfo.getPackageName().equals(appInfo.getPackageName())) {
                    // 记录下锁定信息
                    lockInfo.setLocked(configInfo.isLocked());
                    found = true;
                    break;
                }
            }

            // 如果没有找到，则删除数据库中的配置
            if (found == false) {
                deleteLockInfo(configInfo.getPackageName());
            }
        }
    }

    /**
     * 检查指定的包是否待锁定
     * @param packageName 应用的包名
     * @return true 锁定; false 不锁定
     */
    public boolean isPackageLocked (String packageName) {
        boolean locked = false;

        if (isPackageExist(packageName) == false) {
            // 如果包不在配置列表中，添加一条纪录，缺省为不锁定
            addLockInfo(packageName, false);
        } else {
            Cursor cursor = db.query(LockDatabase.appLockListTableName,
                    new String[] {LockDatabase.packageColumnName, LockDatabase.lockColumnName},
                    LockDatabase.packageColumnName + " like ?",
                    new String[] {packageName}, null, null, null);
            if (cursor.moveToFirst()) {
                locked = (cursor.getInt(1) == 1)? true : false;
            }
            cursor.close();
        }

        return locked;
    }

    /**
     * 设置包是否应当锁定
     * @param packageName 应用包名
     */
    public void setPackageLocked (String packageName, boolean lock) {
        if (isPackageExist(packageName)) {
            updateLockInfo(packageName, lock);
        } else {
            addLockInfo(packageName, lock);
        }
    }

    /**
     * 判断包配置是否位于锁定数据库中
     * @param packageName
     * @return true 是; false 否
     */
    public boolean isPackageExist (String packageName) {
        boolean exist = false;
        Cursor cursor = db.query(LockDatabase.appLockListTableName,
                new String[]{LockDatabase.packageColumnName},
                LockDatabase.packageColumnName + " like ?",
                new String[] {packageName}, null, null, null);
        if (cursor.moveToFirst()) {
            exist = true;
        }

        cursor.close();
        return exist;
    }

    /**
     * 添加一条锁定信息纪录
     * @param packageName 需设置锁定信息的包名
     * @param lock 是否要锁定
     * @return true 操作成功; false 操作失败
     */
    public boolean addLockInfo (String packageName, boolean lock) {
        ContentValues values = new ContentValues();
        values.put(LockDatabase.packageColumnName, packageName);
        values.put(LockDatabase.lockColumnName, lock ? 1 : 0);
        long newRow = db.insert(LockDatabase.appLockListTableName, null, values);
        if (newRow == -1) {
            return false;
        }

        // 通知外界数据发生改变
        notifyDataChanged();
        return true;
    }

    /**
     * 删除一条锁定信息纪录
     * @param packageName 需设置锁定信息的包名
     * @return true 操作成功; false 操作失败
     */
    public boolean deleteLockInfo (String packageName) {
        db.delete(
                LockDatabase.appLockListTableName,
                LockDatabase.packageColumnName + " like ?",
                new String[] {packageName});

        // 通知外界数据发生改变
        notifyDataChanged();
        return true;
    }

    /**
     * 更新一条锁定信息纪录
     * @param packageName 需更新锁定设置信息的包名
     * @param lock 是否要锁定
     * @return true 操作成功; false 操作失败
     */
    public boolean updateLockInfo (String packageName, boolean lock) {
        ContentValues values = new ContentValues();
        values.put(LockDatabase.packageColumnName, packageName);
        values.put(LockDatabase.lockColumnName, lock ? 1 : 0);
        int rows = db.update(LockDatabase.appLockListTableName,
                values,
                LockDatabase.packageColumnName + " like ?",
                new String[] {packageName});

        // 通知外界数据发生改变
        notifyDataChanged();
        return (rows != 0);
    }

    /**
     * 获取所有的锁定信息
     * @return 锁定配置列表
     */
    public List<LockConfigInfo> queryAllLockInfo() {
        List<LockConfigInfo> list = new ArrayList<>();

        // 查询数据库
        Cursor cursor = db.query(LockDatabase.appLockListTableName,
                new String[] {LockDatabase.packageColumnName, LockDatabase.lockColumnName},
                null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                LockConfigInfo info = new LockConfigInfo();
                info.setPackageName(cursor.getString(0));
                info.setLocked(cursor.getInt(1) == 1);
                list.add(info);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    /**
     * 检查是否有任意的包需要锁定
     * @return true 是; false 否
     */
    public boolean isAnyPackageNeedLock () {
        Cursor cursor = db.query(LockDatabase.appLockListTableName,
                new String[] {LockDatabase.packageColumnName, LockDatabase.lockColumnName},
                LockDatabase.lockColumnName + " like 1",
                null, null, null, null);
        if (cursor.moveToFirst()) {
            return true;
        }

        cursor.close();
        return false;
    }
}
