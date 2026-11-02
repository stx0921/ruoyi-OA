package com.ruoyi.oa.member.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 成员列表展示对象（敏感字段已按角色处理）
 *
 * @author ruoyi-oa
 */
public class MemberListVO
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long memberId;

    /** 成员编号 */
    private String memberNo;

    /** 姓名 */
    private String name;

    /** 性别 */
    private String gender;

    /** 性别文案 */
    private String genderLabel;

    /** 学号 */
    private String studentNo;

    /** 手机号（干事视角打码） */
    private String phone;

    /** 邮箱（干事视角打码） */
    private String email;

    /** 微信（干事视角打码/置空） */
    private String wechat;

    /** QQ（干事视角打码/置空） */
    private String qq;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 年级 */
    private String grade;

    /** 年级文案 */
    private String gradeLabel;

    /** 届次 */
    private String cohort;

    /** 届次文案 */
    private String cohortLabel;

    /** 职务 */
    private String position;

    /** 职务文案 */
    private String positionLabel;

    /** 状态 */
    private String status;

    /** 状态文案 */
    private String statusLabel;

    /** 来源 */
    private String source;

    /** 来源文案 */
    private String sourceLabel;

    /** 入会日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date joinDate;

    /** 关联账号ID */
    private Long userId;

    /** 账号状态（0正常 1停用） */
    private String accountStatus;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getMemberNo()
    {
        return memberNo;
    }

    public void setMemberNo(String memberNo)
    {
        this.memberNo = memberNo;
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

    public String getGenderLabel()
    {
        return genderLabel;
    }

    public void setGenderLabel(String genderLabel)
    {
        this.genderLabel = genderLabel;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getWechat()
    {
        return wechat;
    }

    public void setWechat(String wechat)
    {
        this.wechat = wechat;
    }

    public String getQq()
    {
        return qq;
    }

    public void setQq(String qq)
    {
        this.qq = qq;
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

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    public String getGradeLabel()
    {
        return gradeLabel;
    }

    public void setGradeLabel(String gradeLabel)
    {
        this.gradeLabel = gradeLabel;
    }

    public String getCohort()
    {
        return cohort;
    }

    public void setCohort(String cohort)
    {
        this.cohort = cohort;
    }

    public String getCohortLabel()
    {
        return cohortLabel;
    }

    public void setCohortLabel(String cohortLabel)
    {
        this.cohortLabel = cohortLabel;
    }

    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position = position;
    }

    public String getPositionLabel()
    {
        return positionLabel;
    }

    public void setPositionLabel(String positionLabel)
    {
        this.positionLabel = positionLabel;
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

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getSourceLabel()
    {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel)
    {
        this.sourceLabel = sourceLabel;
    }

    public Date getJoinDate()
    {
        return joinDate;
    }

    public void setJoinDate(Date joinDate)
    {
        this.joinDate = joinDate;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAccountStatus()
    {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus)
    {
        this.accountStatus = accountStatus;
    }

}
