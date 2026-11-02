package com.ruoyi.oa.apply.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 入会申请操作记录对象 oa_member_apply_log（只追加）
 *
 * @author ruoyi-oa
 */
public class OaMemberApplyLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long logId;

    /** 申请ID（oa_member_apply.id） */
    private Long applyId;

    /** 动作（提交/重提/同意/驳回/撤回/自动关闭/代审） */
    private String action;

    /** 操作人（学生为NULL，系统为-1） */
    private Long operatorId;

    /** 操作人姓名（冗余） */
    private String operatorName;

    /** 意见/原因 */
    private String remark;

    /** 发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

}
