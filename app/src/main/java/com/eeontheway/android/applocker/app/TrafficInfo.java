package com.eeontheway.android.applocker.app;

/**
 * 进程信息类
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class TrafficInfo extends BaseAppInfo {
    /**
     * 总的发送字节数
     */
    private long txBytes;

    /**
     * 总的接受字节数
     */
    protected long rxBytes;

    /**
     * 总的发送与接受字节数之和
     */
    protected long totalBytes;

    /**
     * 获得总的发送字节数
     *
     * @return 总的发送字节数
     */
    public long getTxBytes() {
        return txBytes;
    }

    /**
     * 设置总的发送字节数
     *
     * @param txBytes 总的发送字节数
     */
    public void setTxBytes(long txBytes) {
        this.txBytes = txBytes;
        this.totalBytes = this.txBytes + this.rxBytes;
    }

    /**
     * 获取总的接受字节数
     *
     * @return 总的接受字节数
     */
    public long getRxBytes() {
        return rxBytes;
    }

    /**
     * 设置总的接受字节数
     *
     * @param rxBytes 总的接受字节数
     */
    public void setRxBytes(long rxBytes) {
        this.rxBytes = rxBytes;
        this.totalBytes = this.txBytes + this.rxBytes;
    }

    /**
     * 获取总的接受字节数
     *
     * @return 总的接受字节数
     */
    public long getTotalBytes() {
        return totalBytes;
    }
}
