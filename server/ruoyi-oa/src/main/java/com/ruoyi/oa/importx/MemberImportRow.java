package com.ruoyi.oa.importx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.ruoyi.oa.member.vo.ImportRowErrorVO;

/**
 * Excel 导入行（12 列原始值 + 校验归一化值 + 行级校验结果）
 * <p>第 1 行为表头，第 2 行为示例；数据行物理行号从 2 开始（含表头口径）。</p>
 *
 * @author ruoyi-oa
 */
public class MemberImportRow implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Excel 物理行号（含表头，便于用户定位） */
    private Integer rowNum;

    /** 姓名 */
    private String name;

    /** 性别原始值（男/女/0/1） */
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

    /** 年级原始值（标签，如 大二） */
    private String grade;

    /** 届次原始值（标签/值，如 2026届） */
    private String cohort;

    /** 部门名称 */
    private String deptName;

    /** 职务原始值（标签，如 干事） */
    private String position;

    /** 入会日期原始文本（yyyy-MM-dd） */
    private String joinDateText;

    /** 归一化性别（0男 1女） */
    private String genderCode;

    /** 归一化年级（字典值） */
    private String gradeValue;

    /** 归一化届次（字典值） */
    private String cohortValue;

    /** 归一化职务（字典值） */
    private String positionCode;

    /** 归一化部门ID */
    private Long deptId;

    /** 归一化入会日期 */
    private Date joinDate;

    /** 行结果：NEW / UPDATE / SKIP / ERROR */
    private String result;

    /** 行级错误 */
    private List<ImportRowErrorVO> errors = new ArrayList<>();

    /**
     * 是否全空行（12 列全空）
     */
    public boolean isEmptyRow()
    {
        return isEmpty(name) && isEmpty(gender) && isEmpty(studentNo) && isEmpty(phone)
                && isEmpty(college) && isEmpty(major) && isEmpty(className) && isEmpty(grade)
                && isEmpty(cohort) && isEmpty(deptName) && isEmpty(position) && isEmpty(joinDateText);
    }

    private boolean isEmpty(String value)
    {
        return value == null || value.trim().isEmpty();
    }

    public Integer getRowNum()
    {
        return rowNum;
    }

    public void setRowNum(Integer rowNum)
    {
        this.rowNum = rowNum;
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

    public String getJoinDateText()
    {
        return joinDateText;
    }

    public void setJoinDateText(String joinDateText)
    {
        this.joinDateText = joinDateText;
    }

    public String getGenderCode()
    {
        return genderCode;
    }

    public void setGenderCode(String genderCode)
    {
        this.genderCode = genderCode;
    }

    public String getGradeValue()
    {
        return gradeValue;
    }

    public void setGradeValue(String gradeValue)
    {
        this.gradeValue = gradeValue;
    }

    public String getCohortValue()
    {
        return cohortValue;
    }

    public void setCohortValue(String cohortValue)
    {
        this.cohortValue = cohortValue;
    }

    public String getPositionCode()
    {
        return positionCode;
    }

    public void setPositionCode(String positionCode)
    {
        this.positionCode = positionCode;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Date getJoinDate()
    {
        return joinDate;
    }

    public void setJoinDate(Date joinDate)
    {
        this.joinDate = joinDate;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public List<ImportRowErrorVO> getErrors()
    {
        return errors;
    }

    public void setErrors(List<ImportRowErrorVO> errors)
    {
        this.errors = errors;
    }

}
