package com.ruoyi.oa.volunteer.dto;

import java.io.Serializable;

/**
 * 报名名单查询条件
 *
 * @author ruoyi-oa
 */
public class SignupQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 活动ID（必填） */
    private Long activityId;

    /** 审核状态（0待审核 1通过 2驳回） */
    private String auditStatus;

    /** 关键词（姓名/学号） */
    private String keyword;

    /** 活动名称（导出用，服务端填充） */
    private String activityName;

    /** 志愿时长（导出用，服务端填充） */
    private java.math.BigDecimal durationHours;

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public String getAuditStatus()
    {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public String getActivityName()
    {
        return activityName;
    }

    public void setActivityName(String activityName)
    {
        this.activityName = activityName;
    }

    public java.math.BigDecimal getDurationHours()
    {
        return durationHours;
    }

    public void setDurationHours(java.math.BigDecimal durationHours)
    {
        this.durationHours = durationHours;
    }

}
