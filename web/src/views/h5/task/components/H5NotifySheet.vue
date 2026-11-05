<template>
  <div v-if="visible" class="ht-sheet-mask" @click.self="close">
    <section class="ht-sheet-panel ht-sheet-panel--tall">
      <header class="ht-sheet-head">
        <span class="ht-sheet-head__title">
          站内信
          <span v-if="unread > 0" class="ht-sheet-head__unread">未读 {{ unread }} 条</span>
        </span>
        <button type="button" class="ht-sheet-head__act" :disabled="!unread || marking" @click="markAll">
          {{ marking ? '处理中…' : '全部已读' }}
        </button>
        <button type="button" class="ht-sheet-head__close" aria-label="关闭" @click="close">×</button>
      </header>

      <div class="ht-sheet-body">
        <!-- 首屏骨架 -->
        <div v-if="loading && !items.length" class="notify-skel">
          <div v-for="n in 4" :key="'ns-' + n" class="notify-skel__row">
            <div class="ht-skel-line ht-skel-line--short"></div>
            <div class="ht-skel-line"></div>
          </div>
        </div>

        <!-- 错误态 -->
        <div v-else-if="error" class="ht-empty ht-empty--small">
          <div class="ht-empty__title">消息加载失败</div>
          <div class="ht-empty__sub">请检查网络后重试</div>
          <button type="button" class="ht-btn ht-btn--primary ht-btn--sm ht-empty__action" @click="load(true)">重新加载</button>
        </div>

        <!-- 空态 -->
        <div v-else-if="!items.length" class="ht-empty ht-empty--small">
          <div class="ht-empty__ico">·</div>
          <div class="ht-empty__title">暂无新消息</div>
          <div class="ht-empty__sub">任务派发、验收结果与截止提醒会出现在这里</div>
        </div>

        <!-- 列表 -->
        <template v-else>
          <button
            v-for="item in items"
            :key="item.notifyId"
            type="button"
            class="ht-notify-item"
            :class="{ 'is-unread': !isRead(item) }"
            @click="onItemClick(item)"
          >
            <span class="ht-notify-item__ico" :class="typeIconClass(item.type)">{{ typeIconText(item.type) }}</span>
            <span class="ht-notify-item__main">
              <span class="ht-notify-item__title">{{ item.title || '站内信' }}</span>
              <span class="ht-notify-item__content">{{ item.content || '' }}</span>
              <span class="ht-notify-item__time">
                {{ item.typeLabel || notifyTypeMeta(item.type).label }} · {{ notifyTime(item.createTime) }}
              </span>
            </span>
            <i v-if="!isRead(item)" class="ht-notify-item__dot"></i>
          </button>

          <div v-if="items.length < total" class="ht-notify-more">
            <button type="button" class="ht-btn ht-btn--ghost-2 ht-btn--sm" :disabled="loading" @click="load(false)">
              {{ loading ? '加载中…' : '加载更多' }}
            </button>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { listNotify, markNotifyRead } from '@/api/oa/notify'
import { notifyTypeMeta, toDate } from '@/utils/oaTask'
import { parseTime } from '@/utils/ruoyi'

/**
 * H5 站内信铃铛 + 半屏弹层（H5-01 顶部 / H5-02 顶部共用）
 *
 * - 未读数：`GET /oa/notify/list?unreadOnly=true` 顶层 unreadCount（与 PC 同一口径）
 * - 列表：`GET /oa/notify/list`；点击任务类消息 → 标记已读并跳 H5-02
 * - 全部已读：`PUT /oa/notify/read { all: true }`
 */
defineOptions({ name: 'H5NotifySheet' })

const emit = defineEmits(['unread'])

const router = useRouter()
const PAGE_SIZE = 20

const visible = ref(false)
const items = ref([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)
const error = ref('')
const unread = ref(0)
const marking = ref(false)

onMounted(() => {
  refreshUnread()
})

defineExpose({ open, close, refreshUnread })

function open() {
  visible.value = true
  load(true)
}

function close() {
  visible.value = false
}

/* -------------------------------------------------------------------------- */
/* 未读数                                                                      */
/* -------------------------------------------------------------------------- */
async function refreshUnread() {
  try {
    const res = await listNotify({ pageNum: 1, pageSize: 1, unreadOnly: true })
    setUnread(Number(res && res.unreadCount) || 0)
  } catch (e) {
    // 未读数失败静默降级，不打扰用户
  }
}

function setUnread(value) {
  unread.value = Math.max(0, Number(value) || 0)
  emit('unread', unread.value)
}

/* -------------------------------------------------------------------------- */
/* 列表                                                                        */
/* -------------------------------------------------------------------------- */
async function load(reset) {
  if (reset) {
    pageNum.value = 1
    error.value = ''
    loading.value = true
  } else {
    if (items.value.length >= total.value) return
    pageNum.value += 1
    loading.value = true
  }
  try {
    const res = await listNotify({ pageNum: pageNum.value, pageSize: PAGE_SIZE, unreadOnly: false })
    const rows = Array.isArray(res && res.rows) ? res.rows : []
    items.value = reset ? rows : items.value.concat(rows)
    total.value = Number(res && res.total) || items.value.length
    setUnread(Number(res && res.unreadCount) || 0)
  } catch (e) {
    if (reset) {
      items.value = []
      error.value = '加载失败'
    }
    if (!reset) pageNum.value = Math.max(1, pageNum.value - 1)
  } finally {
    loading.value = false
  }
}

function markAll() {
  if (!unread.value || marking.value) return
  marking.value = true
  markNotifyRead({ all: true })
    .then(res => {
      const count = res && res.data && res.data.unreadCount !== undefined ? res.data.unreadCount : 0
      setUnread(count)
      items.value = items.value.map(item => ({ ...item, readFlag: '1' }))
      ElMessage.success('已全部标记为已读')
    })
    .catch(() => {})
    .finally(() => {
      marking.value = false
    })
}

function onItemClick(item) {
  if (!item) return
  if (!isRead(item)) markRead(item)
  if (String(item.bizType) === 'task' && item.bizId) {
    close()
    router.push(`/h5/task/detail/${item.bizId}`)
  }
}

function markRead(item) {
  markNotifyRead({ ids: [item.notifyId] })
    .then(res => {
      const count = res && res.data && res.data.unreadCount !== undefined ? res.data.unreadCount : Math.max(0, unread.value - 1)
      setUnread(count)
      item.readFlag = '1'
    })
    .catch(() => {})
}

/* -------------------------------------------------------------------------- */
/* 展示辅助                                                                    */
/* -------------------------------------------------------------------------- */
function isRead(item) {
  return !!item && (String(item.readFlag) === '1' || !!item.readTime)
}

const TYPE_ICON_TEXT = { 1: '派', 2: '周', 3: '截', 4: '验', 5: '通' }

function typeIconText(type) {
  return TYPE_ICON_TEXT[String(type)] || '信'
}

function typeIconClass(type) {
  const key = String(type)
  return ['1', '2', '3', '4', '5'].includes(key) ? `is-type-${key}` : ''
}

function notifyTime(value) {
  if (!value) return '—'
  const date = toDate(value)
  if (!date) return String(value)
  const now = new Date()
  const sameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate()
  if (sameDay) return parseTime(date, '{h}:{i}') || ''
  return parseTime(date, '{m}-{d} {h}:{i}') || ''
}
</script>

<style lang="scss" scoped>
.notify-skel__row {
  padding: 10px 0;
  border-bottom: 1px solid #f2f4f7;
}

.notify-skel__row .ht-skel-line {
  margin-bottom: 8px;
}
</style>
