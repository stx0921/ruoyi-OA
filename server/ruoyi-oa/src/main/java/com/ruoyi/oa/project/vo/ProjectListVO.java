package com.ruoyi.oa.project.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 项目列表展示对象（含任务数/已完成数/参与人数统计）
 *
 * @author ruoyi-oa
 */
public class ProjectListVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目编号 */
    private String projectNo;

    /** 项目名称 */
    private String name;

    /** 负责人ID */
    private Long ownerId;

    /** 负责人姓名 */
    private String ownerName;

    /** 状态（0进行中 1已结束） */
    private String status;

    /** 状态文案 */
    private String statusLabel;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endDate;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishTime;

    /** 项目描述 */
    private String description;

    /** 负责人部门ID */
    private Long deptId;

    /** 负责人部门名称 */
    private String deptName;

    /** 参与人数 */
    private Long memberCount;

    /** 任务总数 */
    private Long taskCount;

    /** 已完成任务数 */
    private Long doneCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getProjectNo()
    {
        return projectNo;
    }

    public void setProjectNo(String projectNo)
    {
        this.projectNo = projectNo;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Long getOwnerId()
    {
        return ownerId;
    }

    public void setOwnerId(Long ownerId)
    {
        this.ownerId = ownerId;
    }

    public String getOwnerName()
    {
        return ownerName;
    }

    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatusLabel()
    {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel)
    {
        this.statusLabel = statusLabel;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public void setStartDate(Date startDate)
    {
        this.startDate = startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public void setEndDate(Date endDate)
    {
        this.endDate = endDate;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public Long getMemberCount()
    {
        return memberCount;
    }

    public void setMemberCount(Long memberCount)
    {
        this.memberCount = memberCount;
    }

    public Long getTaskCount()
    {
        return taskCount;
    }

    public void setTaskCount(Long taskCount)
    {
        this.taskCount = taskCount;
    }

    public Long getDoneCount()
    {
        return doneCount;
    }

    public void setDoneCount(Long doneCount)
    {
        this.doneCount = doneCount;
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
