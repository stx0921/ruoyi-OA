package com.ruoyi.oa.task.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.vo.TaskLogVO;

/**
 * 任务流转日志 Service（只追加）
 *
 * @author ruoyi-oa
 */
public interface IOaTaskLogService
{
    /**
     * 记录日志（before / after 仅变更字段，由服务层组装）
     */
    void log(OaTask task, String action, String remark, Map<String, Object> before, Map<String, Object> after);

    /**
     * 记录日志（无字段变更，如开始 / 提交）
     */
    void log(OaTask task, String action, String remark);

    /**
     * 查询任务时间轴（倒序）
     */
    List<TaskLogVO> selectLogList(Long taskId);
}
