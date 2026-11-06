import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'

/* -------------------------------------------------------------------------- */
/* 志愿活动 v1 · 接口（PC / H5 共用，契约 §4.1）                               */
/* -------------------------------------------------------------------------- */

/* ------------------------------ PC 活动管理 ------------------------------ */

// 活动列表（keyword/showStatus/beginTime/endTime + 分页）
export function listActivity(query) {
  return request({
    url: '/oa/volunteer/activity/list',
    method: 'get',
    params: query
  })
}

// 活动详情（活动字段 + 人数统计 + 二维码 base64）
export function getActivity(activityId) {
  return request({
    url: '/oa/volunteer/activity/' + activityId,
    method: 'get'
  })
}

// 新增活动（返回 activityId / activityNo）
export function addActivity(data) {
  return request({
    url: '/oa/volunteer/activity',
    method: 'post',
    data: data
  })
}

// 修改活动（含取消活动 status='1'）
export function updateActivity(data) {
  return request({
    url: '/oa/volunteer/activity',
    method: 'put',
    data: data
  })
}

// 软删除活动（支持数组或逗号串）
export function deleteActivity(ids) {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request({
    url: '/oa/volunteer/activity/' + value,
    method: 'delete'
  })
}

// 活动报名二维码（{ content, base64 }）
export function getActivityQrcode(activityId) {
  return request({
    url: '/oa/volunteer/activity/' + activityId + '/qrcode',
    method: 'get'
  })
}

/* ------------------------------ 报名名单 / 审核 ------------------------------ */

// 某活动报名名单（activityId/auditStatus/keyword + 分页）
export function listSignup(query) {
  return request({
    url: '/oa/volunteer/signup/list',
    method: 'get',
    params: query
  })
}

// 审核报名（body：{ ids: [], auditStatus: 1|2, auditRemark }，支持批量）
export function auditSignup(data) {
  return request({
    url: '/oa/volunteer/signup/audit',
    method: 'put',
    data: data
  })
}

// 导出报名名单（GET 直接返回 xlsx 流）
export function exportSignup(query) {
  return request({
    url: '/oa/volunteer/signup/export',
    method: 'get',
    params: query,
    responseType: 'blob',
    timeout: 60000
  })
}

/* ------------------------------ 免登录 H5 ------------------------------ */

// 活动详情（免登录）
export function getPublicActivity(activityId) {
  return request({
    url: '/oa/volunteer/public/activity/' + activityId,
    method: 'get',
    headers: { isToken: false }
  })
}

// 报名（免登录）
export function publicSignup(data) {
  return request({
    url: '/oa/volunteer/public/signup',
    method: 'post',
    headers: { isToken: false, repeatSubmit: false },
    data: data
  })
}

// 查询报名审核结果（activityId + studentNo + name 三者匹配）
export function queryPublicResult(params) {
  return request({
    url: '/oa/volunteer/public/result',
    method: 'get',
    headers: { isToken: false },
    params: params
  })
}

/* ------------------------------ 统计 ------------------------------ */

// 统计总览（{ totalHours, activityCount, participantCount, avgHours }）
export function getStatOverview(query) {
  return request({
    url: '/oa/volunteer/stat/overview',
    method: 'get',
    params: query
  })
}

// 按月统计（[{ month, activityCount, passCount, totalHours }]）
export function getStatMonthly(query) {
  return request({
    url: '/oa/volunteer/stat/monthly',
    method: 'get',
    params: query
  })
}

// 导出统计数据（GET 直接返回 xlsx 流）
export function exportStat(query) {
  return request({
    url: '/oa/volunteer/stat/export',
    method: 'get',
    params: query,
    responseType: 'blob',
    timeout: 60000
  })
}

/* -------------------------------------------------------------------------- */
/* 展示助手（PC/H5 共用；只做展示兜底，不改变接口语义）                        */
/* -------------------------------------------------------------------------- */

/** 展示状态（后端 showStatus 枚举）：0 报名中 / 1 进行中 / 2 已结束 / 3 已取消 / 4 报名已截止 */
export const SHOW_STATUS_META = {
  0: { label: '报名中', tone: 'green' },
  1: { label: '进行中', tone: 'blue' },
  2: { label: '已结束', tone: 'gray' },
  3: { label: '已取消', tone: 'red' },
  4: { label: '报名已截止', tone: 'orange' }
}

/** 展示状态下拉选项 */
export const SHOW_STATUS_OPTIONS = Object.keys(SHOW_STATUS_META).map(value => ({
  value,
  label: SHOW_STATUS_META[value].label
}))

/** 活动状态（落库 status）：0 正常 / 1 已取消 */
export const ACTIVITY_STATUS_META = {
  0: { label: '正常', tone: 'green' },
  1: { label: '已取消', tone: 'red' }
}

/** 报名审核状态：0 待审核 / 1 通过 / 2 驳回 */
export const AUDIT_STATUS_META = {
  0: { label: '待审核', tone: 'orange' },
  1: { label: '通过', tone: 'green' },
  2: { label: '驳回', tone: 'red' }
}

/** 审核状态下拉选项 */
export const AUDIT_STATUS_OPTIONS = Object.keys(AUDIT_STATUS_META).map(value => ({
  value,
  label: AUDIT_STATUS_META[value].label
}))

/** 展示状态元数据（后端 showStatusLabel 优先、静态映射兜底） */
export function showStatusMeta(showStatus, label) {
  const key = showStatus === undefined || showStatus === null ? '' : String(showStatus)
  const base = SHOW_STATUS_META[key] || { label: label || key || '—', tone: 'gray' }
  return { label: label || base.label, tone: base.tone }
}

/** 活动状态元数据 */
export function activityStatusMeta(status) {
  const key = status === undefined || status === null ? '' : String(status)
  return ACTIVITY_STATUS_META[key] || { label: key || '—', tone: 'gray' }
}

/** 报名审核状态元数据（后端 auditStatusLabel 优先、静态映射兜底） */
export function auditStatusMeta(auditStatus, label) {
  const key = auditStatus === undefined || auditStatus === null ? '' : String(auditStatus)
  const base = AUDIT_STATUS_META[key] || { label: label || key || '—', tone: 'gray' }
  return { label: label || base.label, tone: base.tone }
}

/** 志愿时长展示：5 → 5.0 */
export function formatHours(value) {
  const num = Number(value)
  if (!Number.isFinite(num)) return '—'
  return num.toFixed(1)
}

/** datetime 展示：yyyy-MM-dd HH:mm */
export function formatActivityTime(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}') || '—'
}

/** date 展示：yyyy-MM-dd */
export function formatActivityDay(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d}') || '—'
}

/** 报名人数（兼容 signupCount / totalSignupCount / signupTotal） */
export function signupCountOf(row) {
  if (!row) return 0
  const value = row.signupCount !== undefined ? row.signupCount : row.totalSignupCount !== undefined ? row.totalSignupCount : row.signupTotal
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

/** 通过人数（兼容 passCount / approvedCount / passTotal） */
export function passCountOf(row) {
  if (!row) return 0
  const value = row.passCount !== undefined ? row.passCount : row.approvedCount !== undefined ? row.approvedCount : row.passTotal
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

/**
 * 二维码图片地址：后端返回 base64（契约）或 data URL
 * @param {Object|String} qrcode { base64 } 或字符串
 */
export function qrcodeSrc(qrcode) {
  const raw = typeof qrcode === 'string' ? qrcode : (qrcode && (qrcode.base64 || qrcode.qrcode)) || ''
  if (!raw) return ''
  const text = String(raw)
  if (/^data:image\//i.test(text)) return text
  if (/^https?:/i.test(text)) return text
  return 'data:image/png;base64,' + text
}

/** 二维码内容（H5 报名链接，链接文本仅展示可选中） */
export function qrcodeContentOf(qrcode) {
  if (!qrcode) return ''
  if (typeof qrcode === 'string') return ''
  return qrcode.content || qrcode.url || ''
}
