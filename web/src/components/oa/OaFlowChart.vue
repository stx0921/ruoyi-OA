<template>
  <div class="oa-flow">
    <div class="oa-flow__track">
      <div class="oa-flow__node is-start">
        <div class="oa-flow__name">开始</div>
      </div>

      <template v-for="(node, index) in trace.nodes" :key="node.key">
        <span class="oa-flow__arrow" :class="{ 'is-done': arrowDone(index) }"></span>
        <div class="oa-flow__node" :class="nodeClass(node)">
          <div class="oa-flow__name">{{ node.name }}</div>
          <div class="oa-flow__role">{{ nodeCaption(node) }}</div>
        </div>
      </template>

      <span class="oa-flow__arrow" :class="{ 'is-done': trace.endState === 'done' }"></span>
      <div class="oa-flow__node" :class="trace.endState === 'done' ? 'is-done' : 'is-end'">
        <div class="oa-flow__name">结束</div>
      </div>
    </div>

    <div class="oa-flow__legend">
      <span><i class="is-done"></i>已完成</span>
      <span><i class="is-current"></i>进行中</span>
      <span><i class="is-wait"></i>未开始</span>
      <span v-if="hasRejected"><i class="is-rejected"></i>已被退回</span>
    </div>
  </div>
</template>

<script setup name="OaFlowChart">
import { computed } from 'vue'
import { oaFlowTrace, oaUserName } from '@/utils/oa'

const props = defineProps({
  // 请假单（用于 duration 路由与状态判断）
  leave: { type: Object, default: null },
  // 审批记录（/oa/leave/trace 返回的 records）
  records: { type: Array, default: () => [] },
  // userId → { nickName }
  users: { type: Object, default: () => ({}) }
})

const trace = computed(() => oaFlowTrace(props.leave, props.records))
const hasRejected = computed(() => trace.value.nodes.some(node => node.state === 'rejected'))

function arrowDone(index) {
  const state = trace.value.nodes[index].state
  return state === 'done' || state === 'current'
}

function nodeClass(node) {
  return {
    'is-done': node.state === 'done',
    'is-current': node.state === 'current',
    'is-wait': node.state === 'wait',
    'is-rejected': node.state === 'rejected'
  }
}

function nodeCaption(node) {
  if (node.state === 'current') {
    const who = oaUserName(props.users, node.assignee, props.leave)
    return who === '—' ? '处理中' : who + ' · 处理中'
  }
  if (node.state === 'done') {
    const who = oaUserName(props.users, node.assignee, props.leave)
    return who === '—' ? '已完成' : who
  }
  if (node.state === 'rejected') return '已退回'
  return node.role
}
</script>

<style scoped>
.oa-flow {
  overflow-x: auto;
  padding: 6px 2px 0;
}

.oa-flow__track {
  display: flex;
  align-items: center;
  min-width: max-content;
  padding: 10px 0 16px;
}

.oa-flow__node {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 104px;
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  text-align: center;
}

.oa-flow__name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.oa-flow__role {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
}

.oa-flow__node.is-start,
.oa-flow__node.is-end {
  min-width: 62px;
  padding: 8px 12px;
  border-radius: 18px;
  background: #f5f7fa;
  border-color: #e4e7ed;
}

.oa-flow__node.is-start .oa-flow__name,
.oa-flow__node.is-end .oa-flow__name {
  font-weight: 500;
  color: #909399;
}

.oa-flow__node.is-done {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.oa-flow__node.is-done .oa-flow__name {
  color: #529b2e;
}

.oa-flow__node.is-current {
  border-color: #236de9;
  background: #edf3fe;
  box-shadow: 0 0 0 3px rgba(35, 109, 233, 0.12);
}

.oa-flow__node.is-current .oa-flow__name {
  color: #1d61d5;
}

.oa-flow__node.is-current .oa-flow__role {
  color: #236de9;
}

.oa-flow__node.is-wait {
  border-color: #e4e7ed;
  background: #fafafa;
}

.oa-flow__node.is-wait .oa-flow__name {
  color: #909399;
}

.oa-flow__node.is-rejected {
  border-style: dashed;
  border-color: #f3d19e;
  background: #fdf6ec;
}

.oa-flow__node.is-rejected .oa-flow__name {
  color: #b88230;
}

.oa-flow__arrow {
  position: relative;
  width: 34px;
  height: 2px;
  flex: 0 0 34px;
  background: #dcdfe6;
}

.oa-flow__arrow::after {
  content: '';
  position: absolute;
  top: -3px;
  right: 0;
  border-left: 6px solid #dcdfe6;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
}

.oa-flow__arrow.is-done {
  background: #95d475;
}

.oa-flow__arrow.is-done::after {
  border-left-color: #95d475;
}

.oa-flow__legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.oa-flow__legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.oa-flow__legend i {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
}

.oa-flow__legend i.is-done {
  background: #95d475;
}

.oa-flow__legend i.is-current {
  background: #236de9;
}

.oa-flow__legend i.is-wait {
  background: #dcdfe6;
}

.oa-flow__legend i.is-rejected {
  background: #f3d19e;
}
</style>
