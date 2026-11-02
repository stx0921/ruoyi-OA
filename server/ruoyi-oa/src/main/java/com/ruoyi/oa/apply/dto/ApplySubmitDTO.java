package com.ruoyi.oa.apply.dto;

import java.io.Serializable;
import java.util.List;

/**
 * H5 提交入会申请 DTO（R2：一次可投多个部门）
 *
 * @author ruoyi-oa
 */
public class ApplySubmitDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 姓名 */
    private String name;

    /** 性别（0男 1女） */
    private String gender;

    /** 学号 */
    private String studentNo;

    /** 手机号 */
    private String phone;

    /** 学院 */
    private String college;

    /** 专业 */
    private String major;

    /** 班级 */
    private String className;

    /** 年级（字典 oa_grade 值） */
    private String grade;

    /** 届次（字典 oa_cohort 值，可空） */
    private String cohort;

    /** 特长 */
    private String skills;

    /** 自我介绍 */
    private String intro;

    /** 是否服从调剂（0否 1是） */
    private String obeyAdjust;

    /** 照片文件ID（oa_file.file_id） */
    private Long photoFileId;

    /** 学生证文件ID（oa_file.file_id） */
    private Long cardFileId;

    /** 意向部门ID列表（每个部门生成一条申请） */
    private List<Long> applyDeptIds;

    /** 图形验证码校验令牌（30 分钟） */
    private String verifyToken;

    /** 是否已阅读并同意入会须知 */
    private Boolean agreed;

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

    public List<Long> getApplyDeptIds()
    {
        return applyDeptIds;
    }

    public void setApplyDeptIds(List<Long> applyDeptIds)
    {
        this.applyDeptIds = applyDeptIds;
    }

    public String getVerifyToken()
    {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken)
    {
        this.verifyToken = verifyToken;
    }

    public Boolean getAgreed()
    {
        return agreed;
    }

    public void setAgreed(Boolean agreed)
    {
        this.agreed = agreed;
    }

}
