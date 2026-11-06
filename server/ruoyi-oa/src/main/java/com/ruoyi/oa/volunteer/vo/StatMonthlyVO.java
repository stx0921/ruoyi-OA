package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 志愿活动按月统计 VO
 *
 * @author ruoyi-oa
 */
public class StatMonthlyVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM） */
    private String month;

    /** 活动数（已开始且未取消） */
    private Long activityCount;

    /** 通过人次 */
    private Long passCount;

    /** 总时长（人时） */
    private BigDecimal totalHours;

    public String getMonth()
    {
        return month;
    }

    public void setMonth(String month)
    {
        this.month = month;
    }

    public Long getActivityCount()
    {
        return activityCount;
    }

    public void setActivityCount(Long activityCount)
    {
        this.activityCount = activityCount;
    }

    public Long getPassCount()
    {
        return passCount;
    }

    public void setPassCount(Long passCount)
    {
        this.passCount = passCount;
    }

    public BigDecimal getTotalHours()
    {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours)
    {
        this.totalHours = totalHours;
    }

}
