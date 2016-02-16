package com.eeontheway.android.applocker.update;

/**
 * 升级信息类
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class PackageUpdateInfo {
    private boolean canBeSkipped;
    private int version;
    private String updateLog;
    private String url;

    public boolean isCanBeSkipped() {
        return canBeSkipped;
    }

    public void setCanBeSkipped(boolean canBeSkipped) {
        this.canBeSkipped = canBeSkipped;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getUpdateLog() {
        return updateLog;
    }

    public void setUpdateLog(String updateLog) {
        this.updateLog = updateLog;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
