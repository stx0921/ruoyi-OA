package com.ruoyi.oa.volunteer.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 志愿活动对象 oa_volunteer_activity
 *
 * @author ruoyi-oa
 */
public class VolunteerActivity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 活动编号（VA+yyyyMMdd+3位序号） */
    private String activityNo;

    /** 活动名称 */
    private String name;

    /** 活动地点 */
    private String location;

    /** 活动人数（参考值，不做硬上限） */
    private Integer capacity;

    /** 志愿时长（小时） */
    private BigDecimal durationHours;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 报名截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signupDeadline;

    /** 活动详情 */
    private String content;

    /** 状态（0正常 1已取消） */
    private String status;

    /** 创建人ID */
    private Long creatorId;

    /** 创建人姓名快照 */
    private String creatorName;

    /** 删除标志（0正常 1删除） */
    private String delFlag;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 报名总数（关联统计，非表字段） */
    private Long signupCount;

    /** 通过报名数（关联统计，非表字段） */
    private Long passCount;

    /** 待审核报名数（关联统计，非表字段） */
    private Long pendingCount;

    /** 驳回报名数（关联统计，非表字段） */
    private Long rejectCount;

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public String getActivityNo()
    {
        return activityNo;
    }

    public void setActivityNo(String activityNo)
    {
        this.activityNo = activityNo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Integer getCapacity()
    {
        return capacity;
    }

    public void setCapacity(Integer capacity)
    {
        this.capacity = capacity;
    }

    public BigDecimal getDurationHours()
    {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours)
    {
        this.durationHours = durationHours;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Date getSignupDeadline()
    {
        return signupDeadline;
    }

    public void setSignupDeadline(Date signupDeadline)
    {
        this.signupDeadline = signupDeadline;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
    }

    public String getCreatorName()
    {
        return creatorName;
    }

    public void setCreatorName(String creatorName)
    {
        this.creatorName = creatorName;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public Long getSignupCount()
    {
        return signupCount;
    }

    public void setSignupCount(Long signupCount)
    {
        this.signupCount = signupCount;
    }

    public Long getPassCount()
    {
        return passCount;
    }

    public void setPassCount(Long passCount)
    {
        this.passCount = passCount;
    }

    public Long getPendingCount()
    {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount)
    {
        this.pendingCount = pendingCount;
    }

    public Long getRejectCount()
    {
        return rejectCount;
    }

    public void setRejectCount(Long rejectCount)
    {
        this.rejectCount = rejectCount;
    }

}
