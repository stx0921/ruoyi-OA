package com.ruoyi.oa.recur.dto;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 周期规则新增/编辑入参
 *
 * @author ruoyi-oa
 */
public class RecurSaveDTO
{
    /** 规则ID（编辑必填） */
    private Long recurId;

    /** 规则名称（2–200 字，支持 {date}/{yyyy}/{MM}/{dd} 占位） */
    private String title;

    /** 描述 */
    private String description;

    /** 项目（可空） */
    private Long projectId;

    /** 每周几（1–7，至少一天，服务端去重升序） */
    private List<Integer> weekDays;

    /** 截止时刻（HH:mm 或 HH:mm:ss） */
    private String dueTime;

    /** 主责人 */
    private Long assigneeId;

    /** 优先级（0高 1中 2低） */
    private String priority;

    /** 标签 */
    private List<String> tags;

    /** 开始生效日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /** 修改范围（编辑时）：one 仅本次 / all 本次及以后 */
    private String applyScope;

    /** 仅本次修改的目标任务ID（编辑且 applyScope=one 时可选，缺省取最近生成实例） */
    private Long targetTaskId;

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

    public List<Integer> getWeekDays()
    {
        return weekDays;
    }

    public void setWeekDays(List<Integer> weekDays)
    {
        this.weekDays = weekDays;
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

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public String getApplyScope()
    {
        return applyScope;
    }

    public void setApplyScope(String applyScope)
    {
        this.applyScope = applyScope;
    }

    public Long getTargetTaskId()
    {
        return targetTaskId;
    }

    public void setTargetTaskId(Long targetTaskId)
    {
        this.targetTaskId = targetTaskId;
    }
}
