package com.ruoyi.oa.domain.vo;

/**
 * 审批动作请求体
 *
 * @author ruoyi-oa
 */
public class OaTaskHandleBody
{
    /** 任务ID */
    private String taskId;

    /** 审批意见 */
    private String comment;

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }
}
