package com.eeontheway.android.applocker.main;

import java.util.List;

/**
 * 信息反馈接口
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public interface IFeedBack {
    /**
     * 发送回调接口
     */
    interface SendStatusListener {
        void onStart ();
        void onSuccess ();
        void onFail (String msg);
    }

    /**
     * 查询回调接口
     */
    interface QueryStatusListener {
        void onStart ();
        void onSuccess (List<FeedBackInfo> infoList);
        void onFail (String msg);
    }

    /**
     * 初始化反馈接口
     * @return true 成功; false 失败
     */
     boolean init();

    /**
     * 反送反馈信息
     * @param info 反馈信息
     */
    void sendFeedBack (FeedBackInfo info);

    /**
     * 获取所有的反馈列表
     * @return
     */
    void queryAllFeedBack ();

    /**
     * 设置发送状态的监听器
     * @param listener
     */
    void setSendListener(SendStatusListener listener);

    /**
     * 设置查询状态的监听器
     * @param listener
     */
    void setQueryListener(QueryStatusListener listener);
}
