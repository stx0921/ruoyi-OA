package com.ruoyi.oa.feedback.vo;

import java.util.List;

/**
 * 工单详情 VO（列表字段 + 正文分区 + 附件 + 官方回复）
 *
 * @author ruoyi-oa
 */
public class FeedbackDetailVO extends FeedbackListVO
{
    private static final long serialVersionUID = 1L;

    /** 详细描述 */
    private String content;

    /** 复现步骤 */
    private String steps;

    /** 期望结果 */
    private String expectResult;

    /** 回复人ID */
    private Long replyBy;

    /** 附件列表 */
    private List<FeedbackFileVO> attachments;

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getSteps()
    {
        return steps;
    }

    public void setSteps(String steps)
    {
        this.steps = steps;
    }

    public String getExpectResult()
    {
        return expectResult;
    }

    public void setExpectResult(String expectResult)
    {
        this.expectResult = expectResult;
    }

    public Long getReplyBy()
    {
        return replyBy;
    }

    public void setReplyBy(Long replyBy)
    {
        this.replyBy = replyBy;
    }

    public List<FeedbackFileVO> getAttachments()
    {
        return attachments;
    }

    public void setAttachments(List<FeedbackFileVO> attachments)
    {
        this.attachments = attachments;
    }

}
