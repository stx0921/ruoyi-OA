package com.ruoyi.oa.member.dto;

import java.io.Serializable;

/**
 * 导入确认 DTO
 *
 * @author ruoyi-oa
 */
public class ImportConfirmDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    private Long batchId;

    /** 重复处理策略：true=更新已有档案，false=跳过重复行 */
    private Boolean updateSupport;

    public Long getBatchId()
    {
        return batchId;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public Boolean getUpdateSupport()
    {
        return updateSupport;
    }

    public void setUpdateSupport(Boolean updateSupport)
    {
        this.updateSupport = updateSupport;
    }

}
