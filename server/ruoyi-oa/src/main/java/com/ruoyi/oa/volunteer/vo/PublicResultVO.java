package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * H5 报名结果查询 VO（只返回必要字段）
 *
 * @author ruoyi-oa
 */
public class PublicResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 活动名称 */
    private String activityName;

    /** 志愿时长（小时） */
    private BigDecimal durationHours;

    /** 审核状态（0待审核 1通过 2驳回） */
    private String auditStatus;

    /** 驳回原因 */
    private String auditRemark;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    public String getActivityName()
    {
        return activityName;
    }

    public void setActivityName(String activityName)
    {
        this.activityName = activityName;
    }

    public BigDecimal getDurationHours()
    {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours)
    {
        this.durationHours = durationHours;
    }

    public String getAuditStatus()
    {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

    public Date getAuditTime()
    {
        return auditTime;
    }

    public void setAuditTime(Date auditTime)
    {
        this.auditTime = auditTime;
    }

}
