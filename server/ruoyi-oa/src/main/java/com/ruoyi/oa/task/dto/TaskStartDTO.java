package com.ruoyi.oa.task.dto;

import java.io.Serializable;

/**
 * 开始任务 DTO（待办 → 进行中）
 *
 * @author ruoyi-oa
 */
public class TaskStartDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

}
