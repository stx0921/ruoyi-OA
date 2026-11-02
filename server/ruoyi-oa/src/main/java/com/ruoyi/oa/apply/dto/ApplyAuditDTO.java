package com.ruoyi.oa.apply.dto;

import java.io.Serializable;
import java.util.List;

/**
 * PC 审批 DTO（支持批量同意/驳回，单次 ≤50 条）
 *
 * @author ruoyi-oa
 */
public class ApplyAuditDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 申请ID列表 */
    private List<Long> ids;

    /** true 同意 / false 驳回 */
    private Boolean pass;

    /** 审批意见（驳回时 2–200 字必填） */
    private String remark;

    public List<Long> getIds()
    {
        return ids;
    }

    public void setIds(List<Long> ids)
    {
        this.ids = ids;
    }

    public Boolean getPass()
    {
        return pass;
    }

    public void setPass(Boolean pass)
    {
        this.pass = pass;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

}
