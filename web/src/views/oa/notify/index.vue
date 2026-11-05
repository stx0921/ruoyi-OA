<template>
  <div class="app-container notify-page">
    <div class="oa-page-title">
      站内信
      <small>任务派发、周期提醒、截止提醒与验收结果通知</small>
    </div>

    <div class="oa-card">
      <div class="oa-toolbar">
        <div class="oa-toolbar__left">
          <el-radio-group v-model="unreadOnly" size="small" @change="handleQuery">
            <el-radio-button :value="false">全部</el-radio-button>
            <el-radio-button :value="true">未读</el-radio-button>
          </el-radio-group>
          <span class="oa-muted">未读 {{ unreadCount }} 条</span>
        </div>
        <div class="oa-toolbar__right">
          <el-button size="small" :disabled="unreadCount === 0" @click="handleAllRead">全部已读</el-button>
          <el-button size="small" icon="Refresh" @click="getList">刷新</el-button>
        </div>
      </div>

      <div v-if="loading && !notifyList.length" class="skeleton-list">
        <div v-for="index in 5" :key="index" class="skeleton-row">
          <el-skeleton :rows="1" animated />
        </div>
      </div>

      <div v-else-if="error" class="state-block">
        <el-icon :size="34"><WarningFilled /></el-icon>
        <p>站内信加载失败</p>
        <el-button size="small" type="primary" @click="getList">重新加载</el-button>
      </div>

      <div v-else-if="!notifyList.length" class="state-block">
        <el-icon :size="34"><Message /></el-icon>
        <p>暂无新消息</p>
      </div>

      <template v-else>
        <div class="notify-list">
          <div
            v-for="item in notifyList"
            :key="item.notifyId"
            class="notify-item"
            :class="{ 'is-unread': isUnread(item) }"
            @click="handleClick(item)"
          >
            <div class="notify-ico" :class="'is-' + typeMeta(item.type).tone">
              <el-icon :size="18"><component :is="typeMeta(item.type).icon" /></el-icon>
            </div>
            <div class="notify-main">
              <div class="notify-title-row">
                <span class="notify-title">{{ item.title }}</span>
                <OaTaskStatusTag type="notify" :status="item.type" size="small" />
              </div>
              <div class="notify-content">{{ item.content || '—' }}</div>
            </div>
            <div class="notify-time">
              <div>{{ formatDateTime(item.createTime) }}</div>
              <span v-if="isUnread(item)" class="notify-dot" />
            </div>
          </div>
        </div>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          :page-sizes="[10, 20, 50]"
          @pagination="getList"
        />
      </template>
    </div>
  </div>
</template>

<script setup name="NotifyList">
import OaTaskStatusTag from '@/components/oa/OaTaskStatusTag.vue'
import { listNotify, markNotifyRead } from '@/api/oa/notify'
import { formatDateTime, notifyTypeMeta } from '@/utils/oaTask'

const router = useRouter()

const loading = ref(false)
const error = ref(false)
const notifyList = ref([])
const total = ref(0)
const unreadCount = ref(0)
const unreadOnly = ref(false)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10
})

function typeMeta(type) {
  return notifyTypeMeta(type)
}

function isUnread(item) {
  return String(item.readFlag) === '0'
}

function getList() {
  loading.value = true
  error.value = false
  listNotify({
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize,
    unreadOnly: unreadOnly.value || undefined
  })
    .then(response => {
      notifyList.value = response.rows || []
      total.value = response.total || 0
      unreadCount.value = Number(response.unreadCount) || 0
    })
    .catch(() => {
      error.value = true
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function handleClick(item) {
  if (isUnread(item)) {
    markNotifyRead({ ids: [item.notifyId] })
      .then(response => {
        item.readFlag = '1'
        unreadCount.value = response.data && response.data.unreadCount !== undefined
          ? Number(response.data.unreadCount)
          : Math.max(0, unreadCount.value - 1)
      })
      .catch(() => {})
  }
  if (item.bizType === 'task' && item.bizId) {
    router.push({ path: '/task/list', query: { taskId: item.bizId } })
  }
}

function handleAllRead() {
  markNotifyRead({ all: true })
    .then(response => {
      notifyList.value = notifyList.value.map(item => ({ ...item, readFlag: '1' }))
      unreadCount.value = response.data && response.data.unreadCount !== undefined
        ? Number(response.data.unreadCount)
        : 0
      if (unreadOnly.value) getList()
    })
    .catch(() => {})
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.notify-page {
  .skeleton-list {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 8px 0;
  }

  .skeleton-row {
    padding: 10px 12px;
    border-bottom: 1px solid #f5f7fa;
  }

  .state-block {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    padding: 56px 0;
    color: #909399;
    font-size: 13px;

    p {
      margin: 0;
    }
  }

  .notify-list {
    display: flex;
    flex-direction: column;
  }

  .notify-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 13px 6px;
    border-bottom: 1px solid #f5f7fa;
    cursor: pointer;
    transition: background 0.15s;

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: #f7f9fb;
    }

    &.is-unread .notify-title {
      font-weight: 600;
    }
  }

  .notify-ico {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: 8px;
    flex: 0 0 34px;

    &.is-blue {
      background: #ecf5ff;
      color: #409eff;
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

  .notify-main {
    flex: 1;
    min-width: 0;
  }

  .notify-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .notify-title {
    font-size: 14px;
    color: #303133;
  }

  .notify-content {
    margin-top: 4px;
    color: #909399;
    font-size: 12px;
    line-height: 1.6;
    word-break: break-word;
  }

  .notify-time {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 0 0 auto;
    color: #c0c4cc;
    font-size: 12px;
  }

  .notify-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #409eff;
  }
}
</style>
