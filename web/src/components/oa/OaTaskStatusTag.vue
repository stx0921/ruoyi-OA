<template>
  <span
    class="oa-task-tag"
    :class="[size === 'small' ? 'is-small' : '', meta.strike ? 'is-strike' : '']"
    :style="tagStyle"
  >
    <i v-if="type === 'priority'" class="oa-task-tag__dot" :style="{ background: meta.color }" />
    {{ meta.label }}
  </span>
</template>

<script setup name="OaTaskStatusTag">
import { computed } from 'vue'
import {
  notifyTypeMeta,
  priorityMeta,
  projectStatusMeta,
  recurStatusMeta,
  statusMeta,
  sourceLabel,
  sourceTone
} from '@/utils/oaTask'

const props = defineProps({
  // 状态值：任务状态 / 周期状态 / 项目状态 / 站内信类型，按 type 解释
  status: { type: [String, Number], default: '' },
  // 优先级值：0 高 / 1 中 / 2 低
  priority: { type: [String, Number], default: '' },
  // status 任务状态 | priority 优先级 | source 来源 | recur 周期规则 | project 项目 | notify 站内信
  type: { type: String, default: 'status' },
  // default | small
  size: { type: String, default: 'default' }
})

const meta = computed(() => {
  switch (props.type) {
    case 'priority':
      return priorityMeta(props.priority)
    case 'recur':
      return recurStatusMeta(props.status)
    case 'project':
      return projectStatusMeta(props.status)
    case 'notify':
      return notifyTypeMeta(props.status)
    case 'source':
      return { label: sourceLabel(props.status), tone: sourceTone(props.status) }
    default:
      return statusMeta(props.status)
  }
})

const tagStyle = computed(() => ({
  color: meta.value.color || '#909399',
  background: meta.value.bg || '#F4F4F5',
  borderColor: meta.value.border || '#E4E7ED'
}))
</script>

<style scoped lang="scss">
.oa-task-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;

  &.is-small {
    height: 20px;
    padding: 0 6px;
    font-size: 11px;
  }

  &.is-strike {
    text-decoration: line-through;
  }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 2px;
    flex: 0 0 8px;
  }
}
</style>
