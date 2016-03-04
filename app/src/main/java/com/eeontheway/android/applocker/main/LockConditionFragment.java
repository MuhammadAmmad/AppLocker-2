package com.eeontheway.android.applocker.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.lock.BaseLockCondition;
import com.eeontheway.android.applocker.lock.GpsLockCondition;
import com.eeontheway.android.applocker.lock.LockConfigManager;
import com.eeontheway.android.applocker.lock.TimeLockCondition;

import java.util.Observable;
import java.util.Observer;

/**
 * 配置何时锁定的Fragment
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class LockConditionFragment extends Fragment {
    private static final int REQUEST_EDIT_TIME = 0;
    private static final int REQUEST_EDIT_POS = 1;

    private RecyclerView rcv_list;
    private TextView tv_empty;
    private Button bt_del;
    private FloatingActionButton fab_add;

    private LockConditionAdapter lockListAdapter;
    private Activity parentActivity;
    private LockConfigManager lockConfigManager;

    /**
     * Fragment的OnCreate()回调
     * @param savedInstanceState 之前保存的状态
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parentActivity = getActivity();
        lockConfigManager = LockConfigManager.getInstance(parentActivity);

        initAdapter();
        initDataObserver();
    }

    /**
     * Fragment的onCreateView()回调
     * @param savedInstanceState 之前保存的状态
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lock_time_config, container, false);

        // 配置删除按钮
        bt_del = (Button) view.findViewById(R.id.bt_del);
        bt_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockListAdapter.removeSelectedLockConfig();
                Toast.makeText(parentActivity, R.string.deleteOk, Toast.LENGTH_SHORT).show();
            }
        });

        // 初始化列表
        rcv_list = (RecyclerView) view.findViewById(R.id.rcv_list);
        rcv_list.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(parentActivity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rcv_list.setLayoutManager(layoutManager);
        rcv_list.setAdapter(lockListAdapter);

        // 初始化空白显示页
        tv_empty = (TextView) view.findViewById(R.id.tv_empty);
        if (lockListAdapter.getItemCount() == 0) {
            tv_empty.setVisibility(View.VISIBLE);
        }

        // 初始化浮动按钮
        fab_add = (FloatingActionButton) view.findViewById(R.id.fab_add);
        fab_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateLockTimeConfigDialog();
            }
        });

        return view;
    }

    /**
     * Fragment的onDestroy()回调
     */
    @Override
    public void onDestroy() {
        lockConfigManager.freeInstance();
        super.onDestroy();
    }

    /**
     * 获取Activity的处理结果
     * @param requestCode 请求码
     * @param resultCode 处理结果
     * @param data 数据
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 用户取消了编辑
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        // 按请求码不同做不同处理
        boolean isEditMode;
        BaseLockCondition condition;
        switch (requestCode) {
            case REQUEST_EDIT_TIME:     // 时间编辑返回
                isEditMode = TimeConditionEditActivity.isEditMode(data);
                condition = TimeConditionEditActivity.getCondition(data);
                break;
            case REQUEST_EDIT_POS:
                isEditMode = LocationConditionEditActivity.isEditMode(data);
                condition = LocationConditionEditActivity.getCondition(data);
                break;
            default:
                return;
        }

        if (isEditMode) {
            // 使用配置复制
            lockConfigManager.updateLockCondition(condition);
        } else {
            // 新建数据
            lockConfigManager.addLockConditionIntoMode(condition);
        }
    }

    /**
     * 显示创建模式配置的对话框
     */
    private void showCreateLockTimeConfigDialog() {
        PopupMenu popupMenu = new PopupMenu(parentActivity, fab_add, Gravity.TOP);
        popupMenu.inflate(R.menu.menu_add_lock_time_config);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_add_time_config:
                        TimeConditionEditActivity.start(LockConditionFragment.this, REQUEST_EDIT_TIME);
                        return true;
                    case R.id.action_add_postion_config:
                        LocationConditionEditActivity.start(LockConditionFragment.this, REQUEST_EDIT_POS);
                        return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * 初始化适配器
     */
    private void initAdapter () {
        lockListAdapter = new LockConditionAdapter(parentActivity, lockConfigManager);
        lockListAdapter.setItemSelectedListener(new LockConditionAdapter.ItemSelectedListener() {
            @Override
            public void onItemSelected(int pos, boolean selected) {
                if (selected) {
                    if (bt_del.getVisibility() != View.VISIBLE) {
                        // 显示删除按钮
                        Animation animation = AnimationUtils.loadAnimation(parentActivity,
                                R.anim.listview_cleanbutton_bottom_in);
                        animation.setFillAfter(true);
                        bt_del.setVisibility(View.VISIBLE);
                        bt_del.startAnimation(animation);
                    }
                } else if (!lockListAdapter.isAnyItemSelected()){
                    if (bt_del.getVisibility() != View.GONE) {
                        // 隐藏删除按钮
                        Animation animation = AnimationUtils.loadAnimation(parentActivity,
                                R.anim.listview_cleanbutton_bottom_in);
                        animation.setFillAfter(true);
                        bt_del.setVisibility(View.GONE);
                        bt_del.startAnimation(animation);
                    }
                }
            }

            @Override
            public void onItemClicked(int pos) {
                // 选中编辑
                BaseLockCondition config = lockConfigManager.getLockCondition(pos);
                if (config instanceof TimeLockCondition) {
                    TimeConditionEditActivity.start(LockConditionFragment.this,
                                        (TimeLockCondition)config, REQUEST_EDIT_TIME);
                } else if (config instanceof GpsLockCondition) {
                    LocationConditionEditActivity.start(LockConditionFragment.this,
                                        (GpsLockCondition)config, REQUEST_EDIT_POS);
                }
            }
        });
    }

    /**
     * 注册数据变化监听器
     */
    private void initDataObserver () {
        lockConfigManager.registerObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                lockListAdapter.notifyDataSetChanged();
                if (lockListAdapter.getItemCount() > 0) {
                    tv_empty.setVisibility(View.GONE);
                }
            }
        });
    }
}
