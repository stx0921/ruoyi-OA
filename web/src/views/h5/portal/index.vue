<template>
  <div class="h5-page">
    <header class="h5-nav">
      <button type="button" class="h5-nav__back" aria-label="返回" @click="handleBack">‹</button>
      <span class="h5-nav__title">{{ navTitle }}</span>
      <button type="button" class="h5-nav__side h5-nav__side--muted" @click="handleLogout">退出</button>
    </header>

    <!-- 顶部个人信息 -->
    <div class="h5-profile-head">
      <div class="h5-ava h5-ava--lg">
        <img v-if="avatarUrl" :src="avatarUrl" alt="头像" @error="avatarError = true" />
        <template v-else>{{ nameInitial }}</template>
      </div>
      <div class="h5-profile-head__main">
        <div class="h5-profile-head__name">{{ selfInfo.name || userStore.nickName || '干事' }}</div>
        <div class="h5-profile-head__sub">
          {{ selfInfo.deptName || '—' }} · {{ selfInfo.positionLabel || '—' }}
        </div>
        <div class="h5-profile-head__sub">
          学号 {{ selfInfo.studentNo || '—' }}<template v-if="selfInfo.memberNo"> · {{ selfInfo.memberNo }}</template>
        </div>
      </div>
    </div>

    <div ref="scrollRef" class="h5-scroll">
      <template v-if="selfLoading && !selfLoaded">
        <div class="h5-skeleton">
          <div class="h5-skel-line h5-skel-line--short"></div>
          <div class="h5-skel-line"></div>
          <div class="h5-skel-line h5-skel-line--wide"></div>
        </div>
      </template>

      <template v-else-if="selfError">
        <div class="h5-empty">
          <div class="h5-empty__title">{{ selfError }}</div>
          <div class="h5-empty__sub">请确认账号已开通成员档案，或联系部长处理</div>
          <button type="button" class="h5-submit h5-empty__action" @click="fetchSelf">重新加载</button>
        </div>
      </template>

      <!-- ================= Tab：通讯录 ================= -->
      <template v-else-if="tab === 'contact'">
        <div class="h5-search">
          <div class="h5-search__box">
            <svg class="h5-search__ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <circle cx="11" cy="11" r="6.5" />
              <path d="M16 16l4.5 4.5" />
            </svg>
            <input
              v-model="keyword"
              class="h5-search__input"
              type="search"
              placeholder="搜索姓名"
              autocomplete="off"
              @input="onKeywordInput"
              @keyup.enter="reloadMembers"
            />
            <button v-if="keyword" type="button" class="h5-search__clear" @click="clearKeyword">×</button>
          </div>
        </div>

        <div class="h5-contact-head">
          <span>{{ selfInfo.deptName || '本部门' }} · {{ total }} 人</span>
          <button type="button" class="h5-contact-head__refresh" @click="reloadMembers">刷新</button>
        </div>
        <p class="h5-contact-tip">干事视角仅可见本部门成员；手机号、邮箱等敏感字段由后端按权限打码，不提供明文复制。</p>

        <div v-if="listLoading && !members.length">
          <div v-for="n in 5" :key="'skel-' + n" class="h5-skeleton h5-skeleton--row">
            <div class="h5-skel-line h5-skel-line--short"></div>
            <div class="h5-skel-line h5-skel-line--wide"></div>
          </div>
        </div>

        <div v-else-if="listError" class="h5-empty">
          <div class="h5-empty__title">{{ listError }}</div>
          <div class="h5-empty__sub">请检查网络后重试</div>
          <button type="button" class="h5-submit h5-empty__action" @click="reloadMembers">重新加载</button>
        </div>

        <template v-else>
          <div v-if="!members.length" class="h5-empty">
            <div class="h5-empty__title">{{ keyword ? '没有找到匹配的成员' : '本部门暂无成员' }}</div>
            <div class="h5-empty__sub">{{ keyword ? '试试换一个姓名关键字' : '新成员入会后会出现在这里' }}</div>
            <button v-if="keyword" type="button" class="h5-submit h5-empty__action" @click="clearKeyword">清空搜索</button>
          </div>

          <div v-else class="h5-contact-list">
            <div
              v-for="member in sortedMembers"
              :key="member.memberId"
              class="h5-contact-item"
              @click="toggleExpand(member.memberId)"
            >
              <div class="h5-ava">{{ (member.name || '?').charAt(0) }}</div>
              <div class="h5-contact-main">
                <div class="h5-contact-name">
                  {{ member.name || '—' }}
                  <span class="h5-contact-pos">{{ member.positionLabel || positionLabel(member.position) }}</span>
                  <span v-if="member.memberId === selfInfo.memberId" class="h5-contact-me">我</span>
                </div>
                <div class="h5-contact-sub">
                  <OaSensitiveText :value="member.phone || ''" :masked="isMasked(member.phone)" />
                </div>
                <div v-if="expandedId === member.memberId" class="h5-contact-detail">
                  <div><span>学号</span>{{ member.studentNo || '—' }}</div>
                  <div><span>年级</span>{{ member.gradeLabel || gradeLabel(member.grade) }}</div>
                  <div><span>入会</span>{{ member.joinDate || '—' }}</div>
                  <div v-if="member.wechat"><span>微信</span><OaSensitiveText :value="member.wechat" :masked="isMasked(member.wechat)" /></div>
                  <div v-if="member.email"><span>邮箱</span><OaSensitiveText :value="member.email" :masked="isMasked(member.email)" /></div>
                </div>
              </div>
              <div class="h5-contact-dept">{{ member.deptName || '' }}</div>
              <span class="h5-contact-caret" :class="{ 'is-open': expandedId === member.memberId }">›</span>
            </div>
          </div>

          <div v-if="members.length && members.length < total" class="h5-list-foot">
            <button type="button" class="h5-submit is-plain is-sm" :disabled="listLoading" @click="loadMore">
              {{ listLoading ? '加载中…' : '加载更多' }}
            </button>
          </div>
          <p class="h5-contact-tip h5-contact-tip--foot">共 {{ total }} 人，按职务（部长 → 副部长 → 干事）排序</p>
        </template>
      </template>

      <!-- ================= Tab：我的资料 ================= -->
      <template v-else>
        <div class="h5-card">
          <div class="h5-sec-title">身份信息<span class="h5-sec-title__tip">只读</span></div>
          <div class="h5-readonly-grid">
            <div class="h5-readonly-grid__item"><span>姓名</span><b>{{ selfInfo.name || '—' }}</b></div>
            <div class="h5-readonly-grid__item"><span>学号</span><b>{{ selfInfo.studentNo || '—' }}</b></div>
            <div class="h5-readonly-grid__item"><span>部门</span><b>{{ selfInfo.deptName || '—' }}</b></div>
            <div class="h5-readonly-grid__item"><span>职务</span><b>{{ selfInfo.positionLabel || '—' }}</b></div>
            <div class="h5-readonly-grid__item"><span>年级</span><b>{{ selfInfo.gradeLabel || gradeLabel(selfInfo.grade) }}</b></div>
            <div class="h5-readonly-grid__item"><span>学院</span><b>{{ selfInfo.college || '—' }}</b></div>
            <div class="h5-readonly-grid__item"><span>专业</span><b>{{ selfInfo.major || '—' }}</b></div>
            <div class="h5-readonly-grid__item"><span>手机号</span><b>{{ selfInfo.phone || '—' }}</b></div>
          </div>
          <div class="h5-hint">姓名、学号、部门、职务由管理员维护；如需变更请联系部长</div>
        </div>

        <div class="h5-card">
          <div class="h5-sec-title">联系方式<span class="h5-sec-title__tip">可编辑</span></div>

          <div class="h5-field">
            <label class="h5-field__label">邮箱</label>
            <input
              v-model="profile.email"
              class="h5-input"
              :class="{ 'is-error': profileErrors.email }"
              type="email"
              maxlength="100"
              placeholder="未填写"
              @blur="validateProfileField('email')"
            />
            <div v-if="profileErrors.email" class="h5-err">{{ profileErrors.email }}</div>
          </div>

          <div class="h5-field">
            <label class="h5-field__label">微信</label>
            <input
              v-model="profile.wechat"
              class="h5-input"
              :class="{ 'is-error': profileErrors.wechat }"
              type="text"
              maxlength="50"
              placeholder="未填写"
              @blur="validateProfileField('wechat')"
            />
            <div v-if="profileErrors.wechat" class="h5-err">{{ profileErrors.wechat }}</div>
          </div>

          <div class="h5-field">
            <label class="h5-field__label">QQ</label>
            <input
              v-model="profile.qq"
              class="h5-input"
              :class="{ 'is-error': profileErrors.qq }"
              type="text"
              inputmode="numeric"
              maxlength="20"
              placeholder="未填写"
              @blur="validateProfileField('qq')"
            />
            <div v-if="profileErrors.qq" class="h5-err">{{ profileErrors.qq }}</div>
          </div>
        </div>

        <div class="h5-card">
          <div class="h5-sec-title">个人介绍<span class="h5-sec-title__tip">选填</span></div>

          <div class="h5-field">
            <label class="h5-field__label">特长<small>{{ profile.skills.length }}/500</small></label>
            <input
              v-model="profile.skills"
              class="h5-input"
              :class="{ 'is-error': profileErrors.skills }"
              type="text"
              maxlength="500"
              placeholder="未填写"
              @blur="validateProfileField('skills')"
            />
            <div v-if="profileErrors.skills" class="h5-err">{{ profileErrors.skills }}</div>
          </div>

          <div class="h5-field">
            <label class="h5-field__label">自我介绍<small>{{ profile.intro.length }}/500</small></label>
            <textarea
              v-model="profile.intro"
              class="h5-textarea"
              :class="{ 'is-error': profileErrors.intro }"
              maxlength="500"
              placeholder="未填写"
              @blur="validateProfileField('intro')"
            ></textarea>
            <div v-if="profileErrors.intro" class="h5-err">{{ profileErrors.intro }}</div>
          </div>
        </div>

        <div class="h5-card">
          <div class="h5-sec-title">照片<span class="h5-sec-title__tip">jpg / png ≤ 2MB</span></div>
          <div class="h5-photo-field">
            <div class="h5-photo" @click="pickPhoto">
              <img v-if="photoPreview" :src="photoPreview" alt="个人照片" />
              <span v-else>{{ photoUploading ? '…' : '＋' }}</span>
            </div>
            <div class="h5-photo__meta">
              <div>{{ profile.photoFileId ? '照片已上传' : '尚未上传照片' }}</div>
              <div class="h5-hint">用于通讯录与成员档案展示；重新上传后点击「保存修改」生效</div>
            </div>
            <input ref="photoInput" class="h5-file-input" type="file" accept="image/jpeg,image/png" @change="onPhotoChange" />
          </div>
        </div>

        <p class="h5-tip-line">保存后写入操作日志；手机号变更需联系部长在 PC 端处理</p>
        <div class="h5-footer-gap"></div>
      </template>
    </div>

    <!-- 保存按钮（我的资料 Tab） -->
    <div v-if="tab === 'profile' && selfLoaded && !selfError" class="h5-submit-bar">
      <button type="button" class="h5-submit" :disabled="saving" @click="saveProfile">
        {{ saving ? '保存中…' : '保存修改' }}
      </button>
    </div>

    <!-- 底部 Tab -->
    <nav class="h5-tabbar">
      <button type="button" class="h5-tab" :class="{ 'is-active': tab === 'contact' }" @click="switchTab('contact')">
        <span class="h5-tab__ico">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="8" r="3.2" />
            <path d="M3 19c0-3 2.7-5 6-5s6 2 6 5" />
            <path d="M16.5 9.5h4M18.5 7.5v4" />
          </svg>
        </span>
        <span>通讯录</span>
      </button>
      <button type="button" class="h5-tab" :class="{ 'is-active': tab === 'profile' }" @click="switchTab('profile')">
        <span class="h5-tab__ico">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="8" r="3.4" />
            <path d="M4.5 19.5c0-3.4 3.2-5.8 7.5-5.8s7.5 2.4 7.5 5.8" />
          </svg>
        </span>
        <span>我的资料</span>
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import { listMember, getSelfMember, updateSelfMember, uploadOaFile, getOaFileBlob } from '@/api/oa/member'
import OaSensitiveText from '@/components/oa/OaSensitiveText.vue'

defineOptions({ name: 'H5Portal' })

const router = useRouter()
const userStore = useUserStore()

const POSITION_ORDER = { '1': 1, '2': 2, '3': 3, '4': 4, '5': 5 }
const POSITION_LABEL = { '1': '会长', '2': '副会长', '3': '部长', '4': '副部长', '5': '干事' }
const GRADE_LABEL = {
  '1': '大一',
  '2': '大二',
  '3': '大三',
  '4': '大四',
  '5': '研一',
  '6': '研二',
  '7': '研三'
}
const PHOTO_MAX = 2 * 1024 * 1024
const PAGE_SIZE = 50

const tab = ref('contact')
const scrollRef = ref(null)
const photoInput = ref(null)

const selfInfo = ref({})
const selfLoading = ref(false)
const selfLoaded = ref(false)
const selfError = ref('')
const avatarError = ref(false)

const profile = reactive({ email: '', wechat: '', qq: '', skills: '', intro: '', photoFileId: null })
const profileErrors = reactive({})
const saving = ref(false)
const photoPreview = ref('')
const photoUploading = ref(false)

const members = ref([])
const total = ref(0)
const pageNum = ref(1)
const keyword = ref('')
const listLoading = ref(false)
const listError = ref('')
const expandedId = ref(null)
let searchTimer = null

const navTitle = computed(() => (tab.value === 'contact' ? '通讯录' : '我的资料'))
const nameInitial = computed(() => {
  const name = selfInfo.value.name || userStore.nickName || '干'
  return String(name).charAt(0)
})
const avatarUrl = computed(() => (!avatarError.value && userStore.avatar) || '')
const sortedMembers = computed(() => {
  return [...members.value].sort((a, b) => {
    const pa = POSITION_ORDER[String(a.position)] || 9
    const pb = POSITION_ORDER[String(b.position)] || 9
    if (pa !== pb) return pa - pb
    return String(a.joinDate || '').localeCompare(String(b.joinDate || ''))
  })
})

onMounted(() => {
  if (!getToken()) {
    router.replace('/login?redirect=%2Fh5%2Fportal')
    return
  }
  // 先取本人档案（用于确定本部门），再拉通讯录
  fetchSelf().then(() => {
    if (!selfError.value) fetchMembers(true)
  })
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
  if (photoPreview.value) URL.revokeObjectURL(photoPreview.value)
})

/* -------------------------------------------------------------------------- */
/* 我的档案                                                                    */
/* -------------------------------------------------------------------------- */
async function fetchSelf() {
  selfLoading.value = true
  selfError.value = ''
  try {
    const res = await getSelfMember()
    const data = (res && res.data !== undefined ? res.data : res) || {}
    if (!data || (!data.memberId && !data.studentNo)) {
      throw new Error('empty self')
    }
    selfInfo.value = data
    profile.email = data.email || ''
    profile.wechat = data.wechat || ''
    profile.qq = data.qq || ''
    profile.skills = data.skills || ''
    profile.intro = data.intro || ''
    profile.photoFileId = data.photoFileId || null
    selfLoaded.value = true
    if (data.photoFileId) loadPhoto(data.photoFileId)
  } catch (e) {
    selfError.value = '档案加载失败，请稍后重试'
  } finally {
    selfLoading.value = false
  }
}

async function loadPhoto(fileId) {
  try {
    const blob = await getOaFileBlob(fileId)
    if (blob instanceof Blob) {
      if (photoPreview.value) URL.revokeObjectURL(photoPreview.value)
      photoPreview.value = URL.createObjectURL(blob)
    }
  } catch (e) {
    // 附件鉴权失败时静默降级为占位
  }
}

/* -------------------------------------------------------------------------- */
/* 通讯录                                                                      */
/* -------------------------------------------------------------------------- */
async function fetchMembers(reset) {
  if (reset) {
    pageNum.value = 1
    listError.value = ''
  }
  listLoading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      status: '0'
    }
    if (keyword.value.trim()) params.name = keyword.value.trim()
    if (selfInfo.value.deptId) params.deptId = selfInfo.value.deptId
    const res = await listMember(params)
    const rows = (res && res.rows) || []
    total.value = Number(res && res.total) || 0
    members.value = reset ? rows : members.value.concat(rows)
    if (reset) expandedId.value = null
  } catch (e) {
    if (reset) members.value = []
    listError.value = '通讯录加载失败'
  } finally {
    listLoading.value = false
  }
}

function reloadMembers() {
  fetchMembers(true)
}

function loadMore() {
  if (listLoading.value || members.value.length >= total.value) return
  pageNum.value += 1
  fetchMembers(false)
}

function onKeywordInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchMembers(true)
  }, 240)
}

function clearKeyword() {
  keyword.value = ''
  fetchMembers(true)
}

function toggleExpand(memberId) {
  expandedId.value = expandedId.value === memberId ? null : memberId
}

/* -------------------------------------------------------------------------- */
/* 我的资料编辑                                                                */
/* -------------------------------------------------------------------------- */
const PROFILE_RULES = {
  email(value) {
    const text = String(value || '').trim()
    if (!text) return ''
    if (!/^[\w.-]+@[\w-]+(\.[\w-]+)+$/.test(text)) return '请输入正确的邮箱地址'
    return ''
  },
  wechat(value) {
    const text = String(value || '').trim()
    if (!text) return ''
    if (!/^[a-zA-Z][-_a-zA-Z0-9]{5,19}$/.test(text)) return '微信号需字母开头，6–20 位字母 / 数字 / 下划线 / 减号'
    return ''
  },
  qq(value) {
    const text = String(value || '').trim()
    if (!text) return ''
    if (!/^\d{5,12}$/.test(text)) return 'QQ 号需为 5–12 位数字'
    return ''
  },
  skills(value) {
    return String(value || '').length > 500 ? '特长最多 500 字' : ''
  },
  intro(value) {
    return String(value || '').length > 500 ? '自我介绍最多 500 字' : ''
  }
}

function validateProfileField(field) {
  if (!PROFILE_RULES[field]) return true
  profileErrors[field] = PROFILE_RULES[field](profile[field])
  return !profileErrors[field]
}

async function saveProfile() {
  if (saving.value) return
  const fields = ['email', 'wechat', 'qq', 'skills', 'intro']
  const valid = fields.reduce((ok, field) => {
    validateProfileField(field)
    return ok && !profileErrors[field]
  }, true)
  if (!valid) {
    ElMessage.warning('请先修正标红的字段')
    return
  }
  saving.value = true
  try {
    await updateSelfMember({
      email: profile.email.trim(),
      wechat: profile.wechat.trim(),
      qq: profile.qq.trim(),
      skills: profile.skills.trim(),
      intro: profile.intro.trim(),
      photoFileId: profile.photoFileId || null
    })
    selfInfo.value = {
      ...selfInfo.value,
      email: profile.email.trim(),
      wechat: profile.wechat.trim(),
      qq: profile.qq.trim(),
      skills: profile.skills.trim(),
      intro: profile.intro.trim(),
      photoFileId: profile.photoFileId || null
    }
    ElMessage.success('已保存')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理，保留输入
  } finally {
    saving.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* 照片                                                                        */
/* -------------------------------------------------------------------------- */
function pickPhoto() {
  if (photoUploading.value) return
  photoInput.value && photoInput.value.click()
}

async function onPhotoChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!/^image\/(jpeg|png)$/i.test(file.type)) {
    ElMessage.error('仅支持 jpg / png，且不超过 2MB')
    return
  }
  if (file.size > PHOTO_MAX) {
    ElMessage.error('仅支持 jpg / png，且不超过 2MB')
    return
  }
  photoUploading.value = true
  try {
    const res = await uploadOaFile(file, 'member_photo')
    const data = (res && res.data !== undefined ? res.data : res) || {}
    if (!data.fileId) throw new Error('upload failed')
    profile.photoFileId = data.fileId
    if (photoPreview.value) URL.revokeObjectURL(photoPreview.value)
    photoPreview.value = URL.createObjectURL(file)
    ElMessage.success('照片已上传，点击「保存修改」后生效')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理
  } finally {
    photoUploading.value = false
  }
}

/* -------------------------------------------------------------------------- */
/* Tab / 退出                                                                  */
/* -------------------------------------------------------------------------- */
function switchTab(next) {
  if (tab.value === next) return
  tab.value = next
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = 0
  })
}

function handleBack() {
  if (tab.value === 'profile') {
    switchTab('contact')
    return
  }
  if (window.history.length > 1) {
    router.back()
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'h5-message-box'
    })
  } catch (e) {
    return
  }
  try {
    await userStore.logOut()
  } catch (e) {
    // 忽略退出接口异常，本地清理由 store 完成
  }
  router.replace('/login')
}

/* -------------------------------------------------------------------------- */
/* 展示辅助                                                                    */
/* -------------------------------------------------------------------------- */
function isMasked(value) {
  return typeof value === 'string' && value.includes('*')
}

function positionLabel(position) {
  if (position === undefined || position === null || position === '') return '—'
  return POSITION_LABEL[String(position)] || String(position)
}

function gradeLabel(grade) {
  if (grade === undefined || grade === null || grade === '') return '—'
  return GRADE_LABEL[String(grade)] || String(grade)
}
</script>

<style lang="scss" scoped>
.h5-profile-head__main {
  flex: 1;
  min-width: 0;
}

.h5-ava--lg img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.h5-search__ico {
  flex: 0 0 16px;
  width: 16px;
  height: 16px;
  color: #c0c4cc;
}

.h5-search__clear {
  flex: 0 0 auto;
  padding: 0 2px;
  border: none;
  background: none;
  color: #c0c4cc;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.h5-search__input::-webkit-search-cancel-button {
  display: none;
}

.h5-contact-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 2px 14px 6px;
  color: #303133;
  font-size: 13px;
  font-weight: 600;

  &__refresh {
    padding: 0;
    border: none;
    background: none;
    color: #236de9;
    font-size: 12.5px;
    font-weight: 400;
    cursor: pointer;
  }
}

.h5-contact-tip {
  margin: 0;
  padding: 0 14px 8px;
  color: #909399;
  font-size: 11.5px;
  line-height: 1.7;

  &--foot {
    padding: 12px 14px 24px;
    text-align: center;
  }
}

.h5-contact-list {
  margin-top: 2px;
  border-top: 1px solid #f2f4f7;
  background: #fff;
}

.h5-skeleton--row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.h5-photo__remove {
  margin-top: 4px;
  padding: 0;
  border: none;
  background: none;
  color: #f56c6c;
  font-size: 12px;
  cursor: pointer;
}

.h5-footer-gap {
  height: 16px;
}

.h5-list-foot {
  padding: 12px 14px 4px;
}
</style>
