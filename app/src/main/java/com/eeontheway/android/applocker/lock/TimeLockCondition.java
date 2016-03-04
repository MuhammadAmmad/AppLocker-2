package com.eeontheway.android.applocker.lock;

import android.app.Fragment;
import android.content.Context;

import com.eeontheway.android.applocker.R;
import com.eeontheway.android.applocker.main.LocationConditionEditActivity;
import com.eeontheway.android.applocker.main.TimeConditionEditActivity;
import com.eeontheway.android.applocker.utils.SystemUtils;

import java.util.Calendar;
import java.util.Date;

/**
 * 时间锁定配置
 * @author lishutong
 * @version v1.0
 * @Time 2016-12-15
 */
public class TimeLockCondition extends BaseLockCondition {
    public static int DAY7 = (1 << 7);
    public static int DAY6 = (1 << 6);
    public static int DAY5 = (1 << 5);
    public static int DAY4 = (1 << 4);
    public static int DAY3 = (1 << 3);
    public static int DAY2 = (1 << 2);
    public static int DAY1 = (1 << 1);

    private String startTime;
    private String endTime;
    private int day;

    /**
     * 复制锁定信息
     * 只复制应用层的信息
     * @param lockConfig 锁定信息
     */
    public void copy (BaseLockCondition lockConfig) {
        super.copy(lockConfig);

        TimeLockCondition newConfig = (TimeLockCondition)lockConfig;
        this.startTime = newConfig.startTime;
        this.endTime = newConfig.endTime;
        this.day = newConfig.day;
    }

    /**
     * 克隆接口
     * @return
     */
    @Override
    public Object clone() {
        TimeLockCondition timeLockCondition = (TimeLockCondition)super.clone();
        timeLockCondition.startTime = new String(startTime);
        timeLockCondition.endTime = new String(endTime);
        return timeLockCondition;
    }

    /**
     * 获取起始时间
     * @return 起始时间
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * 设置起始时间
     * @param startTime 起始时间
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取结束时间
     * @return 结束时间
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * 设置结束时间
     * @param endTime 结束时间
     */
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /**
     * 获取有效的日
     * @return 有效日
     */
    public int getDay() {
        return day;
    }

    /**
     * 设置有效日
     * @param day 有效日
     */
    public void setDay(int day) {
        this.day = day;
    }

    /**
     * 检查指定日是否被设置
     * @param day 取DAY7~DAY1
     * @return true/false
     */
    public boolean isDaySet (int day) {
        return (this.day & day) == day;
    }

    /**
     * 获取星期的字符串列表
     * @return 字符串列表
     */
    public String getDayString (Context context) {
        StringBuffer stringBuffer = new StringBuffer();

        if (isDaySet(DAY1)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day1)));
        }
        if (isDaySet(DAY2)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day2)));
        }
        if (isDaySet(DAY3)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day3)));
        }
        if (isDaySet(DAY4)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day4)));
        }
        if (isDaySet(DAY5)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day5)));
        }
        if (isDaySet(DAY6)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day6)));
        }
        if (isDaySet(DAY7)) {
            stringBuffer.append(context.getString(R.string.week, context.getString(R.string.day7)));
        }

        return stringBuffer.toString();
    }

    /**
     * 检查指定的日期是否在该范围内
     * @param calendar 日间
     * @return true/false
     */
    public boolean isMatch (Calendar calendar) {
        // 比较星期
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                if (!isDaySet(TimeLockCondition.DAY1)) {
                    return false;
                }
                break;
            case Calendar.TUESDAY:
                if (!isDaySet(TimeLockCondition.DAY2)) {
                    return false;
                }
                break;
            case Calendar.WEDNESDAY:
                if (!isDaySet(TimeLockCondition.DAY3)) {
                    return false;
                }
                break;
            case Calendar.THURSDAY:
                if (!isDaySet(TimeLockCondition.DAY4)) {
                    return false;
                }
                break;
            case Calendar.FRIDAY:
                if (!isDaySet(TimeLockCondition.DAY5)) {
                    return false;
                }
                break;
            case Calendar.SATURDAY:
                if (!isDaySet(TimeLockCondition.DAY6)) {
                    return false;
                }
                break;
            case Calendar.SUNDAY:
                if (!isDaySet(TimeLockCondition.DAY7)) {
                    return false;
                }
                break;
        }

        // 比较时/分
        Date date = calendar.getTime();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(SystemUtils.formatDate(getStartTime(), "HH:mm"));
        startCalendar.set(0, 0, 0);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(SystemUtils.formatDate(getEndTime(), "HH:mm"));
        endCalendar.set(0, 0, 0);

        if (startCalendar.compareTo(calendar) <= 0) {
            if (endCalendar.compareTo(calendar) >= 0) {
                return true;
            }
        }

        return false;
    }
}
