package com.ruoyi.oa.task.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务看板 VO
 *
 * @author ruoyi-oa
 */
public class TaskBoardVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 4 列（待办 / 进行中 / 待验收 / 已完成） */
    private List<TaskColumnVO> columns = new ArrayList<TaskColumnVO>();

    /** 已取消任务数（入口展示） */
    private Long canceledCount = 0L;

    public List<TaskColumnVO> getColumns()
    {
        return columns;
    }

    public void setColumns(List<TaskColumnVO> columns)
    {
        this.columns = columns;
    }

    public Long getCanceledCount()
    {
        return canceledCount;
    }

    public void setCanceledCount(Long canceledCount)
    {
        this.canceledCount = canceledCount;
    }

}
