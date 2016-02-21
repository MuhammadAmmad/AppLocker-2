package com.eeontheway.android.applocker.update;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.net.HttpDownloadTools;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;

/**
 * APK升级文件器
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class ApkUpdater {
    private Activity activity;
    private PackageInfoAccess packageInfoAccess;
    private ProgressDialog progressDialog;
    private PackageUpdateInfo packageUpdateInfo;
    public final static int REQUEST_INSTALL_APP = 0;

    /**
     * 构造器
     */
    public ApkUpdater(Activity activity) {
        this.activity = activity;

        packageInfoAccess = new PackageInfoAccess(activity);
    }

    /**
     * 显示升级对话框
     */
    private void showUpdateDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.foundNewVersion);
        builder.setMessage(packageUpdateInfo.getUpdateLog());
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadApk(packageUpdateInfo.getUrl());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }

    /**
     * 显示下载进度框
     */
    private void showDownloadProgressDialog () {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage(activity.getResources().getString(R.string.updating));
        progressDialog.setTitle(activity.getResources().getString(R.string.downloading));
        progressDialog.setIcon(R.mipmap.ic_launcher);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    /**
     * 启动新版本检查机制
     */
    public void start (String serverUrl) {
        // 解析下载路径
        final String destFileName = activity.getCacheDir() + "/" +
                serverUrl.substring(serverUrl.lastIndexOf("/") + 1);

        // 配置下载工具
        HttpDownloadTools downloadTools = new HttpDownloadTools(serverUrl, destFileName);
        downloadTools.setProgressListener(new HttpDownloadTools.DownloadListener() {
            @Override
            public void startDownload(int totalSize) {

            }

            @Override
            public void onProgress(int donwloadedSize, int totalSize) {

            }

            @Override
            public void endDownload(int result, String message) {
                if (result == 0) {
                    try {
                        // 如果当前包的版本比服务器上的低，则需要更新，否则什么都不做
                        packageUpdateInfo = new ApkUpdateInfoParse(destFileName).getUpdateInfo();
                        if (packageInfoAccess.getVersionCode() < packageUpdateInfo.getVersion()) {
                            showUpdateDialog();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XmlPullParserException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 发现错误，提示
                    String msg = activity.getString(R.string.checkUpdateFaild);
                    Toast.makeText(activity, "更新失败:" + msg + "-" + message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 启动下载
        downloadTools.start();
    }

    /**
     * 下载Apk
     */
    private void downloadApk (String apkUrl) {
        // 解析apk文件名
        final String destFileName = Environment.getExternalStorageDirectory().getPath() + "/" +
                                                apkUrl.substring(apkUrl.lastIndexOf("/") + 1);

        // 创建下载进度对话框
        showDownloadProgressDialog();

        // 创建http下载器
        HttpDownloadTools tools = new HttpDownloadTools(apkUrl, destFileName);
        tools.setProgressListener(new HttpDownloadTools.DownloadListener() {
            @Override
            public void startDownload(int totalSize) {
                progressDialog.setMax(totalSize);
            }

            @Override
            public void onProgress(int downloadedSize, int totalSize) {
                progressDialog.setProgress(downloadedSize);
            }

            @Override
            public void endDownload(int result, String message) {
                progressDialog.dismiss();

                if (result == 0) {
                    installApk(destFileName);
                } else {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
        tools.start();
    }

    /**
     * 安装APK
     */
    private void installApk (String apkPath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
        activity.startActivityForResult(intent, REQUEST_INSTALL_APP);
    }
}
