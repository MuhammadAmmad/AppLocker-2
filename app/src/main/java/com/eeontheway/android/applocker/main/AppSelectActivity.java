package com.eeontheway.android.applocker.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.app.AppInfo;
import com.eeontheway.android.applocker.app.AppInfoManager;
import com.eeontheway.android.applocker.app.BaseAppInfo;
import com.eeontheway.android.applocker.lock.AppLockInfo;
import com.eeontheway.android.applocker.lock.LockConfigManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用锁的主界面
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class AppSelectActivity extends AppCompatActivity {
    private View rl_loading;
    private ExpandableListView el_listview;
    private FloatingActionButton fab_add;
    private AppSelectAdapter el_adapter;

    private AppInfoManager appInfoManager;
    private LockConfigManager lockConfigManager;
    private BroadcastReceiver packageRemoveReceiver;
    private BroadcastReceiver packageInstallReceiver;

    private List<AppSelectInfo> userInfoList = new ArrayList<>();
    private List<AppSelectInfo> systemInfoList = new ArrayList<>();

    /**
     * 启动Activity
     */
    public static void start(Fragment fragment) {
        Intent intent = new Intent(fragment.getActivity(), AppSelectActivity.class);
        fragment.startActivity(intent);
    }

    /**
     * Activity的OnCreate()回调
     *
     * @param savedInstanceState 之前保存的状态
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_applocker_config);

        appInfoManager = new AppInfoManager(this);
        lockConfigManager = LockConfigManager.getInstance(this);
        el_adapter = createAdapter();

        registerPackageRemoveListener();
        registerPackageInstallListener();

        setTitle(R.string.select_app);
        initView();
        initToolBar();

        startLoadAllAppInfo(AppInfoManager.AppType.ALL_APP);
    }

    /**
     * Activity的onDestroy()回调
     */
    public void onDestroy() {
        lockConfigManager.freeInstance();
        removePackageListener();

        super.onDestroy();
    }

    /**
     * 初始化UI
     */
    public void initView() {
        // 修改进度条
        rl_loading = findViewById(R.id.rl_loading);

        // 配置显示列表
        el_listview = (ExpandableListView)findViewById(R.id.el_listview);
        el_listview.setAdapter(el_adapter);
        el_listview.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                                    int groupPosition, int childPosition, long id) {
                List<AppSelectInfo> listInfo = (List<AppSelectInfo>)el_adapter.getGroup(groupPosition);

                // 按下后，切换CheckBox状态
                AppSelectInfo info = listInfo.get(childPosition);
                info.setSelected(!info.isSelected());
                el_adapter.notifyDataSetChanged();

                // 根据是否有任何项选中，配置fab的按钮状态
                if (info.isSelected()) {
                    fab_add.setEnabled(true);
                } else {
                    long count = el_adapter.getChildSelectedCount(0);
                    count += el_adapter.getChildSelectedCount(1);
                    fab_add.setEnabled(count > 0);
                }
                return false;
            }
        });

        fab_add = (FloatingActionButton) findViewById(R.id.fab_add);
        fab_add.setEnabled(false);
        fab_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(saveSelectedApps());
                finish();
            }
        });
    }

    /**
     * 初始化ToolBar
     */
    private void initToolBar() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.tl_header);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }


    /**
     * Activiy的onCreateOptionMenu回调
     *
     * @param menu 创建的菜单
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 处理返回按钮按下的响应
     *
     * @param item 被按下的项
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(0);
                finish();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * 返回键处理
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(0);
    }

    /**
     * 保存已经选择的App
     * @return 总共选择的App总数
     */
    private int saveSelectedApps () {
        int count = 0;

        // 链表合并
        List<List<AppSelectInfo>> totalList = new ArrayList<>();
        totalList.add(userInfoList);
        totalList.add(systemInfoList);

        for (List<AppSelectInfo> infoList : totalList) {
            for (AppSelectInfo appSelectInfo : infoList) {
                if (appSelectInfo.isSelected()) {
                    AppLockInfo appLockInfo = new AppLockInfo();
                    appLockInfo.setAppInfo(appSelectInfo.getAppInfo());

                    // 保存至数据库中
                    boolean ok = lockConfigManager.addAppInfoToMode(appLockInfo);
                    if (ok) count++;
                }
            }
        }

        return count;
    }

    /**
     * 获取显示列表的Adapter
     */
    protected AppSelectAdapter createAdapter() {
        return new AppSelectAdapter(this, userInfoList, systemInfoList);
    }

    /**
     * 加载所有的App信息列表
     * 由于这是一个比较耗时的操作，所以要延后处理
     */
    protected void startLoadAllAppInfo(final AppInfoManager.AppType type) {
        // 加载所有的App信息
        new AsyncTask<Void, Void, List<BaseAppInfo>>() {
            @Override
            protected void onPreExecute() {
                rl_loading.setVisibility(View.VISIBLE);
            }

            @Override
            protected List<BaseAppInfo> doInBackground(Void... params) {
                List<BaseAppInfo> list = appInfoManager.queryAllAppInfo(type);
                return list;
            }

            @Override
            protected void onPostExecute(List<BaseAppInfo> appInfoList) {
                List<AppSelectInfo> lockerInfoList = new ArrayList<>();

                // 转换为AppInfo
                for (BaseAppInfo info : appInfoList) {
                    // 不允许锁定自己
                    String packageName = info.getPackageName();
                    if (packageName.equals(getPackageName())) {
                        continue;
                    }

                    // 过滤掉已经在锁定队列中的
                    if (lockConfigManager.isPackageInAppLockList(packageName)) {
                        continue;
                    }

                    AppSelectInfo lockerInfo = new AppSelectInfo();
                    lockerInfo.setAppInfo((AppInfo)info);
                    lockerInfoList.add(lockerInfo);

                    // 根据类型，添加到相应的队列里
                    if (info.isUserApp()) {
                        userInfoList.add(lockerInfo);
                    } else {
                        systemInfoList.add(lockerInfo);
                    }
                }

                // 隐藏进度条，展开第0组
                el_adapter.notifyDataSetChanged();
                rl_loading.setVisibility(View.GONE);
                el_listview.expandGroup(0);
            }
        }.execute();
    }

    /**
     * 注册安装包安装的监听器
     */
    private void registerPackageInstallListener() {
        packageInstallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();

                AppInfo appInfo = appInfoManager.querySimpleAppInfo(packageName);
                AppSelectInfo appSelectInfo = new AppSelectInfo();
                appSelectInfo.setAppInfo(appInfo);
                appSelectInfo.setSelected(false);
                if (appInfo.isUserApp()) {
                    userInfoList.add(appSelectInfo);
                } else {
                    systemInfoList.add(appSelectInfo);
                }
                el_adapter.notifyDataSetChanged();
            }
        };

        // 注册监听器
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(packageInstallReceiver, intentFilter);
    }

    /**
     * 注册安装包移除的监听器
     * 该监听器的用处是当包移除时，同时删除数据库中的配置，避免用户再次安装时用的老数据
     */
    private void registerPackageRemoveListener() {
        packageRemoveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();

                for (AppSelectInfo info : userInfoList) {
                    AppInfo appInfo = info.getAppInfo();
                    if (appInfo.getPackageName().equals(packageName)) {
                        userInfoList.remove(info);
                        el_adapter.notifyDataSetChanged();
                        return;
                    }
                }

                for (AppSelectInfo info : systemInfoList) {
                    AppInfo appInfo = info.getAppInfo();
                    if (appInfo.getPackageName().equals(packageName)) {
                        systemInfoList.remove(info);
                        el_adapter.notifyDataSetChanged();
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
     * 取消安装包移除的监听器
     */
    private void removePackageListener() {
        unregisterReceiver(packageRemoveReceiver);
        unregisterReceiver(packageInstallReceiver);
    }
}
