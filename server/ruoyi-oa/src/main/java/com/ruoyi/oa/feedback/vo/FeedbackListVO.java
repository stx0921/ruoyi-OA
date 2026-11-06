package com.ruoyi.oa.feedback.vo;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工单列表 VO（用户端与管理端共用，匿名脱敏由服务端决定）
 *
 * @author ruoyi-oa
 */
public class FeedbackListVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long feedbackId;

    /** 工单编号 */
    private String ticketNo;

    /** 类型 */
    private String type;

    /** 类型标签 */
    private String typeLabel;

    /** 标题 */
    private String title;

    /** 影响模块ID */
    private Long moduleId;

    /** 影响模块名称 */
    private String moduleName;

    /** 影响页面ID */
    private Long pageId;

    /** 影响页面名称 */
    private String pageName;

    /** 是否匿名（0否 1是） */
    private String isAnonymous;

    /** 状态 */
    private String status;

    /** 状态标签 */
    private String statusLabel;

    /** 提交人ID（匿名且无管理权限时为 null） */
    private Long submitterId;

    /** 提交人姓名（匿名且无管理权限时为「匿名用户」） */
    private String submitterName;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 官方回复 */
    private String replyContent;

    /** 回复人姓名 */
    private String replyByName;

    /** 回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date replyTime;

    /** 附件数 */
    private Integer attachmentCount;

    /** 当前登录人是否可撤回（本人 + 待处理 + 未删除） */
    private Boolean canWithdraw;

    public Long getFeedbackId()
    {
        return feedbackId;
    }

    public void setFeedbackId(Long feedbackId)
    {
        this.feedbackId = feedbackId;
    }

    public String getTicketNo()
    {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo)
    {
        this.ticketNo = ticketNo;
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

    public Long getModuleId()
    {
        return moduleId;
    }

    public void setModuleId(Long moduleId)
    {
        this.moduleId = moduleId;
    }

    public String getModuleName()
    {
        return moduleName;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public Long getPageId()
    {
        return pageId;
    }

    public void setPageId(Long pageId)
    {
        this.pageId = pageId;
    }

    public String getPageName()
    {
        return pageName;
    }

    public void setPageName(String pageName)
    {
        this.pageName = pageName;
    }

    public String getIsAnonymous()
    {
        return isAnonymous;
    }

    public void setIsAnonymous(String isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatusLabel()
    {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel)
    {
        this.statusLabel = statusLabel;
    }

    public Long getSubmitterId()
    {
        return submitterId;
    }

    public void setSubmitterId(Long submitterId)
    {
        this.submitterId = submitterId;
    }

    public String getSubmitterName()
    {
        return submitterName;
    }

    public void setSubmitterName(String submitterName)
    {
        this.submitterName = submitterName;
    }

    public Date getSubmitTime()
    {
        return submitTime;
    }

    public void setSubmitTime(Date submitTime)
    {
        this.submitTime = submitTime;
    }

    public String getReplyContent()
    {
        return replyContent;
    }

    public void setReplyContent(String replyContent)
    {
        this.replyContent = replyContent;
    }

    public String getReplyByName()
    {
        return replyByName;
    }

    public void setReplyByName(String replyByName)
    {
        this.replyByName = replyByName;
    }

    public Date getReplyTime()
    {
        return replyTime;
    }

    public void setReplyTime(Date replyTime)
    {
        this.replyTime = replyTime;
    }

    public Integer getAttachmentCount()
    {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount)
    {
        this.attachmentCount = attachmentCount;
    }

    public Boolean getCanWithdraw()
    {
        return canWithdraw;
    }

    public void setCanWithdraw(Boolean canWithdraw)
    {
        this.canWithdraw = canWithdraw;
    }

}
