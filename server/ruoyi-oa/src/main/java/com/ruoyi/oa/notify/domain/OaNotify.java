package com.ruoyi.oa.notify.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内信对象 oa_notify
 *
 * <p>type：1 被派任务 / 2 周期提醒 / 3 截止提醒 / 4 验收结果 / 5 系统通知；
 * dedupeKey 非空时按 (user_id, dedupe_key) 唯一约束去重（INSERT IGNORE），为空表示不参与去重。</p>
 *
 * @author ruoyi-oa
 */
public class OaNotify implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 站内信ID */
    private Long notifyId;

    /** 接收人（sys_user.user_id） */
    private Long userId;

    /** 类型（1被派任务 2周期提醒 3截止提醒 4验收结果 5系统通知） */
    private String type;

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
    private Date readTime;

    /** 提醒幂等键（NULL 表示不做唯一约束） */
    private String dedupeKey;

    /** 创建时间 */
    private Date createTime;

    public Long getNotifyId()
    {
        return notifyId;
    }

    public void setNotifyId(Long notifyId)
    {
        this.notifyId = notifyId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
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

    public String getDedupeKey()
    {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey)
    {
        this.dedupeKey = dedupeKey;
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
