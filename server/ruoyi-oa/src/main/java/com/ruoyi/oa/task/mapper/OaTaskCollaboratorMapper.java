package com.ruoyi.oa.task.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.task.vo.UserBriefVO;

/**
 * 任务协作人 Mapper 接口
 *
 * @author ruoyi-oa
 */
public interface OaTaskCollaboratorMapper
{
    /**
     * 批量插入协作人（唯一索引 uk_task_user 兜底）
     */
    int insertBatch(@Param("taskId") Long taskId, @Param("userIds") List<Long> userIds,
            @Param("createBy") String createBy);

    /**
     * 删除任务全部协作人（改派 / 编辑重建）
     */
    int deleteByTaskId(Long taskId);

    /**
     * 查询任务协作人ID列表
     */
    List<Long> selectUserIdsByTaskId(Long taskId);

    /**
     * 查询任务协作人（含姓名 / 部门）
     */
    List<UserBriefVO> selectCollaborators(Long taskId);

    /**
     * 统计任务协作人数
     */
    int countByTaskId(Long taskId);

    /**
     * 判断某人是否为任务协作人
     */
    int countByTaskAndUser(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /**
     * 批量删除任务协作人（任务硬删除时级联）
     */
    int deleteByTaskIds(@Param("taskIds") Long[] taskIds);
}
