<template>
  <div class="oa-stat">
    <div class="oa-stat__ico" :class="'is-' + tone">
      <el-icon :size="20"><component :is="icon" /></el-icon>
    </div>
    <div class="oa-stat__body">
      <div class="oa-stat__num">
        {{ displayValue }}<small v-if="suffix">{{ suffix }}</small>
      </div>
      <div class="oa-stat__label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup name="OaStatCard">
import { computed } from 'vue'

const props = defineProps({
  // 数值（数字或已格式化字符串）
  value: { type: [String, Number], default: 0 },
  label: { type: String, required: true },
  // 单位后缀，例如 "天"
  suffix: { type: String, default: '' },
  // 色调：blue / green / orange / red
  tone: { type: String, default: 'blue' },
  // Element Plus 图标组件名，例如 Tickets / CircleCheck
  icon: { type: String, default: 'Tickets' }
})

const displayValue = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return 0
  return props.value
})
</script>

<style scoped>
.oa-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.oa-stat__ico {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  flex: 0 0 44px;
}

.oa-stat__ico.is-blue {
  background: #ecf5ff;
  color: #409eff;
}

.oa-stat__ico.is-green {
  background: #f0f9eb;
  color: #67c23a;
}

.oa-stat__ico.is-orange {
  background: #fdf6ec;
  color: #e6a23c;
}

.oa-stat__ico.is-red {
  background: #fef0f0;
  color: #f56c6c;
}

.oa-stat__body {
  min-width: 0;
}

.oa-stat__num {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #303133;
}

.oa-stat__num small {
  margin-left: 3px;
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.oa-stat__label {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
</style>
