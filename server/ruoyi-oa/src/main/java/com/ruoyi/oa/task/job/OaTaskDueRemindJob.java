package com.ruoyi.oa.task.job;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.oa.notify.domain.OaNotify;
import com.ruoyi.oa.notify.service.IOaNotifyService;
import com.ruoyi.oa.recur.support.RecurDateCalculator;
import com.ruoyi.oa.support.OaLockService;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.mapper.OaTaskMapper;

/**
 * 到期提醒 Job：每日 09:00 发站内信
 *
 * <p>① 今日生成的周期任务（source=1, generate_date=today, status 0/1）发 TPL-2；
 * ② 明日截止（due_time ∈ [明天00:00, 后天00:00), status 0/1）发 TPL-3。
 * dedupe_key 分别为 recur:{taskId}:{date}、due:{taskId}:{date}；按用户聚合，超过阈值合并为一条汇总
 * digest:{userId}:{date}。只发站内信，不做逾期每日提醒、不发短信（TR10）。</p>
 *
 * <p>bean 名与 sys_job.invoke_target 一致：oaTaskDueRemindJob.run()</p>
 *
 * @author ruoyi-oa
 */
@Component("oaTaskDueRemindJob")
public class OaTaskDueRemindJob
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskDueRemindJob.class);

    /** 锁 TTL：30 分钟 */
    private static final long LOCK_TTL_SECONDS = 1800L;

    /** 业务日期格式 */
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    /** 任务状态文案 */
    private static final Map<String, String> STATUS_LABELS = new LinkedHashMap<String, String>();

    static
    {
        STATUS_LABELS.put("0", "待办");
        STATUS_LABELS.put("1", "进行中");
        STATUS_LABELS.put("2", "待验收");
        STATUS_LABELS.put("3", "已完成");
        STATUS_LABELS.put("4", "已取消");
    }

    @Autowired
    private OaTaskJobProperties properties;

    @Autowired
    private OaLockService lockService;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private IOaNotifyService notifyService;

    @Autowired
    private OaTaskJobSupport support;

    /**
     * Job 入口（Quartz 反射调用，异常不得抛出中断调度）
     */
    public void run()
    {
        if (!properties.isEnabled())
        {
            log.info("到期提醒 Job 已关闭（oa.task.job.enabled=false），跳过");
            return;
        }
        LocalDate today = RecurDateCalculator.today();
        String dateText = today.toString();
        String lockKey = "oa:lock:job:task-remind:" + today.format(DAY_FORMATTER);
        if (!lockService.tryLock(lockKey, LOCK_TTL_SECONDS))
        {
            log.warn("到期提醒 Job 未获取到锁，可能有其他实例正在执行：{}", lockKey);
            return;
        }
        try
        {
            List<OaNotify> drafts = new ArrayList<OaNotify>();
            // ① 周期任务当日提醒
            List<OaTask> recurTasks = taskMapper.selectRecurGeneratedByDate(today.format(DAY_FORMATTER));
            if (recurTasks != null)
            {
                for (OaTask task : recurTasks)
                {
                    if (task == null || task.getAssigneeId() == null)
                    {
                        continue;
                    }
                    drafts.add(draft(task.getAssigneeId(), "2", "今天要做：" + task.getTitle(),
                            "周期任务「" + task.getTitle() + "」今天 " + timeText(task.getDueTime()) + " 截止，请及时处理。",
                            task.getTaskId(), "recur:" + task.getTaskId() + ":" + dateText));
                }
            }
            // ② 截止前 N 天提醒（默认 1 天）
            int remindDays = Math.max(1, properties.getDueRemindDays());
            LocalDate beginDate = today.plusDays(remindDays);
            Date begin = Date.from(beginDate.atStartOfDay(RecurDateCalculator.ZONE).toInstant());
            Date end = Date.from(beginDate.plusDays(1).atStartOfDay(RecurDateCalculator.ZONE).toInstant());
            List<OaTask> dueTasks = taskMapper.selectUnfinishedDueBetween(begin, end);
            if (dueTasks != null)
            {
                for (OaTask task : dueTasks)
                {
                    if (task == null || task.getAssigneeId() == null)
                    {
                        continue;
                    }
                    String dayLabel = remindDays == 1 ? "明天" : ("未来 " + remindDays + " 天");
                    drafts.add(draft(task.getAssigneeId(), "3", dayLabel + "截止：" + task.getTitle(),
                            "任务「" + task.getTitle() + "」将于" + dayLabel + " " + timeText(task.getDueTime())
                                    + " 截止，当前状态：" + STATUS_LABELS.get(task.getStatus()) + "。",
                            task.getTaskId(), "due:" + task.getTaskId() + ":" + dateText));
                }
            }
            // ③ 按用户聚合：超过阈值合并为一条汇总，防通知风暴
            Map<Long, List<OaNotify>> byUser = new LinkedHashMap<Long, List<OaNotify>>();
            for (OaNotify notify : drafts)
            {
                List<OaNotify> list = byUser.get(notify.getUserId());
                if (list == null)
                {
                    list = new ArrayList<OaNotify>();
                    byUser.put(notify.getUserId(), list);
                }
                list.add(notify);
            }
            int threshold = Math.max(1, properties.getRemindMergeThreshold());
            List<OaNotify> finalList = new ArrayList<OaNotify>();
            for (Map.Entry<Long, List<OaNotify>> entry : byUser.entrySet())
            {
                if (entry.getValue().size() > threshold)
                {
                    OaNotify digest = new OaNotify();
                    digest.setUserId(entry.getKey());
                    digest.setType("5");
                    digest.setTitle("今日任务提醒");
                    digest.setContent("你有 " + entry.getValue().size() + " 条任务提醒，请及时处理。");
                    digest.setBizType("");
                    digest.setDedupeKey("digest:" + entry.getKey() + ":" + dateText);
                    finalList.add(digest);
                }
                else
                {
                    finalList.addAll(entry.getValue());
                }
            }
            // ④ 分批写入，每批独立事务；通知失败只记日志
            int batchSize = Math.max(1, properties.getNotifyBatchSize());
            int written = 0;
            for (int from = 0; from < finalList.size(); from += batchSize)
            {
                List<OaNotify> chunk = finalList.subList(from, Math.min(from + batchSize, finalList.size()));
                try
                {
                    support.transactionTemplate().executeWithoutResult(status -> notifyService.notifyBatch(chunk));
                    written += chunk.size();
                }
                catch (Exception e)
                {
                    log.error("到期提醒写入站内信失败 size={}", chunk.size(), e);
                }
            }
            log.info("到期提醒 Job 结束：周期当日={} 截止提醒={} 实际写入={}", recurTasks == null ? 0 : recurTasks.size(),
                    dueTasks == null ? 0 : dueTasks.size(), written);
        }
        catch (Exception e)
        {
            log.error("到期提醒 Job 异常", e);
        }
        finally
        {
            lockService.unlock(lockKey);
        }
    }

    /**
     * 构造一条待写入站内信
     */
    private OaNotify draft(Long userId, String type, String title, String content, Long taskId, String dedupeKey)
    {
        OaNotify notify = new OaNotify();
        notify.setUserId(userId);
        notify.setType(type);
        notify.setTitle(title);
        notify.setContent(content);
        notify.setBizType("task");
        notify.setBizId(taskId);
        notify.setDedupeKey(dedupeKey);
        return notify;
    }

    /**
     * 截止时间 → HH:mm 文案
     */
    private String timeText(Date dueTime)
    {
        if (dueTime == null)
        {
            return "";
        }
        return new SimpleDateFormat("HH:mm").format(dueTime);
    }
}
