import request from '@/utils/request'

/* -------------------------------------------------------------------------- */
/* 周期任务规则（契约 §4 #18–#21）                                               */
/* -------------------------------------------------------------------------- */

// 18. 规则列表：status / assigneeId / projectId / keyword / pageNum / pageSize
export function listRecur(query) {
  return request({
    url: '/oa/recur/list',
    method: 'get',
    params: query
  })
}

// 19. 新建规则：{ title, description, projectId, weekDays[], dueTime, assigneeId, priority, tags[], startDate }
//     响应 data：{ recurId, nextDate }
export function addRecur(data) {
  return request({
    url: '/oa/recur',
    method: 'post',
    data: data
  })
}

// 20. 编辑规则：同上 + recurId + applyScope(one/all) + targetTaskId(one 时必填)
export function updateRecur(data) {
  return request({
    url: '/oa/recur',
    method: 'put',
    data: data
  })
}

// 21. 暂停 / 恢复 / 结束：{ recurId, status }（1 暂停 / 0 恢复 / 2 结束）
export function updateRecurStatus(data) {
  return request({
    url: '/oa/recur/status',
    method: 'put',
    data: data
  })
}

// 22.（追加需求 §9）删除周期规则（硬删除，不可恢复；已生成任务保留，recur_id 置空）
export function deleteRecur(recurIds) {
  const ids = Array.isArray(recurIds) ? recurIds.join(',') : recurIds
  return request({
    url: '/oa/recur/' + ids,
    method: 'delete'
  })
}
