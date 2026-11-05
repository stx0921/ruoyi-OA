package com.ruoyi.oa.task.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.oa.support.OaCurrentUser;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.domain.OaTaskLog;
import com.ruoyi.oa.task.mapper.OaTaskLogMapper;
import com.ruoyi.oa.task.service.IOaTaskLogService;
import com.ruoyi.oa.task.vo.TaskLogVO;

/**
 * 任务流转日志 Service 实现
 *
 * @author ruoyi-oa
 */
@Service
public class OaTaskLogServiceImpl implements IOaTaskLogService
{
    @Autowired
    private OaTaskLogMapper taskLogMapper;

    @Autowired
    private OaCurrentUser currentUser;

    @Override
    public void log(OaTask task, String action, String remark, Map<String, Object> before,
            Map<String, Object> after)
    {
        if (task == null || task.getTaskId() == null)
        {
            return;
        }
        OaTaskLog log = new OaTaskLog();
        log.setTaskId(task.getTaskId());
        log.setAction(action);
        log.setRemark(remark);
        log.setBeforeJson(before == null || before.isEmpty() ? null : JSON.toJSONString(before));
        log.setAfterJson(after == null || after.isEmpty() ? null : JSON.toJSONString(after));
        try
        {
            log.setOperatorId(currentUser.userId());
            log.setOperatorName(currentUser.nickName());
        }
        catch (Exception e)
        {
            // 系统任务（Job / 监听器）无登录上下文
            log.setOperatorId(-1L);
            log.setOperatorName("系统");
        }
        log.setCreateTime(new Date());
        taskLogMapper.insertTaskLog(log);
    }

    @Override
    public void log(OaTask task, String action, String remark)
    {
        log(task, action, remark, null, null);
    }

    @Override
    public List<TaskLogVO> selectLogList(Long taskId)
    {
        return taskLogMapper.selectTaskLogs(taskId);
    }
}
