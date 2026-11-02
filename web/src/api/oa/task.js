import request from '@/utils/request'

// 我的待办（Flowable taskAssignee = 当前用户）
export function listTodoTask(query) {
  return request({
    url: '/oa/task/todo',
    method: 'get',
    params: query
  })
}

// 我的已办（历史任务，taskAssignee = 当前用户且已结束）
export function listDoneTask(query) {
  return request({
    url: '/oa/task/done',
    method: 'get',
    params: query
  })
}
