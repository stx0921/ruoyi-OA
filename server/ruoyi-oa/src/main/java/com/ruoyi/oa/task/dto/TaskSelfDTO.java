package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 给自己建独立任务 DTO（主责 / 验收人强制 = 当前用户，不挂项目）
 *
 * @author ruoyi-oa
 */
public class TaskSelfDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 标题（2-200 字） */
    private String title;

    /** 描述 */
    private String description;

    /** 截止时间（必填） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 优先级（默认中） */
    private String priority;

    /** 标签（最多 3 个） */
    private List<String> tags;

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

    public List<Long> getAttachmentFileIds()
    {
        return attachmentFileIds;
    }

    public void setAttachmentFileIds(List<Long> attachmentFileIds)
    {
        this.attachmentFileIds = attachmentFileIds;
    }

}
