package com.ruoyi.oa.apply.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 批量审批结果 VO（部分失败不回滚已成功项）
 *
 * @author ruoyi-oa
 */
public class AuditResultVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 成功条数 */
    private Integer successCount;

    /** 失败条数 */
    private Integer failCount;

    /** 逐条结果 */
    private List<AuditItemVO> items;

    public Integer getSuccessCount()
    {
        return successCount;
    }

    public void setSuccessCount(Integer successCount)
    {
        this.successCount = successCount;
    }

    public Integer getFailCount()
    {
        return failCount;
    }

    public void setFailCount(Integer failCount)
    {
        this.failCount = failCount;
    }

    public List<AuditItemVO> getItems()
    {
        return items;
    }

    public void setItems(List<AuditItemVO> items)
    {
        this.items = items;
    }

}
