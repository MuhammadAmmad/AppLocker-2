package com.eeontheway.android.applocker.share;

import android.content.Context;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

/**
 * 微信分享接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class WXShare implements IShare {
    private static final String WX_APPID = "wxb1f98f7e3d3563a9";
    private static final int TIMELINE_SUPPORTED_VERSION = 0x21020001;   // 分享到朋友圈的最低版本

    private Context context;
    private IWXAPI api;
    private boolean toFriend;

    private static IShare instance;

    /**
     * 构迼函数，无用
     */
    protected WXShare() {}

    /**
     * 获取微信分享接口实例
     *
     * @param context 上下文
     * @return 实例
     */
    public static IShare getInstance (Context context, boolean toFriend) {
        if (instance == null) {
            WXShare wxShareSDK = new WXShare();
            wxShareSDK.context = context;
            wxShareSDK.toFriend = toFriend;
            instance = wxShareSDK;
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
     * 初始化分享接口
     */
    @Override
    public void init () {
        api = WXAPIFactory.createWXAPI(context, WX_APPID, false);
        api.registerApp(WX_APPID);
    }

    /**
     * 反初始化接口
     */
    @Override
    public void uninit() {
        api.unregisterApp();
    }

    /**
     * 获取api
     */
    public IWXAPI getApi() {
        return api;
    }

    /**
     * 判断是否支持分享功能
     * @return true 支持分享; false 不支持分享
     */
    public boolean isSupported () {
        return api.isWXAppInstalled();
    }

    /**
     * 分享信息到相应接口
     * @param info 待分享的信息
     */
    public void share (ShareInfo info) {
        // 检查是否支持发送朋友圈
        if (toFriend) {
            if (api.getWXAppSupportAPI() < TIMELINE_SUPPORTED_VERSION) {
                Toast.makeText(context, R.string.wx_share_timeline_notsupport, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 构造一个网页对像
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = info.url;

        // 创建消息对像
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = info.title;
        msg.description = info.content;
        msg.setThumbImage(info.thumbBitmap);

        // 分享信息到微信会话
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = toFriend ? req.WXSceneSession : req.WXSceneTimeline;
        api.sendReq(req);
    }
}
