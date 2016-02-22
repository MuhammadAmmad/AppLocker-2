package com.eeontheway.android.applocker.share;

import android.content.Context;

import com.tencent.tauth.Tencent;

/**
 * 微信分享接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class QQShare implements IShare {
    private static final String APPID = "1105120437";

    private Context context;
    private Tencent tencent;
    private boolean toFriend;

    private static IShare instance;

    /**
     * 构迼函数，无用
     */
    protected QQShare() {}

    /**
     * 获取微信分享接口实例
     *
     * @param context 上下文
     * @return 实例
     */
    public static IShare getInstance (Context context, boolean toFriend) {
        if (instance == null) {
            QQShare shareSDK = new QQShare();
            shareSDK.context = context;
            shareSDK.toFriend = toFriend;
            instance = shareSDK;
        }
        return instance;
    }

    /**
     * 释放实例
     */
    public static void freeInstance () {
        instance = null;
    }

    /**
     * 获取Tencent对像
     * @return Tencent对像
     */
    public Tencent getTencent() {
        return tencent;
    }

    /**
     * 是否分享到朋友
     * @return true 分享到朋友; false 否
     */
    public boolean isToFriend() {
        return toFriend;
    }

    /**
     * 初始化分享接口
     */
    @Override
    public void init () {
        tencent = Tencent.createInstance(APPID, context);
    }

    /**
     * 反初始化接口
     */
    @Override
    public void uninit() {
    }

    /**
     * 判断是否支持分享功能
     * @return true 支持分享; false 不支持分享
     */
    public boolean isSupported () {
        return true;
    }

    /**
     * 分享信息到相应接口
     * @param info 待分享的信息
     */
    public void share (ShareInfo info) {
        QQShareActivity.startActivity(context, info);
    }
}
