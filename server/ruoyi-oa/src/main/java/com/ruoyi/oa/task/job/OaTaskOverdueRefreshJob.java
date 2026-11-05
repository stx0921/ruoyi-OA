package com.ruoyi.oa.task.job;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.oa.recur.support.RecurDateCalculator;
import com.ruoyi.oa.support.OaLockService;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.mapper.OaTaskMapper;

/**
 * 逾期刷新与退会对账 Job：每日 00:10
 *
 * <p>① 刷新 oa_task.overdue_flag（只更新有变化的行）；
 * ② 退会联动补偿对账：status 0/1 且 assignee 账号停用/删除（或成员退会/毕业）的任务补取消 + 系统日志 + 通知派发人，
 * 防止退会事件丢失导致任务悬挂。</p>
 *
 * <p>bean 名与 sys_job.invoke_target 一致：oaTaskOverdueRefreshJob.run()</p>
 *
 * @author ruoyi-oa
 */
@Component("oaTaskOverdueRefreshJob")
public class OaTaskOverdueRefreshJob
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskOverdueRefreshJob.class);

    /** 锁 TTL：30 分钟 */
    private static final long LOCK_TTL_SECONDS = 1800L;

    /** 业务日期格式 */
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private OaTaskJobProperties properties;

    @Autowired
    private OaLockService lockService;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private OaTaskJobSupport support;

    /**
     * Job 入口（Quartz 反射调用，异常不得抛出中断调度）
     */
    public void run()
    {
        if (!properties.isEnabled())
        {
            log.info("逾期刷新 Job 已关闭（oa.task.job.enabled=false），跳过");
            return;
        }
        LocalDate today = RecurDateCalculator.today();
        String lockKey = "oa:lock:job:task-overdue:" + today.format(DAY_FORMATTER);
        if (!lockService.tryLock(lockKey, LOCK_TTL_SECONDS))
        {
            log.warn("逾期刷新 Job 未获取到锁，可能有其他实例正在执行：{}", lockKey);
            return;
        }
        try
        {
            // ① 刷新逾期标记（只更新有变化的行）
            Integer refreshed = support.transactionTemplate().execute(status -> taskMapper.refreshOverdueFlags());
            log.info("逾期标记刷新完成：影响行数={}", refreshed == null ? 0 : refreshed);

            // ② 退会联动补偿对账
            List<OaTask> tasks = taskMapper.selectUnfinishedWithInactiveAssignee();
            int canceled = 0;
            if (tasks != null)
            {
                for (OaTask task : tasks)
                {
                    try
                    {
                        Boolean done = support.transactionTemplate().execute(status -> reconcileOne(task));
                        if (Boolean.TRUE.equals(done))
                        {
                            canceled++;
                        }
                    }
                    catch (Exception e)
                    {
                        log.error("退会对账补取消任务[{}]失败", task.getTaskId(), e);
                    }
                }
            }
            log.info("逾期刷新 Job 结束：逾期刷新行数={} 退会补偿取消数={}", refreshed == null ? 0 : refreshed, canceled);
        }
        catch (Exception e)
        {
            log.error("逾期刷新 Job 异常", e);
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    /**
     * 单条退会补偿：条件取消 + 写日志 + 通知派发人（dedupe_key=retire:task:{taskId} 幂等）
     */
    private Boolean reconcileOne(OaTask task)
    {
        if (task == null || task.getTaskId() == null)
        {
            return Boolean.FALSE;
        }
        int rows = taskMapper.cancelTask(task.getTaskId());
        if (rows == 0)
        {
            return Boolean.FALSE;
        }
        support.writeTaskLog(task.getTaskId(), "取消", "负责人退会自动取消");
        if (task.getCreatorId() != null && !task.getCreatorId().equals(task.getAssigneeId()))
        {
            support.notify(task.getCreatorId(), "5", "任务已自动取消",
                    "你派发的任务「" + task.getTitle() + "」因负责人退会已自动取消", "task", task.getTaskId(),
                    "retire:task:" + task.getTaskId());
        }
        return Boolean.TRUE;
    }
}
