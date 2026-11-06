package com.ruoyi.oa.volunteer.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;

/**
 * 报名名单导出 VO（ExcelUtil + @Excel）
 *
 * @author ruoyi-oa
 */
public class SignupExportVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 序号 */
    @Excel(name = "序号", cellType = ColumnType.NUMERIC)
    private Integer index;

    /** 姓名 */
    @Excel(name = "姓名")
    private String name;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 学院 */
    @Excel(name = "学院")
    private String college;

    /** 专业 */
    @Excel(name = "专业")
    private String major;

    /** 报名时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "报名时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date signupTime;

    /** 审核状态标签 */
    @Excel(name = "审核状态")
    private String auditStatusLabel;

    /** 驳回原因 */
    @Excel(name = "驳回原因", width = 30)
    private String auditRemark;

    /** 志愿时长（小时，取活动当前时长） */
    @Excel(name = "志愿时长(小时)", cellType = ColumnType.NUMERIC)
    private BigDecimal durationHours;

    public Integer getIndex()
    {
        return index;
    }

    public void setIndex(Integer index)
    {
        this.index = index;
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

    public Date getSignupTime()
    {
        return signupTime;
    }

    public void setSignupTime(Date signupTime)
    {
        this.signupTime = signupTime;
    }

    public String getAuditStatusLabel()
    {
        return auditStatusLabel;
    }

    public void setAuditStatusLabel(String auditStatusLabel)
    {
        this.auditStatusLabel = auditStatusLabel;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

    public BigDecimal getDurationHours()
    {
        return durationHours;
    }

    public void setDurationHours(BigDecimal durationHours)
    {
        this.durationHours = durationHours;
    }

}
