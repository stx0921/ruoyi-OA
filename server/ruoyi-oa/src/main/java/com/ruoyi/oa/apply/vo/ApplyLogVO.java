package com.ruoyi.oa.apply.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 入会申请时间轴 VO
 *
 * @author ruoyi-oa
 */
public class ApplyLogVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long logId;

    /** 动作 */
    private String action;

    /** 操作人姓名（学生操作为 NULL，系统显示「系统」） */
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

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
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
