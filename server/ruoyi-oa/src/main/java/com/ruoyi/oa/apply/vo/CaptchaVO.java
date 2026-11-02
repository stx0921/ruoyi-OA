package com.ruoyi.oa.apply.vo;

import java.io.Serializable;

/**
 * 图形验证码 VO
 *
 * @author ruoyi-oa
 */
public class CaptchaVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 验证码 UUID */
    private String uuid;

    /** 图片 base64（jpg） */
    private String img;

    /** 有效期（秒） */
    private Integer expireIn;

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public String getImg()
    {
        return img;
    }

    public void setImg(String img)
    {
        this.img = img;
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
