package com.ruoyi.oa.task.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 任务模块 Job 配置（application.yml 的 oa.task.* 节点，全部带默认值）
 *
 * <p>注意：application.yml 由 Lead 维护，本类只读取不修改。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class OaTaskJobProperties
{
    /** 三个 Job 总开关（压测/维护期可关） */
    @Value("${oa.task.job.enabled:true}")
    private boolean enabled;

    /** 单用户单次提醒超过该条数则合并为一条汇总 */
    @Value("${oa.task.remind-merge-threshold:10}")
    private int remindMergeThreshold;

    /** 站内信批量写入大小 */
    @Value("${oa.task.notify-batch-size:200}")
    private int notifyBatchSize;

    /** 退会批量取消任务大小 */
    @Value("${oa.task.retire-cancel-batch:100}")
    private int retireCancelBatch;

    /** 截止前 N 天提醒（基线=1） */
    @Value("${oa.task.due-remind-days:1}")
    private int dueRemindDays;

    public boolean isEnabled()
    {
        return enabled;
    }

    public int getRemindMergeThreshold()
    {
        return remindMergeThreshold;
    }

    public int getNotifyBatchSize()
    {
        return notifyBatchSize;
    }

    public int getRetireCancelBatch()
    {
        return retireCancelBatch;
    }

    public int getDueRemindDays()
    {
        return dueRemindDays;
    }
}
