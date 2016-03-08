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
public class ConditionDatabase {
    private Context context;
    private SQLiteDatabase db;
    private ConditionDatabaseOpenHelper lockListDatabase;
    private AppInfoManager appInfoManager;

    /**
     * 构造函数
     * @param context
     */
    public ConditionDatabase(Context context) {
        this.context = context;
        lockListDatabase = new ConditionDatabaseOpenHelper(context);
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

        // 扫描所有支持的类型
        List<Integer> typeList = LockConditionFactory.getConditionTypeList();
        for (Integer type : typeList) {
            // 查询数据库对像
            String tableName = LockConditionFactory.createConditionTypeName(type);
            Cursor cursor = db.query(tableName,
                                     LockConditionFactory.createConditionFieldNames(type),
                                     "mode_id=?",
                                     new String[] {modeInfo.getId() + ""},
                                     null, null, null);
            boolean ok = cursor.moveToFirst();
            while (ok) {
                // 转换结果
                ContentValues contentValues = new ContentValues();
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    int dataType = cursor.getType(i);
                    switch (dataType) {
                        case Cursor.FIELD_TYPE_STRING:
                            contentValues.put(cursor.getColumnName(i), cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            contentValues.put(cursor.getColumnName(i), cursor.getFloat(i));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            contentValues.put(cursor.getColumnName(i), cursor.getInt(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            contentValues.put(cursor.getColumnName(i), cursor.getBlob(i));
                            break;
                    }
                }

                // 生成对像并保留到队列中
                BaseLockCondition condition = LockConditionFactory.createCondition(type);
                condition.setMapValues(contentValues);
                configList.add(condition);

                ok = cursor.moveToNext();
            }
            cursor.close();
        }

        return configList;
    }

    /**
     * 删除锁定配置
     * @param config 待删除的配置
     */
    public boolean deleteLockCondition(BaseLockCondition config) {
        int row = db.delete(config.getName(), "id=?", new String[]{config.getId() + ""});
        return (row > 0);
    }

    /**
     * 更新时间锁定信息
     * @param config 待更新的时间锁定信息
     * @return true 成功; false 失败
     */
    public boolean updateLockCondition(BaseLockCondition config) {
        ContentValues values = new ContentValues(config.getMapValues());
        int row = db.update(config.getName(), values, "id=?", new String[] {config.getId() + ""});
        return (row > 0);
    }

    /**
     * 在指定模式下添加一个锁定时机信息
     * @param config 待添加的锁定时机信息
     * @param modeInfo 指定模式
     * @return true 成功; false 失败
     */
    public boolean addLockConditionIntoMode(BaseLockCondition config, LockModeInfo modeInfo) {
        ContentValues values = new ContentValues(config.getMapValues());
        values.put("mode_id", modeInfo.getId());

        long rowId = db.insert(config.getName(), null, values);
        if (rowId != -1) {
            config.setId((int)rowId);
            return true;
        }

        return false;
    }

    /**
     * 添加一条锁定日志
     * @param accessLog 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean addAccessInfo(AccessLog accessLog) {
        ContentValues values = new ContentValues();
        values.put("appName", accessLog.getAppName());
        values.put("packageName", accessLog.getPackageName());
        values.put("passErrorCount", accessLog.getPasswordErrorCount());
        values.put("photoPath", accessLog.getPhotoPath());
        values.put("resason", "unknown");

        long row = db.insert("app_log_list", null, values);
        if (row >= 0) {
            accessLog.setId(row);
            return true;
        }
        return false;
    }

    /**
     * 删除一条锁定日志
     * @param logInfo 锁定日志
     * @return true 操作成功; false 操作失败
     */
    public boolean deleteAccessLog (AccessLog logInfo) {
        int row = db.delete("app_log_list", "id=?", new String[]{logInfo.getId() + ""});
        return (row > 0);
    }

    /**
     * 获取指定包所有的锁定日志
     * @return 锁定日志
     */
    public List<AccessLog> queryAccessLog (int startPos, int count) {
        List<AccessLog> logList = new ArrayList<>();

        String queryString = "select id, appName, packageName, " +
                " passErrorCount, photoPath, resason from app_log_list " +
                " limit ? offset ?;";
        Cursor cursor = db.rawQuery(queryString, new String[] {"" + count, "" + startPos});
        boolean ok = cursor.moveToFirst();
        while (ok) {
            AccessLog info = new AccessLog();
            info.setId(cursor.getInt(0));
            info.setAppName(cursor.getString(1));
            info.setPackageName(cursor.getString(2));
            info.setPasswordErrorCount(cursor.getInt(3));
            info.setPhotoPath(cursor.getString(4));
            logList.add(info);

            ok = cursor.moveToNext();
        }
        cursor.close();
        return logList;
    }
}
