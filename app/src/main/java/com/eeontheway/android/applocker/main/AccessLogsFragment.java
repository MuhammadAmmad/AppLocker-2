package com.eeontheway.android.applocker.main;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.lock.LockConfigManager;
import com.eeontheway.android.applocker.lock.AccessLog;

import java.util.Observable;
import java.util.Observer;

/**
 * 应用锁访问日志查看列表
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-9
 */
public class AccessLogsFragment extends Fragment {
    private static final int LOAD_MORE_COUNT = 20;

    private RecyclerView rcv_list;
    private Button bt_remove;
    private TextView tv_count;
    private View rl_loading;
    private TextView tv_empty_show;
    private CheckBox cb_select_all;

    private Activity parentActivity;
    private LockConfigManager lockConfigManager;
    private AccessLogsAdapter rcv_adapter;

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
        lockConfigManager = LockConfigManager.getInstance(parentActivity);
        rcv_adapter = new AccessLogsAdapter(parentActivity, lockConfigManager);

        initDataObserver();
    }

    /**
     * Activity的onDestroy回调
     */
    @Override
    public void onDestroy() {
        lockConfigManager.freeInstance();

        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = View.inflate(parentActivity, R.layout.fragment_app_lock_log_list, null);

        // 获取各种View引用
        rcv_list = (RecyclerView) view.findViewById(R.id.rcv_list);
        bt_remove = (Button)view.findViewById(R.id.bt_remove);
        tv_count = (TextView)view.findViewById(R.id.tv_count);
        //rl_loading = view.findViewById(R.id.rl_loading);
        cb_select_all = (CheckBox)view.findViewById(R.id.cb_select_all);
        //tv_empty_show = (TextView)view.findViewById(R.id.tv_empty_show);

        initAnimation();
        initRemoveButton();
        initRcvList();
        initCheckAll();

        updateTotalCountShow(0);
        startLoadingLogList();
        return view;
    }

    /**
     * 配置全选按钮
     */
    private void initCheckAll() {
        // 配置全选及监听事件
        cb_select_all.setChecked(false);
        cb_all_listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean selectAll = cb_select_all.isChecked();
                lockConfigManager.selectAllAccessLogs(selectAll);

                // 更新按钮状态
                updateRemoveButtonState();
            }
        };
        cb_select_all.setOnCheckedChangeListener(cb_all_listener);
    }

    /**
     * 配置显示列表
     */
    private void initRcvList() {
        // 配置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(parentActivity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rcv_list.setLayoutManager(layoutManager);
        rcv_list.setHasFixedSize(true);

        // 配置选中事件
        rcv_adapter.setItemSelectedListener(new AccessLogsAdapter.ItemSelectedListener() {
            @Override
            public void onItemClicked(int pos) {
                AccessLog log = lockConfigManager.getAccessLog(pos);
                showDetailLogDialog(log);
            }

            @Override
            public void onItemLongClicked(int pos) {}

            @Override
            public void onItemSelected(int pos, boolean selected) {}
        });
        rcv_list.setAdapter(rcv_adapter);
    }

    /**
     * 配置删除按钮
     */
    private void initRemoveButton() {
        bt_remove.setVisibility(View.GONE);
        bt_remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSeletedItems();
            }
        });
    }

    /**
     * 配置删除按钮的移入移出动画
     */
    private void initAnimation() {
        // 按钮移入动画
        animationRemoveButtonIn = AnimationUtils.loadAnimation(parentActivity,
                                            R.anim.listview_cleanbutton_bottom_in);
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

        // 按钮移出动画
        animationRemoveButtonOut = AnimationUtils.loadAnimation(parentActivity,
                                                    R.anim.listview_cleanbutton_bottom_out);
        animationRemoveButtonOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                bt_remove.setVisibility(View.GONE);}

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    /**
     * 删除选中项
     */
    private void removeSeletedItems () {
        int count = lockConfigManager.deleteSelectedAccessLogs();

        updateRemoveButtonState();
        updateTotalCountShow(lockConfigManager.getAcessLogsCount());
        Toast.makeText(parentActivity, getString(R.string.already_deleted_count, count),
                                            Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示锁定日志的详细信息
     * @param accessLog 锁定日志信息
     */
    private void showDetailLogDialog (AccessLog accessLog) {
        // 渲染界面
        View view = View.inflate(parentActivity, R.layout.dialog_app_lock_log_list, null);
        ImageView iv_photo = (ImageView)view.findViewById(R.id.iv_photo);
        TextView tv_appName = (TextView)view.findViewById(R.id.tv_appName);
        TextView tv_time = (TextView)view.findViewById(R.id.tv_time);
        TextView tv_error_count = (TextView)view.findViewById(R.id.tv_error_count);

        // 界面设置
        iv_photo.setImageBitmap(rcv_adapter.loadPhoto(accessLog));
        tv_appName.setText(accessLog.getAppName());
        tv_time.setText(accessLog.getTime());
        tv_error_count.setText(getString(R.string.password_err_counter,
                            accessLog.getPasswordErrorCount()));

        // 显示在窗口中
        AlertDialog dialog = new AlertDialog.Builder(parentActivity).setView(view).create();
        dialog.setView(view);
        dialog.setCancelable(true);
        dialog.show();
    }

    /**
     * 刷新删除按钮的状态
     */
    private void updateRemoveButtonState () {
        int count = lockConfigManager.selectedAppCount();
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
        lockConfigManager.loadAccessLogsMore(LOAD_MORE_COUNT);
    }

    /**
     * 各个CheckBox状态改变的监听器
     */
    class SelectCheckBoxChanged implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            AccessLog accessLog = (AccessLog)buttonView.getTag();

            // 切换选中状态,如果有任意一项未选中，则去掉全选状态
            accessLog.setSelected(isChecked);
            if (!accessLog.isSelected()) {
                // 临时取消check监听器，避免状态错误
                cb_select_all.setOnCheckedChangeListener(null);
                cb_select_all.setChecked(false);
                cb_select_all.setOnCheckedChangeListener(cb_all_listener);
            }
            rcv_adapter.notifyDataSetChanged();

            // 更新按钮状态
            updateRemoveButtonState();
        }
    }

    /**
     * 注册数据变化监听器
     */
    private void initDataObserver () {
        lockConfigManager.registerObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                // 通知数据发生改变，刷新界面
                rcv_adapter.notifyDataSetChanged();
            }
        });
    }
}
