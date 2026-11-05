package com.ruoyi.oa.recur.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 周期规则保存结果（返回规则ID与下次生成日）
 *
 * @author ruoyi-oa
 */
public class RecurSaveResultVO
{
    /** 规则ID */
    private Long recurId;

    /** 下次生成日 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date nextDate;

    public Long getRecurId()
    {
        return recurId;
    }

    public void setRecurId(Long recurId)
    {
        this.recurId = recurId;
    }

    public Date getNextDate()
    {
        return nextDate;
    }

    public void setNextDate(Date nextDate)
    {
        this.nextDate = nextDate;
    }
}
