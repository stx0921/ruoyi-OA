import request from '@/utils/request'

/**
 * 查询部门列表（含在籍人数、正 / 副部长）
 *
 * 返回 DeptVO：{ deptId, deptName, parentId, memberCount, orderNum, status,
 *               leaderUserId, leaderName, viceLeaders:[{userId,userName}] }
 */
export function listDept(query) {
  return request({
    url: '/oa/dept/list',
    method: 'get',
    params: query
  })
}

/**
 * 保存部门负责人（正 / 副部长）
 *
 * @param {object} data
 *   deptId：部门 id
 *   leaders：[{ userId, leaderType: '1' 正 / '2' 副, sort }]
 */
export function updateDeptLeader(data) {
  return request({
    url: '/oa/dept/leader',
    method: 'put',
    data: data
  })
}

/**
 * 新增内设部门（挂在组织架构根部门下）
 *
 * @param {object} data { deptName, orderNum, status }
 */
export function addDept(data) {
  return request({
    url: '/oa/dept',
    method: 'post',
    data: data
  })
}

/**
 * 编辑内设部门（名称 / 排序 / 状态）
 *
 * @param {object} data { deptId, deptName, orderNum, status }
 */
export function updateDept(data) {
  return request({
    url: '/oa/dept',
    method: 'put',
    data: data
  })
}

/**
 * 删除内设部门
 *
 * @param {number} deptId 部门 id
 */
export function delDept(deptId) {
  return request({
    url: '/oa/dept/' + deptId,
    method: 'delete'
  })
}
