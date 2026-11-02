package com.ruoyi.oa.member.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * 编辑成员参数（在新增字段基础上加 memberId）
 *
 * @author ruoyi-oa
 */
public class MemberUpdateDTO extends MemberAddDTO
{
    private static final long serialVersionUID = 1L;

    /** 成员ID */
    private Long memberId;

    /** 离会日期（仅状态变更时使用，编辑忽略） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date leaveDate;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Date getLeaveDate()
    {
        return leaveDate;
    }

    public void setLeaveDate(Date leaveDate)
    {
        this.leaveDate = leaveDate;
    }

}
