package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务创建 DTO（可带子任务、协作人、附件）
 *
 * @author ruoyi-oa
 */
public class TaskCreateDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标题（2-200 字） */
    private String title;

    /** 描述（≤2000 字） */
    private String description;

    /** 项目ID（可空） */
    private Long projectId;

    /** 父任务ID（可空；非空时只允许一层） */
    private Long parentId;

    /** 主责人ID */
    private Long assigneeId;

    /** 协作人ID列表（≤10 人） */
    private List<Long> collaboratorIds;

    /** 优先级（0高 1中 2低，默认中） */
    private String priority;

    /** 标签（最多 3 个） */
    private List<String> tags;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 截止时间（必填） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 子任务列表（单层） */
    private List<TaskChildDTO> children;

    /** 附件ID列表 */
    private List<Long> attachmentFileIds;

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

    public List<TaskChildDTO> getChildren()
    {
        return children;
    }

    public void setChildren(List<TaskChildDTO> children)
    {
        this.children = children;
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
