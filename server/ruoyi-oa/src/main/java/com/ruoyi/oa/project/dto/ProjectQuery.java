package com.ruoyi.oa.project.dto;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目列表查询条件（@DataScope 依赖 BaseEntity.params）
 *
 * @author ruoyi-oa
 */
public class ProjectQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 状态（0进行中 1已结束） */
    private String status;

    /** 负责人 */
    private Long ownerId;

    /** 关键字（项目名称/编号） */
    private String keyword;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerId(Long ownerId)
    {
        this.ownerId = ownerId;
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
