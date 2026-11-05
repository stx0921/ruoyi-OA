package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 子任务创建 DTO（只允许一层）
 *
 * @author ruoyi-oa
 */
public class TaskChildDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标题 */
    private String title;

    /** 主责人ID */
    private Long assigneeId;

    /** 截止时间（为空时继承父任务） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 优先级（为空时继承父任务） */
    private String priority;

    /** 标签（为空时继承父任务） */
    private List<String> tags;

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Long getAssigneeId()
    {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId)
    {
        this.assigneeId = assigneeId;
    }

    public Date getDueTime()
    {
        return dueTime;
    }

    public void setDueTime(Date dueTime)
    {
        this.dueTime = dueTime;
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

}
