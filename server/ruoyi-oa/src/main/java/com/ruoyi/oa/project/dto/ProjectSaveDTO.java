package com.ruoyi.oa.project.dto;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 项目新增/编辑入参
 *
 * @author ruoyi-oa
 */
public class ProjectSaveDTO
{
    /** 项目ID（编辑必填） */
    private Long projectId;

    /** 项目名称（2–50 字） */
    private String name;

    /** 项目负责人 */
    private Long ownerId;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date endDate;

    /** 项目描述 */
    private String description;

    /** 参与成员（可跨部门，负责人保存时自动补入） */
    private List<Long> memberIds;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<Long> getMemberIds()
    {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds)
    {
        this.memberIds = memberIds;
    }
}
