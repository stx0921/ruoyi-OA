<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑部门' : '新增部门'"
    width="440px"
    append-to-body
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="部门名称" prop="deptName">
        <el-input v-model="form.deptName" maxlength="30" show-word-limit placeholder="如：组织部" />
      </el-form-item>
      <el-form-item label="显示排序" prop="orderNum">
        <el-input-number v-model="form.orderNum" :min="0" :max="999" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 140px">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-alert
        v-if="!isEdit"
        type="info"
        :closable="false"
        show-icon
        title="新部门会创建在当前组织架构根部门下，可随时在这里改名、排序或删除。"
      />
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="DeptFormDialog">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import { addDept, updateDept } from '@/api/oa/dept'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  // 传入部门对象 = 编辑；null = 新增
  dept: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'saved'])

const { proxy } = getCurrentInstance()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({ deptId: null, deptName: '', orderNum: 0, status: '0' })
const isEdit = computed(() => !!form.deptId)
const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  value => {
    if (!value) return
    const dept = props.dept
    form.deptId = dept ? dept.deptId : null
    form.deptName = dept ? dept.deptName || '' : ''
    form.orderNum = dept && dept.orderNum !== null && dept.orderNum !== undefined ? Number(dept.orderNum) : 0
    form.status = dept && dept.status !== null && dept.status !== undefined ? String(dept.status) : '0'
    proxy.$nextTick(() => formRef.value && formRef.value.clearValidate())
  }
)

function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(valid => {
    if (!valid) return
    submitting.value = true
    const payload = {
      deptId: form.deptId,
      deptName: (form.deptName || '').trim(),
      orderNum: form.orderNum,
      status: form.status
    }
    const request = form.deptId ? updateDept(payload) : addDept(payload)
    request
      .then(() => {
        proxy.$modal.msgSuccess(form.deptId ? '修改成功' : '新增成功')
        visible.value = false
        emit('saved')
      })
      .catch(() => {})
      .finally(() => {
        submitting.value = false
      })
  })
}
</script>
