package com.ruoyi.oa.volunteer.dto;

import java.io.Serializable;

/**
 * 统计查询条件（时间范围按活动开始时间，yyyy-MM-dd，服务端补日界）
 *
 * @author ruoyi-oa
 */
public class StatQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 开始日期（yyyy-MM-dd，含当天 00:00:00） */
    private String beginTime;

    /** 结束日期（yyyy-MM-dd，含当天 23:59:59） */
    private String endTime;

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
