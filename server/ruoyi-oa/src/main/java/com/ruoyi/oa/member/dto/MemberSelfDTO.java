package com.ruoyi.oa.member.dto;

/**
 * 干事维护本人资料参数（白名单字段）
 *
 * @author ruoyi-oa
 */
public class MemberSelfDTO
{
    private static final long serialVersionUID = 1L;

    /** 邮箱 */
    private String email;

    /** 微信 */
    private String wechat;

    /** QQ */
    private String qq;

    /** 特长 */
    private String skills;

    /** 自我介绍 */
    private String intro;

    /** 照片文件ID */
    private Long photoFileId;

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getWechat()
    {
        return wechat;
    }

    public void setWechat(String wechat)
    {
        this.wechat = wechat;
    }

    public String getQq()
    {
        return qq;
    }

    public void setQq(String qq)
    {
        this.qq = qq;
    }

    public String getSkills()
    {
        return skills;
    }

    public void setSkills(String skills)
    {
        this.skills = skills;
    }

    public String getIntro()
    {
        return intro;
    }

    public void setIntro(String intro)
    {
        this.intro = intro;
    }

    public Long getPhotoFileId()
    {
        return photoFileId;
    }

    public void setPhotoFileId(Long photoFileId)
    {
        this.photoFileId = photoFileId;
    }

}
