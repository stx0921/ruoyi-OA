package com.ruoyi.oa.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 请假申请对象 oa_leave
 *
 * @author ruoyi-oa
 */
public class OaLeave extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 请假单ID */
    private Long leaveId;

    /** 申请人ID */
    @Excel(name = "申请人ID")
    private Long applyUserId;

    /** 申请人姓名 */
    @Excel(name = "申请人")
    private String applyUserName;

    /** 申请部门ID */
    private Long applyDeptId;

    /** 申请部门名称（关联查询，非表字段） */
    @Excel(name = "部门")
    private String applyDeptName;

    /** 请假类型（字典 oa_leave_type） */
    @Excel(name = "请假类型", readConverterExp = "1=事假,2=病假,3=年假,4=调休,5=婚假,6=产假")
    private String leaveType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm")
    private Date endTime;

    /** 请假时长（天） */
    @Excel(name = "时长(天)")
    private BigDecimal duration;

    /** 请假事由 */
    @Excel(name = "请假事由")
    private String reason;

    /** 附件地址 */
    private String attachmentUrl;

    /** 流程实例ID */
    private String processInstanceId;

    /** 状态（0草稿 1审批中 2已通过 4已撤销） */
    @Excel(name = "状态", readConverterExp = "0=草稿,1=审批中,2=已通过,3=已驳回,4=已撤销")
    private String status;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "提交时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm")
    private Date applyTime;

    public Long getLeaveId()
    {
        return leaveId;
    }

    public void setLeaveId(Long leaveId)
    {
        this.leaveId = leaveId;
    }

    public Long getApplyUserId()
    {
        return applyUserId;
    }

    public void setApplyUserId(Long applyUserId)
    {
        this.applyUserId = applyUserId;
    }

    public String getApplyUserName()
    {
        return applyUserName;
    }

    public void setApplyUserName(String applyUserName)
    {
        this.applyUserName = applyUserName;
    }

    public Long getApplyDeptId()
    {
        return applyDeptId;
    }

    public void setApplyDeptId(Long applyDeptId)
    {
        this.applyDeptId = applyDeptId;
    }

    public String getApplyDeptName()
    {
        return applyDeptName;
    }

    public void setApplyDeptName(String applyDeptName)
    {
        this.applyDeptName = applyDeptName;
    }

    public String getLeaveType()
    {
        return leaveType;
    }

    public void setLeaveType(String leaveType)
    {
        this.leaveType = leaveType;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public BigDecimal getDuration()
    {
        return duration;
    }

    public void setDuration(BigDecimal duration)
    {
        this.duration = duration;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getAttachmentUrl()
    {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl)
    {
        this.attachmentUrl = attachmentUrl;
    }

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getApplyTime()
    {
        return applyTime;
    }

    public void setApplyTime(Date applyTime)
    {
        this.applyTime = applyTime;
    }
}
