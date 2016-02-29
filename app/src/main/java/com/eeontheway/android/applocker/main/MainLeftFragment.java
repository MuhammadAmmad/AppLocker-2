package com.eeontheway.android.applocker.main;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.feedback.FeedBackListActivity;
import com.eeontheway.android.applocker.login.IUserManager;
import com.eeontheway.android.applocker.login.LoginOrRegisterActivity;
import com.eeontheway.android.applocker.login.UserInfo;
import com.eeontheway.android.applocker.login.UserManagerFactory;
import com.eeontheway.android.applocker.share.ShareActivity;
import com.eeontheway.android.applocker.share.ShareInfo;
import com.eeontheway.android.applocker.utils.Configuration;
import com.eeontheway.android.applocker.utils.SystemUtils;

import java.util.Date;

/**
 * 主界面左侧滑动页面显示的内容
 * 包含主菜单和用户账户入口
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class MainLeftFragment extends Fragment {
    // 顶层菜单项
    private static final int MODE_LIST_ROW = 0;
    private static final int SETTING_ROW = 1;
    private static final int ABOUT_ROW = 2;
    private static final MenuInfo [] topMenuInfos = {
            new MenuInfo(R.string.mode_list, MenuInfo.ICON_INVALID),                // 模式列表
            new MenuInfo(R.string.settings, R.drawable.ic_settings_black_24dp),     // 设置
            new MenuInfo(R.string.about, MenuInfo.ICON_INVALID)                     // 关于
    };

    // 模式列表菜单项
    private static final MenuInfo [] modeListMenuInfos = {
            new MenuInfo(R.string.default_mode, MenuInfo.ICON_INVALID),
            new MenuInfo(R.string.child_mode, MenuInfo.ICON_INVALID),
            new MenuInfo(R.string.group_mode, MenuInfo.ICON_INVALID),
            new MenuInfo(R.string.location_mode, MenuInfo.ICON_INVALID)
    };

    private ExpandableListView ev_menu;
    private BaseExpandableListAdapter ev_adapter;
    private ImageView iv_head;
    private TextView tv_welcome;
    private TextView tv_current_time;
    private Button bt_reg_login;
    private Button bt_logout;
    private Button bt_share;
    private Button bt_feedback;

    private Activity parentActivity;
    private IUserManager userManager;

    /**
     * Fragment的onCreate回调
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parentActivity = getActivity();
        ev_adapter = new TopMenuAdapter();

        initUserManager();
    }

    /**
     * Fragment的onCreateView回调
     * @param savedInstanceState
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_left, null);

        // 找到所有的View
        ev_menu = (ExpandableListView) view.findViewById(R.id.ev_menu);
        tv_welcome = (TextView)view.findViewById(R.id.tv_welcome);
        iv_head = (ImageView) view.findViewById(R.id.iv_head);
        tv_current_time = (TextView)view.findViewById(R.id.tv_current_time);
        bt_reg_login = (Button) view.findViewById(R.id.bt_reg_login);
        bt_logout = (Button) view.findViewById(R.id.bt_logout);
        bt_feedback = (Button)view.findViewById(R.id.bt_feedback);
        bt_share = (Button)view.findViewById(R.id.bt_share);

        // 初始化菜单
        initView();
        initMenus();
        initListener();
        return view;
    }

    /**
     * Activity的onDestory函数
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        userManager.unInit();
    }

    /**
     * Fragment的onResume回调
     */
    @Override
    public void onResume() {
        super.onResume();

        updateHeaderView();
    }


    /**
     * 初始化菜单列表
     */
    private void initMenus () {
        ev_menu.setAdapter(new TopMenuAdapter());
    }

    /**
     * 初始化各种UI的Listener
     */
    private void initListener() {
        // 配置菜单列表
        ev_menu.setOnGroupClickListener(new ExpandListGroupClickListener());

        // 配置普通点击事件处理
        ClickListener clickListener = new ClickListener();
        bt_feedback.setOnClickListener(clickListener);
        bt_share.setOnClickListener(clickListener);
    }

    /**
     * 初始化界面显示
     */
    private void initView () {
        // 登陆按钮点击事件
        bt_reg_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginOrRegisterActivity.startForResult(MainLeftFragment.this, true, 0);
            }
        });

        // 登陆出去事件
        bt_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userManager.logout();
                updateHeaderView();
            }
        });
    }

    /**
     * 更新用户登陆状态显示
     */
    private void updateHeaderView () {
        UserInfo userInfo = userManager.getMyUserInfo();
        if (userInfo == null) {
            // 未登陆
            bt_reg_login.setVisibility(View.VISIBLE);
            bt_logout.setVisibility(View.GONE);
            tv_welcome.setText(R.string.you_are_not_logined_yet);
            iv_head.setImageResource(R.drawable.ic_unknown_user);

        } else {
            bt_reg_login.setVisibility(View.GONE);
            bt_logout.setVisibility(View.VISIBLE);
            tv_welcome.setText(getString(R.string.welcome_to_use_app, userInfo.getUserName()));
            iv_head.setImageResource(R.drawable.ic_known_person);
        }
        tv_current_time.setText(SystemUtils.formatDate(new Date(), "yyyy-MM-dd"));
    }

    /**
     * 初始化用户管理器
     */
    private void initUserManager() {
        userManager = UserManagerFactory.create(parentActivity);
        userManager.init(parentActivity);
    }


    /**
     * 按钮点击事件处理
     */
    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bt_share:     // 启动分享机制
                    startShareApp();
                    break;
                case R.id.bt_feedback:  // 启动反馈机制
                    FeedBackListActivity.start(parentActivity);
                    break;
            }
        }
    }

    /**
     * 启动App分享功能
     */
    private void startShareApp () {
        // 创建App分享信息
        ShareInfo shareInfo = new ShareInfo();
        shareInfo.url = Configuration.webSiteUrl;
        shareInfo.title = Configuration.SHARE_APP_TITLE;
        shareInfo.content = Configuration.APP_DESCRIPTION;
        shareInfo.thumbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.app_lanucher);

        // 启动分享界面
        ShareActivity.start(parentActivity, shareInfo);
    }

    /**
     * 列表的组项点击事件处理
     */
    private class ExpandListGroupClickListener implements ExpandableListView.OnGroupClickListener {
        @Override
        public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
            switch (groupPosition) {
                case MODE_LIST_ROW:
                    // 修改修改选中状态，以便切换菜单的指示器
                    MenuInfo menuInfo = topMenuInfos[groupPosition];
                    menuInfo.setSelected(!menuInfo.isSelected());
                    ev_adapter.notifyDataSetChanged();
                    break;
                case SETTING_ROW:
                    SettingsActivity.start(parentActivity);
                    break;
                case ABOUT_ROW:
                    AboutActivity.start(parentActivity);
                    break;
            }
            return false;
        }
    }

    /**
     * 菜单列表显示的Adpter
     */
    private class TopMenuAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return topMenuInfos.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            int cnt = 0;

            if (groupPosition == MODE_LIST_ROW) {
                cnt = modeListMenuInfos.length;
            }
            return cnt;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return topMenuInfos[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            MenuInfo menuInfo = null;

            if (groupPosition == MODE_LIST_ROW) {
                menuInfo = modeListMenuInfos[childPosition];
            }
            return menuInfo;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                                 ViewGroup parent) {
            // 渲染UI，由于菜单列表很少，就不用考虑优化
            View view = View.inflate(parentActivity, R.layout.item_menu_main_left_top, null);
            ImageView iv_icon = (ImageView)view.findViewById(R.id.iv_icon);
            TextView tv_title = (TextView)view.findViewById(R.id.tv_title);
            ViewStub vs_indicator = (ViewStub)view.findViewById(R.id.vs_indicator);

            // 配置显示
            MenuInfo menuInfo = topMenuInfos[groupPosition];
            if (menuInfo.getIconRes() != MenuInfo.ICON_INVALID) {
                iv_icon.setImageResource(menuInfo.getIconRes());
            }
            tv_title.setText(menuInfo.getTitleRes());

            // 仅在模式列表上显示指示器
            if (groupPosition == MODE_LIST_ROW) {
                CheckBox checkBox = (CheckBox)vs_indicator.inflate();
                checkBox.setChecked(menuInfo.isSelected());
            }

            return view;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                                 View convertView, ViewGroup parent) {
            // 渲染UI，由于菜单列表很少，就不用考虑优化
            View view = View.inflate(parentActivity, R.layout.item_menu_main_left_sub, null);
            ImageView iv_icon = (ImageView)view.findViewById(R.id.iv_icon);
            TextView tv_title = (TextView)view.findViewById(R.id.tv_title);

            if (groupPosition == MODE_LIST_ROW) {
                // 配置显示
                MenuInfo menuInfo = modeListMenuInfos[childPosition];
                if (menuInfo.getIconRes() != MenuInfo.ICON_INVALID) {
                    iv_icon.setImageResource(menuInfo.getIconRes());
                }
                tv_title.setText(menuInfo.getTitleRes());
            }

            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
}
