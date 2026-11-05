package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 改派 DTO（只改主责人 / 协作人，状态不变）
 *
 * @author ruoyi-oa
 */
public class TaskReassignDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 新主责人ID */
    private Long assigneeId;

    /** 新协作人ID列表（null 表示不改） */
    private List<Long> collaboratorIds;

    /** 改派备注 */
    private String remark;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getAssigneeId()
    {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId)
    {
        this.assigneeId = assigneeId;
    }

    public List<Long> getCollaboratorIds()
    {
        return collaboratorIds;
    }

    public void setCollaboratorIds(List<Long> collaboratorIds)
    {
        this.collaboratorIds = collaboratorIds;
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
