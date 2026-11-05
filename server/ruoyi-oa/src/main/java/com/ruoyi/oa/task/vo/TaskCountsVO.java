package com.ruoyi.oa.task.vo;

import java.io.Serializable;

/**
 * 我的任务统计卡 VO（今日待办 / 本周 / 已逾期 / 待我验收 / 我发起 / 总数）
 *
 * @author ruoyi-oa
 */
public class TaskCountsVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 今日待办（进行中/待办且截止在今天或更早） */
    private Long today = 0L;

    /** 本周（进行中/待办且截止在本周内） */
    private Long week = 0L;

    /** 已逾期（进行中/待办且已过截止） */
    private Long overdue = 0L;

    /** 待我验收 */
    private Long toAudit = 0L;

    /** 我发起的 */
    private Long created = 0L;

    /** 与我相关的总数 */
    private Long total = 0L;

    public Long getToday()
    {
        return today;
    }

    public void setToday(Long today)
    {
        this.today = today;
    }

    public Long getWeek()
    {
        return week;
    }

    public void setWeek(Long week)
    {
        this.week = week;
    }

    public Long getOverdue()
    {
        return overdue;
    }

    public void setOverdue(Long overdue)
    {
        this.overdue = overdue;
    }

    public Long getToAudit()
    {
        return toAudit;
    }

    public void setToAudit(Long toAudit)
    {
        this.toAudit = toAudit;
    }

    public Long getCreated()
    {
        return created;
    }

    public void setCreated(Long created)
    {
        this.created = created;
    }

    public Long getTotal()
    {
        return total;
    }

    public void setTotal(Long total)
    {
        this.total = total;
    }

}
