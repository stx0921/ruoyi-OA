package com.ruoyi.oa.support;

import java.io.Serializable;

/**
 * 审批人解析结果
 *
 * @author ruoyi-oa
 */
public class LeaderRef implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 审批人账号 ID */
    private Long userId;

    /** 审批人姓名 */
    private String userName;

    /** 是否兜底代审（部门未设置部长） */
    private boolean proxy;

    public LeaderRef()
    {
    }

    public LeaderRef(Long userId, String userName, boolean proxy)
    {
        this.userId = userId;
        this.userName = userName;
        this.proxy = proxy;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public boolean isProxy()
    {
        return proxy;
    }

    public void setProxy(boolean proxy)
    {
        this.proxy = proxy;
    }
}
