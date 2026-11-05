package com.ruoyi.oa.project.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 项目参与成员对象 oa_project_member（支持跨部门，是跨部门派任务的判定依据）
 *
 * @author ruoyi-oa
 */
public class OaProjectMember implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 项目ID */
    private Long projectId;

    /** 参与人（sys_user.user_id） */
    private Long userId;

    /** 成员档案ID（追溯，可空） */
    private Long memberId;

    /** 加入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date joinTime;

    /** 操作人 */
    private String createBy;

    /** 账号名（关联查询，非表字段） */
    private String userName;

    /** 昵称/姓名（关联查询，非表字段） */
    private String nickName;

    /** 部门ID（关联查询，非表字段） */
    private Long deptId;

    /** 部门名称（关联查询，非表字段） */
    private String deptName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Date getJoinTime()
    {
        return joinTime;
    }

    public void setJoinTime(Date joinTime)
    {
        this.joinTime = joinTime;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
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
