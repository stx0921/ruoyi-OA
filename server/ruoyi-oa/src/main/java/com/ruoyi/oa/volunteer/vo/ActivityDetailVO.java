package com.ruoyi.oa.volunteer.vo;

/**
 * 志愿活动详情 VO（列表字段 + 详情正文 + 报名统计 + 二维码）
 *
 * @author ruoyi-oa
 */
public class ActivityDetailVO extends ActivityListVO
{
    private static final long serialVersionUID = 1L;

    /** 活动详情 */
    private String content;

    /** 待审核报名数 */
    private Long pendingCount;

    /** 驳回报名数 */
    private Long rejectCount;

    /** 二维码 base64（data:image/png;base64,...） */
    private String qrcode;

    /** 二维码内容（H5 报名页链接） */
    private String qrcodeContent;

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Long getPendingCount()
    {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount)
    {
        this.pendingCount = pendingCount;
    }

    public Long getRejectCount()
    {
        return rejectCount;
    }

    public void setRejectCount(Long rejectCount)
    {
        this.rejectCount = rejectCount;
    }

    public String getQrcode()
    {
        return qrcode;
    }

    public void setQrcode(String qrcode)
    {
        this.qrcode = qrcode;
    }

    public String getQrcodeContent()
    {
        return qrcodeContent;
    }

    public void setQrcodeContent(String qrcodeContent)
    {
        this.qrcodeContent = qrcodeContent;
    }

}
