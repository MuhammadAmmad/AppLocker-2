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
public class LockDatabaseOpenHelper extends SQLiteOpenHelper {
    private static final int currentVersion = 21;
    private static final String dbName = "applocklist.db";

    // 数据库表项配置
    private static final String app_list_tableName = "app_list";
    public static final String app_list_creator =       // App列表
            "CREATE TABLE app_list (" +
            "   id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
            "   package_name        STRING  NOT NULL" +
            ");";
    private static final String mode_list_tableName = "mode_list";
    public static final String mode_list_creator =          // 模式列表
            "CREATE TABLE mode_list (" +
            "   id                  INTEGER PRIMARY KEY AUTOINCREMENT," +
            "   mode_name           STRING  NOT NULL" +
            ");";
    private static final String app_lock_config_tableName = "app_lock_config";
    public static final String app_lock_config_creator =   // App锁定配置
            "CREATE TABLE app_lock_config ( " +
            "   id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
            "   app_id              INTEGER REFERENCES app_list (id) ON DELETE CASCADE NOT NULL," +
            "   mode_id             INTEGER NOT NULL REFERENCES mode_list (id) ON DELETE CASCADE," +
            "   enable              BOOLEAN NOT NULL" +
            ")";

    private static final String time_lock_config_tableName = "time_lock_config";
    public static final String time_lock_config_creator =   // 时间锁定配置
            "CREATE TABLE time_lock_config (" +
            "   id                 INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL," +
            "   start_time         DATETIME NOT NULL," +
            "   end_time           DATETIME NOT NULL," +
            "   day                INTEGER  NOT NULL," +
            "   mode_id            INTEGER  REFERENCES mode_list (id) ON DELETE CASCADE, " +
            "   enable             BOOLEAN  NOT NULL" +
            ");";
    private static final String gps_lock_config_tableName = "gps_lock_config";
    public static final String gps_lock_config_creator =    // gps锁定配置
            "CREATE TABLE gps_lock_config (" +
            "   id                 INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
            "   mode_id            INTEGER  REFERENCES mode_list (id) ON DELETE CASCADE, " +
            "   enable             BOOLEAN NOT NULL" +
            ");";
    private static final String app_log_list_tableName = "app_log_list";
    private static final String app_log_list_creator =      // 锁定日志配置
            "CREATE TABLE app_log_list (" +
            "   id             INTEGER PRIMARY KEY AUTOINCREMENT," +
            "   appName        STRING  NOT NULL," +
            "   packageName    STRING  NOT NULL," +
            "   passErrorCount INTEGER NOT NULL," +
            "   photoPath      STRING," +
            "   resason        STRING  NOT NULL" +
            ");";


    /**
     * 构造函数
     * @param context 上下文
     */
    public LockDatabaseOpenHelper(Context context) {
        super(context, dbName, null, currentVersion);
    }

    /**
     * OnCreate回调
     * @param db 数据库
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(app_list_creator);
        db.execSQL(mode_list_creator);
        db.execSQL(app_lock_config_creator);
        db.execSQL(time_lock_config_creator);
        db.execSQL(gps_lock_config_creator);
        db.execSQL(app_log_list_creator);
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
                db.execSQL("DROP TABLE if exists " + app_list_tableName + " ;");
                db.execSQL("DROP TABLE if exists " + mode_list_tableName + " ;");
                db.execSQL("DROP TABLE if exists " + app_lock_config_tableName + " ;");
                db.execSQL("DROP TABLE if exists " + time_lock_config_tableName + " ;");
                db.execSQL("DROP TABLE if exists " + gps_lock_config_tableName + " ;");
                db.execSQL("DROP TABLE if exists " + app_log_list_tableName + " ;");
                onCreate(db);
                break;
        }
    }
}
