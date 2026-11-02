<template>
  <span class="oa-pill" :class="'oa-pill--' + meta.tone">{{ meta.label }}</span>
</template>

<script setup name="OaStatusTag">
import { computed } from 'vue'
import { LEAVE_STATUS_MAP } from '@/utils/oa'

const props = defineProps({
  // 请假单状态值：0 草稿 1 审批中 2 已通过 3 已驳回 4 已撤销
  status: { type: [String, Number], default: '' },
  // 需要展示其他状态集时传入自定义映射 { value: { label, tone } }
  map: { type: Object, default: () => LEAVE_STATUS_MAP }
})

const meta = computed(() => {
  const key = String(props.status)
  return props.map[key] || { label: props.status === '' || props.status === null ? '—' : String(props.status), tone: 'gray' }
})
</script>
