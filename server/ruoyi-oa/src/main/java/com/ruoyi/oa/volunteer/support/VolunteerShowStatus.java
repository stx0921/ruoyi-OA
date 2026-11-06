package com.ruoyi.oa.volunteer.support;

import java.util.Date;

/**
 * 志愿活动展示状态（不落库，列表/详情/H5 共用同一套推导规则）
 *
 * <pre>
 * 1) status='1'                              → 已取消
 * 2) now > end_time                         → 已结束
 * 3) start_time <= now <= end_time          → 进行中
 * 4) now < start_time && now <= deadline    → 报名中
 * 5) 其余                                   → 报名已截止
 * </pre>
 *
 * @author ruoyi-oa
 */
public enum VolunteerShowStatus
{
    /** 报名中 */
    SIGNING_UP(0, "报名中"),

    /** 进行中 */
    ONGOING(1, "进行中"),

    /** 已结束 */
    FINISHED(2, "已结束"),

    /** 已取消 */
    CANCELED(3, "已取消"),

    /** 报名已截止 */
    DEADLINE_PASSED(4, "报名已截止");

    private final int code;

    private final String label;

    VolunteerShowStatus(int code, String label)
    {
        this.code = code;
        this.label = label;
    }

    public int getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    /**
     * 推导展示状态
     */
    public static VolunteerShowStatus resolve(String status, Date startTime, Date endTime, Date signupDeadline,
            Date now)
    {
        Date current = now == null ? new Date() : now;
        if ("1".equals(status))
        {
            return CANCELED;
        }
        if (endTime != null && current.after(endTime))
        {
            return FINISHED;
        }
        if (startTime != null && !current.before(startTime))
        {
            return ONGOING;
        }
        if ((startTime == null || current.before(startTime))
                && (signupDeadline == null || !current.after(signupDeadline)))
        {
            return SIGNING_UP;
        }
        return DEADLINE_PASSED;
    }

    /**
     * 当前是否可以报名：未取消、当前 < end_time、当前 <= signup_deadline
     */
    public static boolean canSignup(String status, Date endTime, Date signupDeadline, Date now)
    {
        Date current = now == null ? new Date() : now;
        if ("1".equals(status))
        {
            return false;
        }
        if (endTime != null && !current.before(endTime))
        {
            return false;
        }
        if (signupDeadline != null && current.after(signupDeadline))
        {
            return false;
        }
        return true;
    }

    /**
     * 不可报名原因（可报名返回空串）
     */
    public static String closedReason(String status, Date endTime, Date signupDeadline, Date now)
    {
        Date current = now == null ? new Date() : now;
        if ("1".equals(status))
        {
            return "活动已取消";
        }
        if (endTime != null && !current.before(endTime))
        {
            return "活动已结束";
        }
        if (signupDeadline != null && current.after(signupDeadline))
        {
            return "报名已截止";
        }
        return "";
    }

}
