package com.ruoyi.oa.notify.dto;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 站内信查询条件
 *
 * <p>userId 由 Controller 强制取当前登录人，不接受前端传入，保证只能查询本人站内信。</p>
 *
 * @author ruoyi-oa
 */
public class NotifyQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 接收人（服务端强制填充为当前登录人） */
    private Long userId;

    /** 仅看未读 */
    private Boolean unreadOnly;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Boolean getUnreadOnly()
    {
        return unreadOnly;
    }

    public void setUnreadOnly(Boolean unreadOnly)
    {
        this.unreadOnly = unreadOnly;
    }
}
