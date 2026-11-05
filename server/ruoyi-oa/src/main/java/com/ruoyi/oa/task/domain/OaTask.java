package com.ruoyi.oa.task.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 任务对象 oa_task
 *
 * <p>5 态状态机：0 待办 / 1 进行中 / 2 待验收 / 3 已完成 / 4 已取消。</p>
 *
 * @author ruoyi-oa
 */
public class OaTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 任务编号（T+yyyyMMdd+3位序号） */
    private String taskNo;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 项目ID（可空，独立任务无项目） */
    private Long projectId;

    /** 父任务ID（只允许一层） */
    private Long parentId;

    /** 主责人（sys_user.user_id） */
    private Long assigneeId;

    /** 优先级（0高 1中 2低） */
    private String priority;

    /** 标签（逗号分隔） */
    private String tags;

    /** 状态（0待办 1进行中 2待验收 3已完成 4已取消） */
    private String status;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 截止时间（含时刻） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dueTime;

    /** 实际完成时间（验收通过时写入） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 提交待验收时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 打回次数 */
    private Integer rejectCount;

    /** 派发人 */
    private Long creatorId;

    /** 验收人（默认=派发人） */
    private Long auditorId;

    /** 来源（0手工创建 1周期生成 2子任务） */
    private String source;

    /** 周期规则ID */
    private Long recurId;

    /** 周期任务生成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date generateDate;

    /** 逾期标记（0否 1是，Job 刷新 + 查询兜底） */
    private String overdueFlag;

    /** 派发人部门（数据权限锚点） */
    private Long deptId;

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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public Integer getRejectCount()
    {
        return rejectCount;
    }

    public void setRejectCount(Integer rejectCount)
    {
        this.rejectCount = rejectCount;
    }

    public Long getCreatorId()
    {
        return creatorId;
    }

    public void setCreatorId(Long creatorId)
    {
        this.creatorId = creatorId;
    }

    public Long getAuditorId()
    {
        return auditorId;
    }

    public void setAuditorId(Long auditorId)
    {
        this.auditorId = auditorId;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public Long getRecurId()
    {
        return recurId;
    }

    public void setRecurId(Long recurId)
    {
        this.recurId = recurId;
    }

    public Date getGenerateDate()
    {
        return generateDate;
    }

    public void setGenerateDate(Date generateDate)
    {
        this.generateDate = generateDate;
    }

    public String getOverdueFlag()
    {
        return overdueFlag;
    }

    public void setOverdueFlag(String overdueFlag)
    {
        this.overdueFlag = overdueFlag;
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
