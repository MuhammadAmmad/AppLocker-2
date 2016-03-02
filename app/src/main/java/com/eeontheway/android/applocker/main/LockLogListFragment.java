package com.eeontheway.android.applocker.main;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.lock.LockLogInfo;
import com.eeontheway.android.applocker.lock.LockLogViewInfo;
import com.eeontheway.android.applocker.lock.LockLogDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用锁访问日志查看列表
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-9
 */
public class LockLogListFragment extends Fragment {
    private ListView lv_logs;
    private Button bt_remove;
    private TextView tv_count;
    private View rl_loading;
    private TextView tv_empty_show;
    private CheckBox cb_select_all;

    private Activity parentActivity;
    private List<LockLogViewInfo> logViewInfoList = new ArrayList<>();
    private LockLogDao logDao;
    private BaseAdapter logListAdapter;
    private CompoundButton.OnCheckedChangeListener cb_all_listener;
    private Animation animationRemoveButtonIn;
    private Animation animationRemoveButtonOut;

    /**
     * Activity的onCreate回调
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parentActivity = getActivity();
        logDao = new LockLogDao(parentActivity);
        logListAdapter = createAdapter();

        // 开始加载日志列表
        startLoadingLogList();
    }

    /**
     * Activity的onDestroy回调
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        logDao.close();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = View.inflate(parentActivity, R.layout.fragment_app_lock_log_list, null);

        // 获取各种View引用
        lv_logs = (ListView)view.findViewById(R.id.lv_logs);
        bt_remove = (Button)view.findViewById(R.id.bt_remove);
        tv_count = (TextView)view.findViewById(R.id.tv_count);
        rl_loading = view.findViewById(R.id.rl_loading);
        cb_select_all = (CheckBox)view.findViewById(R.id.cb_select_all);
        tv_empty_show = (TextView)view.findViewById(R.id.tv_empty_show);

        // 配置删除按钮及监听事件
        animationRemoveButtonIn = AnimationUtils.loadAnimation(parentActivity, R.anim.listview_cleanbutton_bottom_in);
        animationRemoveButtonIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                bt_remove.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        animationRemoveButtonOut = AnimationUtils.loadAnimation(parentActivity, R.anim.listview_cleanbutton_bottom_out);
        animationRemoveButtonOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                bt_remove.setVisibility(View.GONE);}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        bt_remove.setVisibility(View.GONE);
        bt_remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 遍历所有纪录，删除选中项
                int totalCount = logViewInfoList.size();
                int deletedCount = 0;
                for (int i = 0, index = 0; i < totalCount; i++) {
                    LockLogViewInfo info = logViewInfoList.get(index);
                    if (info.isSelected()) {
                        // 从文件系统中删除照片
                        removePhoto(info);

                        // 从数据库中删除纪录
                        logDao.deleteLockLog(info.getLogInfo());
                        logViewInfoList.remove(info);
                        deletedCount++;
                    } else {
                        index++;
                    }
                }

                // 通知数据发生改变，刷新界面
                logListAdapter.notifyDataSetChanged();

                updateRemoveButtonState();
                updateTotalCountShow(logViewInfoList.size());
                String message = getString(R.string.already_deleted_count, deletedCount);
                Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 配置日志列表及监听事件
        lv_logs.setEmptyView(tv_empty_show);
        lv_logs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LockLogViewInfo logViewInfo = logViewInfoList.get(position);
                showDetailLogInfoInDialog(logViewInfo);
            }
        });

        // 配置全选及监听事件
        cb_select_all.setChecked(false);
        cb_all_listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean selectAll = cb_select_all.isChecked();
                for (LockLogViewInfo info : logViewInfoList) {
                    info.setSelected(selectAll);
                }
                logListAdapter.notifyDataSetChanged();

                // 更新按钮状态
                updateRemoveButtonState();
            }
        };
        cb_select_all.setOnCheckedChangeListener(cb_all_listener);

        // 初始没有任何项目
        updateTotalCountShow(0);
        return view;
    }

    /**
     * 显示锁定日志的详细信息
     * @param logViewInfo 锁定日志信息
     */
    private void showDetailLogInfoInDialog (LockLogViewInfo logViewInfo) {
        // 渲染界面
        View view = View.inflate(parentActivity, R.layout.dialog_app_lock_log_list, null);
        ImageView iv_photo = (ImageView)view.findViewById(R.id.iv_photo);
        TextView tv_appName = (TextView)view.findViewById(R.id.tv_appName);
        TextView tv_time = (TextView)view.findViewById(R.id.tv_time);
        TextView tv_error_count = (TextView)view.findViewById(R.id.tv_error_count);

        // 界面设置
        LockLogInfo logInfo = logViewInfo.getLogInfo();
        iv_photo.setImageBitmap(loadPhoto(logInfo));
        tv_appName.setText(logInfo.getAppName());
        tv_time.setText(logInfo.getTime());
        tv_error_count.setText(getString(R.string.password_err_counter, logInfo.getPasswordErrorCount()));

        // 显示在窗口中
        AlertDialog dialog = new AlertDialog.Builder(parentActivity).setView(view).create();
        dialog.setView(view);
        dialog.setCancelable(true);
        dialog.show();
    }

    /**
     * 从文件系统中删除指定日志信息对应的照片
     * @param logViewInfo 日志信息
     */
    private void removePhoto (LockLogViewInfo logViewInfo) {
        // 只删除内部的照片，存储在相册中的不删除
        LockLogInfo logInfo = logViewInfo.getLogInfo();
        if (logInfo.getPhotoPath() != null) {
            if (logInfo.isPhotoInInternal()) {
                String path = logInfo.getPhotoPath();
                new File(path).delete();
            }
        }
    }

    /**
     * 刷新删除按钮的状态
     */
    private void updateRemoveButtonState () {
        // 检查选中计数
        int count = 0;
        for (LockLogViewInfo info : logViewInfoList) {
            if (info.isSelected()) {
                count++;
            }
        }

        if (count > 0) {
            // 如果不可见且未播放动画，则播放动画显示移入按钮
            if ((bt_remove.getVisibility() != View.VISIBLE)) {
                bt_remove.startAnimation(animationRemoveButtonIn);
            }

            bt_remove.setText(getString(R.string.total_delete_count, count));
        } else {
            // 如果可见且未播放动画，则播放动画移除删除按钮
            if ((bt_remove.getVisibility() != View.GONE)) {
                bt_remove.startAnimation(animationRemoveButtonOut);
            }

            bt_remove.setText(getString(R.string.delete));
        }
    }

    /**
     * 更新总的纪录条目数显示
     * @param count 总的条目纪录数
     */
    private void updateTotalCountShow (int count) {
        tv_count.setText(getString(R.string.total_applock_log_count, count));
    }

    /**
     * 开始加载日志列表
     */
    private void startLoadingLogList () {
        // 使用异步任务来加载
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // 查询数据库，获取需要的数据
                List<LockLogInfo> logInfoList= logDao.queryAllLockerLog();

                // 将获取的对像转换成界面处理需要的
                for (LockLogInfo info : logInfoList) {
                    LockLogViewInfo viewInfo = new LockLogViewInfo();
                    viewInfo.setLogInfo(info);

                    logViewInfoList.add(viewInfo);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                // 加载完成以后，才配置适配器给ListView
                lv_logs.setAdapter(logListAdapter);

                rl_loading.setVisibility(View.GONE);
                updateTotalCountShow(logViewInfoList.size());
            }
        }.execute();
    }

    /**
     * 显示锁定日志的Adapter
     */
    protected BaseAdapter createAdapter() {
        return new BaseAdapter() {
            /**
             * 各个CheckBox状态改变的监听器
             */
            class SelectCheckBoxChanged implements CompoundButton.OnCheckedChangeListener {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    LockLogViewInfo logViewInfo = (LockLogViewInfo)buttonView.getTag();

                    // 切换选中状态,如果有任意一项未选中，则去掉全选状态
                    logViewInfo.setSelected(isChecked);
                    if (!logViewInfo.isSelected()) {
                        // 临时取消check监听器，避免状态错误
                        cb_select_all.setOnCheckedChangeListener(null);
                        cb_select_all.setChecked(false);
                        cb_select_all.setOnCheckedChangeListener(cb_all_listener);
                    }
                    logListAdapter.notifyDataSetChanged();

                    // 更新按钮状态
                    updateRemoveButtonState();
                }
            }

            private SelectCheckBoxChanged checkBoxListener = new SelectCheckBoxChanged();

            @Override
            public int getCount() {
                return logViewInfoList.size();
            }

            @Override
            public Object getItem(int position) {
                return logViewInfoList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                class ViewHolder {
                    CheckBox cb_selected;
                    ImageView iv_photo;
                    TextView tv_appName;
                    TextView tv_time;
                    TextView tv_error_count;
                }

                // 重刷UI
                ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(parentActivity).inflate(
                            R.layout.item_app_lock_log_list, null);
                    viewHolder = new ViewHolder();
                    viewHolder.cb_selected = (CheckBox)convertView.findViewById(R.id.cb_selected);
                    viewHolder.cb_selected.setOnCheckedChangeListener(checkBoxListener);
                    viewHolder.iv_photo = (ImageView)convertView.findViewById(R.id.iv_photo);
                    viewHolder.tv_appName = (TextView)convertView.findViewById(R.id.tv_appName);
                    viewHolder.tv_time = (TextView)convertView.findViewById(R.id.tv_time);
                    viewHolder.tv_error_count = (TextView)convertView.findViewById(R.id.tv_error_count);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                // 关联下ViewHolder和对应的数据，以便于在CheckBox的事件监听器中取出对应的数据
                LockLogViewInfo viewInfo = logViewInfoList.get(position);
                viewHolder.cb_selected.setTag(viewInfo);

                // 界面重新设置
                LockLogInfo logInfo = viewInfo.getLogInfo();
                viewHolder.cb_selected.setChecked(viewInfo.isSelected());
                viewHolder.iv_photo.setImageBitmap(loadPhoto(logInfo));
                viewHolder.tv_appName.setText(logInfo.getAppName());
                viewHolder.tv_time.setText(logInfo.getTime());
                viewHolder.tv_error_count.setText(getString(R.string.password_err_counter,
                                                        logInfo.getPasswordErrorCount()));
                return convertView;
            }
        };
    }

    /**
     * 加载拍摄的照片
     * @param logInfo 日志信息
     * @return 拍摄的照片
     */
    private Bitmap loadPhoto (LockLogInfo logInfo) {
        Bitmap bitmap = null;

        if (logInfo.getPhotoPath() != null) {
            if (logInfo.isPhotoInInternal()) {
                // 加载内部图像文件
                bitmap = BitmapFactory.decodeFile(logInfo.getPhotoPath());
            } else {
                // 加载相册中的文件
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(parentActivity.getContentResolver(),
                            Uri.parse(logInfo.getPhotoPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // 如果相册中的图像已经被删除，则加载缺省的图像
                    if (bitmap == null) {
                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.icon_person);
                    }
                }
            }
        }
        return bitmap;
    }
}
