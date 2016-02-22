package com.eeontheway.android.applocker.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * QQ分享所用的Activity
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class QQShareActivity extends AppCompatActivity {
    private static final String PARAM_INFO = "ShareInfo";
    private QQShare shareSDK;
    private Tencent tencent;
    private ShareListener listener;
    private String imageUrl;

    /**
     * 启动Activity
     * @param info 分享信息
     */
    public static void startActivity (Context context, ShareInfo info) {
        Intent intent = new Intent(context, QQShareActivity.class);
        intent.putExtra(PARAM_INFO, info);
        context.startActivity(intent);
    }

    /**
     * Activity的onCreate回调
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qqshare);

        // 获取Tencent对像
        shareSDK = (QQShare) QQShare.getInstance(this, true);
        tencent = shareSDK.getTencent();

        // 分享出去
        share();
    }

    /**
     * 将信息分享出去
     */
    public void share () {
        // 创建分享消息
        ShareInfo info = getIntent().getParcelableExtra(PARAM_INFO);
        final Bundle params = new Bundle();
        params.putInt(com.tencent.connect.share.QQShare.SHARE_TO_QQ_KEY_TYPE, com.tencent.connect.share.QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        if (shareSDK.isToFriend()) {
            params.putInt(com.tencent.connect.share.QQShare.SHARE_TO_QQ_EXT_INT, com.tencent.connect.share.QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE);
        } else {
            params.putInt(com.tencent.connect.share.QQShare.SHARE_TO_QQ_EXT_INT, com.tencent.connect.share.QQShare.SHARE_TO_QQ_FLAG_QZONE_AUTO_OPEN);
        }
        params.putString(com.tencent.connect.share.QQShare.SHARE_TO_QQ_TITLE, info.title);
        params.putString(com.tencent.connect.share.QQShare.SHARE_TO_QQ_SUMMARY, info.content);
        params.putString(com.tencent.connect.share.QQShare.SHARE_TO_QQ_TARGET_URL, info.url);
        params.putString(com.tencent.connect.share.QQShare.SHARE_TO_QQ_APP_NAME, getString(R.string.app_name));
        try {
            // 保存图像文件，需要存储到SD卡上，因为文件将会被QQ访问，否则QQ无权限
            imageUrl = Environment.getExternalStorageDirectory() + File.separator + "share_thumb.png";
            OutputStream os = new FileOutputStream(imageUrl);
            info.thumbBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

            params.putString(com.tencent.connect.share.QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, imageUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 分享出去
        listener = new ShareListener();
        tencent.shareToQQ(this, params, listener);
    }

    /**
     * 回调接口
     * @param requestCode 请求码
     * @param resultCode 结果
     * @param data 数据
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != tencent) {
            Tencent.onActivityResultData(requestCode,resultCode,data, listener);
        }
    }

    /**
     * 清理分享中的缓存数据
     */
    private void cleanCacheData () {
        if (imageUrl != null) {
            // 删除缓存的图像文件
            File file = new File(imageUrl);
            file.delete();

            imageUrl = null;
        }
    }

    /**
     * 分享结果回调
     */
    private class ShareListener implements IUiListener {

        @Override
        public void onCancel() {
            cleanCacheData();
            Toast.makeText(QQShareActivity.this, R.string.share_cancel, Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void onComplete(Object arg0) {
            cleanCacheData();
            Toast.makeText(QQShareActivity.this, R.string.share_success, Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void onError(UiError arg0) {
            cleanCacheData();
            Toast.makeText(QQShareActivity.this, R.string.share_error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
