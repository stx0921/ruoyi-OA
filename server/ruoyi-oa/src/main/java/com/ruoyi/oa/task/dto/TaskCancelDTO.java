package com.ruoyi.oa.task.dto;

import java.io.Serializable;

/**
 * 取消任务 DTO（原因必填 2-200 字；待验收须先打回）
 *
 * @author ruoyi-oa
 */
public class TaskCancelDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 取消原因 */
    private String remark;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

}
