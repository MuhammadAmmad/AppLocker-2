package com.eeontheway.android.applocker.share;

import android.content.Context;

import com.eeontheway.android.applocker.utils.Configuration;
import com.tencent.tauth.Tencent;

/**
 * 微信分享接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class QQShareBase implements IShare {
    protected Context context;
    protected boolean toFriend;

    protected static Tencent tencent;
    private static int instanceCount;

    /**
     * 获取Tencent对像
     * @return Tencent对像
     */
    public static Tencent getTencent () {
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
    public void init (Context context) {
        this.context = context;

        if (tencent == null) {
            tencent = Tencent.createInstance(Configuration.QQ_APPID,
                                                    context.getApplicationContext());
        }
        instanceCount++;
    }

    /**
     * 反初始化接口
     */
    @Override
    public void uninit() {
        if (instanceCount > 0) {
            if (--instanceCount == 0) {
                tencent.releaseResource();
                tencent = null;
            }
        }
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
        QQShareActivity.startActivity(context, info, toFriend);
    }
}
