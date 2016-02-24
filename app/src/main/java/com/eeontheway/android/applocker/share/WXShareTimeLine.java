package com.eeontheway.android.applocker.share;

import android.content.Context;

/**
 * 将消息分享到微信朋友圈
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class WXShareTimeLine extends WXShareBase {
    /**
     * 初始化分享接口
     * @param context 上下文
     */
    @Override
    public void init(Context context) {
        super.init(context);

        toFriend = false;
    }

    /**
     * 判断是否支持分享功能
     * @return true 支持分享; false 不支持分享
     */
    public boolean isSupported () {
        // 0x21020001及以上支持发送朋友圈
        if (api.getWXAppSupportAPI() >= 0x21020001) {
            return true;
        } else {
            return false;
        }
    }
}
