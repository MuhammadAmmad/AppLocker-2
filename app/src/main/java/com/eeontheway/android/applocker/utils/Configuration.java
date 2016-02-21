package com.eeontheway.android.applocker.utils;

/**
 * App配置类
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public final class Configuration {
    /**
     * App官方网站
     */
    public static final String webSiteUrl = "http://app.eeontheway.com/";

    /**
     * APP更新地址
     */
    public static final String updateSiteUrl = "http://android.eeontheway.com/update/updateinfo.xml";

    /**
     * 本应用支持的应用市场包名
     */
    public static final String [] appMarketPackageName = {
            "com.qihoo.appstore",                   // 360手机助手
            "com.taobao.appcenter",                 // 淘宝手机助手
            "com.tencent.android.qqdownloader",     // 应用宝
            "com.hiapk.marketpho",                  // 安卓市场
            "com.huawei.appmarket",                 // 华为应用市场
            "cn.goapk.market"                       // 安智市场
    };

    public static final int BMOB_FEEDBACK = 0;      // BMOB的反馈管理器

    /**
     * 反馈管理器所用的SDK组件
     */
    public static final int FeedBackMangerType = BMOB_FEEDBACK;
}
