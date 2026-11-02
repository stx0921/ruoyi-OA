package com.ruoyi.oa.member.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 部门负责人映射对象 oa_dept_leader
 *
 * @author ruoyi-oa
 */
public class OaDeptLeader extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 部门ID */
    private Long deptId;

    /** 负责人账号ID */
    private Long userId;

    /** 负责人类型（1正部长 2副部长） */
    private String leaderType;

    /** 展示顺序 */
    private Integer sort;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getLeaderType()
    {
        return leaderType;
    }

    public void setLeaderType(String leaderType)
    {
        this.leaderType = leaderType;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

}
