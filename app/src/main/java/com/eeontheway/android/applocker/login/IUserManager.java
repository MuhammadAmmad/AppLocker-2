package com.eeontheway.android.applocker.login;

import android.content.Context;

import cn.bmob.v3.BmobSMS;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.RequestSMSCodeListener;
import cn.bmob.v3.listener.VerifySMSCodeListener;

/**
 * 用户管理接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public interface IUserManager {
    int SMS_SEND_OK = 0;            // 短信发送成功
    int SMS_SEND_FAIL = 1;          // 短信发送失败
    int SMS_SEND_SENDING = 2;       // 短信正在发送

    /**
     * 初始化反馈接口
     * @return true 成功; false 失败
     */
    boolean init (Context context);

    /**
     * 反初始化反馈接口
     */
    void unInit();

    /**
     * 回调接口
     */
    interface OnResultListener {
        /**
         * 登陆失败
         * @param code 错误码
         * @param msg 错误原因消息
         */
        void onFail (int code, String msg);

        /**
         * 登陆成功
         */
        void onSuccess ();
    }

    /**
     * 短信注册码的回调
     */
    interface OnRequestSmsCodeListener {
        /**
         * 登陆失败
         * @param code 错误码
         * @param msg 错误原因消息
         */
        void onFail (int code, String msg);

        /**
         * 登陆成功
         * @param smsId 注册码的ID
         */
        void onSuccess (String smsId);
    }

    /**
     * 查询短信注册码的回调
     */
    interface OnQuerySmsCodeListener {
        /**
         * 查询结果回调
         * @param state 短信状态
         * @param verified 是否已经验证
         */
        void onQueryResult (int state, boolean verified);
    }

    /**
     * 设置注册回调
     * @param listener 注册事件监听器
     */
    void setOnSignUpListener (OnResultListener listener);

    /**
     * 使用指定的用户名/密码/邮件/手机号 注册
     * @param userName 用户名
     * @param password 密码
     */
    void signUp (String userName, String password);

    /**
     * 设置登陆回调
     * @param listener
     */
    void setOnLoginListener (OnResultListener listener);

    /**
     * 使用短信验证码一键注册和登陆
     * @param phoneNumber 手机号
     * @param smsCode 短信验证码
     * @param phoneNumber 手机号
     */
    void signUpAndLoginBySmsCode(String phoneNumber, String smsCode);

    /**
     * 使用指定的用户名和密码登陆
     * @param userName 用户名
     * @param password 密码
     */
    void loginByUserName (String userName, String password);

    /**
     * 使用指定的手机号和密码登陆
     * @param phoneNumber 手机号
     * @param password 密码
     */
    void loginByPhoneNumber (String phoneNumber, String password);

    /**
     * 获取已登陆的用户信息
     * @return 用户信息; 为null表示用户未登陆
     */
    UserInfo getMyUserInfo ();

    /**
     * 设置修改资料回调
     * @param listener 注册事件监听器
     */
    void setUpdateInfoListener (OnResultListener listener);

    /**
     * 更新自己的用户信息
     * @param userInfo 用户信息
     */
    void updateMyUserInfo (UserInfo userInfo);

    /**
     * 设置退出登陆事件
     * @param listener 注册事件监听器
     */
    void setOnLogoutListener (OnResultListener listener);

    /**
     * 退出登陆
     */
    void logout ();

    /**
     * 设置密码修改监听器
     * @param listener 注册事件监听器
     */
    void setModifyPasswordListener (OnResultListener listener);

    /**
     * 修改密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void modifyPassword (String oldPassword, String newPassword);


    /**
     * 设置验证码申请的监听器
     * @param listener 注册事件监听器
     */
    void setRequestSmsCodeListener(OnRequestSmsCodeListener listener);

    /**
     * 设置短信注册码的查询回调
     * @param listener 注册事件监听器
     */
    void setQuerySmsCodeListener(OnQuerySmsCodeListener listener);

    /**
     * 设置短信注册码的校验回调
     * @param listener 注册事件监听器
     */
    void setVerifySmsCodeListener(OnResultListener listener);

    /**
     * 请求发送手机验证码用于登陆
     * @param phoneNumber 手机号
     * @param msgTemplate 发送消息的模板
     */
    void reqeustSmsCode (String phoneNumber, String msgTemplate);

    /**
     * 检查手机验证码的有效性
     * @param phoneNumber 手机号
     * @param code 手同验证码
     */
    void verifySmsCode (String phoneNumber, String code);

    /**
     * 查询手机验证码的发送状态
     * @param smsId 短信ID
     */
    void querySmsCode (String smsId);
}
