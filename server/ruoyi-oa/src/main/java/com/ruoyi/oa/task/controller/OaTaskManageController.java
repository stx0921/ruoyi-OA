package com.ruoyi.oa.task.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.oa.task.dto.TaskAuditDTO;
import com.ruoyi.oa.task.dto.TaskCancelDTO;
import com.ruoyi.oa.task.dto.TaskCreateDTO;
import com.ruoyi.oa.task.dto.TaskQuery;
import com.ruoyi.oa.task.dto.TaskReassignDTO;
import com.ruoyi.oa.task.dto.TaskSelfDTO;
import com.ruoyi.oa.task.dto.TaskStartDTO;
import com.ruoyi.oa.task.dto.TaskSubmitDTO;
import com.ruoyi.oa.task.dto.TaskUpdateDTO;
import com.ruoyi.oa.task.service.IOaTaskService;
import com.ruoyi.oa.task.vo.TaskDetailVO;
import com.ruoyi.oa.task.vo.TaskListVO;
import com.ruoyi.oa.task.vo.TaskMyTable;

/**
 * 任务协作 Controller（契约 23 接口中的任务 12 个）
 *
 * <p>注意：类名与请假模块 {@code com.ruoyi.oa.controller.OaTaskController} 不同，避免 Spring bean 重名；
 * 请假待办 {@code /oa/task/todo|done} 仍由原 Controller 提供，本类不得覆盖。</p>
 *
 * @author ruoyi-oa
 */
@RestController
@RequestMapping("/oa/task")
public class OaTaskManageController extends BaseController
{
    @Autowired
    private IOaTaskService taskService;

    /**
     * 任务列表（数据范围 OR 我参与）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(TaskQuery query)
    {
        startPage();
        return getDataTable(taskService.selectTaskList(query));
    }

    /**
     * 我的任务（含统计卡 counts；只按参与关系，不受部门限制）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/my")
    public TaskMyTable my(TaskQuery query)
    {
        startPage();
        List<TaskListVO> rows = taskService.selectMyTaskList(query);
        TaskMyTable table = new TaskMyTable();
        table.setCode(HttpStatus.SUCCESS);
        table.setMsg("查询成功");
        table.setRows(rows);
        table.setTotal(new PageInfo<TaskListVO>(rows).getTotal());
        table.setCounts(taskService.selectMyCounts());
        return table;
    }

    /**
     * 任务看板（4 列 + 已取消数，排除归档项目任务）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/board")
    public AjaxResult board(TaskQuery query)
    {
        return success(taskService.selectBoard(query));
    }

    /**
     * 任务详情（子任务 / 协作人 / 附件 / 流转日志 / 只读标记）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:query')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable("taskId") Long taskId)
    {
        TaskDetailVO detail = taskService.selectTaskDetail(taskId);
        return success(detail);
    }

    /**
     * 新建任务（可多人协作、可挂项目、可带子任务）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:add')")
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TaskCreateDTO dto)
    {
        return success(taskService.insertTask(dto));
    }

    /**
     * 编辑任务（仅派发人 / 主席团 / 管理员；仅未完成态）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:edit')")
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TaskUpdateDTO dto)
    {
        taskService.updateTask(dto);
        return success();
    }

    /**
     * 开始任务（待办 → 进行中）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:edit')")
    @Log(title = "任务开始", businessType = BusinessType.UPDATE)
    @PutMapping("/start")
    public AjaxResult start(@RequestBody TaskStartDTO dto)
    {
        taskService.startTask(dto == null ? null : dto.getTaskId());
        return success();
    }

    /**
     * 提交完成（主责人；子任务需全部完成）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:submit')")
    @Log(title = "任务提交", businessType = BusinessType.UPDATE)
    @PutMapping("/submit")
    public AjaxResult submit(@RequestBody TaskSubmitDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        taskService.submitTask(dto);
        return success();
    }

    /**
     * 验收通过 / 打回（打回理由必填）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:audit')")
    @Log(title = "任务验收", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public AjaxResult audit(@RequestBody TaskAuditDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        taskService.auditTask(dto);
        return success();
    }

    /**
     * 取消任务（原因必填；待验收须先打回）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:edit')")
    @Log(title = "任务取消", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel")
    public AjaxResult cancel(@RequestBody TaskCancelDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        taskService.cancelTask(dto);
        return success();
    }

    /**
     * 改派（主责人 / 协作人，状态不变）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:assign')")
    @Log(title = "任务改派", businessType = BusinessType.GRANT)
    @PutMapping("/reassign")
    public AjaxResult reassign(@RequestBody TaskReassignDTO dto)
    {
        if (dto == null || dto.getTaskId() == null)
        {
            throw new ServiceException("任务不存在", HttpStatus.NOT_FOUND);
        }
        taskService.reassignTask(dto);
        return success();
    }

    /**
     * 给自己建独立任务（登录即可；主责 / 验收人强制=当前用户，不挂项目）
     */
    @Log(title = "新建自有任务", businessType = BusinessType.INSERT)
    @PostMapping("/self")
    public AjaxResult self(@RequestBody TaskSelfDTO dto)
    {
        return success(taskService.insertSelfTask(dto));
    }

    /**
     * 硬删除任务（逗号分隔批量；级联子任务 / 协作人 / 流转日志 / 附件 / 站内信）
     *
     * <p>契约 §9 追加；任意状态可删（含归档项目任务），不存在 ID 跳过。</p>
     */
    @PreAuthorize("@ss.hasPermi('oa:task:remove')")
    @Log(title = "任务协作", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskIds}")
    public AjaxResult remove(@PathVariable("taskIds") Long[] taskIds)
    {
        taskService.deleteTasks(taskIds);
        return success();
    }
}
