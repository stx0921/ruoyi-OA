package com.ruoyi.oa.member.vo;

/**
 * 部门负责人展示对象
 *
 * @author ruoyi-oa
 */
public class DeptLeaderVO
{
    private static final long serialVersionUID = 1L;

    /** 账号ID */
    private Long userId;

    /** 姓名 */
    private String userName;

    /** 负责人类型（1正 2副） */
    private String leaderType;

    /** 排序 */
    private Integer sort;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
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
