package com.ruoyi.oa.task.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 任务查询条件
 *
 * <p>列表/看板走 {@code @DataScope(deptAlias="t")} + 参与关系 OR；「我的任务」只按参与关系。</p>
 *
 * @author ruoyi-oa
 */
public class TaskQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 主责人ID */
    private Long assigneeId;

    /** 派发人ID */
    private Long creatorId;

    /** 验收人ID */
    private Long auditorId;

    /** 状态（0待办 1进行中 2待验收 3已完成 4已取消） */
    private String status;

    /** 优先级（0高 1中 2低） */
    private String priority;

    /** 标签（精确命中 tags 逗号串中的一项） */
    private String tag;

    /** 关键字（标题 / 任务编号） */
    private String keyword;

    /** 父任务ID（查子任务） */
    private Long parentId;

    /** 我的任务 Tab（today / week / all / created） */
    private String tab;

    /** 范围（my / list），预留 */
    private String scope;

    /** 只看逾期 */
    private Boolean overdueOnly;

    /** 排除归档项目任务（/my、/board 强制 true；列表默认 false） */
    private Boolean excludeArchived;

    /** 截止时间起（yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginDueTime;

    /** 截止时间止 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endDueTime;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getAssigneeId()
    {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId)
    {
        this.assigneeId = assigneeId;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getPriority()
    {
        return priority;
    }

    public void setPriority(String priority)
    {
        this.priority = priority;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getTab()
    {
        return tab;
    }

    public void setTab(String tab)
    {
        this.tab = tab;
    }

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public Boolean getOverdueOnly()
    {
        return overdueOnly;
    }

    public void setOverdueOnly(Boolean overdueOnly)
    {
        this.overdueOnly = overdueOnly;
    }

    public Boolean getExcludeArchived()
    {
        return excludeArchived;
    }

    public void setExcludeArchived(Boolean excludeArchived)
    {
        this.excludeArchived = excludeArchived;
    }

    public Date getBeginDueTime()
    {
        return beginDueTime;
    }

    public void setBeginDueTime(Date beginDueTime)
    {
        this.beginDueTime = beginDueTime;
    }

    public Date getEndDueTime()
    {
        return endDueTime;
    }

    public void setEndDueTime(Date endDueTime)
    {
        this.endDueTime = endDueTime;
    }

}
