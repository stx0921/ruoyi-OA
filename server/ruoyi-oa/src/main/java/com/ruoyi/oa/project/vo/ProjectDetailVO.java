package com.ruoyi.oa.project.vo;

import java.util.List;

/**
 * 项目详情展示对象：列表字段 + 参与成员
 *
 * @author ruoyi-oa
 */
public class ProjectDetailVO extends ProjectListVO
{
    private static final long serialVersionUID = 1L;

    /** 参与成员 */
    private List<ProjectMemberVO> members;

    public List<ProjectMemberVO> getMembers()
    {
        return members;
    }

    public void setMembers(List<ProjectMemberVO> members)
    {
        this.members = members;
    }
}
