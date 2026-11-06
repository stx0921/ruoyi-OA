package com.ruoyi.oa.volunteer.dto;

import java.io.Serializable;

/**
 * H5 免登录报名 DTO
 *
 * @author ruoyi-oa
 */
public class PublicSignupDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 姓名（2–20 字） */
    private String name;

    /** 学号（8–12 位数字，同活动内唯一） */
    private String studentNo;

    /** 学院（2–50 字） */
    private String college;

    /** 专业（2–50 字） */
    private String major;

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

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

}
