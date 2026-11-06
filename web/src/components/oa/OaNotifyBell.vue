<template>
  <div class="right-menu-item hover-effect oa-bell-wrap">
    <el-popover
      placement="bottom-end"
      :width="360"
      trigger="click"
      popper-class="oa-notify-popover"
      v-model:visible="visible"
      @show="loadList"
    >
      <template #reference>
        <div class="oa-bell">
          <svg-icon icon-class="bell" />
          <span v-if="unreadCount > 0" class="oa-bell__badge">{{ badgeText }}</span>
        </div>
      </template>

      <div class="oa-notify">
        <div class="oa-notify__head">
          <span class="oa-notify__title">
            任务消息
            <small v-if="unreadCount > 0">未读 {{ unreadCount }} 条</small>
          </span>
          <el-button link type="primary" size="small" :disabled="unreadCount === 0" @click="handleAllRead">
            全部已读
          </el-button>
        </div>

        <div v-loading="loading" class="oa-notify__list">
          <div v-if="!loading && list.length === 0" class="oa-notify__empty">暂无新消息</div>
          <div
            v-for="item in list"
            :key="item.notifyId"
            class="oa-notify__item"
            :class="{ 'is-unread': isUnread(item) }"
            @click="handleClick(item)"
          >
            <div class="oa-notify__ico" :class="'is-' + typeMeta(item.type).tone">
              <el-icon :size="16"><component :is="typeMeta(item.type).icon" /></el-icon>
            </div>
            <div class="oa-notify__main">
              <div class="oa-notify__item-title">{{ item.title }}</div>
              <div class="oa-notify__content">{{ item.content || '—' }}</div>
              <div class="oa-notify__time">{{ formatTime(item.createTime) }}</div>
            </div>
            <span v-if="isUnread(item)" class="oa-notify__dot" />
          </div>
        </div>

        <div class="oa-notify__foot">
          <span class="oa-notify__foot-tip">仅展示最近 10 条</span>
          <el-button link type="primary" size="small" @click="goAll">查看全部</el-button>
        </div>
      </div>
    </el-popover>
  </div>
</template>

<script setup name="OaNotifyBell">
import { listNotify, markNotifyRead } from '@/api/oa/notify'
import { notifyTypeMeta } from '@/utils/oaTask'
import { oaTimeAgo } from '@/utils/oa'

const router = useRouter()

const visible = ref(false)
const loading = ref(false)
const list = ref([])
const unreadCount = ref(0)
const pollTimer = ref(null)

const badgeText = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))

function typeMeta(type) {
  return notifyTypeMeta(type)
}

function isUnread(item) {
  return String(item.readFlag) === '0'
}

function formatTime(value) {
  if (!value) return ''
  const text = oaTimeAgo(value)
  return text === '刚刚' ? text : text
}

/** 拉取最近 10 条 + 未读数（未读数以接口 unreadCount 为准） */
function loadList() {
  loading.value = true
  listNotify({ pageNum: 1, pageSize: 10 })
    .then(response => {
      list.value = response.rows || []
      unreadCount.value = Number(response.unreadCount) || 0
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false
    })
}

/** 轮询未读数（30s，页面不可见时暂停） */
function loadUnread() {
  if (document.hidden) return
  listNotify({ pageNum: 1, pageSize: 1, unreadOnly: true })
    .then(response => {
      unreadCount.value = Number(response.unreadCount) || 0
    })
    .catch(() => {})
}

function startPolling() {
  stopPolling()
  pollTimer.value = setInterval(loadUnread, 30000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

function handleVisibilityChange() {
  if (document.hidden) {
    stopPolling()
  } else {
    loadUnread()
    startPolling()
  }
}

function handleClick(item) {
  if (isUnread(item)) {
    markNotifyRead({ ids: [item.notifyId] })
      .then(() => {
        item.readFlag = '1'
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      })
      .catch(() => {})
  }
  visible.value = false
  if (item.bizType === 'task' && item.bizId) {
    router.push({ path: '/task/list', query: { taskId: item.bizId } })
  } else if (item.bizType === 'feedback') {
    router.push({ path: '/feedback/list' })
  }
}

function handleAllRead() {
  markNotifyRead({ all: true })
    .then(() => {
      list.value = list.value.map(item => ({ ...item, readFlag: '1' }))
      unreadCount.value = 0
    })
    .catch(() => {})
}

function goAll() {
  visible.value = false
  router.push('/oa/notify')
}

onMounted(() => {
  loadUnread()
  startPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<style scoped lang="scss">
.oa-bell-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  cursor: pointer;
}

.oa-bell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;

  .svg-icon {
    width: 1.2em;
    height: 1.2em;
    vertical-align: -0.2em;
  }

  &__badge {
    position: absolute;
    top: 8px;
    right: -6px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    border-radius: 10px;
    background: #f56c6c;
    color: #fff;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
    white-space: nowrap;
    pointer-events: none;
  }
}
</style>

<style lang="scss">
/* 下拉面板（popover 挂载在 body，无法使用 scoped） */
.oa-notify-popover {
  padding: 0 !important;

  .oa-notify__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 14px;
    background: #f7f9fb;
    border-bottom: 1px solid #ebeef5;
  }

  .oa-notify__title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;

    small {
      margin-left: 6px;
      font-size: 11px;
      font-weight: 400;
      color: #f56c6c;
    }
  }

  .oa-notify__list {
    max-height: 420px;
    min-height: 80px;
    overflow-y: auto;
  }

  .oa-notify__empty {
    padding: 32px 0;
    text-align: center;
    font-size: 12px;
    color: #909399;
  }

  .oa-notify__item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 11px 14px;
    border-bottom: 1px solid #f5f7fa;
    cursor: pointer;
    transition: background 0.15s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f7f9fb;
    }
  }

  .oa-notify__ico {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    flex: 0 0 30px;
    margin-top: 2px;

    &.is-blue {
      background: #edf3fe;
      color: #236de9;
    }

    &.is-orange {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.is-green {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.is-gray {
      background: #f4f4f5;
      color: #909399;
    }
  }

  .oa-notify__main {
    flex: 1;
    min-width: 0;
  }

  .oa-notify__item-title {
    font-size: 13px;
    color: #303133;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .is-unread .oa-notify__item-title {
    font-weight: 600;
  }

  .oa-notify__content {
    margin-top: 2px;
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .oa-notify__time {
    margin-top: 3px;
    font-size: 11px;
    color: #c0c4cc;
  }

  .oa-notify__dot {
    width: 6px;
    height: 6px;
    margin-top: 8px;
    border-radius: 50%;
    background: #236de9;
    flex: 0 0 6px;
  }

  .oa-notify__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 14px;
    border-top: 1px solid #ebeef5;
    background: #fafafa;
  }

  .oa-notify__foot-tip {
    font-size: 11px;
    color: #c0c4cc;
  }
}
</style>
