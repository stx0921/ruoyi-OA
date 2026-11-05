package com.ruoyi.oa.project.vo;

/**
 * 项目参与成员展示对象
 *
 * @author ruoyi-oa
 */
public class ProjectMemberVO
{
    /** 参与人账号ID */
    private Long userId;

    /** 账号名 */
    private String userName;

    /** 姓名/昵称 */
    private String nickName;

    /** 部门名称 */
    private String deptName;

    /** 成员档案ID（可空） */
    private Long memberId;

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

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }
}
