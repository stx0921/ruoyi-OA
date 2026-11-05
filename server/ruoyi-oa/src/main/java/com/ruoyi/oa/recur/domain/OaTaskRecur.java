package com.ruoyi.oa.recur.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 周期任务规则对象 oa_task_recur
 *
 * <p>week_days：多选，逗号分隔 1–7（1=周一…7=周日），保存时归一化为无空格升序，如 "1,3,5"；
 * due_time 为 TIME 列，按字符串 "HH:mm:ss" 传输；creator_id 为规则创建人（生成任务的派发人/验收人）。</p>
 *
 * @author ruoyi-oa
 */
public class OaTaskRecur extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long recurId;

    /** 规则名称/生成任务标题（支持 {date}/{yyyy}/{MM}/{dd} 占位） */
    private String title;

    /** 描述（生成任务时带过去） */
    private String description;

    /** 项目ID（可空） */
    private Long projectId;

    /** 每周几（逗号分隔 1–7，1=周一…7=周日） */
    private String weekDays;

    /** 截止时刻（HH:mm:ss） */
    private String dueTime;

    /** 主责人 */
    private Long assigneeId;

    /** 优先级（0高 1中 2低） */
    private String priority;

    /** 标签（逗号分隔） */
    private String tags;

    /** 开始生效日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /** 状态（0启用 1暂停 2已结束） */
    private String status;

    /** 下次生成日（派生，列表展示/Job 辅助） */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date nextDate;

    /** 规则创建人（生成任务的派发人/验收人） */
    private Long creatorId;

    /** 创建人部门（数据权限） */
    private Long deptId;

    /** 项目名称（关联查询，非表字段） */
    private String projectName;

    /** 主责人姓名（关联查询，非表字段） */
    private String assigneeName;

    /** 创建人姓名（关联查询，非表字段） */
    private String creatorName;

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

    public String getWeekDays()
    {
        return weekDays;
    }

    public void setWeekDays(String weekDays)
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

    public String getTags()
    {
        return tags;
    }

    public void setTags(String tags)
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

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getAssigneeName()
    {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName)
    {
        this.assigneeName = assigneeName;
    }

    public String getCreatorName()
    {
        return creatorName;
    }

    public void setCreatorName(String creatorName)
    {
        this.creatorName = creatorName;
    }
}
