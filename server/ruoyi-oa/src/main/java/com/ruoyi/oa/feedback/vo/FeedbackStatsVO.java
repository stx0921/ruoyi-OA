package com.ruoyi.oa.feedback.vo;

import java.io.Serializable;

/**
 * 工单统计卡 VO
 *
 * @author ruoyi-oa
 */
public class FeedbackStatsVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 待处理 */
    private Long pendingCount;

    /** 已回复 */
    private Long repliedCount;

    /** 今日新增 */
    private Long todayCount;

    /** 本周新增 */
    private Long weekCount;

    public Long getPendingCount()
    {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount)
    {
        this.pendingCount = pendingCount;
    }

    public Long getRepliedCount()
    {
        return repliedCount;
    }

    public void setRepliedCount(Long repliedCount)
    {
        this.repliedCount = repliedCount;
    }

    public Long getTodayCount()
    {
        return todayCount;
    }

    public void setTodayCount(Long todayCount)
    {
        this.todayCount = todayCount;
    }

    public Long getWeekCount()
    {
        return weekCount;
    }

    public void setWeekCount(Long weekCount)
    {
        this.weekCount = weekCount;
    }

}
