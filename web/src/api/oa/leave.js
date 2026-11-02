import request from '@/utils/request'

// 查询请假申请列表（支持 applyUserName / leaveType / status / applyUserId / params[beginTime|endTime]）
export function listLeave(query) {
  return request({
    url: '/oa/leave/list',
    method: 'get',
    params: query
  })
}

// 查询请假申请详细
export function getLeave(leaveId) {
  return request({
    url: '/oa/leave/' + leaveId,
    method: 'get'
  })
}

// 新增请假申请（草稿）
export function addLeave(data) {
  return request({
    url: '/oa/leave',
    method: 'post',
    data: data
  })
}

// 修改请假申请（仅草稿 / 被驳回待修改）
export function updateLeave(data) {
  return request({
    url: '/oa/leave',
    method: 'put',
    data: data
  })
}

// 删除请假申请（仅草稿）
export function delLeave(leaveIds) {
  return request({
    url: '/oa/leave/' + leaveIds,
    method: 'delete'
  })
}

// 提交审批（首次提交 / 驳回后重新提交）
export function submitLeave(leaveId) {
  return request({
    url: '/oa/leave/submit/' + leaveId,
    method: 'post'
  })
}

// 同意
export function approveLeave(data) {
  return request({
    url: '/oa/leave/approve',
    method: 'post',
    data: data
  })
}

// 驳回（退回发起人修改，审批意见必填）
export function rejectLeave(data) {
  return request({
    url: '/oa/leave/reject',
    method: 'post',
    data: data
  })
}

// 撤回（仅审批中）
export function revokeLeave(leaveId) {
  return request({
    url: '/oa/leave/revoke/' + leaveId,
    method: 'post'
  })
}

// 流程跟踪（基本信息 + 审批记录）
export function getLeaveTrace(leaveId) {
  return request({
    url: '/oa/leave/trace/' + leaveId,
    method: 'get'
  })
}
