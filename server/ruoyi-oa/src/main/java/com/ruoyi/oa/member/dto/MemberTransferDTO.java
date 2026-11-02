package com.ruoyi.oa.member.dto;

/**
 * 成员调部门/升职参数
 *
 * @author ruoyi-oa
 */
public class MemberTransferDTO
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long memberId;

    /** 目标部门ID */
    private Long deptId;

    /** 目标职务 */
    private String position;

    /** 备注 */
    private String remark;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position = position;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

}
