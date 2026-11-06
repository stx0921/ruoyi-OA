package com.ruoyi.oa.feedback.domain;

import java.io.Serializable;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 工单 / 意见反馈对象 oa_feedback
 *
 * @author ruoyi-oa
 */
public class OaFeedback implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long feedbackId;

    /** 工单编号（FB+yyyyMMdd+3位序号） */
    private String ticketNo;

    /** 类型（1 Bug / 2 优化建议 / 3 新增功能 / 4 其他） */
    private String type;

    /** 标题（2–50 字） */
    private String title;

    /** 影响模块ID（sys_menu.menu_id） */
    private Long moduleId;

    /** 影响模块名称快照 */
    private String moduleName;

    /** 影响页面ID（sys_menu.menu_id） */
    private Long pageId;

    /** 影响页面名称快照 */
    private String pageName;

    /** 详细描述（10–2000 字） */
    private String content;

    /** 复现步骤（≤2000 字） */
    private String steps;

    /** 期望结果（≤1000 字） */
    private String expectResult;

    /** 是否匿名（0否 1是） */
    private String isAnonymous;

    /** 状态（0待处理 1已回复） */
    private String status;

    /** 提交人 user_id */
    private Long submitterId;

    /** 提交人姓名快照 */
    private String submitterName;

    /** 提交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /** 官方回复 */
    private String replyContent;

    /** 回复人 user_id */
    private Long replyBy;

    /** 回复人姓名快照 */
    private String replyByName;

    /** 回复时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date replyTime;

    /** 逻辑删除（0正常 1管理员删除 2提交人撤回） */
    private String delFlag;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 附件数（关联统计，非表字段） */
    private Integer attachmentCount;

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

    public Long getReplyBy()
    {
        return replyBy;
    }

    public void setReplyBy(Long replyBy)
    {
        this.replyBy = replyBy;
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

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public Integer getAttachmentCount()
    {
        return attachmentCount;
    }

    public void setAttachmentCount(Integer attachmentCount)
    {
        this.attachmentCount = attachmentCount;
    }

}
