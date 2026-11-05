package com.ruoyi.oa.project.dto;

/**
 * 结束项目入参
 *
 * @author ruoyi-oa
 */
public class ProjectFinishDTO
{
    /** 项目ID */
    private Long projectId;

    /** 是否忽略未完成任务强制归档（true=前端已二次确认） */
    private Boolean force;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Boolean getForce()
    {
        return force;
    }

    public void setForce(Boolean force)
    {
        this.force = force;
    }
}
