package com.eeontheway.android.applocker.sdk.bmob;

import com.eeontheway.android.applocker.main.FeedBackInfo;

import cn.bmob.v3.BmobObject;

/**
 * 专用于BmobSDK的反馈信息类
 * 用于FeedBackInfo与BmobFeedBackInfo间的转换
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class BmobFeedBackInfo extends BmobObject {
    private String content;
    private String contact;

    /**
     * 构造函数
     * @param baseInfo
     */
    public BmobFeedBackInfo(FeedBackInfo baseInfo) {
        // 设置表名与FeedBack的类名一致
        setTableName(FeedBackInfo.class.getSimpleName());

        // Bmob要求信息域必须定义在BmobObject的子类中
        // 所以，只能重复定义下，重复使用了
        contact = baseInfo.getContact();
        content = baseInfo.getContent();
    }

    /**
     * 获取反馈内容
     * @return 反馈内容
     */
    public FeedBackInfo toFeedBackInfo () {
        FeedBackInfo info = new FeedBackInfo();
        info.setContent(content);
        info.setContact(contact);
        return info;
    }
}
