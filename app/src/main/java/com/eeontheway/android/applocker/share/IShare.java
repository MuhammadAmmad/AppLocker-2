package com.eeontheway.android.applocker.share;

import android.content.Context;

/**
 * 第三方分享接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public interface IShare {
    int SHARE_TYPE_WX_FRIEND = 0;       // 微信朋友分享
    int SHARE_TYPE_WX_TIMELINE = 1;     // 微信朋友圈分享
    int SHARE_TYPE_QQ_FRIEND = 2;       // QQ好友分享
    int SHARE_TYPE_QZONE = 3;           // QQ空间分享
    int SHARE_TYPE_SYSTEM = 4;          // 使用系统分享

    /**
     * 初始化分享接口
     *
     */
    void init (Context context);

    /**
     * 反初始化微信
     */
    void uninit ();

    /**
     * 判断是否支持分享功能
     * @return true 支持分享; false 不支持分享
     */
    boolean isSupported ();

    /**
     * 分享信息到相应接口
     * @param info 待分享的信息
     */
    void share (ShareInfo info);
}
