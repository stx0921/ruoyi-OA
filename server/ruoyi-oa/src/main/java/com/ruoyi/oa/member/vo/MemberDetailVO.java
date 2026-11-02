package com.ruoyi.oa.member.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

/**
 * 成员详情展示对象
 *
 * @author ruoyi-oa
 */
public class MemberDetailVO extends MemberListVO
{
    private static final long serialVersionUID = 1L;

    /** 学院 */
    private String college;

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 特长 */
    private String skills;

    /** 自我介绍 */
    private String intro;

    /** 是否服从调剂 */
    private String obeyAdjust;

    /** 照片文件ID */
    private Long photoFileId;

    /** 照片逻辑地址 */
    private String photoUrl;

    /** 学生证文件ID */
    private Long cardFileId;

    /** 学生证逻辑地址 */
    private String cardUrl;

    /** 备注 */
    private String remark;

    /** 离会日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date leaveDate;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

    /** 是否可见账号操作区（仅管理员） */
    private Boolean canViewAccount;

    /** 最近变动记录（最多 20 条） */
    private List<MemberLogVO> memberLogs;

    public String getCollege()
    {
        return college;
    }

    public void setCollege(String college)
    {
        this.college = college;
    }

    public String getMajor()
    {
        return major;
    }

    public void setMajor(String major)
    {
        this.major = major;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getSkills()
    {
        return skills;
    }

    public void setSkills(String skills)
    {
        this.skills = skills;
    }

    public String getIntro()
    {
        return intro;
    }

    public void setIntro(String intro)
    {
        this.intro = intro;
    }

    public String getObeyAdjust()
    {
        return obeyAdjust;
    }

    public void setObeyAdjust(String obeyAdjust)
    {
        this.obeyAdjust = obeyAdjust;
    }

    public Long getPhotoFileId()
    {
        return photoFileId;
    }

    public void setPhotoFileId(Long photoFileId)
    {
        this.photoFileId = photoFileId;
    }

    public String getPhotoUrl()
    {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl)
    {
        this.photoUrl = photoUrl;
    }

    public Long getCardFileId()
    {
        return cardFileId;
    }

    public void setCardFileId(Long cardFileId)
    {
        this.cardFileId = cardFileId;
    }

    public String getCardUrl()
    {
        return cardUrl;
    }

    public void setCardUrl(String cardUrl)
    {
        this.cardUrl = cardUrl;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public Date getLeaveDate()
    {
        return leaveDate;
    }

    public void setLeaveDate(Date leaveDate)
    {
        this.leaveDate = leaveDate;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public Boolean getCanViewAccount()
    {
        return canViewAccount;
    }

    public void setCanViewAccount(Boolean canViewAccount)
    {
        this.canViewAccount = canViewAccount;
    }

    public List<MemberLogVO> getMemberLogs()
    {
        return memberLogs;
    }

    public void setMemberLogs(List<MemberLogVO> memberLogs)
    {
        this.memberLogs = memberLogs;
    }

}
