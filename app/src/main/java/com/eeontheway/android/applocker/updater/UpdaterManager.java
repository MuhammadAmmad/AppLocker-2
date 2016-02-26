package com.eeontheway.android.applocker.updater;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.utils.SystemUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * APK升级文件器
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class UpdaterManager {
    private Context context;
    private SimpleDateFormat simpleDateFormat;

    private IUpdateInfoGetter updateInfoGetter;
    private IUpdateLogOp updateLogOp;
    private IDownloadTool downloadTool;

    private ProgressDialog progressDialog;
    private long fileTotalSize;
    private UpdateInfo updateInfo;

    /**
     * 构造器
     * @param context 上下文对像
     */
    public UpdaterManager(Context context) {
        this.context = context;

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        updateInfoGetter = UpdateInfoGetterFactory.create(context);
        updateLogOp = UpdateLogManagerFactory.create(context);
        downloadTool = DownloadToolFactory.create(context);

        progressDialog = new ProgressDialog(context);
    }

    /**
     * 显示升级对话框
     */
    private void showUpdateDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.foundNewVersion);
        builder.setMessage(updateInfo.getUpdateLog());
        builder.setIcon(R.drawable.main_logo_small);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startDownloadAndInstall(updateInfo.getUrl());
                dialog.dismiss();
            }
        });

        // 如果是强制更新，不显示取消按钮，要求用户必须选择升级
        if (updateInfo.isForece() == false) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // 升级被取消，保存取消日志
                    UpdateLog updateLog = new UpdateLog();
                    updateLog.setOldVersionNum(SystemUtils.getVersionCode(context));
                    updateLog.setNewVersionNum(updateInfo.getVersionNum());
                    updateLog.setSkipped(true);
                    updateLog.setTime(simpleDateFormat.format(new Date()));
                    updateLogOp.saveUpdateLog(updateLog);

                    dialog.dismiss();
                }
            });
        }
        builder.create().show();
    }

    /**
     * 显示下载进度框
     */
    private void showDownloadProgressDialog () {
        progressDialog.setMessage(context.getString(R.string.downloading));
        progressDialog.setTitle(context.getString(R.string.updating));
        progressDialog.setMax(100);
        progressDialog.setIcon(R.drawable.main_logo_small);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.show();
    }

    /**
     * 检查是否需要更新
     * @return true/false
     */
    public boolean CheckIfNeedUpdate () {
        if (updateInfo.getVersionNum() > SystemUtils.getVersionCode(context)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取App更新信息
     * @param srcPath 更新信息的获取路径
     */
    public void startAppUpdate (String srcPath) {
        // 配置监听器
        updateInfoGetter.setResultListener(new IUpdateInfoGetter.ResultListener() {
            @Override
            public void onFail(int errorCode, String msg) {
                // 获取失败，提示信息
                Toast.makeText(context, context.getString(R.string.checkUpdateFaild, msg),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(UpdateInfo info) {
                // 获取成功，缓存信息
                updateInfo = info;
                if (CheckIfNeedUpdate()) {
                    // 如果需要更新，弹出提示对话框让用户选择或放弃
                    showUpdateDialog();
                } else {
                    
                }
            }
        });

        // 请求获取信息，之后就继续开始下载更新
        updateInfoGetter.requestGetInfo(srcPath);
    }

    /**
     * 开始下载并安装App
     * @param srcPath 下载的源文件路径
     */
    public void startDownloadAndInstall (String srcPath) {
        // 保存在外部SD卡上
        final String destPath = Environment.getExternalStorageDirectory() + File.separator +
                            new File(srcPath).getName();

        // 配置下载监听器
        downloadTool.setDownloadListener(new IDownloadTool.DownloadResultListener() {
            @Override
            public void onStart(long totalSize) {
                // 启动下载，显示下载进度条
                fileTotalSize = totalSize;
                showDownloadProgressDialog();
            }

            @Override
            public void onProgress(long downloadedSize) {
                // 正在下载，刷新进度条
                progressDialog.setProgress((int)(downloadedSize * 100 / fileTotalSize));
            }

            @Override
            public void onFail(int errorCode, String msg) {
                // 下载失败，提示信息，关闭下载进度对话框
                Toast.makeText(context, context.getString(R.string.download_failed, msg),
                                                    Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }

            @Override
            public void onSuccess() {
                // 下载完成后，不管用户有没有安装，都假定升级完成
                UpdateLog updateLog = new UpdateLog();
                updateLog.setOldVersionNum(SystemUtils.getVersionCode(context));
                updateLog.setNewVersionNum(updateInfo.getVersionNum());
                updateLog.setSkipped(false);
                updateLog.setTime(simpleDateFormat.format(new Date()));
                updateLogOp.saveUpdateLog(updateLog);

                // 下载成功，安装App
                SystemUtils.installApp(context, destPath);

                progressDialog.hide();
            }
        });

        // 启动下载
        downloadTool.startDownload(srcPath, destPath);
    }
}
