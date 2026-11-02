package com.ruoyi.oa.apply.vo;

import java.io.Serializable;

/**
 * 图形验证码校验通过后的令牌 VO
 *
 * @author ruoyi-oa
 */
public class VerifyTokenVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 校验令牌（30 分钟内可复用，用于上传/提交） */
    private String verifyToken;

    /** 有效期（秒） */
    private Integer expireIn;

    public String getVerifyToken()
    {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken)
    {
        this.verifyToken = verifyToken;
    }

    public Integer getExpireIn()
    {
        return expireIn;
    }

    public void setExpireIn(Integer expireIn)
    {
        this.expireIn = expireIn;
    }

}
