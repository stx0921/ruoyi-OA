import request from '@/utils/request'

/**
 * 查询成员操作日志（字段级变更留痕，只读）
 *
 * @param {object} query
 *   pageNum / pageSize
 *   memberId：对象成员 id
 *   action：动作（新增 / 编辑 / 状态变更 ……）
 *   operatorName：操作人
 *   keyword：对象关键字（成员姓名 / 学号）
 *   params[beginTime|endTime]：时间范围（默认近 7 天、最大 90 天）
 */
export function listMemberLog(query) {
  return request({
    url: '/oa/member/log/list',
    method: 'get',
    params: query
  })
}
