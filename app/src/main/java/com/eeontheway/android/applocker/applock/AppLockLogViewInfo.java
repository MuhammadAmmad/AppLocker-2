package com.eeontheway.android.applocker.applock;

/**
 * App锁定日志观察信息
 *
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class AppLockLogViewInfo {
    /**
     * 日志信息
     */
    private AppLockLogInfo logInfo;

    /**
     * 获取日志信息
     * @return 获取日志信息
     */
    public AppLockLogInfo getLogInfo() {
        return logInfo;
    }

    /**
     * 设置日志信息
     * @param logInfo 日志信息
     */
    public void setLogInfo(AppLockLogInfo logInfo) {
        this.logInfo = logInfo;
    }

    /**
     * 是否被选中
     */
    private boolean selected;

    /**
     * 是否被选中
     * @return true 选中; false 未选中
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * 设置是否被选中
     * @param selected 是否选中
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
