package com.ruoyi.oa.feedback.dto;

import java.io.Serializable;

/**
 * 批量标记已回复 DTO
 *
 * @author ruoyi-oa
 */
public class FeedbackBatchReplyDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 工单ID列表（≤200） */
    private Long[] ids;

    /** 统一回复文案（为空时使用默认文案「已批量处理，感谢反馈」） */
    private String replyContent;

    public Long[] getIds()
    {
        return ids;
    }

    public void setIds(Long[] ids)
    {
        this.ids = ids;
    }

    public String getReplyContent()
    {
        return replyContent;
    }

    public void setReplyContent(String replyContent)
    {
        this.replyContent = replyContent;
    }

}
