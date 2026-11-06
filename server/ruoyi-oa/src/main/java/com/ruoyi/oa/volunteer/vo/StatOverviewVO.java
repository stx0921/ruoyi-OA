package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 志愿活动统计概览 VO
 *
 * @author ruoyi-oa
 */
public class StatOverviewVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 总志愿时长（人时）= Σ(duration_hours × 通过人数) */
    private BigDecimal totalHours;

    /** 开展活动次数（已开始且未取消） */
    private Long activityCount;

    /** 参与人次（通过报名数） */
    private Long participantCount;

    /** 平均每场时长（小时，1 位小数） */
    private BigDecimal avgHours;

    public BigDecimal getTotalHours()
    {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours)
    {
        this.totalHours = totalHours;
    }

    public Long getActivityCount()
    {
        return activityCount;
    }

    public void setActivityCount(Long activityCount)
    {
        this.activityCount = activityCount;
    }

    public Long getParticipantCount()
    {
        return participantCount;
    }

    public void setParticipantCount(Long participantCount)
    {
        this.participantCount = participantCount;
    }

    public BigDecimal getAvgHours()
    {
        return avgHours;
    }

    public void setAvgHours(BigDecimal avgHours)
    {
        this.avgHours = avgHours;
    }

}
