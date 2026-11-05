package com.ruoyi.oa.notify.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 站内信列表展示对象
 *
 * @author ruoyi-oa
 */
public class NotifyVO
{
    /** 站内信ID */
    private Long notifyId;

    /** 类型（1被派任务 2周期提醒 3截止提醒 4验收结果 5系统通知） */
    private String type;

    /** 类型文案 */
    private String typeLabel;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 业务类型（task/recur/project） */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 是否已读（0未读 1已读） */
    private String readFlag;

    /** 已读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date readTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public Long getNotifyId()
    {
        return notifyId;
    }

    public void setNotifyId(Long notifyId)
    {
        this.notifyId = notifyId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getTypeLabel()
    {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel)
    {
        this.typeLabel = typeLabel;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public String getReadFlag()
    {
        return readFlag;
    }

    public void setReadFlag(String readFlag)
    {
        this.readFlag = readFlag;
    }

    public Date getReadTime()
    {
        return readTime;
    }

    public void setReadTime(Date readTime)
    {
        this.readTime = readTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
