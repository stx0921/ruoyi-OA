package com.ruoyi.oa.recur.vo;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 周期规则列表展示对象
 *
 * @author ruoyi-oa
 */
public class RecurListVO
{
    /** 规则ID */
    private Long recurId;

    /** 规则名称 */
    private String title;

    /** 描述 */
    private String description;

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 每周几（1–7） */
    private List<Integer> weekDays;

    /** 每周几文案（周一/周三/周五） */
    private List<String> weekDayLabels;

    /** 截止时刻（HH:mm:ss） */
    private String dueTime;

    /** 主责人 */
    private Long assigneeId;

    /** 主责人姓名 */
    private String assigneeName;

    /** 优先级（0高 1中 2低） */
    private String priority;

    /** 优先级文案 */
    private String priorityLabel;

    /** 标签 */
    private List<String> tags;

    /** 开始生效日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /** 状态（0启用 1暂停 2已结束） */
    private String status;

    /** 状态文案 */
    private String statusLabel;

    /** 下次生成日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date nextDate;

    /** 创建人 */
    private Long creatorId;

    /** 创建人姓名 */
    private String creatorName;

    /** 创建人部门ID */
    private Long deptId;

    public Long getRecurId()
    {
        return recurId;
    }

    public void setRecurId(Long recurId)
    {
        this.recurId = recurId;
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

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public List<Integer> getWeekDays()
    {
        return weekDays;
    }

    public void setWeekDays(List<Integer> weekDays)
    {
        this.weekDays = weekDays;
    }

    public List<String> getWeekDayLabels()
    {
        return weekDayLabels;
    }

    public void setWeekDayLabels(List<String> weekDayLabels)
    {
        this.weekDayLabels = weekDayLabels;
    }

    public String getDueTime()
    {
        return dueTime;
    }

    public void setDueTime(String dueTime)
    {
        this.dueTime = dueTime;
    }

    public Long getAssigneeId()
    {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId)
    {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName()
    {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName)
    {
        this.assigneeName = assigneeName;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getPriorityLabel()
    {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel)
    {
        this.priorityLabel = priorityLabel;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
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

    public Date getNextDate()
    {
        return nextDate;
    }

    public void setNextDate(Date nextDate)
    {
        this.nextDate = nextDate;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
    }

    public String getCreatorName()
    {
        return creatorName;
    }

    public void setCreatorName(String creatorName)
    {
        this.creatorName = creatorName;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }
}
