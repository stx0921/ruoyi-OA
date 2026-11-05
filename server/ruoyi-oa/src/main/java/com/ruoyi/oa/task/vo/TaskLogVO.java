package com.ruoyi.oa.task.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务流转日志 VO（时间轴倒序）
 *
 * @author ruoyi-oa
 */
public class TaskLogVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 动作 */
    private String action;

    /** 操作人姓名 */
    private String operatorName;

    /** 备注 / 原因 */
    private String remark;

    /** 变更前（仅变更字段） */
    private String beforeJson;

    /** 变更后（仅变更字段） */
    private String afterJson;

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

    public String getBeforeJson()
    {
        return beforeJson;
    }

    public void setBeforeJson(String beforeJson)
    {
        this.beforeJson = beforeJson;
    }

    public String getAfterJson()
    {
        return afterJson;
    }

    public void setAfterJson(String afterJson)
    {
        this.afterJson = afterJson;
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
