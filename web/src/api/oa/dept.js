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
