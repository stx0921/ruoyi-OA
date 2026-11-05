package com.ruoyi.oa.recur.dto;

/**
 * 周期规则状态变更入参（0启用 1暂停 2已结束）
 *
 * @author ruoyi-oa
 */
public class RecurStatusDTO
{
    /** 规则ID */
    private Long recurId;

    /** 目标状态（0启用 1暂停 2已结束） */
    private String status;

    public Long getRecurId()
    {
        return recurId;
    }

    public void setRecurId(Long recurId)
    {
        this.recurId = recurId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
