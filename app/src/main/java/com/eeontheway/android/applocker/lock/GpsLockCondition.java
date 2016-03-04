package com.eeontheway.android.applocker.lock;

import android.location.Location;

import com.eeontheway.android.applocker.locate.Position;


/**
 * GPS锁定配置
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class GpsLockCondition extends BaseLockCondition {
    private Position position;

    /**
     * 获取位置
     * @return 位置
     */
    public Position getPosition() {
        return position;
    }

    /**
     * 设置位置
     * @param position 位置
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * 复制锁定信息
     * 只复制应用层的信息
     * @param lockConfig 锁定信息
     */
    public void copy (BaseLockCondition lockConfig) {
        super.copy(lockConfig);

        GpsLockCondition newConfig = (GpsLockCondition)lockConfig;
        this.position = newConfig.position;
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

    /**
     * 检查指定的地址是否在该地址范围内
     * @param cmpPosition 待检查的地址
     * @return true/false
     */
    public boolean isMatch (Position cmpPosition) {
        float[] distance = new float[1];

        Location.distanceBetween(position.getLatitude(), position.getLongitude(),
                cmpPosition.getLatitude(), cmpPosition.getLongitude(), distance);
        if(distance[0] >= 200){
            return false;
        }

        return true;
    }
}
