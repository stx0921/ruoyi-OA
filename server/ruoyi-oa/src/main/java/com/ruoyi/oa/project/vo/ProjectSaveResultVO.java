package com.ruoyi.oa.project.vo;

/**
 * 项目新增结果（返回给前端的项目ID与编号）
 *
 * @author ruoyi-oa
 */
public class ProjectSaveResultVO
{
    /** 项目ID */
    private Long projectId;

    /** 项目编号 */
    private String projectNo;

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
}
