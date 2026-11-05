package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 提交完成 DTO（仅主责人；子任务需全部完成）
 *
 * @author ruoyi-oa
 */
public class TaskSubmitDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 提交备注 */
    private String remark;

    /** 完成凭证附件ID列表 */
    private List<Long> attachmentFileIds;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public List<Long> getAttachmentFileIds()
    {
        return attachmentFileIds;
    }

    public void setAttachmentFileIds(List<Long> attachmentFileIds)
    {
        this.attachmentFileIds = attachmentFileIds;
    }

}
