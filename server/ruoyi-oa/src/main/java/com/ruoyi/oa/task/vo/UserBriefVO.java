package com.ruoyi.oa.task.vo;

import java.io.Serializable;

/**
 * 用户简要信息 VO（协作人 / 详情展示）
 *
 * @author ruoyi-oa
 */
public class UserBriefVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 显示姓名（昵称优先） */
    private String userName;

    /** 部门ID（内部判权用） */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

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
