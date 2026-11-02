<template>
  <el-dialog v-model="visible" title="变更前后对比" width="760px" append-to-body>
    <div class="oa-diff__meta">
      <span>操作人：{{ log.operatorName || '系统' }}</span>
      <span>动作：{{ actionLabel(log.action) }}</span>
      <span>时间：{{ oaDateTime(log.createTime) }}</span>
      <span>IP：{{ log.ip || '—' }}</span>
      <span v-if="objectText">对象：{{ objectText }}</span>
    </div>

    <el-table v-if="diffRows.length" :data="diffRows" max-height="420" class="oa-diff__table">
      <el-table-column label="字段" width="150">
        <template #default="{ row }">
          <span class="oa-strong">{{ row.label }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变更前" width="250">
        <template #default="{ row }">
          <span :class="row.before === '-' || row.before === '（空）' ? 'oa-diff__empty' : 'oa-diff__before'">{{ row.before }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变更后">
        <template #default="{ row }">
          <span :class="row.after === '（空）' ? 'oa-diff__empty' : 'oa-diff__after'">{{ row.after }}</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="该记录无字段变更明细" :image-size="72" />

    <div class="oa-diff__notice">
      数据取自 oa_member_log.before_json / after_json；同一操作内的多字段变更合并为 1 条日志。
    </div>

    <template #footer>
      <el-button @click="visible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="MemberLogDiffDialog">
import { actionLabel, diffMemberLog, oaDateTime } from '@/utils/oaMember'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // MemberLogVO
  log: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

/** beforeJson 是否有实际字段（新增类日志 before 为空，展示「-」） */
const hasBefore = computed(() => {
  const before = props.log.beforeJson
  if (!before) return false
  if (typeof before === 'object') return Object.keys(before).length > 0
  try {
    const data = JSON.parse(String(before))
    return !!data && typeof data === 'object' && Object.keys(data).length > 0
  } catch (e) {
    return false
  }
})

/**
 * 字段级 diff：旧值红色删除线、新值绿色加粗（样式见下方 class）
 * 新增类日志「变更前」统一显示 -
 */
const diffRows = computed(() => {
  const rows = diffMemberLog(props.log.beforeJson, props.log.afterJson)
  return rows.map(row => ({
    ...row,
    before: hasBefore.value ? row.before : '-'
  }))
})

const objectText = computed(() => {
  const log = props.log || {}
  if (log.memberName) {
    return log.studentNo ? `${log.memberName}（${log.studentNo}）` : log.memberName
  }
  if (log.applyNo) return log.applyNo
  if (log.memberId) return '成员 #' + log.memberId
  return ''
})
</script>

<style scoped lang="scss">
.oa-diff__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  padding: 8px 12px;
  margin-bottom: 14px;
  border-radius: 4px;
  background: #f5f7fa;
  font-size: 12px;
  color: #606266;
}

.oa-diff__table {
  :deep(.el-table__header th) {
    background: #fafafa;
    color: #606266;
    font-weight: 600;
  }
}

/* 旧值：红色删除线 */
.oa-diff__before {
  color: #f56c6c;
  text-decoration: line-through;
  word-break: break-all;
}

/* 新值：绿色加粗 */
.oa-diff__after {
  color: #67c23a;
  font-weight: 600;
  word-break: break-all;
}

/* 无值：灰色 */
.oa-diff__empty {
  color: #c0c4cc;
}

.oa-diff__notice {
  margin-top: 14px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #f4f8ff;
  border: 1px solid #d9ecff;
  font-size: 12px;
  line-height: 1.7;
  color: #606266;
}
</style>
