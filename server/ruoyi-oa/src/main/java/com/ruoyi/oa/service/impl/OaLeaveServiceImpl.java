package com.ruoyi.oa.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.oa.constant.OaConstants;
import com.ruoyi.oa.domain.OaLeave;
import com.ruoyi.oa.domain.vo.OaTaskVo;
import com.ruoyi.oa.flow.OaFlowService;
import com.ruoyi.oa.mapper.OaLeaveMapper;
import com.ruoyi.oa.service.IOaLeaveService;

/**
 * 请假申请 Service 实现
 *
 * @author ruoyi-oa
 */
@Service
public class OaLeaveServiceImpl implements IOaLeaveService
{
    private static final Logger log = LoggerFactory.getLogger(OaLeaveServiceImpl.class);

    @Autowired
    private OaLeaveMapper oaLeaveMapper;

    @Autowired
    private OaFlowService oaFlowService;

    @Override
    public List<OaLeave> selectOaLeaveList(OaLeave oaLeave)
    {
        return oaLeaveMapper.selectOaLeaveList(oaLeave);
    }

    @Override
    public OaLeave selectOaLeaveById(Long leaveId)
    {
        return oaLeaveMapper.selectOaLeaveById(leaveId);
    }

    @Override
    public int insertOaLeave(OaLeave oaLeave)
    {
        fillApplyUser(oaLeave);
        oaLeave.setStatus(OaConstants.LEAVE_STATUS_DRAFT);
        return oaLeaveMapper.insertOaLeave(oaLeave);
    }

    @Override
    public int updateOaLeave(OaLeave oaLeave)
    {
        OaLeave db = oaLeaveMapper.selectOaLeaveById(oaLeave.getLeaveId());
        if (db == null)
        {
            throw new ServiceException("请假单不存在");
        }
        if (!isEditable(db))
        {
            throw new ServiceException("当前状态不允许修改");
        }
        return oaLeaveMapper.updateOaLeave(oaLeave);
    }

    @Override
    public int deleteOaLeaveById(Long leaveId)
    {
        OaLeave db = oaLeaveMapper.selectOaLeaveById(leaveId);
        if (db == null)
        {
            return 0;
        }
        if (!OaConstants.LEAVE_STATUS_DRAFT.equals(db.getStatus()))
        {
            throw new ServiceException("只有草稿状态的请假单才能删除");
        }
        return oaLeaveMapper.deleteOaLeaveById(leaveId);
    }

    @Override
    public int deleteOaLeaveByIds(Long[] leaveIds)
    {
        for (Long leaveId : leaveIds)
        {
            OaLeave db = oaLeaveMapper.selectOaLeaveById(leaveId);
            if (db != null && !OaConstants.LEAVE_STATUS_DRAFT.equals(db.getStatus()))
            {
                throw new ServiceException("只有草稿状态的请假单才能删除");
            }
        }
        return oaLeaveMapper.deleteOaLeaveByIds(leaveIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitLeave(Long leaveId)
    {
        OaLeave leave = oaLeaveMapper.selectOaLeaveById(leaveId);
        if (leave == null)
        {
            throw new ServiceException("请假单不存在");
        }
        if (OaConstants.LEAVE_STATUS_APPROVED.equals(leave.getStatus())
                || OaConstants.LEAVE_STATUS_REVOKED.equals(leave.getStatus()))
        {
            throw new ServiceException("当前状态不允许提交");
        }

        String processInstanceId = leave.getProcessInstanceId();
        if (StringUtils.isNotEmpty(processInstanceId) && !oaFlowService.isFinished(processInstanceId))
        {
            // 被驳回后退回发起人节点，这里继续同一流程实例
            oaFlowService.completeSubmitTask(processInstanceId);
        }
        else
        {
            if (leave.getDuration() == null)
            {
                throw new ServiceException("请假时长不能为空");
            }
            processInstanceId = oaFlowService.startLeaveProcess(
                    leave.getLeaveId(), leave.getApplyUserId(), leave.getApplyDeptId(),
                    leave.getDuration().doubleValue());
            leave.setProcessInstanceId(processInstanceId);
        }

        leave.setStatus(OaConstants.LEAVE_STATUS_RUNNING);
        leave.setApplyTime(new Date());
        leave.setUpdateBy(SecurityUtils.getUsername());
        oaLeaveMapper.updateOaLeave(leave);
        log.info("请假单[{}]提交审批，流程实例={}", leaveId, processInstanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(String taskId, String comment)
    {
        Task task = oaFlowService.getTask(taskId);
        if (task == null)
        {
            throw new ServiceException("任务不存在或已被处理");
        }
        String processInstanceId = task.getProcessInstanceId();
        oaFlowService.approve(taskId, comment);
        // 流程结束则回写业务状态
        if (oaFlowService.isFinished(processInstanceId))
        {
            OaLeave leave = oaLeaveMapper.selectOaLeaveByProcessInstanceId(processInstanceId);
            if (leave != null)
            {
                leave.setStatus(OaConstants.LEAVE_STATUS_APPROVED);
                leave.setUpdateBy(SecurityUtils.getUsername());
                oaLeaveMapper.updateOaLeave(leave);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(String taskId, String comment)
    {
        if (StringUtils.isEmpty(comment))
        {
            throw new ServiceException("驳回时必须填写审批意见");
        }
        // 驳回不改业务状态：流程仍在进行，只是退回到发起人节点
        oaFlowService.reject(taskId, comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeLeave(Long leaveId)
    {
        OaLeave leave = oaLeaveMapper.selectOaLeaveById(leaveId);
        if (leave == null)
        {
            throw new ServiceException("请假单不存在");
        }
        if (!OaConstants.LEAVE_STATUS_RUNNING.equals(leave.getStatus()))
        {
            throw new ServiceException("只有审批中的请假单才能撤回");
        }
        oaFlowService.revoke(leave.getProcessInstanceId(), "发起人撤回");
        leave.setStatus(OaConstants.LEAVE_STATUS_REVOKED);
        leave.setUpdateBy(SecurityUtils.getUsername());
        oaLeaveMapper.updateOaLeave(leave);
    }

    @Override
    public List<OaTaskVo> selectTodoTasks()
    {
        Long userId = SecurityUtils.getUserId();
        List<Task> tasks = oaFlowService.listTodoTasks(String.valueOf(userId));
        List<OaTaskVo> result = new ArrayList<>(tasks.size());
        for (Task task : tasks)
        {
            OaTaskVo vo = new OaTaskVo();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setAssignee(task.getAssignee());
            vo.setCreateTime(task.getCreateTime());
            fillLeaveInfo(vo, task.getProcessInstanceId());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<OaTaskVo> selectDoneTasks()
    {
        Long userId = SecurityUtils.getUserId();
        List<HistoricTaskInstance> tasks = oaFlowService.listDoneTasks(String.valueOf(userId));
        List<OaTaskVo> result = new ArrayList<>(tasks.size());
        for (HistoricTaskInstance task : tasks)
        {
            OaTaskVo vo = new OaTaskVo();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setTaskDefinitionKey(task.getTaskDefinitionKey());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setAssignee(task.getAssignee());
            vo.setCreateTime(task.getCreateTime());
            vo.setEndTime(task.getEndTime());
            fillLeaveInfo(vo, task.getProcessInstanceId());
            result.add(vo);
        }
        return result;
    }

    @Override
    public Map<String, Object> selectLeaveTrace(Long leaveId)
    {
        OaLeave leave = oaLeaveMapper.selectOaLeaveById(leaveId);
        if (leave == null)
        {
            throw new ServiceException("请假单不存在");
        }
        Map<String, Object> trace = new HashMap<>(8);
        trace.put("leave", leave);
        trace.put("processInstanceId", leave.getProcessInstanceId());
        trace.put("finished", StringUtils.isEmpty(leave.getProcessInstanceId())
                || oaFlowService.isFinished(leave.getProcessInstanceId()));

        List<Map<String, Object>> records = new ArrayList<>();
        if (StringUtils.isNotEmpty(leave.getProcessInstanceId()))
        {
            List<HistoricTaskInstance> tasks = oaFlowService.listHistoricTasks(leave.getProcessInstanceId());
            for (HistoricTaskInstance task : tasks)
            {
                Map<String, Object> item = new HashMap<>(8);
                item.put("taskId", task.getId());
                item.put("taskName", task.getName());
                item.put("assignee", task.getAssignee());
                item.put("startTime", task.getStartTime());
                item.put("endTime", task.getEndTime());
                item.put("comment", oaFlowService.getTaskComment(task.getId()));
                item.put("finished", task.getEndTime() != null);
                records.add(item);
            }
        }
        trace.put("records", records);
        return trace;
    }

    /**
     * 业务单据是否可编辑：草稿，或被驳回退回到发起人节点时
     */
    private boolean isEditable(OaLeave leave)
    {
        if (OaConstants.LEAVE_STATUS_DRAFT.equals(leave.getStatus()))
        {
            return true;
        }
        if (!OaConstants.LEAVE_STATUS_RUNNING.equals(leave.getStatus()))
        {
            return false;
        }
        if (StringUtils.isEmpty(leave.getProcessInstanceId()))
        {
            return false;
        }
        Long userId = SecurityUtils.getUserId();
        List<Task> tasks = oaFlowService.listTodoTasks(String.valueOf(userId));
        for (Task task : tasks)
        {
            if (OaConstants.TASK_SUBMIT.equals(task.getTaskDefinitionKey())
                    && leave.getProcessInstanceId().equals(task.getProcessInstanceId()))
            {
                return true;
            }
        }
        return false;
    }

    private void fillApplyUser(OaLeave oaLeave)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        oaLeave.setApplyUserId(loginUser.getUserId());
        oaLeave.setApplyUserName(user.getNickName());
        oaLeave.setApplyDeptId(loginUser.getDeptId());
        oaLeave.setCreateBy(SecurityUtils.getUsername());
    }

    private void fillLeaveInfo(OaTaskVo vo, String processInstanceId)
    {
        OaLeave leave = oaLeaveMapper.selectOaLeaveByProcessInstanceId(processInstanceId);
        if (leave != null)
        {
            vo.setLeaveId(leave.getLeaveId());
            vo.setLeaveNo("QJ" + leave.getLeaveId());
            vo.setApplyUserName(leave.getApplyUserName());
            vo.setLeaveType(leave.getLeaveType());
            vo.setDuration(leave.getDuration() == null ? "" : leave.getDuration().toPlainString());
        }
    }
}
