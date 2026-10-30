package com.ruoyi.oa.flow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaConstants;

/**
 * 流程门面
 *
 * <p>刻意不做"万能封装"，只暴露业务真正需要的几个动作，
 * 让调用方看清每一步在 Flowable 里到底做了什么——这是本项目的技术练习点。</p>
 *
 * @author ruoyi-oa
 */
@Service
public class OaFlowService
{
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    /**
     * 启动请假流程，并自动完成"提交申请"节点（首次提交由系统代劳）
     *
     * @return 流程实例ID
     */
    public String startLeaveProcess(Long leaveId, Long applyUserId, Long applyDeptId, double days)
    {
        Map<String, Object> variables = new HashMap<>(8);
        variables.put("leaveId", leaveId);
        variables.put("applyUserId", String.valueOf(applyUserId));
        variables.put("applyDeptId", applyDeptId);
        variables.put("days", days);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                OaConstants.PROCESS_KEY_LEAVE, String.valueOf(leaveId), variables);

        Task submitTask = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .taskDefinitionKey(OaConstants.TASK_SUBMIT)
                .singleResult();
        if (submitTask != null)
        {
            taskService.complete(submitTask.getId());
        }
        return instance.getId();
    }

    /**
     * 完成"提交申请"节点：驳回后发起人修改完重新提交走这里
     */
    public void completeSubmitTask(String processInstanceId)
    {
        Task submitTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskDefinitionKey(OaConstants.TASK_SUBMIT)
                .singleResult();
        if (submitTask == null)
        {
            throw new ServiceException("当前流程不在可提交状态，请刷新后重试");
        }
        taskService.complete(submitTask.getId());
    }

    public Task getTask(String taskId)
    {
        return taskService.createTaskQuery().taskId(taskId).singleResult();
    }

    /**
     * 同意：完成任务，记录审批意见
     */
    public void approve(String taskId, String comment)
    {
        Task task = requireTask(taskId);
        if (StringUtils.isNotEmpty(comment))
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }
        taskService.complete(taskId);
    }

    /**
     * 驳回：把执行流从当前节点退回 {@code submitTask}，同一流程实例继续
     */
    public void reject(String taskId, String comment)
    {
        Task task = requireTask(taskId);
        if (StringUtils.isNotEmpty(comment))
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), OaConstants.TASK_SUBMIT)
                .changeState();
    }

    /**
     * 撤回：终止流程实例（历史保留，删除原因记为撤回）
     */
    public void revoke(String processInstanceId, String reason)
    {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (instance == null)
        {
            throw new ServiceException("流程已结束，无法撤回");
        }
        runtimeService.deleteProcessInstance(processInstanceId, reason);
    }

    /**
     * 流程实例是否已结束
     */
    public boolean isFinished(String processInstanceId)
    {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult() == null;
    }

    /**
     * 我的待办
     */
    public List<Task> listTodoTasks(String assignee)
    {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();
    }

    /**
     * 我的已办
     */
    public List<HistoricTaskInstance> listDoneTasks(String assignee)
    {
        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
    }

    /**
     * 流程实例上的历史任务（审批轨迹）
     */
    public List<HistoricTaskInstance> listHistoricTasks(String processInstanceId)
    {
        return historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();
    }

    /**
     * 流程实例是否存在于历史中（用于判断业务单是否真的发起过流程）
     */
    public HistoricProcessInstance getHistoricProcessInstance(String processInstanceId)
    {
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
    }

    /**
     * 任务审批意见
     */
    public String getTaskComment(String taskId)
    {
        try
        {
            List<org.flowable.engine.task.Comment> comments = taskService.getTaskComments(taskId);
            if (comments != null && !comments.isEmpty())
            {
                return comments.get(0).getFullMessage();
            }
        }
        catch (Exception ignored)
        {
            // 任务无意见时 Flowable 可能抛异常，忽略即可
        }
        return "";
    }

    private Task requireTask(String taskId)
    {
        Task task = getTask(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已被处理，请刷新后重试");
        }
        return task;
    }
}
