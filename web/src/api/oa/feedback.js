import request from '@/utils/request'
import { parseTime } from '@/utils/ruoyi'

/* -------------------------------------------------------------------------- */
/* 工单 / 意见反馈 v1 · 接口（用户端 + 管理端共用，契约 §1 / §3）               */
/* -------------------------------------------------------------------------- */

// 用户端列表（scope=all|mine；type/status/keyword/beginTime/endTime；submitTime 倒序）
export function listFeedback(query) {
  return request({
    url: '/oa/feedback/list',
    method: 'get',
    params: query
  })
}

// 工单详情（含附件、官方回复；匿名由后端决定 submitterName）
export function getFeedback(feedbackId) {
  return request({
    url: '/oa/feedback/' + feedbackId,
    method: 'get'
  })
}

// 提交工单（返回工单编号，后端 R<String> 或 { ticketNo }）
export function addFeedback(data) {
  return request({
    url: '/oa/feedback',
    method: 'post',
    data: data
  })
}

// 撤回工单（仅本人 + 待处理）
export function withdrawFeedback(feedbackId) {
  return request({
    url: '/oa/feedback/withdraw/' + feedbackId,
    method: 'put'
  })
}

// 附件上传（类型/大小/魔数二次校验，落 feedback_temp）
export function uploadFeedbackFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/oa/feedback/upload',
    method: 'post',
    // 多张图片连续上传间隔可能 <1s，关闭 request 的防重复提交拦截
    headers: { 'Content-Type': 'multipart/form-data', repeatSubmit: false },
    data: formData,
    timeout: 60000
  })
}

// 影响模块两级下拉（sys_menu 目录 + 菜单）
export function getModuleOptions() {
  return request({
    url: '/oa/feedback/module-options',
    method: 'get'
  })
}

// 管理端统计卡（{ pendingCount, repliedCount, todayCount, weekCount }）
export function getFeedbackStats() {
  return request({
    url: '/oa/feedback/stats',
    method: 'get'
  })
}

// 管理端列表（待处理优先 + 提交时间倒序）
export function getManageList(query) {
  return request({
    url: '/oa/feedback/manage/list',
    method: 'get',
    params: query
  })
}

// 官方回复（{ feedbackId, replyContent, status }）
export function replyFeedback(data) {
  return request({
    url: '/oa/feedback/reply',
    method: 'put',
    data: data
  })
}

// 逻辑删除（支持数组或逗号串，DELETE /oa/feedback/{ids}）
export function deleteFeedback(ids) {
  const value = Array.isArray(ids) ? ids.join(',') : ids
  return request({
    url: '/oa/feedback/' + value,
    method: 'delete'
  })
}

// 批量标记已回复（{ ids, replyContent? }）
export function batchReplyFeedback(data) {
  return request({
    url: '/oa/feedback/batch-reply',
    method: 'put',
    data: data
  })
}

/* -------------------------------------------------------------------------- */
/* 前端展示助手（用户端 / 管理端共用；只做展示兜底，不改变接口语义）           */
/* -------------------------------------------------------------------------- */

/** 类型元数据：1 Bug / 2 优化建议 / 3 新增功能 / 4 其他 */
export const FEEDBACK_TYPE_META = {
  1: { label: 'Bug', tone: 'red', icon: '🐞', desc: '功能异常 / 报错' },
  2: { label: '优化建议', tone: 'blue', icon: '💡', desc: '体验改进 / 效率提升' },
  3: { label: '新增功能', tone: 'green', icon: '✨', desc: '希望支持的新能力' },
  4: { label: '其他', tone: 'gray', icon: '💬', desc: '其他任何想法' }
}

/** 状态元数据：0 待处理 / 1 已回复 */
export const FEEDBACK_STATUS_META = {
  0: { label: '待处理', tone: 'orange' },
  1: { label: '已回复', tone: 'green' }
}

/** 类型下拉选项 */
export const FEEDBACK_TYPE_OPTIONS = Object.keys(FEEDBACK_TYPE_META).map(value => ({
  value,
  label: FEEDBACK_TYPE_META[value].label
}))

/** 状态下拉选项 */
export const FEEDBACK_STATUS_OPTIONS = Object.keys(FEEDBACK_STATUS_META).map(value => ({
  value,
  label: FEEDBACK_STATUS_META[value].label
}))

/**
 * 类型展示元数据（字典 oa_feedback_type 优先、静态映射兜底）
 * @param {*} type 后端 type（'1'–'4'）
 * @param {Array} dicts useDict 返回的数组（可为空）
 */
export function feedbackTypeMeta(type, dicts) {
  const key = type === undefined || type === null || type === '' ? '' : String(type)
  const base = FEEDBACK_TYPE_META[key] || { label: key || '—', tone: 'gray', icon: '💬', desc: '' }
  if (Array.isArray(dicts)) {
    const hit = dicts.find(item => String(item.value) === key)
    if (hit && hit.label) return { ...base, label: hit.label }
  }
  return { ...base }
}

/**
 * 状态展示元数据（字典 oa_feedback_status 优先、静态映射兜底）
 * @param {*} status 后端 status（'0' / '1'）
 * @param {Array} dicts useDict 返回的数组（可为空）
 */
export function feedbackStatusMeta(status, dicts) {
  const key = status === undefined || status === null || status === '' ? '' : String(status)
  const base = FEEDBACK_STATUS_META[key] || { label: key || '—', tone: 'gray' }
  if (Array.isArray(dicts)) {
    const hit = dicts.find(item => String(item.value) === key)
    if (hit && hit.label) return { ...base, label: hit.label }
  }
  return { ...base }
}

/** 提交时间展示：yyyy-MM-dd HH:mm */
export function formatFeedbackTime(value) {
  if (!value) return '—'
  return parseTime(value, '{y}-{m}-{d} {h}:{i}') || '—'
}

/** 详情 / 列表里的附件数组（兼容 files / attachments / fileList 三种命名） */
export function feedbackFilesOf(vo) {
  if (!vo) return []
  const list = vo.files || vo.attachments || vo.fileList || vo.fileInfos
  return Array.isArray(list) ? list : []
}

/** 附件相对路径 → 可访问地址（VITE_APP_BASE_API + url；绝对地址原样返回） */
export function feedbackFileUrl(file) {
  const raw =
    typeof file === 'string' ? file : (file && (file.url || file.filePath || file.fileUrl)) || ''
  if (!raw) return ''
  if (/^(https?:|blob:|data:)/i.test(raw)) return raw
  const base = import.meta.env.VITE_APP_BASE_API || ''
  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base
  const path = raw.charAt(0) === '/' ? raw : '/' + raw
  return normalizedBase + path
}

/** 附件展示名：originalName 优先，其次 fileName / URL 文件名 */
export function feedbackFileName(file) {
  if (!file) return ''
  if (typeof file === 'string') return file.split('/').pop() || file
  return (
    file.originalName ||
    file.fileName ||
    (file.url ? String(file.url).split('/').pop() : '') ||
    ''
  )
}

/** 附件字节数 */
export function feedbackFileSize(file) {
  if (!file) return 0
  const value = file.size !== undefined ? file.size : file.fileSize
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

/**
 * 附件种类：image / word / other
 * 后端下发 fileType 优先，缺失时按扩展名推断
 */
export function feedbackFileType(file) {
  const raw = file && file.fileType ? String(file.fileType).toLowerCase() : ''
  if (raw === 'image' || raw === 'img' || raw === 'picture') return 'image'
  if (raw === 'word' || raw === 'doc' || raw === 'document') return 'word'
  const name = feedbackFileName(file)
  if (/\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(name)) return 'image'
  if (/\.(doc|docx)$/i.test(name)) return 'word'
  return 'other'
}

/** 是否图片附件 */
export function isImageFile(file) {
  return feedbackFileType(file) === 'image'
}

/** 是否 Word 附件 */
export function isWordFile(file) {
  return feedbackFileType(file) === 'word'
}

/** 字节数 → 可读大小 */
export function formatFileSize(size) {
  const value = Number(size) || 0
  if (value <= 0) return '0 B'
  if (value < 1024) return value + ' B'
  if (value < 1024 * 1024) return (value / 1024).toFixed(1) + ' KB'
  return (value / 1024 / 1024).toFixed(1) + ' MB'
}

/** 附件汇总：图片数 / Word 数 / 总数（列表列展示用） */
export function feedbackAttachmentSummary(vo) {
  const files = feedbackFilesOf(vo)
  if (files.length) {
    let images = 0
    let words = 0
    files.forEach(file => {
      const kind = feedbackFileType(file)
      if (kind === 'image') images++
      else if (kind === 'word') words++
    })
    return { images, words, total: files.length }
  }
  const total = Number(
    vo && (vo.attachmentCount !== undefined ? vo.attachmentCount : vo.fileCount)
  )
  const safeTotal = Number.isFinite(total) ? total : 0
  return { images: 0, words: 0, total: safeTotal }
}
