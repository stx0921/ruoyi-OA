package com.ruoyi.oa.apply.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 入会审批列表 VO（PC-04 rows[]）
 *
 * @author ruoyi-oa
 */
public class ApplyListVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long id;

    /** 申请编号 */
    private String applyNo;

    /** 姓名 */
    private String name;

    /** 学号 */
    private String studentNo;

    /** 性别（0男 1女） */
    private String gender;

    /** 手机号 */
    private String phone;

    /** 意向部门ID */
    private Long deptId;

    /** 意向部门名称 */
    private String deptName;

    /** 年级（字典值） */
    private String grade;

    /** 年级标签 */
    private String gradeLabel;

    /** 状态（0待审批 1已通过 2已驳回 3已撤回 4已关闭） */
    private String status;

    /** 状态标签 */
    private String statusLabel;

    /** 审批人姓名 */
    private String auditUserName;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审批意见 */
    private String auditRemark;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 当前登录人是否可审批该条 */
    private Boolean canAudit;

    /** 审批人是否为代审（审批人不是该部门负责人时 true） */
    private Boolean proxy;

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

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Boolean getCanAudit()
    {
        return canAudit;
    }

    public void setCanAudit(Boolean canAudit)
    {
        this.canAudit = canAudit;
    }

    public Boolean getProxy()
    {
        return proxy;
    }

    public void setProxy(Boolean proxy)
    {
        this.proxy = proxy;
    }

}
