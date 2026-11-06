<template>
  <!-- 我的任务版（88px 单列卡片） -->
  <div
    v-if="variant === 'my'"
    class="oa-task-card-my"
    :class="{ 'is-overdue': overdue }"
    :style="{ '--prio-bar': barColor }"
    @click="emit('open', task)"
  >
    <span class="prio-bar" />
    <div class="card-main">
      <div class="card-title" :title="task.title">{{ task.title }}</div>
      <div class="card-meta">
        <span v-if="task.projectName" class="proj-tag">{{ task.projectName }}</span>
        <OaTaskStatusTag :status="task.status" size="small" />
        <OaTaskStatusTag type="priority" :priority="task.priority" size="small" />
        <span v-if="task.source === '1'" class="tag-chip">周期生成</span>
        <span v-if="overdue" class="badge-overdue">{{ overdueLabel }}</span>
        <span class="due" :class="{ 'is-red': overdue }">{{ dueText }}</span>
        <span v-if="task.assigneeName" class="who">主责：{{ task.assigneeName }}</span>
        <span v-if="roleText" class="role">{{ roleText }}</span>
      </div>
    </div>
    <div class="card-actions" @click.stop>
      <el-button
        v-if="showStart"
        v-hasPermi="['oa:task:edit']"
        type="primary"
        size="small"
        @click="emit('start', task)"
      >开始</el-button>
      <el-button
        v-if="showSubmit"
        v-hasPermi="['oa:task:submit']"
        type="primary"
        plain
        size="small"
        @click="emit('submit', task)"
      >提交完成</el-button>
      <el-button v-if="showAudit" type="primary" size="small" @click="emit('open', task)">去验收</el-button>
      <el-button v-if="!showStart && !showSubmit && !showAudit" size="small" @click="emit('open', task)">详情</el-button>
      <el-dropdown v-if="canRemove" trigger="click" class="card-more" @command="handleMoreCommand" @click.stop>
        <el-button size="small" icon="MoreFilled" circle title="更多" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="detail">详情</el-dropdown-item>
            <el-dropdown-item command="delete" divided style="color: #f56c6c">删除</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>

  <!-- 看板版（约 110px） -->
  <div
    v-else
    class="oa-task-card-board"
    :class="{ 'is-overdue': overdue, 'is-dragging': dragging, 'is-static': !draggable }"
    :style="{ '--prio-bar': barColor }"
    :draggable="draggable"
    :title="!draggable ? dragDisabledTip : ''"
    @dragstart="onDragStart"
    @dragend="onDragEnd"
    @click="emit('open', task)"
  >
    <div class="kb-title" :title="task.title">{{ task.title }}</div>
    <div class="kb-tags">
      <span v-if="task.projectName" class="proj-tag">{{ task.projectName }}</span>
      <OaTaskStatusTag type="priority" :priority="task.priority" size="small" />
      <span v-if="task.source === '1'" class="tag-chip">周期</span>
      <span v-if="overdue" class="badge-overdue">{{ overdueLabel }}</span>
    </div>
    <div class="kb-foot">
      <span class="kb-due" :class="{ 'is-red': overdue }">{{ boardDueText }}</span>
      <span class="kb-who">
        <span class="avatar-xs" :style="{ background: avatarColor(task.assigneeId) }">{{ nameInitial(task.assigneeName) }}</span>
        {{ task.assigneeName || '—' }}
      </span>
      <el-dropdown trigger="click" class="kb-more" @command="onMove" @click.stop>
        <span class="kb-more-btn" title="移动到" @click.stop>⋯</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="column in BOARD_COLUMNS"
              :key="column.status"
              :command="column.status"
              :disabled="!canMoveTo(column.status)"
            >
              <span :title="canMoveTo(column.status) ? '' : moveTip(column.status)">
                移动到 {{ column.label }}
              </span>
            </el-dropdown-item>
            <el-dropdown-item v-if="canRemove" command="__delete__" divided style="color: #f56c6c">
              删除
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup name="TaskCard">
import OaTaskStatusTag from '@/components/oa/OaTaskStatusTag.vue'
import useUserStore from '@/store/modules/user'
import {
  BOARD_COLUMNS,
  BOARD_STATUS_LABEL,
  avatarColor,
  boardDropCheck,
  canDragCard,
  formatDueTime,
  isOverdue,
  nameInitial,
  overdueText,
  priorityBarColor,
  TASK_STATUS
} from '@/utils/oaTask'
import { parseTime } from '@/utils/ruoyi'

const props = defineProps({
  task: { type: Object, required: true },
  // my 我的任务版 | board 看板版
  variant: { type: String, default: 'my' },
  // 看板拖拽中（父级控制透明度）
  dragging: { type: Boolean, default: false }
})

const { proxy } = getCurrentInstance()
const emit = defineEmits(['open', 'start', 'submit', 'delete', 'move', 'move-invalid', 'dragstart', 'dragend'])
const userStore = useUserStore()

/** 是否具备删除任务权限（契约追加需求 §9） */
const canRemove = computed(() => proxy.$auth.hasPermi('oa:task:remove'))

const overdue = computed(() => isOverdue(props.task))
const overdueLabel = computed(() => overdueText(props.task))
const barColor = computed(() => priorityBarColor(props.task.priority))
const dueText = computed(() => {
  if (String(props.task.status) === TASK_STATUS.DONE) {
    return props.task.finishTime ? '✓ ' + parseTime(props.task.finishTime, '{y}-{m}-{d} {h}:{i}') + ' 完成' : '已完成'
  }
  if (String(props.task.status) === TASK_STATUS.CANCELED) return '已取消'
  return formatDueTime(props.task.dueTime) + ' 截止'
})
const boardDueText = computed(() => {
  if (String(props.task.status) === TASK_STATUS.DONE) {
    return props.task.finishTime ? '✓ ' + parseTime(props.task.finishTime, '{m}-{d}') + ' 完成' : '✓ 已完成'
  }
  return formatDueTime(props.task.dueTime) + ' 截止'
})

/** 当前用户是否为主责人（我的任务页快捷按钮只给主责人） */
const isAssignee = computed(() => String(props.task.assigneeId) === String(userStore.id))

const showStart = computed(() => props.variant === 'my' && String(props.task.status) === TASK_STATUS.TODO && isAssignee.value)
const showSubmit = computed(() => props.variant === 'my' && String(props.task.status) === TASK_STATUS.DOING && isAssignee.value)
const showAudit = computed(() => props.variant === 'my' && String(props.task.status) === TASK_STATUS.AUDIT && canAuditForMe.value)
// 待验收卡：本人为验收人 / 派发人（具体以后端 canAudit 为准）
const canAuditForMe = computed(() => props.task.canAudit !== false && String(props.task.status) === TASK_STATUS.AUDIT)

/** 我的任务卡上的角色提示 */
const roleText = computed(() => {
  const labels = []
  if (isAssignee.value) labels.push('主责')
  if (String(props.task.auditorId) === String(userStore.id)) labels.push('验收人')
  if (String(props.task.creatorId) === String(userStore.id)) labels.push('派发人')
  return labels.length ? '我：' + labels.join(' / ') : ''
})

/* ---------------------------- 看板拖拽 ---------------------------- */
const draggable = computed(() => props.variant === 'board' && canDragCard(props.task))
const dragDisabledTip = computed(() => {
  if (props.task.readOnly) return '项目已结束，任务只读'
  if (String(props.task.status) === TASK_STATUS.DONE) return '已完成任务不可再流转'
  return '当前账号无权流转该任务'
})

function onDragStart(event) {
  if (!draggable.value) {
    event.preventDefault()
    return
  }
  try {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', JSON.stringify({ taskId: props.task.taskId, fromStatus: String(props.task.status) }))
  } catch (e) {
    // 部分浏览器 / 环境不支持 dataTransfer，父级用 emit 兜底
  }
  emit('dragstart', props.task)
}

function onDragEnd() {
  emit('dragend')
}

function canMoveTo(status) {
  return boardDropCheck(props.task.status, status, props.task).ok
}

function moveTip(status) {
  return boardDropCheck(props.task.status, status, props.task).reason || '不可移动到 ' + BOARD_STATUS_LABEL[String(status)]
}

function onMove(status) {
  if (status === '__delete__') {
    emit('delete', props.task)
    return
  }
  if (!canMoveTo(status)) {
    emit('move-invalid', { task: props.task, toStatus: status, reason: moveTip(status) })
    return
  }
  emit('move', { task: props.task, toStatus: status })
}

/** 我的任务卡「更多」菜单 */
function handleMoreCommand(command) {
  if (command === 'delete') {
    emit('delete', props.task)
    return
  }
  emit('open', props.task)
}
</script>

<style scoped lang="scss">
/* ---------------- 我的任务卡片 ---------------- */
.oa-task-card-my {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-height: 88px;
  padding: 12px 14px 12px 18px;
  margin-bottom: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s;

  &:hover {
    border-color: #b3d8ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  &.is-overdue {
    background: #fef0f0;
    border-color: #fbc4c4;

    .prio-bar {
      background: #f56c6c !important;
    }
  }

  .prio-bar {
    position: absolute;
    left: 0;
    top: 10px;
    bottom: 10px;
    width: 4px;
    border-radius: 2px;
    background: var(--prio-bar, #c0c4cc);
  }

  .card-main {
    flex: 1;
    min-width: 0;
  }

  .card-title {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-break: break-all;
  }

  .card-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-top: 7px;
    font-size: 12px;
    color: #606266;
  }

  .due {
    color: #909399;

    &.is-red {
      color: #f56c6c;
      font-weight: 600;
    }
  }

  .who,
  .role {
    color: #909399;
  }

  .card-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    align-self: center;

    :deep(.el-button) {
      width: 76px;
      height: 32px;
    }
  }
}

/* ---------------- 看板卡片 ---------------- */
.oa-task-card-board {
  position: relative;
  padding: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-left: 3px solid var(--prio-bar, #c0c4cc);
  border-radius: 4px;
  cursor: grab;
  user-select: none;
  transition: box-shadow 0.15s, border-color 0.15s, opacity 0.15s;

  &:hover {
    border-color: #b3d8ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  &.is-static {
    cursor: default;
  }

  &.is-dragging {
    opacity: 0.6;
  }

  &.is-overdue {
    .kb-due {
      color: #f56c6c;
      font-weight: 600;
    }
  }

  .kb-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    line-height: 1.55;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-break: break-all;
  }

  .kb-tags {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    margin-top: 8px;
  }

  .kb-foot {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }

  .kb-who {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    margin-left: auto;
    color: #909399;
  }

  .kb-more {
    flex: 0 0 auto;
  }

  .kb-more-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 4px;
    color: #909399;
    cursor: pointer;

    &:hover {
      background: #f5f7fa;
      color: #236de9;
    }
  }
}

/* 通用小标签（与原型 .proj-tag / .tag-chip 对齐） */
.proj-tag {
  display: inline-block;
  padding: 1px 6px;
  border: 1px solid #dbe7fc;
  border-radius: 3px;
  background: #edf3fe;
  color: #1d61d5;
  font-size: 11px;
  white-space: nowrap;
}

.tag-chip {
  display: inline-block;
  padding: 1px 6px;
  border: 1px solid #e9e9eb;
  border-radius: 3px;
  background: #f4f4f5;
  color: #606266;
  font-size: 11px;
  white-space: nowrap;
}

.badge-overdue {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 6px;
  border: 1px solid #fbc4c4;
  border-radius: 9px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
}

.avatar-xs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 10px;
  flex: 0 0 20px;
}
</style>
