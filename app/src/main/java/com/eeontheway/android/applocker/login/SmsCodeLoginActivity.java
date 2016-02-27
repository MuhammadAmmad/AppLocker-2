package com.eeontheway.android.applocker.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;

/**
 * 利用手机验证码登陆的的Activity
 * @author lishutong
 * @version v1.0
 * @Time 2016-2-8
 */
public class SmsCodeLoginActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText et_phone;
    private EditText et_sms_code;
    private Button bt_request_smscode;
    private Button btn_sumbit;

    private IUserManager userManager;
    private SmsCodeResendTimer resendTimer;
    private ProgressDialog progressDialog;

    /**
     * 启动应用锁配置界面
     *
     * @param context 上下文
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, SmsCodeLoginActivity.class);
        context.startActivity(intent);
    }

    /**
     * Activity的onCreate回调
     *
     * @param savedInstanceState 之前保存的状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smscode_login);

        resendTimer = new SmsCodeResendTimer(60000, 1000);

        setTitle(R.string.phone_login);
        initUserManager();

        initToolBar();
        initViews();
    }

    /**
     * 初始化ToolBar
     */
    private void initToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.tl_header);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    /**
     * 初始化各种View
     */
    private void initViews() {
        et_phone = (EditText) findViewById(R.id.et_phone);
        et_sms_code = (EditText) findViewById(R.id.et_sms_code);
        bt_request_smscode = (Button) findViewById(R.id.bt_request_smscode);
        bt_request_smscode.setOnClickListener(this);
        btn_sumbit = (Button) findViewById(R.id.btn_sumbit);
        btn_sumbit.setOnClickListener(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);
    }


    /**
     * Activiy的onCreateOptionMenu回调
     *
     * @param menu 创建的菜单
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 处理返回按钮按下的响应
     *
     * @param item 被按下的项
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                LoginOrRegisterActivity.start(this, true, 0);
                finish();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     *
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * 初始化用户管理器
     */
    private void initUserManager () {
        userManager = UserManagerFactory.create(this);

        // 注册验证码请求事件监听器
        userManager.setRequestSmsCodeListener(new IUserManager.OnRequestSmsCodeListener() {
            @Override
            public void onFail(int code, String msg) {
                Toast.makeText(SmsCodeLoginActivity.this, R.string.smscode_send_fail,
                                                Toast.LENGTH_SHORT).show();
                closeProgressDialog();

                // 发送失败，允许重试
                resendTimer.cancel();
                bt_request_smscode.setText(R.string.send_sms_code);
                bt_request_smscode.setEnabled(true);
            }

            @Override
            public void onSuccess(String smsId) {
                Toast.makeText(SmsCodeLoginActivity.this, R.string.smscode_send_ok,
                                                Toast.LENGTH_SHORT).show();
                closeProgressDialog();
            }
        });

        // 注册验证码登陆监听器
        userManager.setOnSignUpListener(new IUserManager.OnResultListener() {
            @Override
            public void onFail(int code, String msg) {
                Toast.makeText(SmsCodeLoginActivity.this, getString(R.string.login_failed, msg),
                                                    Toast.LENGTH_SHORT).show();
                closeProgressDialog();
            }

            @Override
            public void onSuccess() {
                Toast.makeText(SmsCodeLoginActivity.this, R.string.login_scuess,
                        Toast.LENGTH_SHORT).show();
                closeProgressDialog();
                finish();
            }
        });
    }

    /**
     * 请求发送验证码
     */
    private void startRequestSMSCode(String phoneNumber) {
        userManager.reqeustSmsCode(phoneNumber, "一键注册或登录模板");
    }

    /**
     * 请求发送验证码
     */
    private void startLogin (String phoneNumber, String smsCode) {
        userManager.signUpAndLoginBySmsCode(phoneNumber, smsCode);
    }

    /**
     * 点击事件处理器
     * @param v
     */
    @Override
    public void onClick(View v) {
        // 电话号码不能为空，必须以1开头，11位
        String telRegex = "[1]\\d{10}";
        String phone = et_phone.getText().toString();
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, R.string.phone_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phone.matches(telRegex)) {
            Toast.makeText(this, R.string.phone_error, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (v.getId()) {
            case R.id.bt_request_smscode:   // 请求验证码
                showProgressDialog(getString(R.string.smscode_requesting));
                resendTimer.start();
                bt_request_smscode.setEnabled(false);

                startRequestSMSCode(phone);
                break;
            case R.id.btn_sumbit:           // 提交验证码登陆
                // 检查验证码有效性
                String smscode = et_sms_code.getText().toString();
                if (TextUtils.isEmpty(smscode)) {
                    Toast.makeText(this, R.string.smscode_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgressDialog(getString(R.string.logining));
                startLogin(phone, smscode);
                break;
        }
    }

    /**
     * 显示进度对话框
     * @param msg 待显示的消息
     */
    private void showProgressDialog (String msg) {
        progressDialog.setMessage(msg);
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog () {
        progressDialog.dismiss();
    }

    /**
     * SMScode重发的计时器
     */
    class SmsCodeResendTimer extends CountDownTimer {
        public SmsCodeResendTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            bt_request_smscode.setText(getString(R.string.smscode_resend_after_time,
                                                            millisUntilFinished / 1000));
        }

        @Override
        public void onFinish() {
            bt_request_smscode.setText(R.string.smscode_resend);
        }
    }
}
