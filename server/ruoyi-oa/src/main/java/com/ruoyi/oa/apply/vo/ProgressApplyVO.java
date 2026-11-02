package com.ruoyi.oa.apply.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * H5 报名进度单条申请 VO
 *
 * @author ruoyi-oa
 */
public class ProgressApplyVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long applyId;

    /** 申请编号 */
    private String applyNo;

    /** 意向部门ID */
    private Long deptId;

    /** 意向部门名称 */
    private String deptName;

    /** 状态（0待审批 1已通过 2已驳回 3已撤回 4已关闭） */
    private String status;

    /** 状态标签 */
    private String statusLabel;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 审批意见（驳回原因 / 关闭原因） */
    private String auditRemark;

    /** 审批人姓名 */
    private String auditUserName;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 通过后生成的成员ID */
    private Long memberId;

    /** 账号指引（通过后展示） */
    private String accountTip;

    /** 原申请表单快照：姓名（H5-02 修改重提预填） */
    private String name;

    /** 原申请表单快照：性别 */
    private String gender;

    /** 原申请表单快照：手机号 */
    private String phone;

    /** 原申请表单快照：学院 */
    private String college;

    /** 原申请表单快照：专业 */
    private String major;

    /** 原申请表单快照：班级 */
    private String className;

    /** 原申请表单快照：年级（字典值） */
    private String grade;

    /** 原申请表单快照：届次 */
    private String cohort;

    /** 原申请表单快照：特长 */
    private String skills;

    /** 原申请表单快照：自我介绍 */
    private String intro;

    /** 原申请表单快照：是否服从调剂 */
    private String obeyAdjust;

    /** 原申请表单快照：照片文件ID */
    private Long photoFileId;

    /** 原申请表单快照：学生证文件ID */
    private Long cardFileId;

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public String getApplyNo()
    {
        return applyNo;
    }

    public void setApplyNo(String applyNo)
    {
        this.applyNo = applyNo;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatusLabel()
    {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel)
    {
        this.statusLabel = statusLabel;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
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

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getAccountTip()
    {
        return accountTip;
    }

    public void setAccountTip(String accountTip)
    {
        this.accountTip = accountTip;
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
