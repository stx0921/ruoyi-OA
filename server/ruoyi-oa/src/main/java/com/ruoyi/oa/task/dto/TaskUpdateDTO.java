package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务编辑 DTO（不含子任务；仅派发人 / 主席团 / 管理员）
 *
 * @author ruoyi-oa
 */
public class TaskUpdateDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 项目ID（为空表示不修改；挂项目时子任务项目跟随） */
    private Long projectId;

    /** 父任务ID（不支持修改，传了需与现有值一致） */
    private Long parentId;

    /** 主责人ID */
    private Long assigneeId;

    /** 协作人ID列表（传空数组表示清空；null 表示不修改） */
    private List<Long> collaboratorIds;

    /** 优先级 */
    private String priority;

    /** 标签（传空数组表示清空；null 表示不修改） */
    private List<String> tags;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 追加附件ID列表 */
    private List<Long> attachmentFileIds;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
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

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getDueTime()
    {
        return dueTime;
    }

    public void setDueTime(Date dueTime)
    {
        this.dueTime = dueTime;
    }

    public List<Long> getAttachmentFileIds()
    {
        return attachmentFileIds;
    }

    public void setAttachmentFileIds(List<Long> attachmentFileIds)
    {
        this.attachmentFileIds = attachmentFileIds;
    }

}
