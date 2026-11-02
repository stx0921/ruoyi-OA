package com.ruoyi.oa.apply.dto;

import java.io.Serializable;

/**
 * H5 驳回后修改重提 DTO（R5）
 * <p>dept_id 不允许修改（换部门等于新申请）；手机号允许修改，但身份凭 queryToken 绑定原申请手机号校验。</p>
 *
 * @author ruoyi-oa
 */
public class ApplyResubmitDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long applyId;

    /** 手机号（可为新手机号） */
    private String phone;

    /** 进度查询令牌（30 分钟，绑定原申请手机号） */
    private String queryToken;

    /** 姓名 */
    private String name;

    /** 性别（0男 1女） */
    private String gender;

    /** 学院 */
    private String college;

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 年级（字典 oa_grade 值） */
    private String grade;

    /** 届次（字典 oa_cohort 值） */
    private String cohort;

    /** 特长 */
    private String skills;

    /** 自我介绍 */
    private String intro;

    /** 是否服从调剂（0否 1是） */
    private String obeyAdjust;

    /** 照片文件ID */
    private Long photoFileId;

    /** 学生证文件ID */
    private Long cardFileId;

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getQueryToken()
    {
        return queryToken;
    }

    public void setQueryToken(String queryToken)
    {
        this.queryToken = queryToken;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

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

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public String getCohort()
    {
        return cohort;
    }

    public void setCohort(String cohort)
    {
        this.cohort = cohort;
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

    public Long getCardFileId()
    {
        return cardFileId;
    }

    public void setCardFileId(Long cardFileId)
    {
        this.cardFileId = cardFileId;
    }

}
