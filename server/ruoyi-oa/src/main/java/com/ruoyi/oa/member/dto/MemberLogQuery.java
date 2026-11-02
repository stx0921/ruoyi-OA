package com.ruoyi.oa.member.dto;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员变动日志查询条件
 *
 * @author ruoyi-oa
 */
public class MemberLogQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long memberId;

    /** 动作 */
    private String action;

    /** 操作人 */
    private String operatorName;

    /** 对象关键字（姓名/学号） */
    private String keyword;

    /** 部门ID */
    private Long deptId;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

}
