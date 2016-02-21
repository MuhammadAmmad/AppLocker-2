package com.eeontheway.android.applocker.sdk.bmob;

import android.content.Context;

import com.eeontheway.android.applocker.main.FeedBackInfo;
import com.eeontheway.android.applocker.main.IFeedBack;

import java.util.ArrayList;
import java.util.List;

import cn.bmob.v3.BmobPushManager;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;

/**
 * 反馈信息管理器
 * 用于发送/查看反馈列表
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class BombFeedBackManager implements IFeedBack {
    private Context context;
    private BmobPushManager bmobPush;
    private SendStatusListener sendListener;
    private QueryStatusListener queryListener;

    /**
     * 构造函数
     * @param context
     */
    public BombFeedBackManager(Context context) {
        this.context = context;
    }

    /**
     * 初始化反馈接口
     * @return true 成功; false 失败
     */
    @Override
    public boolean init() {
        bmobPush = new BmobPushManager(context);
        return true;
    }

    /**
     * 发送反馈信息
     * @param info 反馈信息
     * @return true 发送成功; false 发送失败
     */
    @Override
    public void sendFeedBack (FeedBackInfo info) {
        // 转换信息结构
        BmobFeedBackInfo bmobFeedBackInfo = new BmobFeedBackInfo(info);

        // 开始调用
        if (sendListener != null) {
            sendListener.onStart();
        }

        // 将数据保存到服务器
        bmobFeedBackInfo.save(context, new SaveListener() {
            @Override
            public void onSuccess() {
                if (sendListener != null) {
                    sendListener.onSuccess();
                }
            }

            @Override
            public void onFailure(int i, String s) {
                if (sendListener != null) {
                    sendListener.onFail(s);
                }
            }
        });
    }

    /**
     * 获取所有的反馈列表
     * @return
     */
    public void queryAllFeedBack () {
        final BmobQuery<BmobFeedBackInfo> query = new BmobQuery<>();
        query.order("-createdAt");
        query.findObjects(context, new FindListener<BmobFeedBackInfo>() {
            @Override
            public void onSuccess(List<BmobFeedBackInfo> list) {
                // 获取反馈信息列表
                List<FeedBackInfo> infoList = new ArrayList<>();
                for (BmobFeedBackInfo info : list) {
                    infoList.add(info.toFeedBackInfo());
                }

                // 调用回调接口
                if (queryListener != null) {
                    queryListener.onSuccess(infoList);
                }
            }

            @Override
            public void onError(int i, String s) {
                if (queryListener != null) {
                    queryListener.onFail(s);
                }
            }
        });
    }

    /**
     * 设置发送状态的监听器
     * @param listener
     */
    @Override
    public void setSendListener(SendStatusListener listener) {
        sendListener = listener;
    }

    /**
     * 设置查询状态的监听器
     * @param listener
     */
    @Override
    public void setQueryListener(QueryStatusListener listener) {
        queryListener = listener;
    }
}
