package com.ruoyi.oa.feedback.dto;

import java.io.Serializable;

/**
 * 官方回复 DTO
 *
 * @author ruoyi-oa
 */
public class FeedbackReplyDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long feedbackId;

    /** 回复内容（≤500 字；status=1 时必填） */
    private String replyContent;

    /** 状态（0待处理 1已回复，默认 1；置 0 时清空回复内容/人/时间） */
    private Integer status;

    public Long getFeedbackId()
    {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId)
    {
        this.feedbackId = feedbackId;
    }

    public String getReplyContent()
    {
        return replyContent;
    }

    public void setReplyContent(String replyContent)
    {
        this.replyContent = replyContent;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

}
