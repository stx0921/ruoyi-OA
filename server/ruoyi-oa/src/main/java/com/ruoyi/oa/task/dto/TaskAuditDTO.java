package com.ruoyi.oa.task.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 验收 / 打回 DTO（打回时 remark 必填 2-200 字）
 *
 * @author ruoyi-oa
 */
public class TaskAuditDTO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** true=验收通过 false=打回 */
    private Boolean pass;

    /** 验收意见 / 打回理由 */
    private String remark;

    /** 验收通过附件ID列表 */
    private List<Long> finishFileIds;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Boolean getPass()
    {
        return pass;
    }

    public void setPass(Boolean pass)
    {
        this.pass = pass;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public List<Long> getFinishFileIds()
    {
        return finishFileIds;
    }

    public void setFinishFileIds(List<Long> finishFileIds)
    {
        this.finishFileIds = finishFileIds;
    }

}
