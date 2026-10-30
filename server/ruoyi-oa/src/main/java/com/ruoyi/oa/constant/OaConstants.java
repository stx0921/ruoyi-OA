package com.ruoyi.oa.constant;

/**
 * OA 业务常量
 *
 * @author ruoyi-oa
 */
public class OaConstants
{
    /** 请假单状态：草稿 */
    public static final String LEAVE_STATUS_DRAFT = "0";

    /** 请假单状态：审批中（含被驳回待修改） */
    public static final String LEAVE_STATUS_RUNNING = "1";

    /** 请假单状态：已通过 */
    public static final String LEAVE_STATUS_APPROVED = "2";

    /** 请假单状态：已驳回（保留常量，v1 不落库，驳回是中间态） */
    public static final String LEAVE_STATUS_REJECTED = "3";

    /** 请假单状态：已撤销 */
    public static final String LEAVE_STATUS_REVOKED = "4";

    /** 流程定义 key：请假审批 */
    public static final String PROCESS_KEY_LEAVE = "leaveApproval";

    /** BPMN 节点：提交申请（发起人节点，也是驳回退回点） */
    public static final String TASK_SUBMIT = "submitTask";

    /** BPMN 节点：部门负责人审批 */
    public static final String TASK_DEPT_LEADER = "deptLeaderTask";

    /** BPMN 节点：人事备案 */
    public static final String TASK_HR = "hrTask";

    /** BPMN 节点：总经理审批 */
    public static final String TASK_GM = "gmTask";

    /** 兜底审批人：admin */
    public static final String FALLBACK_ASSIGNEE = "1";

    /** 角色 key：人事 */
    public static final String ROLE_HR = "hr";

    /** 角色 key：总经理 */
    public static final String ROLE_GM = "gm";
}
