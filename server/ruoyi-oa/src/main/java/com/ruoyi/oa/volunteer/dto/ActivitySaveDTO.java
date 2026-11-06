package com.ruoyi.oa.volunteer.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 活动新增/编辑 DTO
 *
 * @author ruoyi-oa
 */
public class ActivitySaveDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 活动ID（编辑必填） */
    private Long activityId;

    /** 活动名称（2–100 字） */
    private String name;

    /** 活动地点（2–100 字） */
    private String location;

    /** 活动人数（1–9999，仅参考） */
    private Integer capacity;

    /** 志愿时长（0.5–99.9，步进 0.5） */
    private BigDecimal durationHours;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间（必须晚于开始时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 报名截止（必须不晚于开始时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signupDeadline;

    /** 活动详情（10–5000 字） */
    private String content;

    /** 状态（0正常 1已取消，编辑时可传） */
    private String status;

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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

}
