package com.eeontheway.android.applocker.lock;

/**
 * GPS锁定配置
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class GpsLockCondition extends BaseLockCondition {
    /**
     * 复制锁定信息
     * @param lockConfig 锁定信息
     */
    public void copy (BaseLockCondition lockConfig) {
        super.copy(lockConfig);

        GpsLockCondition newConfig = (GpsLockCondition)lockConfig;
    }

    /**
     * 克隆接口
     * @return
     */
    @Override
    public Object clone() {
        TimeLockCondition timeLockCondition = (TimeLockCondition)super.clone();
        return timeLockCondition;
    }
}
