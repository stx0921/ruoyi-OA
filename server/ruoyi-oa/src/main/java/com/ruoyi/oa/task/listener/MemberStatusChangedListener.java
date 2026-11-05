package com.ruoyi.oa.task.listener;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.oa.constant.OaMemberConstants;
import com.ruoyi.oa.member.domain.OaMember;
import com.ruoyi.oa.member.event.MemberStatusChangedEvent;
import com.ruoyi.oa.member.mapper.OaAccountMapper;
import com.ruoyi.oa.member.mapper.OaMemberMapper;
import com.ruoyi.oa.project.domain.OaProject;
import com.ruoyi.oa.project.mapper.OaProjectMapper;
import com.ruoyi.oa.recur.domain.OaTaskRecur;
import com.ruoyi.oa.recur.mapper.OaTaskRecurMapper;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.job.OaTaskJobProperties;
import com.ruoyi.oa.task.job.OaTaskJobSupport;
import com.ruoyi.oa.task.mapper.OaTaskMapper;

/**
 * 成员状态变更监听器（退会 / 毕业 / 软删除 → 任务域联动，TR7）
 *
 * <p>监听人员模块 {@link MemberStatusChangedEvent}，在事务提交后同步执行：
 * ① 取消该账号未完成任务（待办/进行中，分页批量）并通知派发人；
 * ② 暂停其周期规则（状态 0→1）并通知规则创建人；
 * ③ 其作为验收人的任务转项目负责人（无项目/负责人不可用则转主席团，无则保留）。
 * 失败只记 ERROR 日志，不影响人员模块退会主流程。</p>
 *
 * @author ruoyi-oa
 */
@Component
public class MemberStatusChangedListener
{
    private static final Logger log = LoggerFactory.getLogger(MemberStatusChangedListener.class);

    /** 批量取消任务的最大循环次数（防御脏数据死循环） */
    private static final int MAX_CANCEL_LOOP = 1000;

    @Autowired
    private OaTaskMapper taskMapper;

    @Autowired
    private OaTaskRecurMapper recurMapper;

    @Autowired
    private OaProjectMapper projectMapper;

    @Autowired
    private OaMemberMapper memberMapper;

    @Autowired
    private OaAccountMapper accountMapper;

    @Autowired
    private OaTaskJobProperties properties;

    @Autowired
    private OaTaskJobSupport support;

    /**
     * 成员状态变更（事务提交后同步执行；未在事务中发布时 fallback 直接执行）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(MemberStatusChangedEvent event)
    {
        try
        {
            handle(event);
        }
        catch (Exception e)
        {
            // 退会联动失败不影响人员模块退会成功
            log.error("退会联动处理失败", e);
        }
    }

    private void handle(MemberStatusChangedEvent event)
    {
        if (event == null || event.getUserId() == null)
        {
            return;
        }
        String newStatus = event.getNewStatus();
        if (!OaMemberConstants.MEMBER_STATUS_LEFT.equals(newStatus)
                && !OaMemberConstants.MEMBER_STATUS_GRADUATED.equals(newStatus))
        {
            return;
        }
        Long userId = event.getUserId();
        log.info("收到成员状态变更事件：userId={} memberId={} {}->{} 操作人={}", userId, event.getMemberId(),
                event.getOldStatus(), newStatus, event.getOperatorName());
        // ① 取消未完成任务（每批独立事务，条件更新保证幂等）
        cancelUnfinishedTasks(userId);
        // ② 暂停周期规则并通知创建人
        pauseRecurRules(userId);
        // ③ 验收人转交项目负责人 / 主席团
        transferAuditor(userId);
    }

    /**
     * 分页批量取消未完成任务（待办/进行中），写系统日志并通知派发人
     */
    private void cancelUnfinishedTasks(Long userId)
    {
        int batchSize = Math.max(1, properties.getRetireCancelBatch());
        int total = 0;
        int loop = 0;
        while (loop++ < MAX_CANCEL_LOOP)
        {
            List<OaTask> tasks = taskMapper.selectUnfinishedByAssignee(userId, 0, batchSize);
            if (tasks == null || tasks.isEmpty())
            {
                break;
            }
            Integer canceled = support.transactionTemplate().execute(status -> {
                int count = 0;
                for (OaTask task : tasks)
                {
                    if (cancelOne(task, userId))
                    {
                        count++;
                    }
                }
                return count;
            });
            int current = canceled == null ? 0 : canceled;
            total += current;
            // 本批无任何进展（状态已被其他线程变更）则退出，避免死循环
            if (current == 0)
            {
                break;
            }
        }
        if (total > 0)
        {
            log.info("退会联动：用户[{}] 未完成任务已自动取消 {} 条", userId, total);
        }
    }

    /**
     * 取消单条任务 + 写日志 + 通知派发人
     */
    private boolean cancelOne(OaTask task, Long userId)
    {
        int rows = taskMapper.cancelTask(task.getTaskId());
        if (rows == 0)
        {
            return false;
        }
        support.writeTaskLog(task.getTaskId(), "取消", "负责人退会自动取消");
        if (task.getCreatorId() != null && !task.getCreatorId().equals(userId))
        {
            support.notify(task.getCreatorId(), "5", "任务已自动取消",
                    "你派发的任务「" + task.getTitle() + "」因负责人退会已自动取消", "task", task.getTaskId(),
                    "retire:task:" + task.getTaskId());
        }
        return true;
    }

    /**
     * 暂停该账号名下启用中的周期规则，并通知规则创建人
     */
    private void pauseRecurRules(Long userId)
    {
        List<OaTaskRecur> rules = recurMapper.selectActiveByAssignee(userId);
        if (rules == null || rules.isEmpty())
        {
            return;
        }
        support.transactionTemplate().executeWithoutResult(status -> {
            for (OaTaskRecur rule : rules)
            {
                int rows = recurMapper.updateStatus(rule.getRecurId(), "1", null);
                if (rows > 0 && rule.getCreatorId() != null)
                {
                    support.notify(rule.getCreatorId(), "5", "周期规则已暂停",
                            "周期规则「" + rule.getTitle() + "」因负责人退会已自动暂停", "recur", rule.getRecurId(),
                            "retire:recur:" + rule.getRecurId());
                }
            }
        });
        log.info("退会联动：用户[{}] 周期规则已暂停 {} 条", userId, rules.size());
    }

    /**
     * 其作为验收人的任务：验收人转项目负责人，无项目/负责人不可用则转主席团首个账号，均无则保留
     */
    private void transferAuditor(Long userId)
    {
        List<OaTask> tasks = taskMapper.selectByAuditor(userId);
        if (tasks == null || tasks.isEmpty())
        {
            return;
        }
        support.transactionTemplate().executeWithoutResult(status -> {
            for (OaTask item : tasks)
            {
                OaTask task = taskMapper.selectById(item.getTaskId());
                if (task == null)
                {
                    continue;
                }
                Long fallback = resolveFallbackAuditor(task, userId);
                if (fallback == null || fallback.equals(userId))
                {
                    // 无项目负责人且无主席团账号：保留原验收人
                    continue;
                }
                task.setAuditorId(fallback);
                int rows = taskMapper.updateTask(task);
                if (rows == 0)
                {
                    continue;
                }
                support.writeTaskLog(task.getTaskId(), "验收人转交", "原验收人退会，验收人已自动转交");
                support.notify(fallback, "5", "你被指定为任务验收人",
                        "任务「" + task.getTitle() + "」的验收人已转交给你", "task", task.getTaskId(),
                        "reassign:auditor:" + task.getTaskId());
            }
        });
        log.info("退会联动：用户[{}] 的待转交验收任务 {} 条", userId, tasks.size());
    }

    /**
     * 兜底验收人：任务所属项目负责人（在册且非本人）→ 主席团首个账号
     */
    private Long resolveFallbackAuditor(OaTask task, Long oldUserId)
    {
        if (task.getProjectId() != null)
        {
            OaProject project = projectMapper.selectProjectById(task.getProjectId());
            if (project != null && project.getOwnerId() != null && !project.getOwnerId().equals(oldUserId)
                    && isActiveMember(project.getOwnerId()))
            {
                return project.getOwnerId();
            }
        }
        SysUser president = accountMapper.selectFirstUserByRoleKey(OaMemberConstants.ROLE_PRESIDENT);
        if (president != null && !president.getUserId().equals(oldUserId))
        {
            return president.getUserId();
        }
        return null;
    }

    /**
     * 账号对应成员是否在册
     */
    private boolean isActiveMember(Long userId)
    {
        if (userId == null)
        {
            return false;
        }
        OaMember member = memberMapper.selectByUserId(userId);
        return member != null && OaMemberConstants.MEMBER_STATUS_ACTIVE.equals(member.getStatus());
    }
}
