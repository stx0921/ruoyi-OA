package com.ruoyi.oa.member.vo;

/**
 * 成员统计卡片数据
 *
 * @author ruoyi-oa
 */
public class MemberStatsVO
{
    private static final long serialVersionUID = 1L;

    /** 在籍总人数 */
    private Long total;

    /** 待审批申请数 */
    private Long pendingApply;

    /** 本月新增 */
    private Long monthJoin;

    /** 本月退会/毕业 */
    private Long monthLeave;

    public Long getTotal()
    {
        return total;
    }

    public void setTotal(Long total)
    {
        this.total = total;
    }

    public Long getPendingApply()
    {
        return pendingApply;
    }

    public void setPendingApply(Long pendingApply)
    {
        this.pendingApply = pendingApply;
    }

    public Long getMonthJoin()
    {
        return monthJoin;
    }

    public void setMonthJoin(Long monthJoin)
    {
        this.monthJoin = monthJoin;
    }

    public Long getMonthLeave()
    {
        return monthLeave;
    }

    public void setMonthLeave(Long monthLeave)
    {
        this.monthLeave = monthLeave;
    }

}
