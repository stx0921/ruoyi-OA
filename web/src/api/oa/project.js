import request from '@/utils/request'

/* -------------------------------------------------------------------------- */
/* 项目（契约 §4 #13–#17）                                                      */
/* -------------------------------------------------------------------------- */

// 13. 项目列表：status / ownerId / keyword / pageNum / pageSize
export function listProject(query) {
  return request({
    url: '/oa/project/list',
    method: 'get',
    params: query
  })
}

// 14. 项目详情（含参与成员、任务统计）
export function getProject(projectId) {
  return request({
    url: '/oa/project/' + projectId,
    method: 'get'
  })
}

// 15. 新建项目：{ name, ownerId, startDate, endDate, description, memberIds[] }
export function addProject(data) {
  return request({
    url: '/oa/project',
    method: 'post',
    data: data
  })
}

// 16. 编辑项目：同上 + projectId；仅进行中可改
export function updateProject(data) {
  return request({
    url: '/oa/project',
    method: 'put',
    data: data
  })
}

/**
 * 17. 结束项目（归档）
 * @param {{projectId:number, force?:boolean}} data
 *   未完成 > 0 且未传 force 时后端返回 409（data 带 unfinishedCount），
 *   前端二次确认后带 force=true 重试。
 */
export function finishProject(data) {
  return request({
    url: '/oa/project/finish',
    method: 'put',
    data: data
  })
}

/**
 * 18.（追加需求 §9）删除项目（硬删除，不可恢复）
 * 项目下仍有任务或周期规则时后端返回 409（data 带 {taskCount,recurCount}）
 * @param {Array<number>|string} projectIds 数组或逗号串
 */
export function deleteProject(projectIds) {
  const ids = Array.isArray(projectIds) ? projectIds.join(',') : projectIds
  return request({
    url: '/oa/project/' + ids,
    method: 'delete'
  })
}
