package com.ruoyi.oa.member.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 成员状态变更参数（退会/毕业/恢复）
 *
 * @author ruoyi-oa
 */
public class MemberStatusDTO
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long memberId;

    /** 目标状态（0在籍 1已退会 2已毕业） */
    private String status;

    /** 离会日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date leaveDate;

    /** 备注 */
    private String remark;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getLeaveDate()
    {
        return leaveDate;
    }

    public void setLeaveDate(Date leaveDate)
    {
        this.leaveDate = leaveDate;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

}
