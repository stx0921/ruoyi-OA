<template>
  <el-dialog
    v-model="visible"
    title="修改范围"
    width="420px"
    append-to-body
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div class="scope-tip">仅本次只影响最近一次任务；本次及以后会修改规则模板，历史任务不变。</div>

    <div
      class="scope-opt"
      :class="{ on: scope === 'one', disabled: noInstance }"
      @click="!noInstance && (scope = 'one')"
    >
      <span class="rnd" />
      <div class="so-main">
        <div class="so-title">仅本次</div>
        <div class="so-desc">
          只改最近一次已生成实例的标题 / 描述 / 截止时刻 / 优先级 / 标签，规则模板与历史实例不动
        </div>
        <div v-if="noInstance" class="so-disabled-tip">尚未生成任务实例，无法仅改本次</div>
      </div>
    </div>

    <div class="scope-opt" :class="{ on: scope === 'all' }" @click="scope = 'all'">
      <span class="rnd" />
      <div class="so-main">
        <div class="so-title">本次及以后</div>
        <div class="so-desc">修改规则模板，历史任务保持不变</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" :disabled="noInstance && scope === 'one'" @click="handleConfirm">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="RecurScopeDialog">
const emit = defineEmits(['confirm'])

const visible = ref(false)
const scope = ref('all')
const noInstance = ref(false)

/**
 * @param {{noInstance?: boolean, defaultScope?: 'one'|'all'}} options
 *   noInstance=true 时「仅本次」禁用（尚未生成实例）
 */
function open(options = {}) {
  const opts = options || {}
  noInstance.value = !!opts.noInstance
  scope.value = noInstance.value ? 'all' : (opts.defaultScope || 'all')
  visible.value = true
}

function reset() {
  scope.value = 'all'
  noInstance.value = false
}

function handleConfirm() {
  visible.value = false
  emit('confirm', scope.value)
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.scope-tip {
  margin-bottom: 14px;
  padding: 8px 12px;
  border: 1px solid #f5dab1;
  border-radius: 4px;
  background: #fdf6ec;
  color: #b96a1e;
  font-size: 12px;
  line-height: 1.6;
}

.scope-opt {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
  padding: 12px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;

  &.on {
    border-color: #409eff;
    background: #ecf5ff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.12);

    .rnd {
      border-color: #409eff;

      &::after {
        content: '';
        position: absolute;
        inset: 3px;
        border-radius: 50%;
        background: #409eff;
      }
    }
  }

  &.disabled {
    cursor: not-allowed;
    opacity: 0.72;
  }

  .rnd {
    position: relative;
    width: 16px;
    height: 16px;
    margin-top: 2px;
    border: 1px solid #dcdfe6;
    border-radius: 50%;
    background: #fff;
    flex: 0 0 16px;
  }

  .so-main {
    min-width: 0;
  }

  .so-title {
    margin-bottom: 4px;
    color: #303133;
    font-size: 14px;
    font-weight: 600;
  }

  .so-desc {
    color: #909399;
    font-size: 12px;
    line-height: 1.65;
  }

  .so-disabled-tip {
    margin-top: 4px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
