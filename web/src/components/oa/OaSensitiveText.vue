<template>
  <span
    class="oa-sensitive"
    :class="{ 'is-masked': masked }"
    :title="masked ? '该字段已按权限打码' : ''"
  >
    <span class="oa-sensitive__text">{{ displayValue }}</span>
    <el-icon
      v-if="canCopy"
      class="oa-sensitive__copy"
      title="复制"
      @click.stop="handleCopy"
    >
      <CopyDocument />
    </el-icon>
  </span>
</template>

<script setup name="OaSensitiveText">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 打码文本（02-设计细节 §4.1 / R12）
 *
 * 只渲染后端返回值，前端不做二次打码、不做「点击显示明文」的本地切换：
 * - masked=true：tooltip「该字段已按权限打码」，`user-select:none`，不渲染复制按钮
 * - masked=false：明文展示，copyable=true 时 hover 出现复制按钮
 * - 空值统一显示「—」
 */
const props = defineProps({
  // 后端返回的展示值（可能已打码，可能明文）
  value: { type: [String, Number], default: '' },
  // 是否按权限打码：由后端返回值 / 角色判定后传入
  masked: { type: Boolean, default: false },
  // 是否允许复制（仅在非打码时生效）
  copyable: { type: Boolean, default: false }
})

const displayValue = computed(() => {
  if (props.value === undefined || props.value === null || props.value === '') return '—'
  return String(props.value)
})

const hasValue = computed(() => {
  return !(props.value === undefined || props.value === null || props.value === '')
})

const canCopy = computed(() => props.copyable && !props.masked && hasValue.value)

/** 复制明文；剪贴板不可用时降级为 execCommand */
function handleCopy() {
  const text = String(props.value)
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard
      .writeText(text)
      .then(() => ElMessage.success('已复制'))
      .catch(() => copyByExecCommand(text))
    return
  }
  copyByExecCommand(text)
}

function copyByExecCommand(text) {
  try {
    const input = document.createElement('textarea')
    input.value = text
    input.style.position = 'fixed'
    input.style.opacity = '0'
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选择')
  }
}
</script>

<style scoped>
.oa-sensitive {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  vertical-align: middle;
}

.oa-sensitive__text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 打码值：不可选中复制 */
.oa-sensitive.is-masked {
  cursor: default;
  user-select: none;
  -webkit-user-select: none;
}

.oa-sensitive.is-masked .oa-sensitive__text {
  color: #606266;
  letter-spacing: 0.2px;
  user-select: none;
  -webkit-user-select: none;
}

.oa-sensitive__copy {
  flex: 0 0 auto;
  font-size: 13px;
  color: #c0c4cc;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}

.oa-sensitive:hover .oa-sensitive__copy {
  opacity: 1;
}

.oa-sensitive__copy:hover {
  color: #409eff;
}
</style>
