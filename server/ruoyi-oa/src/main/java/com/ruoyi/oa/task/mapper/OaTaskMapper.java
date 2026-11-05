package com.ruoyi.oa.task.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.oa.task.domain.OaTask;
import com.ruoyi.oa.task.dto.TaskQuery;
import com.ruoyi.oa.task.vo.TaskCountsVO;
import com.ruoyi.oa.task.vo.TaskDetailVO;
import com.ruoyi.oa.task.vo.TaskListVO;
import com.ruoyi.oa.task.vo.UserBriefVO;

/**
 * 任务 Mapper 接口
 *
 * <p>契约 §3.3 的方法签名必须严格保持不变（周期 Job / 退会监听器直接注入使用）。</p>
 *
 * @author ruoyi-oa
 */
public interface OaTaskMapper
{
    /* ==================== 契约 §3.3（跨模块稳定签名） ==================== */

    /**
     * 按主键查询任务
     */
    OaTask selectById(Long taskId);

    /**
     * 按主键查询任务并加行锁
     */
    OaTask selectByIdForUpdate(Long taskId);

    /**
     * 新增任务（useGeneratedKeys 回填 taskId）
     */
    int insertTask(OaTask task);

    /**
     * 周期任务幂等插入：唯一索引 {@code uk_recur_date} 命中时 update_time=update_time
     *
     * @return 1=新插入 0=已存在
     */
    int insertIgnoreRecurDate(OaTask task);

    /**
     * 动态更新任务（仅编辑类字段）
     */
    int updateTask(OaTask task);

    /**
     * 待办 → 进行中（条件更新 status='0'）
     */
    int startTask(@Param("taskId") Long taskId);

    /**
     * 进行中 → 待验收（条件更新 status='1'）
     */
    int submitTask(@Param("taskId") Long taskId);

    /**
     * 待验收 → 已完成（条件更新 status='2'，写 finish_time）
     */
    int finishTask(@Param("taskId") Long taskId);

    /**
     * 打回：待验收 → 进行中（条件更新 status='2'，reject_count+1）
     */
    int rejectTask(@Param("taskId") Long taskId);

    /**
     * 取消：待办/进行中 → 已取消（条件更新 status in ('0','1')）
     */
    int cancelTask(@Param("taskId") Long taskId);

    /**
     * 查询直接子任务
     */
    List<OaTask> selectChildren(Long parentId);

    /**
     * 统计未完成子任务数（status<>'3'）
     */
    int countUnfinishedChildren(Long parentId);

    /**
     * 分页查询某主责人的未完成任务（退会联动，Job/监听器用）
     */
    List<OaTask> selectUnfinishedByAssignee(@Param("userId") Long userId, @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 批量取消某主责人的未完成任务（status in ('0','1') → '4'）
     */
    int cancelUnfinishedByAssignee(@Param("userId") Long userId);

    /**
     * 验收人转交（退会联动）
     */
    int reassignAuditor(@Param("oldUserId") Long oldUserId, @Param("newUserId") Long newUserId);

    /**
     * 刷新逾期标记（仅更新有变化的行）
     */
    int refreshOverdueFlags();

    /**
     * 查询某生成日（yyyyMMdd）的周期任务（source='1'，status in ('0','1')）
     */
    List<OaTask> selectRecurGeneratedByDate(@Param("generateDate") String generateDate);

    /**
     * 查询截止时间落在 [begin,end) 的未完成任务（status in ('0','1')）
     */
    List<OaTask> selectUnfinishedDueBetween(@Param("begin") Date begin, @Param("end") Date end);

    /**
     * 按周期规则查询最近生成的任务
     */
    List<OaTask> selectByRecurId(@Param("recurId") Long recurId, @Param("limit") Integer limit);

    /**
     * 查询某日任务编号的最大序号（yyyyMMdd，用于编号 Redis 兜底）
     */
    Long selectMaxTaskNoSeq(@Param("datePart") String datePart);

    /**
     * 查询主责人账号停用/删除或成员退会的未完成任务（逾期 Job 退会对账）
     */
    List<OaTask> selectUnfinishedWithInactiveAssignee();

    /**
     * 按验收人查询任务（退会监听器转交验收人）
     */
    List<OaTask> selectByAuditor(@Param("auditorId") Long auditorId);

    /* ==================== 列表 / 看板 / 详情查询 ==================== */

    /**
     * 任务列表（@DataScope deptAlias=t + 参与关系 OR）
     */
    List<TaskListVO> selectTaskList(TaskQuery query);

    /**
     * 我的任务（只按参与关系，不受部门限制）
     */
    List<TaskListVO> selectMyTaskList(TaskQuery query);

    /**
     * 看板查询（4 列 + 逾期置顶排序，排除归档项目）
     */
    List<TaskListVO> selectBoardTasks(TaskQuery query);

    /**
     * 看板「已取消」入口数量
     */
    Long countBoardCanceled(TaskQuery query);

    /**
     * 我的任务统计卡
     */
    TaskCountsVO selectMyCounts(@Param("me") Long me);

    /**
     * 详情基础信息（含项目名 / 姓名）
     */
    TaskDetailVO selectTaskDetailBase(Long taskId);

    /**
     * 子任务列表 VO（详情用）
     */
    List<TaskListVO> selectChildVoList(Long parentId);

    /**
     * 未完成子任务（提交拦截时返回标题）
     */
    List<OaTask> selectUnfinishedChildren(Long parentId);

    /* ==================== 轻量关联查询（避免跨模块编译依赖） ==================== */

    /**
     * 查询用户简要信息（不校验状态）
     */
    UserBriefVO selectUserBrief(Long userId);

    /**
     * 查询在册用户简要信息（账号正常且成员未退会/毕业）
     */
    UserBriefVO selectActiveUserBrief(Long userId);

    /**
     * 批量查询用户简要信息
     */
    List<UserBriefVO> selectUserBriefs(@Param("userIds") List<Long> userIds);

    /**
     * 项目状态（0进行中 1已结束，不存在返回 null）
     */
    String selectProjectStatus(Long projectId);

    /**
     * 项目负责人ID（不存在返回 null）
     */
    Long selectProjectOwnerId(Long projectId);

    /**
     * 项目名称（不存在返回 null，通知文案用）
     */
    String selectProjectName(Long projectId);

    /**
     * 项目是否包含某成员
     */
    int countProjectMember(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 父任务项目变更时级联子任务项目
     */
    int updateChildrenProjectId(@Param("parentId") Long parentId, @Param("projectId") Long projectId);

    /* ==================== 删除（追加需求，契约 §9） ==================== */

    /**
     * 硬删除任务（批量）
     */
    int deleteByTaskIds(@Param("taskIds") Long[] taskIds);

    /**
     * 清除任务上的周期规则引用（删除周期规则时保留历史任务）
     */
    int clearRecurIdByRecurIds(@Param("recurIds") Long[] recurIds);

    /**
     * 统计项目下的任务数（项目删除前置校验）
     */
    int countByProjectIds(@Param("projectIds") Long[] projectIds);
}
