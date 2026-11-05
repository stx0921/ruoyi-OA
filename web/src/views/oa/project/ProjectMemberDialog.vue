<template>
  <el-dialog
    v-model="visible"
    title="选择参与成员"
    width="760px"
    append-to-body
    :close-on-click-modal="false"
    @closed="reset"
  >
    <div class="member-dialog">
      <div class="member-search">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名 / 学号 / 手机号"
          clearable
          prefix-icon="Search"
          @input="handleSearch"
          @keyup.enter="handleSearch"
        />
        <span class="oa-muted">可跨部门选择，仅展示在册且已开号成员</span>
      </div>

      <div class="member-body">
        <div v-loading="loading" class="candidate-list">
          <div v-if="!loading && !candidates.length" class="candidate-empty">没有匹配的在册成员</div>
          <div
            v-for="member in candidates"
            :key="member.value"
            class="candidate-item"
            :class="{ 'is-selected': isSelected(member.value) }"
            @click="toggle(member)"
          >
            <el-checkbox :model-value="isSelected(member.value)" @click.stop @change="toggle(member)" />
            <span class="avatar-xs" :style="{ background: avatarColor(member.value) }">{{ nameInitial(member.name) }}</span>
            <span class="candidate-name">{{ member.name }}</span>
            <span class="candidate-dept">{{ member.deptName || '—' }}</span>
          </div>
        </div>

        <div class="selected-panel">
          <div class="selected-title">
            已选 <b>{{ selected.length }}</b> 人
            <el-button link type="primary" size="small" @click="selected = []">清空</el-button>
          </div>
          <div class="selected-list">
            <div v-for="member in selected" :key="member.userId" class="selected-chip">
              <span class="avatar-xs" :style="{ background: avatarColor(member.userId) }">{{ nameInitial(member.userName) }}</span>
              <span class="chip-name">{{ member.userName }}</span>
              <span class="chip-dept">{{ member.deptName || '—' }}</span>
              <el-icon class="chip-close" @click="remove(member.userId)"><Close /></el-icon>
            </div>
            <div v-if="!selected.length" class="oa-muted">尚未选择成员</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" @click="handleConfirm">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="ProjectMemberDialog">
import { avatarColor, nameInitial, searchMemberOptions } from '@/utils/oaTask'

const emit = defineEmits(['confirm'])

const visible = ref(false)
const loading = ref(false)
const keyword = ref('')
const candidates = ref([])
const selected = ref([])
const searchTimer = ref(null)

function open(members) {
  selected.value = (members || []).map(item => ({
    userId: item.userId,
    userName: item.userName || item.nickName || '用户' + item.userId,
    deptName: item.deptName || ''
  }))
  keyword.value = ''
  candidates.value = []
  visible.value = true
  doSearch('')
}

function doSearch(text) {
  loading.value = true
  searchMemberOptions(text).then(options => {
    candidates.value = options
    loading.value = false
  })
}

function handleSearch() {
  if (searchTimer.value) clearTimeout(searchTimer.value)
  searchTimer.value = setTimeout(() => doSearch(keyword.value), 300)
}

function isSelected(userId) {
  return selected.value.some(item => String(item.userId) === String(userId))
}

function toggle(member) {
  const index = selected.value.findIndex(item => String(item.userId) === String(member.value))
  if (index >= 0) {
    selected.value.splice(index, 1)
  } else {
    selected.value.push({ userId: member.value, userName: member.name, deptName: member.deptName || '' })
  }
}

function remove(userId) {
  selected.value = selected.value.filter(item => String(item.userId) !== String(userId))
}

function reset() {
  keyword.value = ''
  candidates.value = []
  selected.value = []
}

function handleConfirm() {
  emit('confirm', selected.value.slice())
  visible.value = false
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.member-dialog {
  .member-search {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
  }

  .member-body {
    display: grid;
    grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
    gap: 14px;
    height: 380px;
  }

  .candidate-list {
    overflow-y: auto;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 6px;
    min-height: 200px;
  }

  .candidate-empty {
    padding: 40px 0;
    text-align: center;
    color: #909399;
    font-size: 12px;
  }

  .candidate-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    border-radius: 4px;
    cursor: pointer;

    &:hover {
      background: #f5f7fa;
    }

    &.is-selected {
      background: #ecf5ff;
    }
  }

  .candidate-name {
    font-size: 13px;
    color: #303133;
  }

  .candidate-dept {
    margin-left: auto;
    font-size: 12px;
    color: #909399;
  }

  .selected-panel {
    display: flex;
    flex-direction: column;
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 10px;
    overflow: hidden;
  }

  .selected-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f2f5;
    font-size: 13px;
    color: #606266;

    b {
      color: #409eff;
    }
  }

  .selected-list {
    flex: 1;
    overflow-y: auto;
    padding-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .selected-chip {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 8px;
    background: #f7f8fa;
    border-radius: 4px;
    font-size: 13px;

    .chip-name {
      color: #303133;
    }

    .chip-dept {
      color: #909399;
      font-size: 12px;
    }

    .chip-close {
      margin-left: auto;
      color: #c0c4cc;
      cursor: pointer;

      &:hover {
        color: #f56c6c;
      }
    }
  }
}

.avatar-xs {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  flex: 0 0 22px;
}
</style>
