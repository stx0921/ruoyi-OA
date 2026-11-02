package com.ruoyi.oa.apply.vo;

import java.util.List;

/**
 * 入会审批详情 VO = 列表字段 + 完整表单 + 附件 + 时间轴
 *
 * @author ruoyi-oa
 */
public class ApplyDetailVO extends ApplyListVO
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

    /** 是否服从调剂（0否 1是） */
    private String obeyAdjust;

    /** 重提次数 */
    private Integer retryCount;

    /** 申请时的届次快照 */
    private String cohort;

    /** 通过后回写的成员ID（前端「查看成员」用） */
    private Long memberId;

    /** 照片文件ID */
    private Long photoFileId;

    /** 照片逻辑地址（/oa/file/{fileId}） */
    private String photoUrl;

    /** 学生证文件ID */
    private Long cardFileId;

    /** 学生证逻辑地址（/oa/file/{fileId}） */
    private String cardUrl;

    /** 审批时间轴（正序） */
    private List<ApplyLogVO> logs;

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

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public String getCohort()
    {
        return cohort;
    }

    public void setCohort(String cohort)
    {
        this.cohort = cohort;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
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

    public List<ApplyLogVO> getLogs()
    {
        return logs;
    }

    public void setLogs(List<ApplyLogVO> logs)
    {
        this.logs = logs;
    }

}
