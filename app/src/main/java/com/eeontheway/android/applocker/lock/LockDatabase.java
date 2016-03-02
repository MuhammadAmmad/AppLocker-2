package com.eeontheway.android.applocker.lock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 应用锁定数据库打开器
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockDatabase extends SQLiteOpenHelper {
    private static final int currentVersion = 14;
    private static final String dbName = "applocklist.db";

    // 数据库表项配置
    public static final String appLockListTableName = "appLockList";
    public static final String packageColumnName = "packageName";
    public static final String lockColumnName = "lock";
    private static final String appLockListTableCreator =
            "CREATE  TABLE appLockList (id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL ," +
            "packageName TEXT unique NOT NULL, lock INTEGER NOT NULL);";

    // 数据库表项配置
    public static final String appLockLogTableName = "appLockLog";
    public static final String idColumnName = "id";
    public static final String appNameColumnName = "appName";
    public static final String timeColumnName = "time";
    public static final String photoPathColumnName = "photoPath";
    public static final String passErrorCountColumnName = "passErrorCount";
    private static final String appLockLogTableCreator =
            "CREATE  TABLE appLockLog (id INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL ," +
                    "appName TEXT NOT NULL," +
                    "packageName TEXT NOT NULL, time TEXT NOT NULL," +
                    "passErrorCount INTEGER NOT NULL, photoPath TEXT);";


    /**
     * 构造函数
     * @param context 上下文
     */
    public LockDatabase(Context context) {
        super(context, dbName, null, currentVersion);
    }

    /**
     * OnCreate回调
     * @param db 数据库
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建黑名单表
        db.execSQL(appLockListTableCreator);
        db.execSQL(appLockLogTableCreator);
    }

    /**
     * onUpgrade回调
     * @param db 数据库
     * @param oldVersion 老版本
     * @param newVersion 新版本
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            default:
                db.execSQL("DROP TABLE if exists " + appLockListTableName + " ;");
                db.execSQL("DROP TABLE if exists " + appLockLogTableName + " ;");
                onCreate(db);
                break;
        }
    }
}
