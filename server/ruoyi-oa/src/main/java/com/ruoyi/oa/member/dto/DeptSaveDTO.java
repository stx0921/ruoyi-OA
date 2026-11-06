package com.ruoyi.oa.member.dto;

/**
 * 组织部门新增 / 编辑参数
 *
 * @author ruoyi-oa
 */
public class DeptSaveDTO
{
    /** 部门 ID（编辑时必填） */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 显示排序 */
    private Integer orderNum;

    /** 状态（0 正常 1 停用） */
    private String status;

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

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
