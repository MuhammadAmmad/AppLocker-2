package com.eeontheway.android.applocker.main;

import android.content.Context;
import android.content.Intent;
import android.nfc.tech.TagTechnology;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.sdk.FeedBackManagerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户历史反馈列表Activity
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class FeedBackListActivity extends AppCompatActivity {
    private SwipeRefreshLayout srl_more;
    private RecyclerView rcv_list;
    private RecyclerViewAdapter rcv_adapter;
    private List<FeedBackInfo> feedBackInfoList = new ArrayList<>();
    private IFeedBack feedBackManager;

    /**
     * 启动Activity
     *
     * @param context 上下文
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, FeedBackListActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_back_list);

        setTitle(R.string.feed_back_list);

        initFeedbackManager();
        initToolBar();
        initViews();
        startLoadData();
    }

    /**
     * 初始化Feedback管理器
     */
    private void initFeedbackManager () {
        feedBackManager = FeedBackManagerFactory.create(this);
        feedBackManager.init();
        feedBackManager.setQueryListener(new IFeedBack.QueryStatusListener() {
            @Override
            public void onStart() {

            }

            @Override
            public void onSuccess(List<FeedBackInfo> infoList) {
                int startPos = feedBackInfoList.size();

                // 将获取到的信息插入缓存尾部
                feedBackInfoList.addAll(startPos, infoList);

                // 通知数据源发生改变，刷新界面
                rcv_adapter.notifyItemRangeInserted(startPos, infoList.size());
            }

            @Override
            public void onFail(String msg) {
                Toast.makeText(FeedBackListActivity.this,
                        R.string.no_more_items, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 初始化ToolBar
     */
    private void initToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_header);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    /**
     * 初始化各种View
     */
    private void initViews() {
        srl_more = (SwipeRefreshLayout)findViewById(R.id.srl_more);
        srl_more.setOnRefreshListener(new SwipeRefreshListener());

        rcv_list = (RecyclerView)findViewById(R.id.rcv_list);
        rcv_list.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rcv_list.setLayoutManager(new LinearLayoutManager(this));

        rcv_adapter = new RecyclerViewAdapter();
        rcv_list.setAdapter(rcv_adapter);
    }

    /**
     * 开始加载数据
     */
    private void startLoadData () {
        srl_more.setRefreshing(true);
    }

    /**
     * Activiy的onCreateOptionMenu回调
     *
     * @param menu 创建的菜单
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_feedback_list, menu);
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
                finish();
                return true;
            case R.id.menu_feedback_history:
                FeedBackListActivity.start(this);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * SwipeRefreshLayout加载更多的监听器
     */
    private class SwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            if (!srl_more.isRefreshing()) {

            }
        }
    }

    /**
     * 反馈列表RecyleView的适配器
     */
    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder> {
        /**
         * 列表项的ViewHolder
         */
        class ItemViewHolder extends RecyclerView.ViewHolder {
            public EditText et_content;
            public TextView tv_time;
            public RatingBar rb_rating;
            public Button bt_view_response;

            public ItemViewHolder(View itemView) {
                super(itemView);

                et_content = (EditText)itemView.findViewById(R.id.et_content);
                tv_time = (TextView)itemView.findViewById(R.id.tv_time);
                rb_rating = (RatingBar)itemView.findViewById(R.id.rb_rating);
                bt_view_response = (Button)findViewById(R.id.bt_view_response);
            }
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = View.inflate(FeedBackListActivity.this, R.layout.item_feedback_info, null);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            FeedBackInfo info = feedBackInfoList.get(position);
            holder.et_content.setText(info.getContent());
            holder.tv_time.setText(info.getCreateTime());
            holder.rb_rating.setRating(0.0f);
        }

        @Override
        public int getItemCount() {
            return feedBackInfoList.size();
        }
    }
}