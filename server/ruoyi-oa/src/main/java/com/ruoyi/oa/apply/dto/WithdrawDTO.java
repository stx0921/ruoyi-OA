package com.ruoyi.oa.apply.dto;

import java.io.Serializable;

/**
 * H5 撤回申请 DTO（凭 queryToken + 手机号校验身份）
 *
 * @author ruoyi-oa
 */
public class WithdrawDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long applyId;

    /** 手机号 */
    private String phone;

    /** 进度查询令牌（30 分钟，绑定手机号） */
    private String queryToken;

    public Long getApplyId()
    {
        return applyId;
    }

    public void setApplyId(Long applyId)
    {
        this.applyId = applyId;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getQueryToken()
    {
        return queryToken;
    }

    public void setQueryToken(String queryToken)
    {
        this.queryToken = queryToken;
    }

}
