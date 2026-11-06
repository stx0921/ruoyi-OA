package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * H5 免登录活动详情 VO（只暴露必要字段，不含创建人/审核人等）
 *
 * @author ruoyi-oa
 */
public class PublicActivityVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 活动名称 */
    private String name;

    /** 活动地点 */
    private String location;

    /** 活动人数 */
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

    /** 展示状态（0报名中 1进行中 2已结束 3已取消 4报名已截止） */
    private Integer showStatus;

    /** 展示状态标签 */
    private String showStatusLabel;

    /** 已报名数 */
    private Long signupCount;

    /** 当前是否可报名 */
    private Boolean canSignup;

    /** 不可报名原因（可报名时为空串） */
    private String closedReason;

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
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

    public Integer getShowStatus()
    {
        return showStatus;
    }

    public void setShowStatus(Integer showStatus)
    {
        this.showStatus = showStatus;
    }

    public String getShowStatusLabel()
    {
        return showStatusLabel;
    }

    public void setShowStatusLabel(String showStatusLabel)
    {
        this.showStatusLabel = showStatusLabel;
    }

    public Long getSignupCount()
    {
        return signupCount;
    }

    public void setSignupCount(Long signupCount)
    {
        this.signupCount = signupCount;
    }

    public Boolean getCanSignup()
    {
        return canSignup;
    }

    public void setCanSignup(Boolean canSignup)
    {
        this.canSignup = canSignup;
    }

    public String getClosedReason()
    {
        return closedReason;
    }

    public void setClosedReason(String closedReason)
    {
        this.closedReason = closedReason;
    }

}
