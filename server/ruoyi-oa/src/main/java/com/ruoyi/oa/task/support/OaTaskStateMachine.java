package com.ruoyi.oa.task.support;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

/**
 * 任务状态机（5 态）与动作常量
 *
 * <pre>
 * 创建 → 待办0 → 开始 → 进行中1 → 提交完成 → 待验收2 → 验收通过 → 已完成3
 *          │            │  ▲                   │
 *          └── 取消4 ────┘  └── 打回（理由必填，reject_count+1）──┘
 * </pre>
 *
 * @author ruoyi-oa
 */
public final class OaTaskStateMachine
{
    private OaTaskStateMachine()
    {
    }

    /** 状态：待办 */
    public static final String STATUS_TODO = "0";

    /** 状态：进行中 */
    public static final String STATUS_DOING = "1";

    /** 状态：待验收 */
    public static final String STATUS_AUDIT = "2";

    /** 状态：已完成 */
    public static final String STATUS_DONE = "3";

    /** 状态：已取消 */
    public static final String STATUS_CANCELED = "4";

    /** 来源：手工创建 */
    public static final String SOURCE_MANUAL = "0";

    /** 来源：周期生成 */
    public static final String SOURCE_RECUR = "1";

    /** 来源：子任务 */
    public static final String SOURCE_CHILD = "2";

    /** 动作：创建 */
    public static final String ACTION_CREATE = "创建";

    /** 动作：开始 */
    public static final String ACTION_START = "开始";

    /** 动作：提交 */
    public static final String ACTION_SUBMIT = "提交";

    /** 动作：验收通过 */
    public static final String ACTION_PASS = "验收通过";

    /** 动作：打回 */
    public static final String ACTION_REJECT = "打回";

    /** 动作：取消 */
    public static final String ACTION_CANCEL = "取消";

    /** 动作：改派 */
    public static final String ACTION_REASSIGN = "改派";

    /** 动作：编辑 */
    public static final String ACTION_EDIT = "编辑";

    /** 动作：周期生成 */
    public static final String ACTION_RECUR_GENERATE = "周期生成";

    /**
     * 是否终态（已完成 / 已取消）
     */
    public static boolean isFinished(String status)
    {
        return STATUS_DONE.equals(status) || STATUS_CANCELED.equals(status);
    }

    /**
     * 是否可编辑（仅待办 / 进行中）
     */
    public static boolean isEditable(String status)
    {
        return STATUS_TODO.equals(status) || STATUS_DOING.equals(status);
    }

    /**
     * 是否可取消（仅待办 / 进行中；待验收须先打回）
     */
    public static boolean isCancelable(String status)
    {
        return STATUS_TODO.equals(status) || STATUS_DOING.equals(status);
    }

    /**
     * 是否可改派（待办 / 进行中 / 待验收）
     */
    public static boolean isReassignable(String status)
    {
        return STATUS_TODO.equals(status) || STATUS_DOING.equals(status) || STATUS_AUDIT.equals(status);
    }

    /**
     * 是否待验收
     */
    public static boolean isAuditable(String status)
    {
        return STATUS_AUDIT.equals(status);
    }

    /**
     * 条件更新影响 0 行时的统一异常（并发 / 状态已变）
     */
    public static ServiceException conflict()
    {
        return new ServiceException("任务当前状态不允许该操作", HttpStatus.CONFLICT);
    }

    /**
     * 终止态统一异常
     */
    public static ServiceException finished()
    {
        return new ServiceException("已完成任务不可再流转", HttpStatus.CONFLICT);
    }
}
