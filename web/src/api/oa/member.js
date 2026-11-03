import request from '@/utils/request'
import { tansParams } from '@/utils/ruoyi'

/* -------------------------------------------------------------------------- */
/* 成员管理（PC-01 / PC-02，契约 §3.1 的 1–10 号接口）                        */
/* -------------------------------------------------------------------------- */

// 1. 查询成员列表（分页；name/studentNo/phone/deptId/grade/position/status/source/cohort/params[beginJoinDate,endJoinDate]）
export function listMember(query) {
  return request({
    url: '/oa/member/list',
    method: 'get',
    params: query
  })
}

// 2. 查询成员详情（含附件 fileId/url、账号状态、最近 20 条变动）
export function getMember(memberId) {
  return request({
    url: '/oa/member/' + memberId,
    method: 'get'
  })
}

// 3. 手动新增成员（免审，开号）
export function addMember(data) {
  return request({
    url: '/oa/member',
    method: 'post',
    data: data
  })
}

// 4. 编辑成员（后端写字段级 diff 日志）
export function updateMember(data) {
  return request({
    url: '/oa/member',
    method: 'put',
    data: data
  })
}

// 5. 状态变更：退会 / 毕业 / 恢复在籍（{ memberId, status, leaveDate, remark }）
export function changeMemberStatus(data) {
  return request({
    url: '/oa/member/status',
    method: 'put',
    data: data
  })
}

// 6. 调部门 / 升职（{ memberId, deptId, position, remark }，仅 admin / president）
export function transferMember(data) {
  return request({
    url: '/oa/member/transfer',
    method: 'put',
    data: data
  })
}

// 7. 成员统计卡（{ total, pendingApply, monthJoin, monthLeave }）
export function getMemberStats() {
  return request({
    url: '/oa/member/stats',
    method: 'get'
  })
}

// 8. 导出成员（POST 同列表参数，返回 xlsx 流；后端写审计日志）
// 与 utils/request#download 保持一致的 urlencoded 提交方式，兼容后端 query 绑定
export function exportMember(params) {
  return request({
    url: '/oa/member/export',
    method: 'post',
    transformRequest: [data => tansParams(data)],
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    responseType: 'blob',
    data: params
  })
}

// 14. 删除成员（软删除：del_flag=1，账号停用、数据保留；支持批量）
export function delMember(memberIds) {
  const ids = Array.isArray(memberIds) ? memberIds.join(',') : memberIds
  return request({
    url: '/oa/member/' + ids,
    method: 'delete'
  })
}

// 9. 当前登录人档案（H5-03 用）
export function getSelfMember() {
  return request({
    url: '/oa/member/self',
    method: 'get'
  })
}

// 10. 修改本人白名单字段（email / wechat / qq / skills / intro / photoFileId）
export function updateSelfMember(data) {
  return request({
    url: '/oa/member/self',
    method: 'put',
    data: data
  })
}

/* -------------------------------------------------------------------------- */
/* 附件与部门（PC-02 复用的公共能力）                                         */
/* -------------------------------------------------------------------------- */

// 20. PC 附件上传（multipart file + bizType：member_photo / member_card）
export function uploadOaFile(file, bizType) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return request({
    url: '/oa/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData,
    timeout: 30000
  })
}

// 21. 文件流（鉴权下载 / 预览，必须带 token 走 blob）
export function getOaFileBlob(fileId) {
  return request({
    url: '/oa/file/' + fileId,
    method: 'get',
    responseType: 'blob'
  })
}

// 部门下拉（走 OA 模块 /oa/member/dept-options：返回 [{deptId,deptName}]，按角色数据范围过滤；
// 管理员/主席团见全部部门，部长/干事仅本部门；PC-01 筛选 / PC-02 部门下拉）
// 注意：本仓库 SysDeptController 没有 /system/dept/treeselect，请求会被 /{deptId} 误匹配（deptId=treeselect 类型错误）
export function deptTreeSelect() {
  return request({
    url: '/oa/member/dept-options',
    method: 'get'
  })
}
