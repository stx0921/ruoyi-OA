package com.ruoyi.oa.task.vo;

import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 我的任务分页表格（多一个统计卡 counts）
 *
 * @author ruoyi-oa
 */
public class TaskMyTable extends TableDataInfo
{
    private static final long serialVersionUID = 1L;

    /** 统计卡 */
    private TaskCountsVO counts;

    public TaskCountsVO getCounts()
    {
        return counts;
    }

    public void setCounts(TaskCountsVO counts)
    {
        this.counts = counts;
    }

}
