package com.eeontheway.android.applocker.lock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用锁定日志存储访问纪录
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-9
 */
public class LockLogDao {
    private LockDatabase lockListDatabase;
    private SQLiteDatabase db;

    /**
     * 构造函数
     * @param context 上下文
     */
    public LockLogDao(Context context) {
        lockListDatabase = new LockDatabase(context);
        db = lockListDatabase.getReadableDatabase();
    }

    /**
     * 关闭数据库访问器
     */
    public void close () {
        db.close();
    }

    /**
     * 添加一条锁定日志
     * @param logInfo 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean addLogInfo (LockLogInfo logInfo) {
        ContentValues values = new ContentValues();

        // 插入数据库
        values.put(LockDatabase.appNameColumnName, logInfo.getAppName());
        values.put(LockDatabase.packageColumnName, logInfo.getPackageName());
        values.put(LockDatabase.timeColumnName, logInfo.getTime());
        values.put(LockDatabase.passErrorCountColumnName, logInfo.getPasswordErrorCount());
        values.put(LockDatabase.photoPathColumnName, logInfo.getPhotoPath());
        long newRow = db.insert(LockDatabase.appLockLogTableName, null, values);
        if (newRow == -1) {
            return false;
        }

        // 获取其lastRowID，以便于后续访问
        Cursor cursor = db.rawQuery("select last_insert_rowid();", null);
        if (cursor.moveToFirst()) {
            logInfo.setId(cursor.getInt(0));
        }
        cursor.close();
        return true;
    }

    /**
     * 删除一条锁定日志
     * @param logInfo 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean deleteLockLog (LockLogInfo logInfo) {
        db.delete(LockDatabase.appLockLogTableName,
                LockDatabase.idColumnName + " like ?",
                new String[] {"" + logInfo.getId()});
        return true;
    }

    /**
     * 更新一条锁定日志
     * 更新时，将会在日志中的id为搜索条件进行更新
     * @param logInfo 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean updateLogInfo (LockLogInfo logInfo) {
        ContentValues values = new ContentValues();

        values.put(LockDatabase.appNameColumnName, logInfo.getAppName());
        values.put(LockDatabase.packageColumnName, logInfo.getPackageName());
        values.put(LockDatabase.timeColumnName, logInfo.getTime());
        values.put(LockDatabase.passErrorCountColumnName, logInfo.getPasswordErrorCount());
        values.put(LockDatabase.photoPathColumnName, logInfo.getPhotoPath());
        int rows = db.update(LockDatabase.appLockLogTableName,
                values,
                LockDatabase.idColumnName + " like ?",
                new String[] {"" + logInfo.getId()});

        return (rows != 0);
    }

    /**
     * 获取指定包最近一次的锁定日志
     * @param packageName 待查询的包名
     * @return 锁定日志
     */
    public LockLogInfo queryLatestLockerLog (String packageName) {
        Cursor cursor = db.query(LockDatabase.appLockLogTableName,
                new String[] {
                        LockDatabase.idColumnName,
                        LockDatabase.appNameColumnName,
                        LockDatabase.packageColumnName,
                        LockDatabase.timeColumnName,
                        LockDatabase.photoPathColumnName,
                        LockDatabase.passErrorCountColumnName
                },
                LockDatabase.packageColumnName + " like ?",
                new String[] {packageName},
                null,
                null,
                LockDatabase.timeColumnName + " desc");

        if (cursor.moveToFirst()) {
            LockLogInfo logInfo = new LockLogInfo();
            logInfo.setId(cursor.getInt(0));
            logInfo.setAppName(cursor.getString(1));
            logInfo.setPackageName(cursor.getString(2));
            logInfo.setTime(cursor.getString(3));
            logInfo.setPhotoPath(cursor.getString(4));
            logInfo.setPasswordErrorCount(cursor.getInt(5));
            return logInfo;
        }
        return null;
    }

    /**
     * 获取指定包所有的锁定日志
     * @return 锁定日志
     */
    public List<LockLogInfo> queryAllLockerLog () {
        List<LockLogInfo> logInfoList = new ArrayList<>();

        Cursor cursor = db.query(LockDatabase.appLockLogTableName,
                new String[] {
                        LockDatabase.idColumnName,
                        LockDatabase.appNameColumnName,
                        LockDatabase.packageColumnName,
                        LockDatabase.timeColumnName,
                        LockDatabase.photoPathColumnName,
                        LockDatabase.passErrorCountColumnName
                },
                null,
                null,
                null,
                null,
                LockDatabase.timeColumnName + " desc");

        if (cursor.moveToFirst()) {
            do {
                LockLogInfo logInfo = new LockLogInfo();
                logInfo.setId(cursor.getInt(0));
                logInfo.setAppName(cursor.getString(1));
                logInfo.setPackageName(cursor.getString(2));
                logInfo.setTime(cursor.getString(3));
                logInfo.setPhotoPath(cursor.getString(4));
                logInfo.setPasswordErrorCount(cursor.getInt(5));

                logInfoList.add(logInfo);
            } while (cursor.moveToNext());
        }
        return logInfoList;
    }
}
