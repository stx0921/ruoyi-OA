package com.ruoyi.oa.project.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.project.domain.OaProject;
import com.ruoyi.oa.project.dto.ProjectQuery;

/**
 * 项目 Mapper
 *
 * <p>契约 §3.3 要求对外提供：selectProjectById / isFinished / countUnfinishedTasks（供任务模块注入使用）。</p>
 *
 * @author ruoyi-oa
 */
public interface OaProjectMapper
{
    /**
     * 项目列表（需配合 @DataScope，SQL 中拼 ${params.dataScope}）
     */
    List<OaProject> selectProjectList(ProjectQuery query);

    /**
     * 按 ID 查询项目（含任务数/已完成数/参与人数统计、部门名称）
     */
    OaProject selectProjectById(Long projectId);

    /**
     * 按 ID 查询项目并加行锁（归档并发保护）
     */
    OaProject selectProjectByIdForUpdate(Long projectId);

    /**
     * 新增项目
     */
    int insertProject(OaProject project);

    /**
     * 更新项目（不更新编号/状态/结束时间）
     */
    int updateProject(OaProject project);

    /**
     * 结束项目（仅 status=0 可置为已结束）
     */
    int finishProject(@Param("projectId") Long projectId, @Param("finishTime") Date finishTime,
            @Param("updateBy") String updateBy);

    /**
     * 项目是否已结束（status=1）
     */
    boolean isFinished(Long projectId);

    /**
     * 项目下未完成任务数（status in 0/1/2）
     */
    int countUnfinishedTasks(Long projectId);

    /**
     * 查询某年前缀下最大项目编号（DB 兜底序号用）
     */
    String selectMaxProjectNo(@Param("prefix") String prefix);

    /**
     * 批量硬删除项目（不存在的 ID 自动跳过，幂等）
     */
    int deleteByIds(@Param("projectIds") Long[] projectIds);
}
