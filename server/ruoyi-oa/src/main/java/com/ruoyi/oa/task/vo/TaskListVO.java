package com.ruoyi.oa.task.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.utils.StringUtils;

/**
 * 任务列表 VO（canXxx / readOnly / overdueDays 均由服务端计算，前端只渲染）
 *
 * @author ruoyi-oa
 */
public class TaskListVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 标题 */
    private String title;

    /** 项目ID */
    private Long projectId;

    /** 项目名称 */
    private String projectName;

    /** 项目状态（0进行中 1已结束，1=任务只读） */
    private String projectStatus;

    /** 主责人ID */
    private Long assigneeId;

    /** 主责人姓名 */
    private String assigneeName;

    /** 主责人部门名称 */
    private String assigneeDeptName;

    /** 派发人ID */
    private Long creatorId;

    /** 派发人姓名 */
    private String creatorName;

    /** 验收人ID */
    private Long auditorId;

    /** 验收人姓名 */
    private String auditorName;

    /** 优先级（0高 1中 2低） */
    private String priority;

    /** 优先级文案 */
    private String priorityLabel;

    /** 标签（逗号串，仅供内部 resultMap 映射） */
    @JsonIgnore
    private String tagsText;

    /** 标签数组（前端直用） */
    private List<String> tags = new ArrayList<String>();

    /** 状态（0待办 1进行中 2待验收 3已完成 4已取消） */
    private String status;

    /** 状态文案 */
    private String statusLabel;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 完成时间（验收通过） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 打回次数 */
    private Integer rejectCount;

    /** 逾期标记（0否 1是） */
    private String overdueFlag;

    /** 逾期天数（未完成且已过截止，最小 1） */
    private Integer overdueDays;

    /** 来源（0手工创建 1周期生成 2子任务） */
    private String source;

    /** 来源文案 */
    private String sourceLabel;

    /** 周期规则ID */
    private Long recurId;

    /** 项目负责人ID（内部用于验收权限判定，不下发） */
    @JsonIgnore
    private Long projectOwnerId;

    /** 归档只读 */
    private Boolean readOnly;

    /** 可开始 */
    private Boolean canStart;

    /** 可提交完成 */
    private Boolean canSubmit;

    /** 可验收 / 打回 */
    private Boolean canAudit;

    /** 可编辑 */
    private Boolean canEdit;

    /** 可取消 */
    private Boolean canCancel;

    /** 可改派 */
    private Boolean canReassign;

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

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
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

    public String getProjectStatus()
    {
        return projectStatus;
    }

    public void setProjectStatus(String projectStatus)
    {
        this.projectStatus = projectStatus;
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

    public String getAssigneeDeptName()
    {
        return assigneeDeptName;
    }

    public void setAssigneeDeptName(String assigneeDeptName)
    {
        this.assigneeDeptName = assigneeDeptName;
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

    public Long getAuditorId()
    {
        return auditorId;
    }

    public void setAuditorId(Long auditorId)
    {
        this.auditorId = auditorId;
    }

    public String getAuditorName()
    {
        return auditorName;
    }

    public void setAuditorName(String auditorName)
    {
        this.auditorName = auditorName;
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

    public String getTagsText()
    {
        return tagsText;
    }

    /**
     * resultMap 映射逗号串后自动拆成数组（前端只读 tags）
     */
    public void setTagsText(String tagsText)
    {
        this.tagsText = tagsText;
        this.tags = new ArrayList<String>();
        if (StringUtils.isNotEmpty(tagsText))
        {
            String[] parts = tagsText.split(",");
            for (String part : parts)
            {
                if (StringUtils.isNotBlank(part))
                {
                    this.tags.add(part.trim());
                }
            }
        }
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
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

    public Date getDueTime()
    {
        return dueTime;
    }

    public void setDueTime(Date dueTime)
    {
        this.dueTime = dueTime;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public Integer getRejectCount()
    {
        return rejectCount;
    }

    public void setRejectCount(Integer rejectCount)
    {
        this.rejectCount = rejectCount;
    }

    public String getOverdueFlag()
    {
        return overdueFlag;
    }

    public void setOverdueFlag(String overdueFlag)
    {
        this.overdueFlag = overdueFlag;
    }

    public Integer getOverdueDays()
    {
        return overdueDays;
    }

    public void setOverdueDays(Integer overdueDays)
    {
        this.overdueDays = overdueDays;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getSourceLabel()
    {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel)
    {
        this.sourceLabel = sourceLabel;
    }

    public Long getRecurId()
    {
        return recurId;
    }

    public void setRecurId(Long recurId)
    {
        this.recurId = recurId;
    }

    public Long getProjectOwnerId()
    {
        return projectOwnerId;
    }

    public void setProjectOwnerId(Long projectOwnerId)
    {
        this.projectOwnerId = projectOwnerId;
    }

    public Boolean getReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public Boolean getCanStart()
    {
        return canStart;
    }

    public void setCanStart(Boolean canStart)
    {
        this.canStart = canStart;
    }

    public Boolean getCanSubmit()
    {
        return canSubmit;
    }

    public void setCanSubmit(Boolean canSubmit)
    {
        this.canSubmit = canSubmit;
    }

    public Boolean getCanAudit()
    {
        return canAudit;
    }

    public void setCanAudit(Boolean canAudit)
    {
        this.canAudit = canAudit;
    }

    public Boolean getCanEdit()
    {
        return canEdit;
    }

    public void setCanEdit(Boolean canEdit)
    {
        this.canEdit = canEdit;
    }

    public Boolean getCanCancel()
    {
        return canCancel;
    }

    public void setCanCancel(Boolean canCancel)
    {
        this.canCancel = canCancel;
    }

    public Boolean getCanReassign()
    {
        return canReassign;
    }

    public void setCanReassign(Boolean canReassign)
    {
        this.canReassign = canReassign;
    }

}
