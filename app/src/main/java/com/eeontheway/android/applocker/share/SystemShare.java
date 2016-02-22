package com.eeontheway.android.applocker.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import com.eeontheway.android.applocker.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * 微信分享接口
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class SystemShare implements IShare {
    private Context context;
    private static IShare instance;

    /**
     * 构迼函数，无用
     */
    protected SystemShare() {}

    /**
     * 获取微信分享接口实例
     *
     * @param context 上下文
     * @return 实例
     */
    public static IShare getInstance (Context context, boolean toFriend) {
        if (instance == null) {
            SystemShare share = new SystemShare();
            share.context = context;
            instance = share;
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
     * 是否支持分享功能
     * @return true 支持; false 不支持
     */
    @Override
    public boolean isSupported() {
        return true;
    }

    /**
     * 初始化分享接口
     */
    @Override
    public void init () {
    }

    /**
     * 反初始化接口
     */
    @Override
    public void uninit() {
    }

    /**
     * 分享信息到相应接口
     * @param info 待分享的信息
     */
    public void share (ShareInfo info) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("image/png");

        String imageUrl;
        try {
            // 保存图像文件到SD卡上，以便于分享使用
            imageUrl = Environment.getExternalStorageDirectory() + File.separator + "share_thumb.png";
            OutputStream os = new FileOutputStream(imageUrl);
            info.thumbBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

            // 将图片分享出去
            ArrayList<Uri> imageList = new ArrayList<>();
            imageList.add(Uri.fromFile(new File(imageUrl)));
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageList);
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_from_app));
            intent.putExtra(Intent.EXTRA_TEXT, info.content);
            intent.putExtra(Intent.EXTRA_TITLE, info.title);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.please_choose)));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
