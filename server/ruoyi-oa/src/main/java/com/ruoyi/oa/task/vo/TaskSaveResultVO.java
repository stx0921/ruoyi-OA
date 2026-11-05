package com.ruoyi.oa.task.vo;

import java.io.Serializable;

/**
 * 任务创建结果 VO（{taskId,taskNo}）
 *
 * @author ruoyi-oa
 */
public class TaskSaveResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    public TaskSaveResultVO()
    {
    }

    public TaskSaveResultVO(Long taskId, String taskNo)
    {
        this.taskId = taskId;
        this.taskNo = taskNo;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getTaskNo()
    {
        return taskNo;
    }

    public void setTaskNo(String taskNo)
    {
        this.taskNo = taskNo;
    }

}
