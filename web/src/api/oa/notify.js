import request from '@/utils/request'

/* -------------------------------------------------------------------------- */
/* 站内信（契约 §4 #22–#23，仅需登录）                                          */
/* -------------------------------------------------------------------------- */

// 22. 站内信列表：unreadOnly / pageNum / pageSize
//     响应 NotifyTable：{ code, msg, rows, total, unreadCount }（unreadCount 在顶层）
export function listNotify(query) {
  return request({
    url: '/oa/notify/list',
    method: 'get',
    params: query
  })
}

// 23. 标记已读：{ ids: [] } 或 { all: true }；响应 data：{ unreadCount }
export function markNotifyRead(data) {
  return request({
    url: '/oa/notify/read',
    method: 'put',
    data: data
  })
}

// 便捷方法：全部已读
export function markAllNotifyRead() {
  return markNotifyRead({ all: true })
}

// 便捷方法：未读数（铃铛轮询 / H5 顶部使用）
export function getNotifyUnreadCount() {
  return listNotify({ pageNum: 1, pageSize: 1, unreadOnly: true }).then(response => Number(response.unreadCount) || 0)
}
