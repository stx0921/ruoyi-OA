package com.ruoyi.oa.task.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.task.domain.OaTaskLog;
import com.ruoyi.oa.task.vo.TaskLogVO;

/**
 * 任务流转日志 Mapper 接口（只追加）
 *
 * @author ruoyi-oa
 */
public interface OaTaskLogMapper
{
    /**
     * 新增日志（对象方式）
     */
    int insertTaskLog(OaTaskLog log);

    /**
     * 新增日志（参数方式，供 Job / 监听器快捷调用）
     */
    int insertLog(@Param("taskId") Long taskId, @Param("action") String action,
            @Param("operatorId") Long operatorId, @Param("operatorName") String operatorName,
            @Param("remark") String remark, @Param("beforeJson") String beforeJson,
            @Param("afterJson") String afterJson);

    /**
     * 查询任务时间轴（倒序）
     */
    List<TaskLogVO> selectTaskLogs(Long taskId);

    /**
     * 批量删除任务流转日志（任务硬删除时级联）
     */
    int deleteByTaskIds(@Param("taskIds") Long[] taskIds);
}
