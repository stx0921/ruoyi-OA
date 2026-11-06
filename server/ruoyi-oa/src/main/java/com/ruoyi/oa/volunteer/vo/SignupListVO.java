package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 报名名单 VO
 *
 * @author ruoyi-oa
 */
public class SignupListVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 报名ID */
    private Long signupId;

    /** 活动ID */
    private Long activityId;

    /** 活动名称 */
    private String activityName;

    /** 姓名 */
    private String name;

    /** 学号 */
    private String studentNo;

    /** 学院 */
    private String college;

    /** 专业 */
    private String major;

    /** 报名时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signupTime;

    /** 审核状态（0待审核 1通过 2驳回） */
    private String auditStatus;

    /** 审核状态标签 */
    private String auditStatusLabel;

    /** 驳回原因 */
    private String auditRemark;

    /** 审核人姓名 */
    private String auditorName;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 活动志愿时长（小时） */
    private BigDecimal durationHours;

    public Long getSignupId()
    {
        return signupId;
    }

    public void setSignupId(Long signupId)
    {
        this.signupId = signupId;
    }

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public String getActivityName()
    {
        return activityName;
    }

    public void setActivityName(String activityName)
    {
        this.activityName = activityName;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getCollege()
    {
        return college;
    }

    public void setCollege(String college)
    {
        this.college = college;
    }

    public String getMajor()
    {
        return major;
    }

    public void setMajor(String major)
    {
        this.major = major;
    }

    public Date getSignupTime()
    {
        return signupTime;
    }

    public void setSignupTime(Date signupTime)
    {
        this.signupTime = signupTime;
    }

    public String getAuditStatus()
    {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditStatusLabel()
    {
        return auditStatusLabel;
    }

    public void setAuditStatusLabel(String auditStatusLabel)
    {
        this.auditStatusLabel = auditStatusLabel;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

    public String getAuditorName()
    {
        return auditorName;
    }

    public void setAuditorName(String auditorName)
    {
        this.auditorName = auditorName;
    }

    public Date getAuditTime()
    {
        return auditTime;
    }

    public void setAuditTime(Date auditTime)
    {
        this.auditTime = auditTime;
    }

    public BigDecimal getDurationHours()
    {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours)
    {
        this.durationHours = durationHours;
    }

}
