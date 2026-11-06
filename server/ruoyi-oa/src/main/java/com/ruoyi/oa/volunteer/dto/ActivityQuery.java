package com.ruoyi.oa.volunteer.dto;

import java.io.Serializable;

/**
 * 活动列表查询条件
 *
 * @author ruoyi-oa
 */
public class ActivityQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关键词（名称/地点） */
    private String keyword;

    /** 展示状态（0报名中 1进行中 2已结束 3已取消 4报名已截止） */
    private Integer showStatus;

    /** 开始时间范围（按活动开始时间，yyyy-MM-dd） */
    private String beginTime;

    /** 结束时间范围（按活动开始时间，yyyy-MM-dd） */
    private String endTime;

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public Integer getShowStatus()
    {
        return showStatus;
    }

    public void setShowStatus(Integer showStatus)
    {
        this.showStatus = showStatus;
    }

    public String getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(String beginTime)
    {
        this.beginTime = beginTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }

}
