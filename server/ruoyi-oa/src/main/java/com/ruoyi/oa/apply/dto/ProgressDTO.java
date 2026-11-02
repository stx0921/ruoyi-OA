package com.ruoyi.oa.apply.dto;

import java.io.Serializable;

/**
 * H5 进度查询 DTO（手机号 + 图形验证码）
 *
 * @author ruoyi-oa
 */
public class ProgressDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 手机号 */
    private String phone;

    /** 图形验证码 UUID */
    private String captchaUuid;

    /** 图形验证码输入值 */
    private String captchaCode;

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getCaptchaUuid()
    {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid)
    {
        this.captchaUuid = captchaUuid;
    }

    public String getCaptchaCode()
    {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode)
    {
        this.captchaCode = captchaCode;
    }

}
