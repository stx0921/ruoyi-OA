<template>
  <span
    class="oa-pill oa-task-tag"
    :class="[pillToneClass(meta.tone), size === 'small' ? 'is-small' : '', meta.strike ? 'is-strike' : '']"
  >
    <i v-if="type === 'priority'" class="oa-task-tag__dot" />
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
import { pillToneClass } from '@/utils/status'
import { useDict } from '@/utils/dict'

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

const { oa_task_status, oa_task_priority, oa_project_status, oa_recur_status, oa_notify_type, oa_task_source } =
  useDict('oa_task_status', 'oa_task_priority', 'oa_project_status', 'oa_recur_status', 'oa_notify_type', 'oa_task_source')

/** 字典文案优先（改字典后标签同步变化），色调仍取前端常量 */
function dictLabel(list, value) {
  if (!Array.isArray(list) || value === null || value === undefined || value === '') {
    return ''
  }
  const hit = list.find(item => String(item.value) === String(value))
  return hit ? hit.label : ''
}

const dictList = computed(() => {
  switch (props.type) {
    case 'priority':
      return oa_task_priority.value
    case 'recur':
      return oa_recur_status.value
    case 'project':
      return oa_project_status.value
    case 'notify':
      return oa_notify_type.value
    case 'source':
      return oa_task_source.value
    default:
      return oa_task_status.value
  }
})

const meta = computed(() => {
  let base
  switch (props.type) {
    case 'priority':
      base = priorityMeta(props.priority)
      break
    case 'recur':
      base = recurStatusMeta(props.status)
      break
    case 'project':
      base = projectStatusMeta(props.status)
      break
    case 'notify':
      base = notifyTypeMeta(props.status)
      break
    case 'source':
      base = { label: sourceLabel(props.status), tone: sourceTone(props.status) }
      break
    default:
      base = statusMeta(props.status)
      break
  }
  const value = props.type === 'priority' ? props.priority : props.status
  const label = dictLabel(dictList.value, value)
  return label ? { ...base, label } : base
})
</script>

<style scoped lang="scss">
/* 与 OaStatusTag 共用 .oa-pill 胶囊基座（999px 圆角 + 玻璃底 + 令牌色文字），
   这里只保留尺寸差异与优先级圆点，不再写内联 hex。 */
.oa-task-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
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
    border-radius: 999px;
    background: currentColor;
    flex: 0 0 8px;
  }
}
</style>
