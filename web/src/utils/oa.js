/**
 * OA 前端公共工具
 *
 * 把"后端契约相关"的常量与格式化收敛到一处，页面只负责展示：
 * - 请假单状态 / 类型文案（后端返回的是字符串状态码）
 * - BPMN 节点定义与流程轨迹推导（与 leave_approval.bpmn20.xml 对齐）
 * - 时间、时长、百分比等展示格式
 * - 工作台 / 统计看板需要的轻量聚合
 */
import { parseTime } from '@/utils/ruoyi'

/** 请假单状态，对应 OaConstants：0草稿 1审批中 2已通过 3已驳回 4已撤销 */
export const LEAVE_STATUS_MAP = {
  0: { label: '草稿', tone: 'gray' },
  1: { label: '审批中', tone: 'orange' },
  2: { label: '已通过', tone: 'green' },
  3: { label: '已驳回', tone: 'red' },
  4: { label: '已撤销', tone: 'red' }
}

/** 状态下拉选项（筛选用） */
export const LEAVE_STATUS_OPTIONS = Object.keys(LEAVE_STATUS_MAP).map(value => ({
  value,
  label: LEAVE_STATUS_MAP[value].label
}))

/** 字典 oa_leave_type 的兜底文案：字典接口不可用或未初始化时仍能展示 */
export const LEAVE_TYPE_MAP = {
  1: '事假',
  2: '病假',
  3: '年假',
  4: '调休',
  5: '婚假',
  6: '产假'
}

/**
 * BPMN 节点定义（顺序即展示顺序）
 * 与 server/ruoyi-oa/src/main/resources/processes/leave_approval.bpmn20.xml 的 id / name 一致
 */
export const OA_FLOW_NODES = [
  { key: 'submitTask', name: '提交申请', role: '发起人' },
  { key: 'deptLeaderTask', name: '部门负责人审批', role: '部门负责人' },
  { key: 'hrTask', name: '人事备案', role: '人事' },
  { key: 'gmTask', name: '总经理审批', role: '总经理' }
]

/** 状态文案 */
export function leaveStatusText(status) {
  return (LEAVE_STATUS_MAP[String(status)] || {}).label || '-'
}

/** 状态色调（对应全局 .oa-pill--xxx） */
export function leaveStatusTone(status) {
  return (LEAVE_STATUS_MAP[String(status)] || {}).tone || 'gray'
}

/** 请假类型文案：优先字典，其次兜底映射 */
export function leaveTypeText(value, dicts) {
  if (value === undefined || value === null || value === '') return '-'
  if (Array.isArray(dicts)) {
    const hit = dicts.find(item => String(item.value) === String(value))
    if (hit) return hit.label
  }
  return LEAVE_TYPE_MAP[String(value)] || String(value)
}

/** 业务单号：后端统一用 QJ + leaveId（见 OaTaskVo#setLeaveNo） */
export function oaLeaveNo(leave) {
  if (!leave || leave.leaveId === undefined || leave.leaveId === null) return '—'
  return 'QJ' + leave.leaveId
}

/** 天数文本：2 → "2.0 天" */
export function oaDurationText(duration) {
  const num = Number(duration)
  if (duration === null || duration === undefined || duration === '' || isNaN(num)) return '—'
  return num.toFixed(1) + ' 天'
}

/** 统一转 Date（兼容 yyyy-MM-dd HH:mm:ss 在部分浏览器的解析差异） */
function toDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  if (typeof value === 'number') return new Date(value)
  const date = new Date(String(value).replace(/-/g, '/'))
  return isNaN(date.getTime()) ? null : date
}

/** 时间戳（排序用，无效时间归 0） */
export function oaTimestamp(value) {
  const date = toDate(value)
  return date ? date.getTime() : 0
}

/** yyyy-MM-dd HH:mm */
export function oaDateTime(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}') || '—'
}

/** yyyy-MM-dd */
export function oaDate(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d}') || '—'
}

/** 把"距今多久"格式化成 15 分钟 / 2 小时 / 1 天 */
export function oaHumanElapsed(value) {
  const date = toDate(value)
  if (!date) return '—'
  const diff = Date.now() - date.getTime()
  if (diff <= 0) return '刚刚'
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return minutes + ' 分钟'
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return hours + ' 小时'
  return Math.floor(hours / 24) + ' 天'
}

/** 已等待 / 已耗时文案 */
export function oaWaitText(value) {
  return oaHumanElapsed(value)
}

/** 相对时间：15 分钟前 */
export function oaTimeAgo(value) {
  const text = oaHumanElapsed(value)
  return text === '刚刚' ? '刚刚' : text + '前'
}

/** 分钟数格式化成 40 分钟 / 2.1 小时 / 1.8 天 */
export function oaHumanSpan(minutes) {
  const num = Number(minutes)
  if (!num || num < 0) return '—'
  if (num < 60) return Math.round(num) + ' 分钟'
  if (num < 60 * 24) return (num / 60).toFixed(1) + ' 小时'
  return (num / 60 / 24).toFixed(1) + ' 天'
}

/** 两点时间之间的分钟数 */
export function oaSpanMinutes(start, end) {
  const from = toDate(start)
  const to = toDate(end)
  if (!from || !to) return null
  return (to.getTime() - from.getTime()) / 60000
}

/** 平均耗时（分钟）：默认按 createTime → endTime 统计，跳过无效记录 */
export function oaAvgMinutes(rows, startKey = 'createTime', endKey = 'endTime') {
  const list = (rows || []).map(row => oaSpanMinutes(row[startKey], row[endKey])).filter(value => value !== null && value >= 0)
  if (!list.length) return null
  return list.reduce((sum, value) => sum + value, 0) / list.length
}

/** 百分比（保留 1 位，分母为 0 时返回 0） */
export function oaPercent(part, total) {
  if (!total) return 0
  return Math.round((part / total) * 1000) / 10
}

/** 按 key 聚合计数，返回 [{ name, value }]，按数量倒序 */
export function oaGroupCount(rows, keyFn) {
  const map = new Map()
  ;(rows || []).forEach(row => {
    const key = keyFn(row)
    if (key === undefined || key === null || key === '') return
    map.set(key, (map.get(key) || 0) + 1)
  })
  return [...map.entries()]
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
}

/** 用户列表 → { userId: { nickName, deptName } } */
export function oaUserMap(users) {
  const map = {}
  ;(users || []).forEach(user => {
    if (user && user.userId !== undefined) {
      map[String(user.userId)] = {
        nickName: user.nickName || user.userName || String(user.userId),
        userName: user.userName,
        deptName: (user.dept && user.dept.deptName) || ''
      }
    }
  })
  return map
}

/**
 * 解析审批人显示名
 * records 里 assignee 存的是 userId 字符串；拿不到用户表时用申请人姓名兜底
 */
export function oaUserName(users, assignee, leave) {
  if (assignee === undefined || assignee === null || assignee === '') return '—'
  const hit = (users || {})[String(assignee)]
  if (hit) return hit.nickName || hit.userName || String(assignee)
  if (leave && String(leave.applyUserId) === String(assignee)) {
    return leave.applyUserName || '用户' + assignee
  }
  return '用户' + assignee
}

/** 按请假天数计算实际会走到的审批节点（与 BPMN 排他网关条件一致） */
export function oaApplicableNodes(duration) {
  const days = Number(duration) || 0
  const nodes = [OA_FLOW_NODES[0], OA_FLOW_NODES[1]]
  if (days > 1) nodes.push(OA_FLOW_NODES[2])
  if (days > 3) nodes.push(OA_FLOW_NODES[3])
  return nodes
}

/** BPMN 节点名 → 节点 key */
export function oaNodeKeyByName(name) {
  const hit = OA_FLOW_NODES.find(node => node.name === name)
  return hit ? hit.key : ''
}

/**
 * 由业务单 + 审批记录推导流程图节点状态
 *
 * 驳回采用"同实例退回"语义：节点可能重复出现，因此每个节点只取最后一次记录；
 * 被退回前处理过、但当前已不在该节点的，标记为 rejected（已退回）。
 *
 * @returns {{ nodes: Array, endState: string }}
 *   node.state: done 已完成 / current 进行中 / wait 未开始 / rejected 已退回
 *   endState: done / wait / revoked
 */
export function oaFlowTrace(leave, records) {
  const list = records || []
  const status = String((leave && leave.status) === undefined ? '' : leave.status)
  const approved = status === '2'
  const revoked = status === '4'
  const nodes = oaApplicableNodes(leave && leave.duration)

  // 同一节点取最后一条记录
  const latest = {}
  list.forEach(record => {
    const key = oaNodeKeyByName(record.taskName)
    if (key) latest[key] = record
  })

  const unfinished = list.find(record => !record.finished)
  const currentKey = unfinished ? oaNodeKeyByName(unfinished.taskName) : ''
  const currentIndex = currentKey ? nodes.findIndex(node => node.key === currentKey) : -1

  const result = nodes.map((node, index) => {
    const record = latest[node.key]
    let state = 'wait'
    if (approved) {
      state = 'done'
    } else if (node.key === currentKey) {
      state = 'current'
    } else if (record && record.finished) {
      state = currentIndex === -1 || index < currentIndex ? 'done' : 'rejected'
    }
    return {
      key: node.key,
      name: node.name,
      role: node.role,
      state,
      assignee: record ? record.assignee : '',
      startTime: record ? record.startTime : '',
      endTime: record ? record.endTime : '',
      comment: record ? record.comment : ''
    }
  })

  let endState = 'wait'
  if (approved) endState = 'done'
  else if (revoked) endState = 'revoked'

  return { nodes: result, endState }
}

/** 当前节点（用于列表/详情展示"当前节点"列） */
export function oaCurrentNode(leave, records) {
  const unfinished = (records || []).find(record => !record.finished)
  if (unfinished) {
    return {
      name: unfinished.taskName,
      assignee: unfinished.assignee,
      startTime: unfinished.startTime
    }
  }
  if (String((leave && leave.status) === undefined ? '' : leave.status) === '2') {
    return { name: '已办结', assignee: '', startTime: '' }
  }
  return { name: '', assignee: '', startTime: '' }
}
