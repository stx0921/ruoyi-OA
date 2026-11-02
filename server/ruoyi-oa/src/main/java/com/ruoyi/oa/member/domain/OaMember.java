package com.ruoyi.oa.member.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 成员档案对象 oa_member
 *
 * @author ruoyi-oa
 */
public class OaMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long memberId;

    /** 成员编号（M+入会年份+4位序号） */
    @Excel(name = "成员编号")
    private String memberNo;

    /** 姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 性别（0男 1女） */
    @Excel(name = "性别", readConverterExp = "0=男,1=女")
    private String gender;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 手机号 */
    @Excel(name = "手机号")
    private String phone;

    /** 学院 */
    @Excel(name = "学院")
    private String college;

    /** 专业 */
    @Excel(name = "专业")
    private String major;

    /** 班级 */
    @Excel(name = "班级")
    private String className;

    /** 年级（字典 oa_grade） */
    @Excel(name = "年级", readConverterExp = "1=大一,2=大二,3=大三,4=大四,5=研一,6=研二,7=研三")
    private String grade;

    /** 届次（字典 oa_cohort） */
    @Excel(name = "届次")
    private String cohort;

    /** 所属部门ID */
    private Long deptId;

    /** 所属部门名称（关联查询，非表字段） */
    @Excel(name = "部门")
    private String deptName;

    /** 职务（字典 oa_position） */
    @Excel(name = "职务", readConverterExp = "1=会长,2=副会长,3=部长,4=副部长,5=干事")
    private String position;

    /** 状态（0在籍 1已退会 2已毕业） */
    @Excel(name = "状态", readConverterExp = "0=在籍,1=已退会,2=已毕业")
    private String status;

    /** 来源（1自助申请 2手动录入 3Excel导入） */
    @Excel(name = "来源", readConverterExp = "1=自助申请,2=手动录入,3=Excel导入")
    private String source;

    /** 入会日期 */
    @Excel(name = "入会日期", dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date joinDate;

    /** 离会日期 */
    @Excel(name = "离会日期", dateFormat = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date leaveDate;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 微信 */
    private String wechat;

    /** QQ */
    private String qq;

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

    /** 关联账号ID */
    private Long userId;

    /** 首次入会来源申请ID */
    private Long applyId;

    /** 账号状态（关联查询，非表字段） */
    private String accountStatus;

    /** 最后登录时间（关联查询，非表字段） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime;

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

    public String getPosition()
    {
        return position;
    }

    public void setPosition(String position)
    {
        this.position = position;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public Date getJoinDate()
    {
        return joinDate;
    }

    public void setJoinDate(Date joinDate)
    {
        this.joinDate = joinDate;
    }

    public Date getLeaveDate()
    {
        return leaveDate;
    }

    public void setLeaveDate(Date leaveDate)
    {
        this.leaveDate = leaveDate;
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

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public String getAccountStatus()
    {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus)
    {
        this.accountStatus = accountStatus;
    }

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

}
