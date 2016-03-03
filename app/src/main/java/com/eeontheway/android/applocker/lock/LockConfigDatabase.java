package com.eeontheway.android.applocker.lock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.eeontheway.android.applocker.app.AppInfo;
import com.eeontheway.android.applocker.app.AppInfoManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用锁定信息存储的数据库
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockConfigDatabase {
    private Context context;
    private SQLiteDatabase db;
    private LockDatabaseOpenHelper lockListDatabase;
    private AppInfoManager appInfoManager;

    /**
     * 构造函数
     * @param context
     */
    public LockConfigDatabase(Context context) {
        this.context = context;
        lockListDatabase = new LockDatabaseOpenHelper(context);
        appInfoManager = new AppInfoManager(context);
    }

    /**
     * 打开数据库
     */
    public void open () {
        db = lockListDatabase.getWritableDatabase();
    }

    /**
     * 关闭数据库访问器
     */
    public void close () {
        db.close();
    }

    /**
     * 获取所有的锁定模式
     * @return 所有模式
     */
    public List<LockModeInfo> queryModeList () {
        List<LockModeInfo> modeList = new ArrayList<>();

        Cursor cursor = db.query("mode_list",
                                    new String[] {"id", "mode_name"},
                                    null, null, null, null, null);
        boolean ok = cursor.moveToFirst();
        while (ok) {
            LockModeInfo info = new LockModeInfo();
            info.setId(cursor.getInt(0));
            info.setName(cursor.getString(1));
            modeList.add(info);

            ok = cursor.moveToNext();
        }
        cursor.close();
        return modeList;
    }

    /**
     * 添加一个新的模式
     * @param modeName 模式名称
     * @return 生成的模式信息
     */
    public LockModeInfo addModeInfo (String modeName) {
        LockModeInfo modeInfo = null;

        ContentValues values = new ContentValues();
        values.put("mode_name", modeName);
        long rowId = db.insert("mode_list", null, values);
        if (rowId > 0) {
            modeInfo = new LockModeInfo();
            modeInfo.setId((int)rowId);
            modeInfo.setName(modeName);
        }
        return modeInfo;
    }

    /**
     * 更新除ID之外的所有Mode信息
     * @param modeInfo 待更新的信息
     * @return true 成功; false 失败
     */
    public boolean updateModeInfo (LockModeInfo modeInfo) {
        ContentValues values = new ContentValues();
        values.put("mode_name", modeInfo.getName());
        int row = db.update("mode_list", values, "id = ?", new String[] {modeInfo.getId() + ""});
        return row > 0;
    }

    /**
     * 删除指定的模式
     * 该模式下的所有App相关配置信息由数据库自动删除，无需编码
     * @param modeInfo 待删除的信息
     */
    public void deleteModeInfo (LockModeInfo modeInfo) {
        db.delete("mode_list", "id = ?", new String[] {modeInfo.getId() + ""});
    }

    /**
     * 获取指定模式下的所有App信息
     * @return 所有的App信息
     */
    public List<AppLockInfo> getAppInfoByMode (LockModeInfo modeInfo) {
        List<AppLockInfo> appList = new ArrayList<>();
        String queryString = "select config.id, config.enable, app.package_name\n" +
                            "from app_lock_config as config inner join app_list as app\n" +
                            "where (config.app_id = app.id) & (config.mode_id = ?);";

        Cursor cursor = db.rawQuery(queryString, new String[] {"" + modeInfo.getId()});
        boolean ok = cursor.moveToFirst();
        while (ok) {
            AppInfo appInfo = appInfoManager.querySimpleAppInfo(cursor.getString(2));
            if (appInfo != null) {
                AppLockInfo info = new AppLockInfo();
                info.setId(cursor.getInt(0));
                info.setEnable(cursor.getInt(1) > 0 ? true : false);
                info.setAppInfo(appInfo);
                info.setModeInfo(modeInfo);
                appList.add(info);
            }

            ok = cursor.moveToNext();
        }
        cursor.close();
        return appList;
    }

    /**
     * 在指定模式下添加一个App锁定信息
     * @param appInfo 待添加的App信息，部分信息在插入过程中会自动完善
     * @param modeInfo 指定模式
     * @return true 成功; false 失败
     */
    public boolean addAppInfoToMode (AppLockInfo appInfo, LockModeInfo modeInfo) {
        // 插入新项至app_list表
        String packageName = appInfo.getAppInfo().getPackageName();
        ContentValues values = new ContentValues();
        values.put("package_name", packageName);
        long rowId = db.insert("app_list", null, values);
        if (rowId > 0) {
            // 表中之前没有该App，获取新插入的id
            appInfo.setId((int)rowId);
        } else {
            // 表中已有该App项，查询其ID
            Cursor cursor = db.query("app_list", new String[]{"id"}, "package_name=?",
                                        new String[]{packageName}, null, null, null);
            if (cursor.moveToFirst()) {
                appInfo.setId(cursor.getInt(0));
            } else {
                return false;
            }
        }

        // 插入新项至app_lock_config表
        values.clear();
        values.put("app_id", appInfo.getId());
        values.put("mode_id", modeInfo.getId());
        values.put("enable", appInfo.isEnable() ? 1 : 0);
        rowId = db.insert("app_lock_config", null, values);

        return (rowId == -1) ? false : true;
    }

    /**
     * 更新App信息（锁定状态）
     * @param appInfo 待更新的App信息
     * @param enable 是否锁定
     * @return true 成功; false 失败
     */
    public boolean updateAppInfo (AppLockInfo appInfo, boolean enable) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("enable", enable);

        int row = db.update("app_lock_config", contentValues, "app_id=?",
                            new String[]{appInfo.getId() + ""});
        return (row > 0);
    }

    /**
     * 删除App信息
     * 所有依赖app_list的表项将会自动由数据库删除，无需编码
     * @param appInfo 待删除的信息
     */
    public boolean deleteAppInfo (AppLockInfo appInfo) {
        int row = db.delete("app_lock_config", "id=?", new String[]{appInfo.getId() + ""});
        return (row > 0);
    }

    /**
     * 获取指定模式所有的锁定时机信息
     * @param modeInfo 指定的模式
     * @return 时间锁定信息配置列表
     */
    public List<BaseLockCondition> queryLockCondition(LockModeInfo modeInfo) {
        List<BaseLockCondition> configList = new ArrayList<>();

        // 查找时间配置
        Cursor cursor = db.query("time_lock_config",
                    new String[]{"id", "start_time", "end_time", "day", "enable" },
                    "mode_id=?",
                    new String[] {modeInfo.getId() + ""}, null, null, null);
        boolean ok = cursor.moveToFirst();
        while (ok) {
            TimeLockCondition info = new TimeLockCondition();
            info.setId(cursor.getInt(0));
            info.setStartTime(cursor.getString(1));
            info.setEndTime(cursor.getString(2));
            info.setDay(cursor.getInt(3));
            info.setEnable(cursor.getInt(4) > 0);
            configList.add(info);

            ok = cursor.moveToNext();
        }
        cursor.close();

        // 查找gps配置信息
        cursor = db.query("gps_lock_config",
                new String[]{"id", "enable" },
                "mode_id=?",
                new String[] {modeInfo.getId() + ""}, null, null, null);
        ok = cursor.moveToFirst();
        while (ok) {
            TimeLockCondition info = new TimeLockCondition();
            info.setId(cursor.getInt(0));
            info.setEnable(cursor.getInt(3) > 0);
            configList.add(info);

            ok = cursor.moveToNext();
        }
        cursor.close();

        return configList;
    }

    /**
     * 删除锁定配置
     * @param config 待删除的配置
     */
    public boolean deleteLockCondition(BaseLockCondition config) {
        String tableName;
        if (config instanceof TimeLockCondition) {
            tableName = "time_lock_config";
        } else {
            tableName = "gps_lock_config";
        }

        int row = db.delete(tableName, "id=?", new String[]{config.getId() + ""});
        return (row > 0);
    }

    /**
     * 更新时间锁定信息
     * @param config 待更新的时间锁定信息
     * @return true 成功; false 失败
     */
    public boolean updateLockCondition(BaseLockCondition config, BaseLockCondition newConfig) {
        if (config instanceof TimeLockCondition) {
            TimeLockCondition timeLockCondition = (TimeLockCondition)newConfig;

            ContentValues values = new ContentValues();
            values.put("start_time", timeLockCondition.getStartTime());
            values.put("end_time", timeLockCondition.getEndTime());
            values.put("day", timeLockCondition.getDay());
            values.put("enable", timeLockCondition.isEnable());
            int row = db.update("time_lock_config", values, "id=?", new String[] {config.getId() + ""});
            return (row > 0);
        }

        return true;
    }


    /**
     * 在指定模式下添加一个锁定时机信息
     * @param config 待添加的锁定时机信息
     * @param modeInfo 指定模式
     * @return true 成功; false 失败
     */
    public boolean addLockConditionIntoMode(BaseLockCondition config, LockModeInfo modeInfo) {
        if (config instanceof TimeLockCondition) {
            TimeLockCondition timeLockCondition = (TimeLockCondition)config;

            ContentValues values = new ContentValues();
            values.put("start_time", timeLockCondition.getStartTime());
            values.put("end_time", timeLockCondition.getEndTime());
            values.put("day", timeLockCondition.getDay());
            values.put("enable", timeLockCondition.isEnable());
            values.put("mode_id", modeInfo.getId());
            long rowId = db.insert("time_lock_config", null, values);
            if (rowId != -1) {
                config.setId((int)rowId);
                return true;
            }
        }

        return false;
    }

    /**
     * 获取指定模式所有的GPS锁定信息
     * @param modeInfo 指定的模式
     * @return GPS锁定信息配置列表
     */
    public List<GpsLockCondition> queryGpsConfigInfo (LockModeInfo modeInfo) {
        List<GpsLockCondition> configList = new ArrayList<>();
        String queryString = "select id, enable" +
                " from gps_lock_config" +
                " where id = ?;";

        Cursor cursor = db.rawQuery(queryString, new String[] {"" + modeInfo.getId()});
        boolean ok = cursor.moveToFirst();
        while (ok) {
            GpsLockCondition info = new GpsLockCondition();
            info.setId(cursor.getInt(0));
            info.setEnable(cursor.getInt(1) > 0); ;
            configList.add(info);

            ok = cursor.moveToNext();
        }
        cursor.close();
        return configList;
    }


    /**
     * 在指定模式下添加一个GPS锁定配置
     * @param config 待添加的GPS锁定信息，部分信息在插入过程中会自动完善
     * @param modeInfo 指定模式
     * @return true 成功; false 失败
     */
    public boolean addGpsConfigIntoToMode (GpsLockCondition config, LockModeInfo modeInfo) {
        String queryString = "insert into gps_lock_config (mode_id, enable)" +
                " values (?, ?);";
        Cursor cursor = db.rawQuery(queryString, new String[] {
                "" + modeInfo.getId(),
                config.isEnable() ? "1" : "0"});
        boolean ok = cursor.moveToFirst();
        cursor.close();
        if (!ok) return false;

        // 获取插入项的ID
        queryString = "select last_insert_rowid() newid";
        cursor = db.rawQuery(queryString, null);
        if (cursor.moveToFirst()) {
            config.setId(cursor.getInt(0));
        }
        cursor.close();

        return true;
    }

    /**
     * 更新GPS锁定信息
     * @param config 待更新的GPS锁定信息
     * @return true 成功; false 失败
     */
    public boolean updateGpsConfigInfo (GpsLockCondition config) {
        String queryString = "update gps_lock_config set enable = ? where id = ?;";
        Cursor cursor = db.rawQuery(queryString, new String[] {
                config.isEnable() ? "1" : "0",
                config.getId() + ""});
        boolean ok = cursor.moveToFirst();
        cursor.close();
        if (!ok) return false;

        return true;
    }

    /**
     * 删除GPS锁定信息
     * @param config GPS锁定信息
     */
    public boolean deleteGpsConfigInfo (GpsLockCondition config) {
        String queryString = "delete from gps_lock_config where id = ?;";
        Cursor cursor = db.rawQuery(queryString, new String[] {"" + config.getId()});
        boolean ok = cursor.moveToFirst();
        cursor.close();
        return ok;
    }

    /**
     * 添加一条锁定日志
     * @param logInfo 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean addLogInfo (LockLogInfo logInfo) {
        String queryString = "insert into lock_log (app_name, package_name, time, location," +
                " photo_path, password_err_cnt )" +
                " values (?, ?, ?, ?, ?, ?);";
        Cursor cursor = db.rawQuery(queryString, new String[] {
                logInfo.getAppName(),
                logInfo.getPackageName(),
                logInfo.getTime(),
                logInfo.getLocation(),
                logInfo.getPhotoPath(),
                "" + logInfo.getPasswordErrorCount()});
        boolean ok = cursor.moveToFirst();
        cursor.close();
        if (!ok) return false;

        // 获取插入项的ID
        queryString = "select last_insert_rowid() newid";
        cursor = db.rawQuery(queryString, null);
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
        String queryString = "delete from lock_log where id = ?;";
        Cursor cursor = db.rawQuery(queryString, new String[] {"" + logInfo.getId()});
        boolean ok = cursor.moveToFirst();
        cursor.close();
        return ok;
    }

    /**
     * 更新一条锁定日志
     * 更新时，将会在日志中的id为搜索条件进行更新
     * @param logInfo 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean updateLogInfo (LockLogInfo logInfo) {
        String queryString = "update gps_lock_config set app_name = ?, package_name = ?," +
                " time = ?, location = ?, photo_path = ?, " +
                " password_err_cnt = ? where id = ?";
        Cursor cursor = db.rawQuery(queryString, new String[] {
                logInfo.getAppName(),
                logInfo.getPackageName(),
                logInfo.getTime(),
                logInfo.getLocation(),
                logInfo.getPhotoPath(),
                "" + logInfo.getPasswordErrorCount()});
        boolean ok = cursor.moveToFirst();
        cursor.close();
        if (!ok) return false;
        return true;
    }

    /**
     * 获取指定包所有的锁定日志
     * @return 锁定日志
     */
    public List<LockLogInfo> queryAllLockerLog (int startPos, int count) {
        List<LockLogInfo> logList = new ArrayList<>();

        String queryString = "select id, app_name, package_name, time, location, " +
                " photo_path, password_err_cnt from lock_log " +
                " set limit ? offset ?;";
        Cursor cursor = db.rawQuery(queryString, new String[] {"" + count, "" + startPos});
        boolean ok = cursor.moveToFirst();
        while (ok) {
            LockLogInfo info = new LockLogInfo();
            info.setId(cursor.getInt(0));
            info.setAppName(cursor.getString(1));
            info.setPackageName(cursor.getString(2));
            info.setLocation(cursor.getString(3));
            info.setPhotoPath(cursor.getString(4));
            info.setPasswordErrorCount(cursor.getInt(5));
            ok = cursor.moveToNext();
        }
        cursor.close();
        return logList;
    }
}
