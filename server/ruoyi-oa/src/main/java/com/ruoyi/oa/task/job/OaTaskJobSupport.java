package com.ruoyi.oa.task.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.oa.notify.service.IOaNotifyService;
import com.ruoyi.oa.task.mapper.OaTaskLogMapper;

/**
 * 任务模块 Job/监听器公共支撑：事务模板、任务日志、站内信（失败只记日志，不打断主流程）
 *
 * @author ruoyi-oa
 */
@Component
public class OaTaskJobSupport
{
    private static final Logger log = LoggerFactory.getLogger(OaTaskJobSupport.class);

    @Autowired
    private OaTaskLogMapper taskLogMapper;

    @Autowired
    private IOaNotifyService notifyService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 新建事务模板（每条规则/每批独立事务用）
     */
    public TransactionTemplate transactionTemplate()
    {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * 写任务流转日志（系统操作人）
     */
    public void writeTaskLog(Long taskId, String action, String remark)
    {
        writeTaskLog(taskId, action, "系统", remark);
    }

    /**
     * 写任务流转日志
     */
    public void writeTaskLog(Long taskId, String action, String operatorName, String remark)
    {
        if (taskId == null)
        {
            return;
        }
        try
        {
            taskLogMapper.insertLog(taskId, action, -1L, operatorName, remark, null, null);
        }
        catch (Exception e)
        {
            log.error("写任务流转日志失败 taskId={} action={}", taskId, action, e);
        }
    }

    /**
     * 写站内信（通知是旁路能力，失败只记日志）
     */
    public void notify(Long userId, String type, String title, String content, String bizType, Long bizId,
            String dedupeKey)
    {
        try
        {
            notifyService.notify(userId, type, title, content, bizType, bizId, dedupeKey);
        }
        catch (Exception e)
        {
            log.error("写站内信失败 userId={} type={} title={}", userId, type, title, e);
        }
    }
}
