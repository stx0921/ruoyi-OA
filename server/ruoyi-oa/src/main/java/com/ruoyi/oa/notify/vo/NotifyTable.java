package com.ruoyi.oa.notify.vo;

import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 站内信分页表格：在标准 TableDataInfo 上追加当前登录人未读数
 *
 * @author ruoyi-oa
 */
public class NotifyTable extends TableDataInfo
{
    private static final long serialVersionUID = 1L;

    /** 当前登录人未读条数 */
    private Long unreadCount;

    public Long getUnreadCount()
    {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount)
    {
        this.unreadCount = unreadCount;
    }
}
