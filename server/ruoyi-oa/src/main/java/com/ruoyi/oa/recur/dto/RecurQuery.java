package com.ruoyi.oa.recur.dto;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 周期规则列表查询条件（@DataScope 依赖 BaseEntity.params）
 *
 * @author ruoyi-oa
 */
public class RecurQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 状态（0启用 1暂停 2已结束） */
    private String status;

    /** 主责人 */
    private Long assigneeId;

    /** 项目 */
    private Long projectId;

    /** 关键字（规则名称） */
    private String keyword;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getAssigneeId()
    {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId)
    {
        this.assigneeId = assigneeId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }
}
