package com.ruoyi.oa.apply.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 入会申请对象 oa_member_apply
 *
 * @author ruoyi-oa
 */
public class OaMemberApply extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 申请ID（基线字段名即 id） */
    private Long id;

    /** 申请编号（AP+yyyyMMdd+3位序号） */
    private String applyNo;

    /** 学号 */
    private String studentNo;

    /** 姓名 */
    private String name;

    /** 性别（0男 1女） */
    private String gender;

    /** 手机号 */
    private String phone;

    /** 学院 */
    private String college;

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 年级（字典 oa_grade） */
    private String grade;

    /** 届次（字典 oa_cohort） */
    private String cohort;

    /** 意向部门ID */
    private Long deptId;

    /** 意向部门名称（关联查询，非表字段） */
    private String deptName;

    /** 特长 */
    private String skills;

    /** 自我介绍 */
    private String intro;

    /** 是否服从调剂（0否 1是） */
    private String obeyAdjust;

    /** 照片逻辑地址（/oa/file/{fileId}） */
    private String photoUrl;

    /** 学生证逻辑地址（/oa/file/{fileId}） */
    private String cardUrl;

    /** 状态（0待审批 1已通过 2已驳回 3已撤回 4已关闭） */
    private String status;

    /** 审批人ID（含代审） */
    private Long auditUserId;

    /** 审批人姓名（冗余） */
    private String auditUserName;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审批意见（驳回必填；自动关闭原因也写这里） */
    private String auditRemark;

    /** 修改重提次数（R5，只统计不限制） */
    private Integer retryCount;

    /** 通过后回写的成员ID */
    private Long memberId;

    /** 提交IP */
    private String ip;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getApplyNo()
    {
        return applyNo;
    }

    public void setApplyNo(String applyNo)
    {
        this.applyNo = applyNo;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
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

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
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

    public String getPhotoUrl()
    {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl)
    {
        this.photoUrl = photoUrl;
    }

    public String getCardUrl()
    {
        return cardUrl;
    }

    public void setCardUrl(String cardUrl)
    {
        this.cardUrl = cardUrl;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getAuditUserId()
    {
        return auditUserId;
    }

    public void setAuditUserId(Long auditUserId)
    {
        this.auditUserId = auditUserId;
    }

    public String getAuditUserName()
    {
        return auditUserName;
    }

    public void setAuditUserName(String auditUserName)
    {
        this.auditUserName = auditUserName;
    }

    public Date getAuditTime()
    {
        return auditTime;
    }

    public void setAuditTime(Date auditTime)
    {
        this.auditTime = auditTime;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

    public Integer getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount)
    {
        this.retryCount = retryCount;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

}
