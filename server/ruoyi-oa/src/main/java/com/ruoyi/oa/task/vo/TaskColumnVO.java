package com.ruoyi.oa.task.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板列 VO（4 列：待办 / 进行中 / 待验收 / 已完成）
 *
 * @author ruoyi-oa
 */
public class TaskColumnVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 状态 */
    private String status;

    /** 状态文案 */
    private String statusLabel;

    /** 该列总数（可能大于 tasks 数量，受单列上限限制） */
    private Integer count = 0;

    /** 卡片列表 */
    private List<TaskListVO> tasks = new ArrayList<TaskListVO>();

    public TaskColumnVO()
    {
    }

    public TaskColumnVO(String status, String statusLabel)
    {
        this.status = status;
        this.statusLabel = statusLabel;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatusLabel()
    {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel)
    {
        this.statusLabel = statusLabel;
    }

    public Integer getCount()
    {
        return count;
    }

    public void setCount(Integer count)
    {
        this.count = count;
    }

    public List<TaskListVO> getTasks()
    {
        return tasks;
    }

    public void setTasks(List<TaskListVO> tasks)
    {
        this.tasks = tasks;
    }

}
