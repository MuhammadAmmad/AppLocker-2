package com.eeontheway.android.applocker.login;

import android.content.Context;

import com.eeontheway.android.applocker.R;

import cn.bmob.v3.BmobSMS;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobSmsState;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import cn.bmob.v3.listener.QuerySMSStateListener;
import cn.bmob.v3.listener.RequestSMSCodeListener;
import cn.bmob.v3.listener.ResetPasswordByCodeListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.VerifySMSCodeListener;

/**
 * 基于BMOB的用户管理接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class BmobUserManager extends UserManagerBase {
    private Context context;

    /**
     * 构造函数
     * @param context 上下文
     */
    public BmobUserManager(Context context) {
        super(context);
        this.context = context;
    }

    /**
     * 初始化反馈接口
     * @return true 成功; false 失败
     */
    @Override
    public boolean init (Context context) {
        return true;
    }

    /**
     * 反初始化反馈接口
     */
    @Override
    public void unInit() {
    }

    /**
     * 使用指定的用户名和密码登陆
     * @param userName 用户名
     * @param password 密码
     */
    public void loginByUserName (String userName, String password) {
        BmobUser.loginByAccount(context, userName, password, new LogInListener<BmobUserInfo>() {
            @Override
            public void done(BmobUserInfo userInfo, BmobException e) {
                if(e == null){
                    if (loginResultListener != null) {
                        loginResultListener.onSuccess();
                    }
                }else{
                    if (loginResultListener != null) {
                        loginResultListener.onFail(e.getErrorCode(), e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 使用指定的手机号和密码登陆
     * @param phoneNumber 手机号
     * @param password 密码
     */
    public void loginByPhoneNumber (String phoneNumber, String password) {
        loginByUserName(phoneNumber, password);
    }

    /**
     * 退出登陆
     */
    public void logout () {
        BmobUser.logOut(context);
        if (logoutResultListener != null) {
            logoutResultListener.onSuccess();
        }
    }

    /**
     * 使用指定的手机号和验证码登陆
     * @param phoneNumber 手机号
     * @param code 验证码
     */
    public void loginBySmsCode (String phoneNumber, String code) {

        BmobUser.loginBySMSCode(context, phoneNumber, code, new LogInListener<BmobUserInfo>() {
            @Override
            public void done(BmobUserInfo userInfo, BmobException e) {
                if(e == null){
                    if (loginResultListener != null) {
                        loginResultListener.onSuccess();
                    }
                }else{
                    if (loginResultListener != null) {
                        loginResultListener.onFail(e.getErrorCode(), e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    /**
     * 请求发送手机验证码用于登陆
     * @param phoneNumber 手机号
     * @param msgTemplate 发送消息的模板
     */
    public void reqeustSmsCode (String phoneNumber, String msgTemplate) {
        BmobSMS.requestSMSCode(context, phoneNumber, msgTemplate, new RequestSMSCodeListener() {
            @Override
            public void done(Integer smsId, BmobException e) {
                if(e == null){
                    if (requestSmsCodeListener != null) {
                        requestSmsCodeListener.onSuccess("" + smsId);
                    }
                }else{
                    if (requestSmsCodeListener != null) {
                        requestSmsCodeListener.onFail(e.getErrorCode(), e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    /**
     * 检查手机验证码的有效性
     * @param phoneNumber 手机号
     * @param code 手同验证码
     */
    public void verifySmsCode (String phoneNumber, String code) {
        if (!isSmsCodeValid(code)) {
            if (verifySmsCodeListener != null) {
                verifySmsCodeListener.onFail(-1, context.getString(R.string.smscode_invalid));
            }
            return;
        }

        BmobSMS.verifySmsCode(context, phoneNumber, code, new VerifySMSCodeListener() {
            @Override
            public void done(BmobException e) {
                if(e == null){
                    if (verifySmsCodeListener != null) {
                        verifySmsCodeListener.onSuccess();
                    }
                }else{
                    if (verifySmsCodeListener != null) {
                        verifySmsCodeListener.onFail(e.getErrorCode(), e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    /**
     * 查询手机验证码的发送状态
     * @param smsId 短信ID
     */
    public void querySmsCode (String smsId) {
        BmobSMS.querySmsState(context, Integer.parseInt(smsId), new QuerySMSStateListener() {
            @Override
            public void done(BmobSmsState state, BmobException e) {
                if (querySmsCodeListener != null) {
                    int finalState;
                    boolean verified;

                    switch (state.getSmsState()) {
                        case "SUCCESS":
                            finalState = SMS_SEND_OK;
                            break;
                        case "SENDING":
                            finalState = SMS_SEND_SENDING;
                            break;
                        case "FAIL":
                        default:
                            finalState = SMS_SEND_FAIL;
                            break;
                    }

                    switch (state.getVerifyState()) {
                        case "true":
                            verified = true;
                            break;
                        case "false":
                        default:
                            verified = false;
                            break;
                    }

                    querySmsCodeListener.onQueryResult(finalState, verified);
                }
            }
        });
    }

    /**
     * 使用指定的用户名/密码/邮件/手机号 注册
     * @param userName 用户名
     * @param password 密码
     */
    public void signUp (String userName, String password) {
        BmobUser user = new BmobUser();
        user.setUsername(userName);
        user.setPassword(password);
        user.signUp(context, new SaveListener() {
            @Override
            public void onSuccess() {
                if (signUpResultListener != null) {
                    signUpResultListener.onSuccess();
                }
            }

            @Override
            public void onFailure(int i, String s) {
                if (signUpResultListener != null) {
                    signUpResultListener.onFail(i, s);
                }
            }
        });
    }

    /**
     * 检查验证码的有效性
     */
    public boolean isSmsCodeValid (String smscode) {
        String smsRegex = "\\d{6}";
        if ((smscode != null) && (!smscode.isEmpty())) {
            return smscode.matches(smsRegex);
        }

        return false;
    }

    /**
     * 使用短信验证码一键注册和登陆
     * @param phoneNumber 手机号
     * @param smsCode 短信验证码
     * @param phoneNumber 手机号
     */
    public void signUpAndLoginBySmsCode(String phoneNumber, String smsCode) {
        if (!isSmsCodeValid(smsCode)) {
            if (signUpResultListener != null) {
                signUpResultListener.onFail(-1, context.getString(R.string.smscode_invalid));
            }
            return;
        }

        BmobUser.signOrLoginByMobilePhone(context, phoneNumber, smsCode, new LogInListener<BmobUserInfo>() {
            @Override
            public void done(BmobUserInfo user, BmobException e) {
                if(user != null){
                    if (signUpResultListener != null) {
                        signUpResultListener.onSuccess();
                    }
                } else {
                    if (signUpResultListener != null) {
                        signUpResultListener.onFail(e.getErrorCode(), e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    /**
     * 获取已登陆的用户信息
     * @return 用户信息; 为null表示用户未登陆
     */
    public UserInfo getMyUserInfo () {
        BmobUserInfo userInfo = BmobUser.getCurrentUser(context, BmobUserInfo.class);
        if (userInfo != null) {
            return userInfo.toUserInfo();
        }
        return null;
    }


    /**
     * 更新自己的用户信息
     * @param userInfo 用户信息
     */
    public void updateMyUserInfo (UserInfo userInfo) {
        BmobUserInfo info = new BmobUserInfo(userInfo);
        info.update(context, new UpdateListener() {
            @Override
            public void onSuccess() {
                if (updateInfoResultListener != null) {
                    updateInfoResultListener.onSuccess();
                }
            }

            @Override
            public void onFailure(int i, String s) {
                if (updateInfoResultListener != null) {
                    updateInfoResultListener.onFail(i, s);
                }
            }
        });
    }

    /**
     * 使用旧密码修改密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void modifyPassword (String oldPassword, String newPassword) {
        BmobUser.updateCurrentUserPassword(context, oldPassword, newPassword, new UpdateListener() {
            @Override
            public void onSuccess() {
                if (passwordModifyResultListener != null) {
                    passwordModifyResultListener.onSuccess();
                }
            }

            @Override
            public void onFailure(int code, String msg) {
                if (passwordModifyResultListener != null) {
                    passwordModifyResultListener.onFail(code, msg);
                }
            }
        });
    }

    /**
     * 使用短信验证码来修改密码
     * @param smsCode 短信验证码
     * @param newPassword 新密码
     */
    public void modifyPasswordBySmsCode (String smsCode, String newPassword) {
        if (!isSmsCodeValid(smsCode)) {
            if (passwordModifyResultListener != null) {
                passwordModifyResultListener.onFail(-1, context.getString(R.string.smscode_invalid));
            }
            return;
        }

        BmobUser.resetPasswordBySMSCode(context, smsCode, newPassword, new ResetPasswordByCodeListener() {
            @Override
            public void done(BmobException e) {
                if(e == null){
                    if (passwordModifyResultListener != null) {
                        passwordModifyResultListener.onSuccess();
                    }
                }else{
                    if (passwordModifyResultListener != null) {
                        passwordModifyResultListener.onFail(e.getErrorCode(), e.getLocalizedMessage());
                    }
                }
            }
        });
    }

    /**
     * 为当前用户绑定手机号
     */
    public void updatePhoneNumer (String phoeNumber) {
//        BmobUser.resetPasswordBySMSCode(context, smsCode, newPassword, new ResetPasswordByCodeListener() {
//            @Override
//            public void done(BmobException e) {
//                if(e == null){
//                    if (passwordModifyResultListener != null) {
//                        passwordModifyResultListener.onSuccess();
//                    }
//                }else{
//                    if (passwordModifyResultListener != null) {
//                        passwordModifyResultListener.onFail(e.getErrorCode(), e.getLocalizedMessage());
//                    }
//                }
//            }
//        });
    }
}
