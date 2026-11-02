import request from '@/utils/request'

/**
 * 查询入会申请列表
 *
 * @param {object} query
 *   pageNum / pageSize
 *   scope：todo 待我审批 / done 我已审批 / all 全部
 *   status：0 待审批 / 1 已通过 / 2 已驳回 / 3 已撤回 / 4 已关闭
 *   deptId、name、studentNo
 *   params[beginTime|endTime]：提交时间范围
 */
export function listApply(query) {
  return request({
    url: '/oa/apply/list',
    method: 'get',
    params: query
  })
}

// 查询入会申请详情（完整表单 + 附件 + 审批时间轴）
export function getApply(applyId) {
  return request({
    url: '/oa/apply/' + applyId,
    method: 'get'
  })
}

/**
 * 审批入会申请（同意 / 驳回，支持批量）
 *
 * @param {object} data
 *   ids：申请 id 数组（单次 ≤50）
 *   pass：true 同意 / false 驳回
 *   remark：审批意见（驳回必填 2–200 字）
 */
export function auditApply(data) {
  return request({
    url: '/oa/apply/audit',
    method: 'post',
    data: data
  })
}

/**
 * 附件鉴权下载（R12：不返回裸 URL，必须带 token 走 blob）
 *
 * 返回文件流（responseType=blob），调用方用 file-saver 保存或生成预览 URL。
 */
export function getApplyFile(fileId) {
  return request({
    url: '/oa/file/' + fileId,
    method: 'get',
    responseType: 'blob'
  })
}
