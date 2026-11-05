package com.ruoyi.oa.task.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 任务流转日志对象 oa_task_log（只追加）
 *
 * @author ruoyi-oa
 */
public class OaTaskLog implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long logId;

    /** 任务ID */
    private Long taskId;

    /** 动作（创建/开始/提交/验收通过/打回/取消/改派/编辑/周期生成） */
    private String action;

    /** 操作人（系统为 -1） */
    private Long operatorId;

    /** 操作人姓名（冗余） */
    private String operatorName;

    /** 原因/意见（打回、取消必填落这里） */
    private String remark;

    /** 变更前（仅变更字段 JSON） */
    private String beforeJson;

    /** 变更后（仅变更字段 JSON） */
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

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
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
