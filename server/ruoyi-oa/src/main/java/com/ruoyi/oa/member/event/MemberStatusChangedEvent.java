package com.ruoyi.oa.member.event;

import java.io.Serializable;

/**
 * 成员状态变更事件（退会 / 毕业 / 软删除）
 *
 * <p>在人员模块 changeStatus / deleteMembers 事务提交后发布，任务模块据此取消未完成任务、暂停周期规则。</p>
 *
 * @author ruoyi-oa
 */
public class MemberStatusChangedEvent implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 关联账号 ID（可能为 null，档案未开号） */
    private Long userId;

    /** 成员档案 ID */
    private Long memberId;

    /** 变更前状态（0在籍 1已退会 2已毕业） */
    private String oldStatus;

    /** 变更后状态（0在籍 1已退会 2已毕业；软删除按 1 处理） */
    private String newStatus;

    /** 操作人账号 ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    public MemberStatusChangedEvent()
    {
    }

    public MemberStatusChangedEvent(Long userId, Long memberId, String oldStatus, String newStatus, Long operatorId,
            String operatorName)
    {
        this.userId = userId;
        this.memberId = memberId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public String getOldStatus()
    {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus)
    {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus()
    {
        return newStatus;
    }

    public void setNewStatus(String newStatus)
    {
        this.newStatus = newStatus;
    }

    public Long getOperatorId()
    {
        return operatorId;
    }

    public void setOperatorId(Long operatorId)
    {
        this.operatorId = operatorId;
    }

    public String getOperatorName()
    {
        return operatorName;
    }

    public void setOperatorName(String operatorName)
    {
        this.operatorName = operatorName;
    }
}
