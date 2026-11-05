package com.ruoyi.oa.task.service;

import java.util.List;
import com.ruoyi.oa.task.dto.TaskAuditDTO;
import com.ruoyi.oa.task.dto.TaskCancelDTO;
import com.ruoyi.oa.task.dto.TaskCreateDTO;
import com.ruoyi.oa.task.dto.TaskQuery;
import com.ruoyi.oa.task.dto.TaskReassignDTO;
import com.ruoyi.oa.task.dto.TaskSelfDTO;
import com.ruoyi.oa.task.dto.TaskSubmitDTO;
import com.ruoyi.oa.task.dto.TaskUpdateDTO;
import com.ruoyi.oa.task.vo.TaskBoardVO;
import com.ruoyi.oa.task.vo.TaskCountsVO;
import com.ruoyi.oa.task.vo.TaskDetailVO;
import com.ruoyi.oa.task.vo.TaskListVO;
import com.ruoyi.oa.task.vo.TaskSaveResultVO;

/**
 * 任务核心 Service
 *
 * @author ruoyi-oa
 */
public interface IOaTaskService
{
    /**
     * 任务列表（数据范围 OR 参与关系）
     */
    List<TaskListVO> selectTaskList(TaskQuery query);

    /**
     * 我的任务（只按参与关系，不受部门限制）
     */
    List<TaskListVO> selectMyTaskList(TaskQuery query);

    /**
     * 我的任务统计卡
     */
    TaskCountsVO selectMyCounts();

    /**
     * 任务看板（4 列 + 已取消数，排除归档项目）
     */
    TaskBoardVO selectBoard(TaskQuery query);

    /**
     * 任务详情（含子任务 / 协作人 / 附件 / 日志 / 只读标记）
     */
    TaskDetailVO selectTaskDetail(Long taskId);

    /**
     * 新建任务（可带子任务 / 协作人 / 附件）
     */
    TaskSaveResultVO insertTask(TaskCreateDTO dto);

    /**
     * 编辑任务（仅派发人 / admin / 主席团；仅待办 / 进行中）
     */
    void updateTask(TaskUpdateDTO dto);

    /**
     * 开始任务（待办 → 进行中）
     */
    void startTask(Long taskId);

    /**
     * 提交完成（进行中 → 待验收；子任务需全部完成）
     */
    void submitTask(TaskSubmitDTO dto);

    /**
     * 验收通过 / 打回
     */
    void auditTask(TaskAuditDTO dto);

    /**
     * 取消任务（待办 / 进行中 → 已取消）
     */
    void cancelTask(TaskCancelDTO dto);

    /**
     * 改派（只改主责人 / 协作人，状态不变）
     */
    void reassignTask(TaskReassignDTO dto);

    /**
     * 给自己建独立任务（主责 / 验收人 = 当前用户，不挂项目）
     */
    TaskSaveResultVO insertSelfTask(TaskSelfDTO dto);

    /**
     * 硬删除任务（批量；级联子任务 / 协作人 / 流转日志 / 附件记录 / 站内信）
     *
     * <p>权限：管理员 / 主席团任意；部长仅本部门任务；其余 403。不存在或已删除的 ID 跳过。</p>
     *
     * @param taskIds 任务ID数组（逗号分隔批量）
     */
    void deleteTasks(Long[] taskIds);
}
