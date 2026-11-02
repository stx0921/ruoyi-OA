<template>
  <div ref="chartRef" class="oa-chart" :style="{ height: height }"></div>
</template>

<script setup name="OaChart">
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 完整 ECharts option
  option: { type: Object, required: true },
  height: { type: String, default: '280px' }
})

const chartRef = ref(null)
let chart = null
let observer = null

function render() {
  if (!chart || !props.option) return
  chart.setOption(props.option, true)
}

function resize() {
  if (chart) chart.resize()
}

onMounted(async () => {
  await nextTick()
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value)
  render()
  window.addEventListener('resize', resize)
  // 侧边栏折叠 / 标签页切换时容器宽度会变化，用 ResizeObserver 兜住
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => resize())
    observer.observe(chartRef.value)
  }
})

watch(
  () => props.option,
  () => render(),
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.oa-chart {
  width: 100%;
  min-width: 0;
}
</style>
