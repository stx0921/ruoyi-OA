package com.ruoyi.oa.member.dto;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 成员查询条件
 *
 * @author ruoyi-oa
 */
public class MemberQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 姓名（模糊） */
    private String name;

    /** 学号（前缀） */
    private String studentNo;

    /** 手机号（模糊） */
    private String phone;

    /** 关键字（姓名 / 学号 / 手机号 模糊） */
    private String keyword;

    /** 部门ID */
    private Long deptId;

    /** 年级 */
    private String grade;

    /** 届次 */
    private String cohort;

    /** 职务 */
    private String position;

    /** 状态 */
    private String status;

    /** 来源 */
    private String source;

    /** 关联账号ID（查本人档案用） */
    private Long userId;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
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

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

}
