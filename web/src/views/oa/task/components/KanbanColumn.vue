<template>
  <div
    class="kb-col"
    :class="{
      'is-drag-over': dragOver && canDrop,
      'is-invalid': dragOver && !!dragging && !canDrop
    }"
    @dragover.prevent="onDragOver"
    @dragleave="onDragLeave"
    @drop.prevent="onDrop"
  >
    <div class="kb-col-head">
      <span class="dot" :style="{ background: color }" />
      <span class="kb-col-name">{{ label }}</span>
      <span class="cnt">{{ tasks.length }}</span>
    </div>

    <div class="kb-col-body">
      <template v-if="loading">
        <div v-for="index in 2" :key="'skeleton-' + index" class="kb-skeleton">
          <el-skeleton :rows="2" animated />
        </div>
      </template>
      <template v-else>
        <TaskCard
          v-for="task in tasks"
          :key="task.taskId"
          :task="task"
          variant="board"
          :dragging="!!dragging && dragging.taskId === task.taskId"
          @open="$emit('open', $event)"
          @start="$emit('start', $event)"
          @submit="$emit('submit', $event)"
          @move="$emit('move', $event)"
          @move-invalid="$emit('move-invalid', $event)"
          @delete="$emit('delete', $event)"
          @dragstart="$emit('dragstart', $event)"
          @dragend="$emit('dragend')"
        />
        <div v-if="!tasks.length" class="kb-empty">
          <el-icon :size="22"><Box /></el-icon>
          <span>暂无任务</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup name="KanbanColumn">
import TaskCard from './TaskCard.vue'
import { boardDropCheck } from '@/utils/oaTask'

const props = defineProps({
  status: { type: String, required: true },
  label: { type: String, required: true },
  color: { type: String, default: '#909399' },
  tasks: { type: Array, default: () => [] },
  // 正在拖拽的任务（null 表示无拖拽）
  dragging: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['drop', 'drop-invalid', 'open', 'start', 'submit', 'delete', 'move', 'move-invalid', 'dragstart', 'dragend'])

const dragOver = ref(false)

const canDrop = computed(() => {
  if (!props.dragging) return false
  return boardDropCheck(props.dragging.status, props.status, props.dragging).ok
})

function onDragOver(event) {
  if (!props.dragging) return
  dragOver.value = true
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = canDrop.value ? 'move' : 'none'
  }
}

function onDragLeave(event) {
  if (!event.relatedTarget || !event.currentTarget.contains(event.relatedTarget)) {
    dragOver.value = false
  }
}

function onDrop() {
  dragOver.value = false
  if (!props.dragging) return
  if (canDrop.value) {
    emit('drop', props.status)
  } else {
    emit('drop-invalid', {
      reason: boardDropCheck(props.dragging.status, props.status, props.dragging).reason
    })
  }
}
</script>

<style scoped lang="scss">
.kb-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 280px;
  min-height: 460px;
  padding: 10px;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: background 0.15s, border-color 0.15s, box-shadow 0.15s;

  &.is-drag-over {
    background: #edf3fe;
    border: 2px dashed #236de9;
    box-shadow: inset 0 0 0 2px rgba(35, 109, 233, 0.18);
    padding: 9px;
  }

  &.is-invalid {
    border-color: #fbc4c4;
    cursor: not-allowed;
  }
}

.kb-col-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 4px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex: 0 0 8px;
  }

  .cnt {
    margin-left: auto;
    height: 20px;
    padding: 0 7px;
    border: 1px solid #ebeef5;
    border-radius: 9px;
    background: #fff;
    color: #909399;
    font-size: 12px;
    font-weight: 400;
    line-height: 18px;
  }
}

.kb-col-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-height: 60px;
}

.kb-skeleton {
  padding: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 36px 0;
  color: #c0c4cc;
  font-size: 12px;
}
</style>
