package com.ruoyi.oa.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 待办 / 已办任务视图对象
 *
 * <p>刻意不直接返回 Flowable 的 {@code Task} 实体：它字段多、含内部引用，
 * 序列化到前端既冗余又容易随版本变化。</p>
 *
 * @author ruoyi-oa
 */
public class OaTaskVo
{
    /** 任务ID */
    private String taskId;

    /** 任务名称（节点名） */
    private String taskName;

    /** 节点 key */
    private String taskDefinitionKey;

    /** 流程实例ID */
    private String processInstanceId;

    /** 业务单号 */
    private Long leaveId;

    /** 业务单号字符串 */
    private String leaveNo;

    /** 申请人 */
    private String applyUserName;

    /** 请假类型 */
    private String leaveType;

    /** 时长 */
    private String duration;

    /** 当前办理人 */
    private String assignee;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 结束时间（已办） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public String getTaskName()
    {
        return taskName;
    }

    public void setTaskName(String taskName)
    {
        this.taskName = taskName;
    }

    public String getTaskDefinitionKey()
    {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey)
    {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public Long getLeaveId()
    {
        return leaveId;
    }

    public void setLeaveId(Long leaveId)
    {
        this.leaveId = leaveId;
    }

    public String getLeaveNo()
    {
        return leaveNo;
    }

    public void setLeaveNo(String leaveNo)
    {
        this.leaveNo = leaveNo;
    }

    public String getApplyUserName()
    {
        return applyUserName;
    }

    public void setApplyUserName(String applyUserName)
    {
        this.applyUserName = applyUserName;
    }

    public String getLeaveType()
    {
        return leaveType;
    }

    public void setLeaveType(String leaveType)
    {
        this.leaveType = leaveType;
    }

    public String getDuration()
    {
        return duration;
    }

    public void setDuration(String duration)
    {
        this.duration = duration;
    }

    public String getAssignee()
    {
        return assignee;
    }

    public void setAssignee(String assignee)
    {
        this.assignee = assignee;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }
}
