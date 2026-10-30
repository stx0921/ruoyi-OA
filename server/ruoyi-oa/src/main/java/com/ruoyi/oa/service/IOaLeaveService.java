package com.ruoyi.oa.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.oa.domain.OaLeave;
import com.ruoyi.oa.domain.vo.OaTaskVo;

/**
 * 请假申请 Service
 *
 * @author ruoyi-oa
 */
public interface IOaLeaveService
{
    List<OaLeave> selectOaLeaveList(OaLeave oaLeave);

    OaLeave selectOaLeaveById(Long leaveId);

    int insertOaLeave(OaLeave oaLeave);

    int updateOaLeave(OaLeave oaLeave);

    int deleteOaLeaveById(Long leaveId);

    int deleteOaLeaveByIds(Long[] leaveIds);

    /** 提交审批（首次启动流程 / 驳回后重新提交） */
    void submitLeave(Long leaveId);

    /** 同意 */
    void approveTask(String taskId, String comment);

    /** 驳回（退回发起人） */
    void rejectTask(String taskId, String comment);

    /** 撤回 */
    void revokeLeave(Long leaveId);

    /** 我的待办 */
    List<OaTaskVo> selectTodoTasks();

    /** 我的已办 */
    List<OaTaskVo> selectDoneTasks();

    /** 流程跟踪 */
    Map<String, Object> selectLeaveTrace(Long leaveId);
}
