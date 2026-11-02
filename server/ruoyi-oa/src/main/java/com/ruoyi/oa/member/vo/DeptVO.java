package com.ruoyi.oa.member.vo;

import java.util.List;

/**
 * 部门与负责人展示对象
 *
 * @author ruoyi-oa
 */
public class DeptVO
{
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 父部门ID */
    private Long parentId;

    /** 排序 */
    private Integer orderNum;

    /** 状态（0正常 1停用） */
    private String status;

    /** 在籍人数 */
    private Long memberCount;

    /** 正部长账号ID */
    private Long leaderUserId;

    /** 正部长姓名 */
    private String leaderName;

    /** 副部长列表 */
    private List<DeptLeaderVO> viceLeaders;

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

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
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

    public Long getMemberCount()
    {
        return memberCount;
    }

    public void setMemberCount(Long memberCount)
    {
        this.memberCount = memberCount;
    }

    public Long getLeaderUserId()
    {
        return leaderUserId;
    }

    public void setLeaderUserId(Long leaderUserId)
    {
        this.leaderUserId = leaderUserId;
    }

    public String getLeaderName()
    {
        return leaderName;
    }

    public void setLeaderName(String leaderName)
    {
        this.leaderName = leaderName;
    }

    public List<DeptLeaderVO> getViceLeaders()
    {
        return viceLeaders;
    }

    public void setViceLeaders(List<DeptLeaderVO> viceLeaders)
    {
        this.viceLeaders = viceLeaders;
    }

}
