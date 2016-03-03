package com.eeontheway.android.applocker.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.lock.LockConfigManager;

import java.util.Observable;
import java.util.Observer;

/**
 * 显示指定锁定模式下所有配置锁定App的应用列表
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class AppLockListFragment extends Fragment {
    private RecyclerView rcv_list;
    private TextView tv_empty;
    private Button bt_del;
    private FloatingActionButton fab_add;

    private AppLockListAdapter lockListAdapter;
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
        View view = inflater.inflate(R.layout.fragment_app_lock_list, container, false);

        // 配置删除按钮
        bt_del = (Button) view.findViewById(R.id.bt_del);
        bt_del.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockListAdapter.removeSelectedApp();
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
                AppSelectActivity.start(AppLockListFragment.this);
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
     * 初始化适配器
     */
    private void initAdapter () {
        lockListAdapter = new AppLockListAdapter(parentActivity, lockConfigManager);
        lockListAdapter.setItemSelectedListener(new AppLockListAdapter.ItemSelectedListener() {
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
