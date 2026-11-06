<template>
  <div class="oa-timeline">
    <div v-if="!ordered.length" class="oa-empty">暂无审批记录</div>
    <div
      v-for="(item, index) in ordered"
      :key="item.taskId || index"
      class="oa-timeline__item"
      :class="item.finished ? 'is-done' : 'is-current'"
    >
      <div class="oa-timeline__axis">
        <span class="oa-timeline__dot"></span>
        <span v-if="index < ordered.length - 1" class="oa-timeline__line"></span>
      </div>
      <div class="oa-timeline__body">
        <div class="oa-timeline__title">
          <span>{{ item.taskName || '审批节点' }}</span>
          <span class="oa-pill" :class="item.finished ? 'oa-pill--green' : 'oa-pill--orange'">
            {{ item.finished ? '已完成' : '处理中' }}
          </span>
        </div>
        <div class="oa-timeline__meta">
          <span>{{ oaUserName(users, item.assignee, leave) }}</span>
          <template v-if="item.startTime">
            <span> · 接单 {{ oaDateTime(item.startTime) }}</span>
          </template>
          <template v-if="!item.finished && item.startTime">
            <span> · 已等待 {{ oaWaitText(item.startTime) }}</span>
          </template>
          <template v-if="item.finished && item.endTime">
            <span> · 完成于 {{ oaDateTime(item.endTime) }}</span>
          </template>
        </div>
        <div v-if="item.comment" class="oa-timeline__comment">
          <el-icon><ChatDotSquare /></el-icon>
          <span>{{ item.comment }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="OaTimeline">
import { computed } from 'vue'
import { oaDateTime, oaTimestamp, oaUserName, oaWaitText } from '@/utils/oa'

const props = defineProps({
  // 后端 /oa/leave/trace/{id} 返回的 records
  records: { type: Array, default: () => [] },
  // userId → { nickName } 映射，用于把 assignee 显示成人名
  users: { type: Object, default: () => ({}) },
  // 业务单，用于 assignee 等于申请人时的兜底显示
  leave: { type: Object, default: null },
  // desc：最新在前（默认，符合原型）；asc：按时间正序
  order: { type: String, default: 'desc' }
})

const ordered = computed(() => {
  const list = [...(props.records || [])]
  list.sort((a, b) => {
    const diff = oaTimestamp(a.startTime) - oaTimestamp(b.startTime)
    return props.order === 'asc' ? diff : -diff
  })
  return list
})
</script>

<style scoped>
.oa-timeline {
  font-size: 13px;
}

.oa-timeline__item {
  display: flex;
  gap: 12px;
}

.oa-timeline__axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 0 0 14px;
  padding-top: 4px;
}

.oa-timeline__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--ink-faint);
  flex: 0 0 10px;
}

.oa-timeline__line {
  flex: 1;
  width: 2px;
  min-height: 26px;
  margin: 4px 0 0;
  background: var(--line-strong);
}

.oa-timeline__item.is-done .oa-timeline__dot {
  background: var(--lime);
}

.oa-timeline__item.is-current .oa-timeline__dot {
  background: var(--warn);
  box-shadow: 0 0 0 4px var(--warn-soft);
}

.oa-timeline__body {
  flex: 1;
  min-width: 0;
  padding-bottom: 18px;
}

.oa-timeline__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--ink);
}

.oa-timeline__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-soft);
  line-height: 1.7;
}

.oa-timeline__comment {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 4px;
  background: var(--hover-bg);
  color: var(--ink-muted);
  line-height: 1.6;
  word-break: break-all;
}
</style>
