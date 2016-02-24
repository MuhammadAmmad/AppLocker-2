package com.eeontheway.android.applocker.share;

/**
 * 微信分享接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class ShareFactory {
    /**
     * 创建分享实例接口
     * @param type 分享类型
     * @return 分享接口
     */
    public static IShare create (int type) {
        IShare shareSDK  = null;

        switch (type) {
            case IShare.SHARE_TYPE_WX_FRIEND:        // 分享到微信朋友
                shareSDK = new WXShareFriend();
                break;
            case IShare.SHARE_TYPE_WX_TIMELINE:      // 分享到微信朋友圈
                shareSDK = new WXShareTimeLine();
                break;
            case IShare.SHARE_TYPE_QQ_FRIEND:        // 分享到QQ好友
                shareSDK = new QQShareFriend();
                break;
            case IShare.SHARE_TYPE_QZONE:            // 分享到QQ空间
                shareSDK = new QQShareQzone();
                break;
            case IShare.SHARE_TYPE_SYSTEM:          // 使用系统分享
                shareSDK = new SystemShare();
                break;
        }
        return shareSDK;
    }
}
