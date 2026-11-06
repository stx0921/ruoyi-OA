package com.ruoyi.oa.feedback.dto;

import java.io.Serializable;

/**
 * 工单列表查询条件（用户端 / 管理端共用）
 *
 * @author ruoyi-oa
 */
public class FeedbackQuery implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 范围：all 全部 / mine 我的（用户端） */
    private String scope;

    /** 类型（1 Bug / 2 优化建议 / 3 新增功能 / 4 其他） */
    private String type;

    /** 状态（0待处理 1已回复） */
    private String status;

    /** 关键词（编号/标题/内容） */
    private String keyword;

    /** 提交时间起（yyyy-MM-dd） */
    private String beginTime;

    /** 提交时间止（yyyy-MM-dd） */
    private String endTime;

    /** 提交人ID（管理端筛选用） */
    private Long submitterId;

    /** 是否匿名（0否 1是，管理端筛选用） */
    private String isAnonymous;

    /** 当前登录人ID（服务端注入，前端不传） */
    private Long currentUserId;

    public String getScope()
    {
        return scope;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public String getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(String beginTime)
    {
        this.beginTime = beginTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }

    public Long getSubmitterId()
    {
        return submitterId;
    }

    public void setSubmitterId(Long submitterId)
    {
        this.submitterId = submitterId;
    }

    public String getIsAnonymous()
    {
        return isAnonymous;
    }

    public void setIsAnonymous(String isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }

    public Long getCurrentUserId()
    {
        return currentUserId;
    }

    public void setCurrentUserId(Long currentUserId)
    {
        this.currentUserId = currentUserId;
    }

}
