package com.ruoyi.oa.task.job;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.project.mapper.OaProjectMapper;
import com.ruoyi.oa.recur.domain.OaTaskRecur;
import com.ruoyi.oa.recur.mapper.OaTaskRecurMapper;
import com.ruoyi.oa.recur.support.RecurDateCalculator;
import com.ruoyi.oa.support.OaLockService;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.mapper.OaTaskMapper;
import com.ruoyi.oa.task.support.OaTaskNoGenerator;

/**
 * 周期任务生成 Job：每日 00:05 扫描启用且今日命中星期的规则，每条规则独立事务生成 1 条待办
 *
 * <p>三层防重：① Redis 跨实例锁；② 规则行锁二次校验；③ uk_recur_date(recur_id, generate_date) 唯一索引
 * （SQL 为 INSERT ... ON DUPLICATE KEY UPDATE update_time=update_time，返回 0 即已生成过，不写日志/不重复通知）。</p>
 *
 * <p>bean 名与 sys_job.invoke_target 一致：oaTaskRecurGenerateJob.run()</p>
 *
 * @author ruoyi-oa
 */
@Component("oaTaskRecurGenerateJob")
public class OaTaskRecurGenerateJob
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskRecurGenerateJob.class);

    /** 锁 TTL：10 分钟 */
    private static final long LOCK_TTL_SECONDS = 600L;

    /** 业务日期格式 */
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    private OaTaskJobProperties properties;

    @Autowired
    private OaLockService lockService;

    @Autowired
    private OaTaskRecurMapper recurMapper;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private OaProjectMapper projectMapper;

    @Autowired
    private OaTaskNoGenerator taskNoGenerator;

    @Autowired
    private OaTaskJobSupport support;

    /**
     * Job 入口（Quartz 反射调用，异常不得抛出中断调度）
     */
    public void run()
    {
        if (!properties.isEnabled())
        {
            log.info("周期任务生成 Job 已关闭（oa.task.job.enabled=false），跳过");
            return;
        }
        LocalDate today = RecurDateCalculator.today();
        String lockKey = "oa:lock:job:recur-generate:" + today.format(DAY_FORMATTER);
        if (!lockService.tryLock(lockKey, LOCK_TTL_SECONDS))
        {
            log.warn("周期任务生成 Job 未获取到锁，可能有其他实例正在执行：{}", lockKey);
            return;
        }
        try
        {
            int weekday = today.getDayOfWeek().getValue();
            List<OaTaskRecur> rules = recurMapper.selectDueRules(today.toString(), weekday);
            log.info("周期任务生成 Job 开始：date={} weekday={} 命中规则数={}", today, weekday,
                    rules == null ? 0 : rules.size());
            int generated = 0;
            if (rules != null)
            {
                for (OaTaskRecur rule : rules)
                {
                    try
                    {
                        Boolean inserted = support.transactionTemplate()
                                .execute(status -> generateOne(rule.getRecurId(), today));
                        if (Boolean.TRUE.equals(inserted))
                        {
                            generated++;
                        }
                    }
                    catch (Exception e)
                    {
                        // 单条规则失败不影响其他规则
                        log.error("周期规则[{}]生成任务失败", rule.getRecurId(), e);
                    }
                }
            }
            log.info("周期任务生成 Job 结束：命中规则数={} 实际生成数={}", rules == null ? 0 : rules.size(), generated);
        }
        catch (Exception e)
        {
            log.error("周期任务生成 Job 异常", e);
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    /**
     * 单条规则生成（独立事务内执行：行锁二次校验 → 幂等插入 → 日志/通知 → 更新下次生成日）
     *
     * @return true=本次新生成
     */
    private Boolean generateOne(Long recurId, LocalDate today)
    {
        OaTaskRecur rule = recurMapper.selectByIdForUpdate(recurId);
        if (rule == null || !"0".equals(rule.getStatus()))
        {
            return Boolean.FALSE;
        }
        LocalDate startDate = RecurDateCalculator.toLocalDate(rule.getStartDate());
        if (startDate != null && startDate.isAfter(today))
        {
            return Boolean.FALSE;
        }
        if (!RecurDateCalculator.hit(rule.getWeekDays(), today))
        {
            return Boolean.FALSE;
        }
        // 归档项目下的规则自动暂停
        if (rule.getProjectId() != null && projectMapper.isFinished(rule.getProjectId()))
        {
            recurMapper.updateStatus(rule.getRecurId(), "1", null);
            log.warn("周期规则[{}]挂在已归档项目，自动暂停", rule.getRecurId());
            return Boolean.FALSE;
        }
        Date dueTime = RecurDateCalculator.toDateTime(today, rule.getDueTime());
        if (dueTime == null)
        {
            log.warn("周期规则[{}]截止时刻非法：{}", recurId, rule.getDueTime());
            return Boolean.FALSE;
        }
        String title = RecurDateCalculator.renderTitle(rule.getTitle(), today);
        OaTask task = new OaTask();
        task.setTaskNo(taskNoGenerator.next());
        task.setTitle(title);
        task.setDescription(StringUtils.nvl(rule.getDescription(), ""));
        task.setProjectId(rule.getProjectId());
        task.setAssigneeId(rule.getAssigneeId());
        task.setPriority(rule.getPriority());
        task.setTags(rule.getTags());
        task.setStatus("0");
        task.setDueTime(dueTime);
        task.setCreatorId(rule.getCreatorId());
        task.setAuditorId(rule.getCreatorId());
        task.setSource("1");
        task.setRecurId(rule.getRecurId());
        task.setGenerateDate(RecurDateCalculator.toDate(today));
        task.setDeptId(rule.getDeptId());
        task.setCreateBy("system");
        int rows = taskMapper.insertIgnoreRecurDate(task);
        if (rows == 1)
        {
            support.writeTaskLog(task.getTaskId(), "周期生成",
                    "recur_id=" + rule.getRecurId() + ", date=" + today);
            support.notify(rule.getAssigneeId(), "2", "今天要做：" + title,
                    "周期任务「" + title + "」今天 " + timeText(rule.getDueTime()) + " 截止，请及时处理。", "task",
                    task.getTaskId(), "recur:" + task.getTaskId() + ":" + today);
        }
        else
        {
            log.debug("周期规则[{}] {} 已生成过任务，幂等跳过", recurId, today);
        }
        // 下次生成日（展示用，Job 扫描不依赖它）
        Date nextDate = RecurDateCalculator
                .toDate(RecurDateCalculator.calcNextDate(rule.getStartDate(), rule.getWeekDays(), today));
        recurMapper.updateNextDate(rule.getRecurId(), nextDate);
        return Boolean.valueOf(rows == 1);
    }

    /**
     * 截止时刻（HH:mm:ss）→ HH:mm 文案（用于站内信）
     */
    private String timeText(String dueTime)
    {
        java.time.LocalTime time = RecurDateCalculator.parseTime(dueTime);
        if (time == null)
        {
            return dueTime == null ? "" : dueTime;
        }
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }
}
