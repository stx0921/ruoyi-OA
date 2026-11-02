package com.ruoyi.oa.member.vo;

/**
 * 部门下拉选项
 *
 * @author ruoyi-oa
 */
public class DeptOptionVO
{
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

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

}
