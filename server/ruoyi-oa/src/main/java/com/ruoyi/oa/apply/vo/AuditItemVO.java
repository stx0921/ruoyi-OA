package com.ruoyi.oa.apply.vo;

import java.io.Serializable;

/**
 * 单条审批结果 VO
 *
 * @author ruoyi-oa
 */
public class AuditItemVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long applyId;

    /** 申请编号 */
    private String applyNo;

    /** 是否成功 */
    private Boolean success;

    /** 失败原因（成功为空） */
    private String message;

    /** 通过后生成的成员ID */
    private Long memberId;

    /** 通过后开通的账号ID */
    private Long userId;

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public String getApplyNo()
    {
        return applyNo;
    }

    public void setApplyNo(String applyNo)
    {
        this.applyNo = applyNo;
    }

    public Boolean getSuccess()
    {
        return success;
    }

    public void setSuccess(Boolean success)
    {
        this.success = success;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
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
