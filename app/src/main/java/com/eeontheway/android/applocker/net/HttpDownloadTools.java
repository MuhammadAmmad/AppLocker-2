package com.eeontheway.android.applocker.net;

import android.os.Handler;
import android.os.Message;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Http文件下载器（单线程)
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class HttpDownloadTools {
    public interface DownloadListener {
        void startDownload(int totalSize);
        void onProgress(int donwloadedSize, int totalSize);
        void endDownload(int result, String message);
    }

    private final int DOWNLOAD_START = 0;
    private final int DOWNLOAD_PROGRESS = 1;
    private final int DOWNLOAD_END = 2;

    private String urlPath;
    private String destPath;
    private Handler handler;
    private DownloadListener listener;

    /**
     * 构造函数
     *
     * @param urlPath  文件下载路径
     * @param destPath 文件存储路径
     */
    public HttpDownloadTools(String urlPath, String destPath) {
        this.urlPath = urlPath;
        this.destPath = destPath;

        // 创建下载事件消息处理器
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == DOWNLOAD_START) {
                    listener.startDownload(msg.arg1);
                } else if (msg.what == DOWNLOAD_PROGRESS) {
                    listener.onProgress(msg.arg1, msg.arg2);
                } else {
                    listener.endDownload(msg.arg1, (String)msg.obj);
                }
            }
        };
    }


    /**
     * 设置下载进度事件监听器
     * @param listener 下载进度监听器
     */
    public void setProgressListener (DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 启动文件下载
     */
    public void start() {
        // 创建一个独立的线程用于文件下载
        new Thread(new Runnable() {
            private HttpURLConnection httpURLConnection;
            private InputStream is;
            private FileOutputStream fo;

            // 发送开始下载信息给主线程
            private void sendStartMessage (int totalSize) {
                Message message = new Message();
                message.what = DOWNLOAD_START;
                message.arg1 = totalSize;
                handler.sendMessage(message);

            }

            // 发送下载进度信息给主线程
            private void sendProgressMessage (int downloadedSize, int totalSize) {
                Message message = new Message();
                message.what = DOWNLOAD_PROGRESS;
                message.arg1 = downloadedSize;
                message.arg2 = totalSize;
                handler.sendMessage(message);

            }

            // 发送最终操作结果信息
            private void sendEndMessage (int result, String errorMessage) {
                Message message = new Message();
                message.what = DOWNLOAD_END;
                message.arg1 = result;
                message.obj = errorMessage;
                handler.sendMessage(message);
            }

            @Override
            public void run() {
                try {
                    // 创建Http请求，下载包含更新信息的文件
                    URL url = new URL(urlPath);
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setRequestProperty("Accept-Encoding", "identity");
                    httpURLConnection.setConnectTimeout(5000);

                    // 检查响应
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode == 200) {
                        // 服务器连接成功
                        // 获取整个文件的大小，并通知主线程
                        int totalSize = httpURLConnection.getContentLength();
                        sendStartMessage(totalSize);

                        // 读取整个文件，写入到目标文件中
                        is = httpURLConnection.getInputStream();
                        fo = new FileOutputStream(destPath);
                        byte[] buffer = new byte[2048];
                        int downloadedSize = 0;
                        while (downloadedSize < totalSize) {
                            // 读取文件内容，并写入
                            int currentSize = is.read(buffer);
                            if (currentSize > 0) {
                                fo.write(buffer, 0, currentSize);

                                // 增加下载字节计数
                                downloadedSize += currentSize;

                                // 发送消息给主线程
                                sendProgressMessage(downloadedSize, totalSize);

                                try {
                                    Thread.sleep(0                                );
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        // 发送结束信息
                        sendEndMessage(0, null);
                    } else {
                        // 产生错误，提示
                        sendEndMessage(-1, httpURLConnection.getResponseMessage());
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    sendEndMessage(-1, e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    sendEndMessage(-1, e.getMessage());
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }

                        if (fo != null) {
                            fo.close();
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                        sendEndMessage(-1, e.getMessage());
                    }

                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        }).start();
    }
}
