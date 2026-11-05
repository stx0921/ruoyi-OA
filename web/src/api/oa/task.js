import request from '@/utils/request'
import { saveAs } from 'file-saver'
import { blobValidate } from '@/utils/ruoyi'

/* -------------------------------------------------------------------------- */
/* 任务（契约 §4 #1–#12）                                                       */
/* 注意：请假模块的 /oa/task/todo、/oa/task/done 已迁到 api/oa/leaveTask.js，   */
/* 本文件只服务新任务模块。                                                     */
/* -------------------------------------------------------------------------- */

// 1. 任务列表：TaskQuery（projectId/assigneeId/creatorId/auditorId/status/priority/tag/
//    keyword/parentId/scope/overdueOnly/pageNum/pageSize + params[beginDue,endDue]）
export function listTask(query) {
  return request({
    url: '/oa/task/list',
    method: 'get',
    params: query
  })
}

// 2. 我的任务：tab=today/week/all/created，响应 TaskMyTable（rows+total+counts）
export function listMyTask(query) {
  return request({
    url: '/oa/task/my',
    method: 'get',
    params: query
  })
}

// 3. 任务看板：projectId / assigneeId，响应 TaskBoardVO（columns + canceledCount）
export function getTaskBoard(query) {
  return request({
    url: '/oa/task/board',
    method: 'get',
    params: query
  })
}

// 4. 任务详情（含 children / collaborators / files / logs / readOnly）
export function getTask(taskId) {
  return request({
    url: '/oa/task/' + taskId,
    method: 'get'
  })
}

// 5. 新建任务（TaskCreateDTO，含 children[] / attachmentFileIds[]）
export function addTask(data) {
  return request({
    url: '/oa/task',
    method: 'post',
    data: data
  })
}

// 6. 编辑任务（TaskUpdateDTO）
export function updateTask(data) {
  return request({
    url: '/oa/task',
    method: 'put',
    data: data
  })
}

// 7. 开始任务：{ taskId }（兼容直接传 taskId）
export function startTask(data) {
  const payload = data && typeof data === 'object' ? data : { taskId: data }
  return request({
    url: '/oa/task/start',
    method: 'put',
    data: payload
  })
}

// 8. 提交完成：{ taskId, remark?, attachmentFileIds[] }
export function submitTask(data) {
  return request({
    url: '/oa/task/submit',
    method: 'put',
    data: data
  })
}

// 9. 验收 / 打回：{ taskId, pass, remark?, finishFileIds[] }
export function auditTask(data) {
  return request({
    url: '/oa/task/audit',
    method: 'put',
    data: data
  })
}

// 10. 取消任务：{ taskId, remark }
export function cancelTask(data) {
  return request({
    url: '/oa/task/cancel',
    method: 'put',
    data: data
  })
}

// 11. 改派：{ taskId, assigneeId, collaboratorIds[], remark? }
export function reassignTask(data) {
  return request({
    url: '/oa/task/reassign',
    method: 'put',
    data: data
  })
}

// 12. 建自有任务（登录即可，主责 = 本人）：{ title, description, dueTime, priority, tags[] }
export function addSelfTask(data) {
  return request({
    url: '/oa/task/self',
    method: 'post',
    data: data
  })
}

// 13. 删除任务（契约 §9，硬删除，不可恢复）：taskIds 支持数组或逗号串
export function deleteTask(taskIds) {
  const ids = Array.isArray(taskIds) ? taskIds.join(',') : taskIds
  return request({
    url: '/oa/task/' + ids,
    method: 'delete'
  })
}

/* -------------------------------------------------------------------------- */
/* 附件（复用人员模块 /oa/file/upload，bizType=task）                           */
/* -------------------------------------------------------------------------- */

/**
 * 上传任务附件
 * @param {File} file
 * @returns {Promise<{code:number,msg:string,data:{fileId:number,fileUrl:string}}>}
 */
export function uploadTaskFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', 'task')
  return request({
    url: '/oa/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: formData,
    timeout: 30000
  })
}

// 附件 blob（鉴权下载，必须带 token）
export function getTaskFileBlob(fileId) {
  return request({
    url: '/oa/file/' + fileId,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 下载任务附件
 * @param {number} fileId
 * @param {string} fileName 展示用文件名
 */
export function downloadTaskFile(fileId, fileName) {
  return getTaskFileBlob(fileId).then(blob => {
    if (!blobValidate(blob)) {
      return Promise.reject(new Error('附件下载失败'))
    }
    saveAs(new Blob([blob]), fileName || '附件-' + fileId)
  })
}
