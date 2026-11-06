package com.ruoyi.oa.volunteer.dto;

import java.io.Serializable;

/**
 * 报名审核 DTO（单条/批量共用）
 *
 * @author ruoyi-oa
 */
public class SignupAuditDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 报名ID列表（≤200） */
    private Long[] ids;

    /** 审核状态（1通过 2驳回） */
    private Integer auditStatus;

    /** 驳回原因（选填，≤200 字；通过时忽略并清空） */
    private String auditRemark;

    public Long[] getIds()
    {
        return ids;
    }

    public void setIds(Long[] ids)
    {
        this.ids = ids;
    }

    public Integer getAuditStatus()
    {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus)
    {
        this.auditStatus = auditStatus;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

}
