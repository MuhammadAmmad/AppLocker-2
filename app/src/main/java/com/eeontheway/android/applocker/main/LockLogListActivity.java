package com.eeontheway.android.applocker.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.eeontheway.android.applocker.R;

/**
 * 应用锁访问日志查看列表
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-9
 */
public class LockLogListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_locker_log_list);

        setTitle(R.string.applock_logs);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * 启动Activity
     * @param context 上下文
     */
    public static void startActivity (Context context) {
        Intent intent = new Intent(context, LockLogListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * 处理返回按钮按下的响应
     * @param item 被按下的项
     * @return 是否需要被继续处理
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
