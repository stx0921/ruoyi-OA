package com.ruoyi.oa.project.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 项目对象 oa_project（两态：0 进行中 / 1 已结束）
 *
 * @author ruoyi-oa
 */
public class OaProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 项目ID */
    private Long projectId;

    /** 项目编号（P+yyyy+2位序号，如 P202601） */
    private String projectNo;

    /** 项目名称（2-50 字） */
    private String name;

    /** 项目负责人（sys_user.user_id） */
    private Long ownerId;

    /** 负责人姓名（冗余，列表直出） */
    private String ownerName;

    /** 状态（0进行中 1已结束） */
    private String status;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /** 结束日期（不得早于开始日期） */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endDate;

    /** 结束时间（置为已结束时写入） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date finishTime;

    /** 项目描述 */
    private String description;

    /** 负责人部门（数据权限锚点） */
    private Long deptId;

    /** 部门名称（关联查询，非表字段） */
    private String deptName;

    /** 参与人数（关联统计，非表字段） */
    private Long memberCount;

    /** 任务总数（关联统计，非表字段） */
    private Long taskCount;

    /** 已完成任务数（关联统计，非表字段） */
    private Long doneCount;

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
}
