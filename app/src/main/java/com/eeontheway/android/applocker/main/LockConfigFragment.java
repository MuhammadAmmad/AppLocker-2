package com.eeontheway.android.applocker.main;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.app.AppInfo;
import com.eeontheway.android.applocker.app.AppInfoManager;
import com.eeontheway.android.applocker.app.BaseAppInfo;
import com.eeontheway.android.applocker.db.LockConfigDao;
import com.eeontheway.android.applocker.lock.LockInfo;
import com.eeontheway.android.applocker.lock.SettingsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用锁的主界面
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class LockConfigFragment extends Fragment {
    private View rl_loading;
    private Activity parentActivity;
    private ExpandableListView el_listview;
    private View lv_header;
    private BaseExpandableListAdapter el_adapter;

    private AppInfoManager appInfoManager;
    private LockConfigDao databaseDao;
    private BroadcastReceiver packageRemoveReceiver;
    private BroadcastReceiver packageInstallReceiver;
    private SettingsManager settingsManager;

    private String locationSD;
    private String locationRom;
    private List<LockInfo> userInfoList = new ArrayList<>();
    private List<LockInfo> systemInfoList = new ArrayList<>();

    /**
     * Activity的OnCreate()回调
     *
     * @param savedInstanceState 之前保存的状态
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parentActivity = getActivity();
        appInfoManager = new AppInfoManager(parentActivity);
        databaseDao = new LockConfigDao(parentActivity);
        settingsManager = SettingsManager.getInstance(parentActivity);
        el_adapter = createAdapter();

        // 获取位置字符串
        locationRom = parentActivity.getString(R.string.rom);
        locationSD = parentActivity.getString(R.string.sdcard);

        // 开始加载各项数据
        registerPackageRemoveListener();
        registerPackageInstallListener();
        startLoadAllAppInfo(AppInfoManager.AppType.ALL_APP);

    }

    /**
     * Activity的onDestroy()回调
     */
    public void onDestroy() {
        removePackageListener();
        databaseDao.close();

        super.onDestroy();
    }

    /**
     * Fragment的onCreateView回调
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = View.inflate(parentActivity, R.layout.fragment_applocker_config, null);

        // 配置顶部头
        lv_header = view.findViewById(R.id.lv_header);
        lv_header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LockLogListActivity.startActivity(parentActivity);
            }
        });

        // 修改进度条
        rl_loading = view.findViewById(R.id.rl_loading);
        if ((systemInfoList.size() > 0) || (userInfoList.size() > 0)) {
            rl_loading.setVisibility(View.GONE);
        }

        // 配置显示列表
        el_listview = (ExpandableListView)view.findViewById(R.id.el_listview);
        el_listview.setAdapter(el_adapter);
        el_listview.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                List<LockInfo> listInfo = (List<LockInfo>)el_adapter.getGroup(groupPosition);

                // 按下后，切换CheckBox状态
                LockInfo info = listInfo.get(childPosition);
                AppInfo appInfo = info.getAppInfo();
                info.setLocked(!info.isLocked());
                databaseDao.setPackageLocked(appInfo.getPackageName(), info.isLocked());

                // 通知数据发生改变，重刷界面，通知用户
                el_adapter.notifyDataSetChanged();
                if (settingsManager.isAlertLockUnlockEnabled()) {
                    String message = appInfo.getName() + " ";
                    message += info.isLocked() ? getString(R.string.locked) : getString(R.string.unlocked);
                    Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });
        return view;
    }

    /**
     * 获取显示列表的Adapter
     */
    protected BaseExpandableListAdapter createAdapter() {
        return new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                return 2;
            }

            @Override
            public int getChildrenCount(int groupPosition) {
                return (groupPosition == 0) ? userInfoList.size() : systemInfoList.size();
            }

            @Override
            public Object getGroup(int groupPosition) {
                return (groupPosition == 0) ? userInfoList : systemInfoList;
            }

            @Override
            public Object getChild(int groupPosition, int childPosition) {
                List<LockInfo> listInfo = (List<LockInfo>)el_adapter.getGroup(groupPosition);
                return listInfo.get(childPosition);
            }

            public String getGroupName (int groupPosition) {
                if (groupPosition == 0) {
                    return getString(R.string.user_software);
                } else {
                    return getString(R.string.system_software);
                }
            }

            @Override
            public long getGroupId(int groupPosition) {
                return 0;
            }

            @Override
            public long getChildId(int groupPosition, int childPosition) {
                return 0;
            }

            public long getChildSelectedCount (int groupPosition) {
                long count = 0;

                List<LockInfo> listInfo = (List<LockInfo>)getGroup(groupPosition);
                for (LockInfo info : listInfo) {
                    if (info.isLocked()) {
                        count++;
                    }
                }

                return count;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                class ViewHolder {
                    TextView tv_name;
                    TextView tv_childcount;
                }
                // 重刷UI
                ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(parentActivity).inflate(
                            R.layout.item_group_applocker_list, null);
                    viewHolder = new ViewHolder();
                    viewHolder.tv_name = (TextView)convertView.findViewById(R.id.tv_name);
                    viewHolder.tv_childcount = (TextView)convertView.findViewById(R.id.tv_childcount);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                // 界面重新设置
                viewHolder.tv_name.setText(getGroupName(groupPosition));
                viewHolder.tv_childcount.setText(getString(R.string.child_selected_count,
                        getChildSelectedCount(groupPosition), getChildrenCount(groupPosition)));
                return convertView;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
                class ViewHolder {
                    ImageView iv_icon;
                    CheckBox cb_lock;
                    TextView tv_name;
                    TextView tv_versionName;
                    TextView tv_location;
                }

                // 重刷UI
                ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(parentActivity).inflate(
                            R.layout.item_applocker_list, null);
                    viewHolder = new ViewHolder();
                    viewHolder.cb_lock = (CheckBox)convertView.findViewById(R.id.cb_lock);
                    viewHolder.iv_icon = (ImageView)convertView.findViewById(R.id.iv_icon);
                    viewHolder.tv_name = (TextView)convertView.findViewById(R.id.tv_name);
                    viewHolder.tv_versionName = (TextView)convertView.findViewById(R.id.tv_version);
                    viewHolder.tv_location = (TextView)convertView.findViewById(R.id.tv_location);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                // 界面重新设置
                LockInfo lockerInfo = (LockInfo)getChild(groupPosition, childPosition);
                AppInfo appInfo = lockerInfo.getAppInfo();
                viewHolder.cb_lock.setChecked(lockerInfo.isLocked());
                viewHolder.iv_icon.setImageDrawable(appInfo.getIcon());
                viewHolder.tv_name.setText(appInfo.getName());
                String versionName = getString(R.string.version, appInfo.getVersionName());
                viewHolder.tv_versionName.setText(versionName);
                String location = getString(R.string.location, appInfo.isInRom() ? locationRom : locationSD);
                viewHolder.tv_location.setText(location);
                return convertView;
            }

            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }
        };
    }

    /**
     * Fragment的onDestroyView回调
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    /**
     * 注册安装包安装的监听器
     */
    private void registerPackageInstallListener() {
        // 注册安装包移除监听器
        packageInstallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();
                databaseDao.addLockInfo(packageName, false);

                // 为界面添加数据，以刷新界面
                AppInfo appInfo = appInfoManager.queryAppInfo(packageName);
                LockInfo lockInfo = new LockInfo();
                lockInfo.setAppInfo(appInfo);
                lockInfo.setLocked(false);
                if (appInfo.isUserApp()) {
                    userInfoList.add(lockInfo);
                } else {
                    systemInfoList.add(lockInfo);
                }
                el_adapter.notifyDataSetChanged();
            }
        };

        // 注册监听器
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        parentActivity.registerReceiver(packageInstallReceiver, intentFilter);
    }


    /**
     * 注册安装包移除的监听器
     * 该监听器的用处是当包移除时，同时删除数据库中的配置，避免用户再次安装时用的老数据
     */
    private void registerPackageRemoveListener() {
        // 注册安装包移除监听器
        packageRemoveReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String packageName = intent.getData().getSchemeSpecificPart();
                databaseDao.deleteLockInfo(packageName);

                // 有安装包移除时，刷新UI
                for (LockInfo info : userInfoList) {
                    AppInfo appInfo = info.getAppInfo();
                    if (appInfo.getPackageName().equals(packageName)) {
                        userInfoList.remove(info);
                        el_adapter.notifyDataSetChanged();
                        return;
                    }
                }

                for (LockInfo info : systemInfoList) {
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
        parentActivity.registerReceiver(packageRemoveReceiver, intentFilter);
    }


    /**
     * 取消安装包移除的监听器
     */
    private void removePackageListener() {
        parentActivity.unregisterReceiver(packageRemoveReceiver);
        parentActivity.unregisterReceiver(packageInstallReceiver);
    }

    /**
     * 加载所有的App信息列表
     * 由于这是一个比较耗时的操作，所以要延后处理
     */
    protected void startLoadAllAppInfo(final AppInfoManager.AppType type) {
        // 加载所有的App信息
        new AsyncTask<Void, Void, List<BaseAppInfo>>() {
            @Override
            protected List<BaseAppInfo> doInBackground(Void... params) {
                // 获取所有的APP信息
                List<BaseAppInfo> list = appInfoManager.queryAllAppInfo(type);
                return list;
            }

            @Override
            protected void onPostExecute(List<BaseAppInfo> appInfoList) {
                List<LockInfo> lockerInfoList = new ArrayList<>();

                // 转换为AppInfo
                for (BaseAppInfo info : appInfoList) {
                    // 不允许锁定自己
                    if (info.getPackageName().equals(parentActivity.getPackageName())) {
                        continue;
                    }

                    LockInfo lockerInfo = new LockInfo();
                    lockerInfo.setAppInfo((AppInfo)info);
                    lockerInfoList.add(lockerInfo);

                    // 根据类型，添加到相应的队列里
                    if (info.isUserApp()) {
                        userInfoList.add(lockerInfo);
                    } else {
                        systemInfoList.add(lockerInfo);
                    }
                }

                // 然后与数据库的配置进行同步
                databaseDao.syncWithAppInfoList(lockerInfoList);

                // 隐藏进度条，展开第0组
                el_adapter.notifyDataSetChanged();
                rl_loading.setVisibility(View.GONE);
                el_listview.expandGroup(0);
            }
        }.execute();
    }
}
