<template>
  <div class="h5-page reg-page">
    <header class="h5-nav">
      <button type="button" class="h5-nav__back" aria-label="返回" @click="handleBack">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M14.5 5.5L8 12l6.5 6.5" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <span class="h5-nav__title">{{ pageTitle }}</span>
      <button v-if="!success" type="button" class="h5-nav__side" @click="noticeVisible = true">须知</button>
      <span v-else class="h5-nav__side h5-nav__side--muted"></span>
    </header>

    <div ref="scrollRef" class="h5-scroll">
      <!-- ================= 提交成功 ================= -->
      <template v-if="success">
        <div class="reg-success">
          <div class="reg-success__badge" aria-hidden="true">
            <svg viewBox="0 0 64 64">
              <circle cx="32" cy="32" r="29" fill="none" stroke="currentColor" stroke-width="2" opacity="0.22" />
              <path d="M19 33.2l8.4 8.4L45 23" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
          <h3 class="reg-success__title">注册成功</h3>
          <p class="reg-success__sub">{{ successDesc }}</p>

          <div class="reg-success__card">
            <div class="reg-success__row">
              <span>申请编号</span>
              <b class="reg-success__no">{{ result.applyNos.length ? result.applyNos.join(' / ') : 'AP••••••••' }}</b>
            </div>
            <div class="reg-success__row"><span>意向部门</span><b>{{ selectedDeptNames || '—' }}</b></div>
            <div class="reg-success__row"><span>当前状态</span><b class="is-pending">待审批</b></div>
          </div>

          <button type="button" class="h5-submit" @click="goProgress">查看报名进度</button>
          <button type="button" class="h5-submit is-plain" @click="applyAnother">继续注册其他部门</button>
          <p class="reg-success__tip">建议截图保存申请编号，便于后续查询</p>
        </div>
      </template>

      <!-- ================= 报名表单 ================= -->
      <template v-else>
        <section class="reg-hero" :class="{ 'is-compact': step > 1 }">
          <div class="reg-hero__brand">
            <span class="reg-hero__logo">青</span>
            <span class="reg-hero__brand-meta">
              <b>青蟹堡</b>
              <i>新成员注册</i>
            </span>
          </div>

          <h1 class="reg-hero__title">{{ resubmitMode ? '修改注册信息' : '开启你的社团之旅' }}</h1>

          <p v-if="resubmitMode" class="reg-hero__desc">
            正在修改申请 <b class="reg-hero__no">{{ resubmitApplyNo || '—' }}</b>
            <span v-if="resubmitDeptName">（{{ resubmitDeptName }}）</span>，核对信息后重新提交即可
          </p>
          <p v-else class="reg-hero__desc">
            填写信息并提交，管理员会在 1–3 个工作日内完成审核，通过后自动开通账号
          </p>

          <ul class="reg-hero__meta">
            <li>
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M12 7.5V12l3 1.8" /></svg>
              1–3 个工作日
            </li>
            <li>
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5" /><path d="M16 16l4 4" /></svg>
              结果可在线查询
            </li>
            <li>
              <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="10" rx="2.4" /><path d="M8.5 10V7.8a3.5 3.5 0 017 0V10" /></svg>
              信息仅内部可见
            </li>
          </ul>
        </section>

        <div class="reg-steps">
          <div class="reg-steps__bar"><i :style="{ width: stepProgress + '%' }"></i></div>
          <div class="reg-steps__list">
            <div
              v-for="(label, index) in steps"
              :key="label"
              class="reg-step"
              :class="{ 'is-active': step === index + 1, 'is-done': step > index + 1 }"
            >
              <span class="reg-step__idx">{{ step > index + 1 ? '✓' : index + 1 }}</span>
              <span class="reg-step__label">{{ label }}</span>
            </div>
          </div>
        </div>

        <!-- ---------- 步骤 1：基本信息 ---------- -->
        <div v-show="step === 1">
          <div class="h5-card">
            <div class="h5-sec-title">基础身份<span class="h5-sec-title__tip">{{ resubmitMode ? '3 项必填' : '4 项必填' }}</span></div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>姓名</label>
              <input
                id="field-name"
                v-model="form.name"
                class="h5-input"
                :class="{ 'is-error': errors.name }"
                type="text"
                maxlength="20"
                placeholder="请输入真实姓名"
                @blur="validateField('name')"
              />
              <div v-if="errors.name" class="h5-err">{{ errors.name }}</div>
            </div>

            <div id="field-gender" class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>性别</label>
              <div class="h5-radio-row">
                <button
                  v-for="item in GENDER_OPTIONS"
                  :key="item.value"
                  type="button"
                  class="h5-radio"
                  :class="{ 'is-on': form.gender === item.value }"
                  @click="form.gender = item.value; errors.gender = ''"
                >
                  <i></i>{{ item.label }}
                </button>
              </div>
              <div v-if="errors.gender" class="h5-err">{{ errors.gender }}</div>
            </div>

            <div v-if="!resubmitMode" class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>学号</label>
              <input
                id="field-studentNo"
                v-model="form.studentNo"
                class="h5-input"
                :class="{ 'is-error': errors.studentNo }"
                type="text"
                inputmode="numeric"
                maxlength="12"
                placeholder="8–12 位数字，如 2024011001"
                @blur="validateField('studentNo')"
              />
              <div v-if="errors.studentNo" class="h5-err">{{ errors.studentNo }}</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>手机号</label>
              <input
                id="field-phone"
                v-model="form.phone"
                class="h5-input"
                :class="{ 'is-error': errors.phone }"
                type="tel"
                inputmode="numeric"
                maxlength="11"
                placeholder="11 位手机号"
                @blur="validateField('phone')"
              />
              <div v-if="errors.phone" class="h5-err">{{ errors.phone }}</div>
              <div v-if="resubmitMode" class="h5-hint">可在此更正手机号；学号不可修改，如需更换请联系组织负责人</div>
              <div v-else class="h5-hint">用于接收面试通知，也是查询报名进度的凭证</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>图形验证码</label>
              <div class="h5-field__row">
                <input
                  id="field-captchaCode"
                  v-model="captchaCode"
                  class="h5-input"
                  :class="{ 'is-error': errors.captchaCode }"
                  type="text"
                  maxlength="8"
                  autocomplete="off"
                  placeholder="请输入右侧验证码"
                  @blur="validateField('captchaCode')"
                />
                <img
                  v-if="captchaSrc"
                  class="h5-captcha"
                  :src="captchaSrc"
                  alt="图形验证码"
                  title="看不清？点击刷新"
                  @click="refreshCaptcha"
                />
                <button v-else type="button" class="h5-captcha h5-captcha--placeholder" @click="refreshCaptcha">
                  {{ captchaLoading ? '加载中…' : '点击获取' }}
                </button>
              </div>
              <div v-if="errors.captchaCode" class="h5-err">{{ errors.captchaCode }}</div>
              <div class="h5-hint">点击图片可刷新；验证通过后 30 分钟内可上传附件并提交</div>
            </div>
          </div>
        </div>

        <!-- ---------- 步骤 2：院系与志愿 ---------- -->
        <div v-show="step === 2">
          <div class="h5-card">
            <div class="h5-sec-title">院系信息<span class="h5-sec-title__tip">4 项必填</span></div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>学院</label>
              <input
                id="field-college"
                v-model="form.college"
                class="h5-input"
                :class="{ 'is-error': errors.college }"
                type="text"
                maxlength="100"
                placeholder="如：计算机科学与技术学院"
                @blur="validateField('college')"
              />
              <div v-if="errors.college" class="h5-err">{{ errors.college }}</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>专业</label>
              <input
                id="field-major"
                v-model="form.major"
                class="h5-input"
                :class="{ 'is-error': errors.major }"
                type="text"
                maxlength="100"
                placeholder="如：软件工程"
                @blur="validateField('major')"
              />
              <div v-if="errors.major" class="h5-err">{{ errors.major }}</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>班级</label>
              <input
                id="field-className"
                v-model="form.className"
                class="h5-input"
                :class="{ 'is-error': errors.className }"
                type="text"
                maxlength="50"
                placeholder="如：2024级软件工程2班"
                @blur="validateField('className')"
              />
              <div v-if="errors.className" class="h5-err">{{ errors.className }}</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label"><span class="h5-req">*</span>年级</label>
              <select
                id="field-grade"
                v-model="form.grade"
                class="h5-select"
                :class="{ 'is-error': errors.grade }"
                @change="validateField('grade')"
              >
                <option value="" disabled>请选择年级</option>
                <option v-for="item in GRADE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option>
              </select>
              <div v-if="errors.grade" class="h5-err">{{ errors.grade }}</div>
            </div>
          </div>

          <div class="h5-card">
            <div class="h5-sec-title">意向部门<span class="h5-sec-title__tip">{{ resubmitMode ? '不可修改' : '可多选' }}</span></div>

            <template v-if="resubmitMode">
              <div class="h5-resubmit-dept">{{ resubmitDeptName || '原意向部门' }}</div>
              <div class="h5-hint">重提不可更换部门；如需投递其他部门，请在报名进度中撤回后重新报名</div>
            </template>

            <template v-else>
              <div v-if="deptsLoading" class="h5-hint">部门加载中…</div>
              <div v-else-if="!depts.length" class="h5-hint">暂无可报名部门，请稍后重试</div>
              <div v-else id="field-deptIds" class="h5-dept-grid">
                <button
                  v-for="dept in depts"
                  :key="dept.deptId"
                  type="button"
                  class="h5-dept"
                  :class="{ 'is-on': form.deptIds.includes(dept.deptId) }"
                  @click="toggleDept(dept.deptId)"
                >
                  <span class="h5-dept__check">✓</span>{{ dept.deptName }}
                </button>
              </div>
              <div v-if="errors.deptIds" class="h5-err">{{ errors.deptIds }}</div>
              <div class="h5-hint">同一学号可同时投递多个部门，各部门独立审批；每个部门同一时间只保留 1 条待审批申请</div>
            </template>
          </div>

          <div class="h5-card">
            <div class="h5-sec-title">招新信息<span class="h5-sec-title__tip">选填</span></div>

            <div class="h5-field">
              <label class="h5-field__label">是否服从调剂</label>
              <div class="h5-radio-row">
                <button
                  v-for="item in ADJUST_OPTIONS"
                  :key="item.value"
                  type="button"
                  class="h5-radio"
                  :class="{ 'is-on': form.obeyAdjust === item.value }"
                  @click="form.obeyAdjust = item.value"
                >
                  <i></i>{{ item.label }}
                </button>
              </div>
              <div class="h5-hint">仅作部门间沟通参考，不会自动调剂（调剂需线下沟通后由管理员操作）</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label">特长<small>{{ form.skills.length }}/500</small></label>
              <input
                v-model="form.skills"
                class="h5-input"
                :class="{ 'is-error': errors.skills }"
                type="text"
                maxlength="500"
                placeholder="如：摄影、活动策划、Excel"
                @blur="validateField('skills')"
              />
              <div v-if="errors.skills" class="h5-err">{{ errors.skills }}</div>
            </div>

            <div class="h5-field">
              <label class="h5-field__label">自我介绍<small>{{ form.intro.length }}/500</small></label>
              <textarea
                v-model="form.intro"
                class="h5-textarea"
                :class="{ 'is-error': errors.intro }"
                maxlength="500"
                placeholder="简单介绍下自己，以及为什么想加入这个部门"
                @blur="validateField('intro')"
              ></textarea>
              <div v-if="errors.intro" class="h5-err">{{ errors.intro }}</div>
            </div>
          </div>

          <div class="h5-card">
            <div class="h5-sec-title">附件材料<span class="h5-sec-title__tip">建议上传</span></div>
            <div class="h5-upload-row">
              <!-- 照片 -->
              <div class="h5-upload" @click="pickPhoto">
                <template v-if="photo.preview">
                  <img class="h5-upload__thumb" :src="photo.preview" alt="一寸照片预览" />
                  <button type="button" class="h5-upload__del" aria-label="删除照片" @click.stop="removePhoto">×</button>
                </template>
                <template v-else-if="photo.fileId">
                  <span class="h5-upload__ico">✓</span>
                  <span>照片已上传</span>
                  <span class="h5-upload__note">{{ photo.original ? '沿用原附件 · 点击可重传' : '点击可重传' }}</span>
                  <button v-if="!photo.original" type="button" class="h5-upload__del" aria-label="删除照片" @click.stop="removePhoto">×</button>
                </template>
                <template v-else>
                  <span class="h5-upload__ico">＋</span>
                  <span>{{ photo.uploading ? '上传中 ' + photo.progress + '%' : '一寸照片' }}</span>
                  <span class="h5-upload__note">jpg / png ≤ 2MB</span>
                </template>
                <div v-if="photo.uploading" class="h5-upload__bar">
                  <div class="h5-upload__bar-inner" :style="{ width: photo.progress + '%' }"></div>
                </div>
              </div>
              <!-- 简历 -->
              <div class="h5-upload" @click="pickCard">
                <template v-if="card.fileName">
                  <span class="h5-upload__ico">📄</span>
                  <span class="h5-upload__file">{{ card.fileName }}</span>
                  <span v-if="card.original" class="h5-upload__note">沿用原附件 · 点击可重传</span>
                  <button v-if="!card.original" type="button" class="h5-upload__del" aria-label="删除简历" @click.stop="removeCard">×</button>
                </template>
                <template v-else>
                  <span class="h5-upload__ico">＋</span>
                  <span>{{ card.uploading ? '上传中 ' + card.progress + '%' : '简历文件' }}</span>
                  <span class="h5-upload__note">pdf / jpg / png ≤ 5MB</span>
                </template>
                <div v-if="card.uploading" class="h5-upload__bar">
                  <div class="h5-upload__bar-inner" :style="{ width: card.progress + '%' }"></div>
                </div>
              </div>
            </div>
            <input ref="photoInput" class="h5-file-input" type="file" accept="image/jpeg,image/png" @change="onPhotoChange" />
            <input ref="cardInput" class="h5-file-input" type="file" accept="application/pdf,image/jpeg,image/png" @change="onCardChange" />
            <div class="h5-hint">照片用于入会建档；简历用于了解你的经历与技能，仅招新审核可见</div>
            <div v-if="resubmitMode" class="h5-hint">已回填原附件；如需更换可重新上传，不重新上传将保留原附件</div>
          </div>
        </div>

        <!-- ---------- 步骤 3：确认提交 ---------- -->
        <div v-show="step === 3">
          <div class="h5-card">
            <div class="h5-sec-title">信息摘要<span class="h5-sec-title__tip">确认无误后提交</span></div>
            <div class="h5-summary">
              <div class="h5-summary__row"><span>姓名</span><b>{{ form.name || '—' }}</b><button type="button" @click="goStep(1)">修改</button></div>
              <div class="h5-summary__row"><span>性别</span><b>{{ genderLabel }}</b><button type="button" @click="goStep(1)">修改</button></div>
              <div class="h5-summary__row" v-if="!resubmitMode"><span>学号</span><b>{{ form.studentNo || '—' }}</b><button type="button" @click="goStep(1)">修改</button></div>
              <div class="h5-summary__row"><span>手机号</span><b>{{ form.phone || '—' }}</b><button type="button" @click="goStep(1)">修改</button></div>
              <div class="h5-summary__row"><span>学院</span><b>{{ form.college || '—' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>专业</span><b>{{ form.major || '—' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>班级</span><b>{{ form.className || '—' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>年级</span><b>{{ gradeLabel }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>意向部门</span><b>{{ selectedDeptNames || '—' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>服从调剂</span><b>{{ form.obeyAdjust === '1' ? '是' : '否' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>特长</span><b>{{ form.skills || '未填写' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>自我介绍</span><b>{{ form.intro || '未填写' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>照片</span><b>{{ photo.fileId ? '已上传' : '未上传' }}</b><button type="button" @click="goStep(2)">修改</button></div>
              <div class="h5-summary__row"><span>简历</span><b>{{ card.fileId ? '已上传' : '未上传' }}</b><button type="button" @click="goStep(2)">修改</button></div>
            </div>
          </div>

          <div class="h5-card">
            <label class="h5-agree">
              <input v-model="agreed" type="checkbox" />
              <span>
                我已阅读并同意
                <button type="button" class="h5-agree__link" @click.prevent="noticeVisible = true">《注册须知》</button>
                ，承诺填写信息真实有效，同意用于本次招新审核与入会建档
              </span>
            </label>
            <div v-if="!agreed" class="h5-err">请先阅读并同意《注册须知》</div>
          </div>

          <p class="h5-tip-line">提交后可在「报名进度」用手机号 + 图形验证码查询结果</p>
        </div>

        <div class="h5-footer-gap"></div>
      </template>
    </div>

    <!-- 底部固定操作条 -->
    <div v-if="!success" class="h5-submit-bar">
      <button v-if="step > 1" type="button" class="h5-btn-text" @click="prevStep">上一步</button>
      <!-- 未勾选须知时不禁用按钮：否则用户点不动也看不到提示，改为点击后由 doSubmit 弹 warning -->
      <button
        type="button"
        class="h5-submit"
        :disabled="submitting || verifying"
        @click="handleNext"
      >
        {{ primaryText }}
      </button>
    </div>

    <!-- 《注册须知》 -->
    <el-dialog v-model="noticeVisible" title="注册须知" width="320px" append-to-body align-center class="h5-dialog">
      <div class="h5-notice">
        <p>1. 注册信息须真实有效，用于本次招新审核与入会建档。</p>
        <p>2. 同一学号可同时向多个部门投递，每个部门同一时间只保留 1 条待审批申请。</p>
        <p>3. 审核结果可在「报名进度」用注册手机号 + 图形验证码查询。</p>
        <p>4. 审核通过后账号为学号，初始密码为注册手机号后 6 位，首次登录请修改密码。</p>
        <p>5. 个人信息仅用于招新与组织内部联络，不对外提供。</p>
      </div>
      <template #footer>
        <button type="button" class="h5-submit is-sm" @click="noticeVisible = false">我已阅读</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import {
  getApplyCaptcha,
  verifyApplyCaptcha,
  listApplyDepts,
  uploadApplyFile,
  submitApply,
  resubmitApply
} from '@/api/oa/public'

defineOptions({ name: 'H5Apply' })

const route = useRoute()
const router = useRouter()

const GRADE_OPTIONS = [
  { value: '1', label: '大一' },
  { value: '2', label: '大二' },
  { value: '3', label: '大三' },
  { value: '4', label: '大四' },
  { value: '5', label: '研一' },
  { value: '6', label: '研二' },
  { value: '7', label: '研三' }
]
const GENDER_OPTIONS = [
  { value: '0', label: '男' },
  { value: '1', label: '女' }
]
const ADJUST_OPTIONS = [
  { value: '1', label: '是' },
  { value: '0', label: '否' }
]
const RESUBMIT_KEY = 'oa:h5:resubmit:'
const PHOTO_MAX = 2 * 1024 * 1024
const CARD_MAX = 5 * 1024 * 1024

const steps = ['基本信息', '院系与志愿', '确认提交']
const step = ref(1)
const success = ref(false)
const submitting = ref(false)
const verifying = ref(false)
const noticeVisible = ref(false)
const scrollRef = ref(null)

const form = reactive({
  name: '',
  gender: '',
  studentNo: '',
  phone: '',
  college: '',
  major: '',
  className: '',
  grade: '',
  deptIds: [],
  obeyAdjust: '0',
  skills: '',
  intro: ''
})
const errors = reactive({})
const captcha = reactive({ uuid: '', img: '' })
const captchaCode = ref('')
const captchaLoading = ref(false)
const verifyToken = ref('')
const verifyExpireAt = ref(0)
const depts = ref([])
const deptsLoading = ref(false)
const photo = reactive({ fileId: null, fileName: '', preview: '', progress: 0, uploading: false, original: false })
const card = reactive({ fileId: null, fileName: '', progress: 0, uploading: false, original: false })
const photoInput = ref(null)
const cardInput = ref(null)
const agreed = ref(false)
const result = reactive({ applyNos: [], applyIds: [], progressTip: '' })

// ---------- 重提模式 ----------
const resubmitApplyId = computed(() => (route.query.resubmitApplyId ? String(route.query.resubmitApplyId) : ''))
const resubmitMode = computed(() => !!resubmitApplyId.value)
const resubmitApplyNo = ref('')
const resubmitDeptName = ref('')
const resubmitQueryToken = ref('')
// 原申请附件 fileId：重提时回填，用户不改动则原样提交（后端保留原附件）
const originalPhotoFileId = ref(null)
const originalCardFileId = ref(null)

const pageTitle = computed(() => (success.value ? '注册成功' : resubmitMode.value ? '修改注册信息' : '注册'))
const stepProgress = computed(() => ((step.value - 1) / (steps.length - 1)) * 100)
const primaryText = computed(() => {
  if (submitting.value) return '提交中…'
  if (verifying.value) return '验证中…'
  return step.value === 3 ? '提交注册' : '下一步'
})
const captchaSrc = computed(() => (captcha.img ? 'data:image/jpeg;base64,' + captcha.img : ''))
const genderLabel = computed(() => (GENDER_OPTIONS.find(item => item.value === form.gender) || {}).label || '—')
const gradeLabel = computed(() => (GRADE_OPTIONS.find(item => item.value === form.grade) || {}).label || '—')
const selectedDeptNames = computed(() => {
  if (resubmitMode.value) return resubmitDeptName.value || ''
  return depts.value
    .filter(dept => form.deptIds.includes(dept.deptId))
    .map(dept => dept.deptName)
    .join('、')
})
const successDesc = computed(
  () =>
    result.progressTip ||
    '注册申请已提交，请保存申请编号。审核结果可在「报名进度」中查看，通过后账号为学号，初始密码为手机号后 6 位。'
)

// ---------- 初始化 ----------
onMounted(() => {
  refreshCaptcha()
  loadDepts()
  if (resubmitMode.value) initResubmit()
})

onBeforeUnmount(() => {
  if (photo.preview) URL.revokeObjectURL(photo.preview)
})

function initResubmit() {
  form.phone = String(route.query.phone || '')
  const raw = sessionStorage.getItem(RESUBMIT_KEY + resubmitApplyId.value)
  if (raw) {
    try {
      const handoff = JSON.parse(raw)
      resubmitQueryToken.value = handoff.queryToken || ''
      resubmitApplyNo.value = handoff.applyNo || ''
      resubmitDeptName.value = handoff.deptName || ''
      // 原申请快照回填（ProgressApplyVO 已含申请表单字段）
      form.name = handoff.name || ''
      form.gender = handoff.gender || form.gender
      form.college = handoff.college || ''
      form.major = handoff.major || ''
      form.className = handoff.className || ''
      form.grade = handoff.grade || ''
      form.skills = handoff.skills || ''
      form.intro = handoff.intro || ''
      form.obeyAdjust = handoff.obeyAdjust === '1' ? '1' : '0'
      if (handoff.deptId) form.deptIds = [Number(handoff.deptId)]
      if (!resubmitDeptName.value && handoff.deptName) resubmitDeptName.value = handoff.deptName
      // 手机号以进度页验证的手机号为准，其次取原申请手机号
      if (!form.phone && handoff.phone) form.phone = String(handoff.phone)
      // 原附件回填：不重新上传时原样保留
      if (handoff.photoFileId) {
        originalPhotoFileId.value = handoff.photoFileId
        photo.fileId = handoff.photoFileId
        photo.original = true
      }
      if (handoff.cardFileId) {
        originalCardFileId.value = handoff.cardFileId
        card.fileId = handoff.cardFileId
        card.original = true
        card.fileName = '简历（原附件）'
      }
    } catch (e) {
      resubmitQueryToken.value = ''
    }
  }
  if (!resubmitQueryToken.value) {
    ElMessage.warning('验证已过期，请返回「报名进度」重新验证后再重提')
  }
}

async function loadDepts() {
  deptsLoading.value = true
  try {
    const res = await listApplyDepts()
    const data = res.data !== undefined ? res.data : res
    depts.value = Array.isArray(data) ? data : []
  } catch (e) {
    depts.value = []
  } finally {
    deptsLoading.value = false
  }
}

async function refreshCaptcha() {
  if (captchaLoading.value) return
  captchaLoading.value = true
  try {
    const res = await getApplyCaptcha()
    const data = res.data !== undefined ? res.data : res || {}
    captcha.uuid = data.uuid || ''
    captcha.img = data.img || ''
    captchaCode.value = ''
    errors.captchaCode = ''
  } catch (e) {
    captcha.uuid = ''
    captcha.img = ''
  } finally {
    captchaLoading.value = false
  }
}

// ---------- 字段校验 ----------
const VALIDATORS = {
  name(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写姓名'
    if (!/^[\u4e00-\u9fa5A-Za-z·]{2,20}$/.test(text)) return '姓名需为 2–20 个中英文字符'
    return ''
  },
  gender(value) {
    return value ? '' : '请选择性别'
  },
  studentNo(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写学号'
    if (!/^\d{8,12}$/.test(text)) return '学号需为 8–12 位数字'
    return ''
  },
  phone(value) {
    const text = String(value || '').trim()
    if (!text) return '请填写手机号'
    if (!/^1[3-9]\d{9}$/.test(text)) return '请输入 11 位手机号'
    return ''
  },
  captchaCode(value) {
    const text = String(value || '').trim()
    if (!text) return '请输入图形验证码'
    if (!/^[A-Za-z0-9]{1,8}$/.test(text)) return '请输入正确的图形验证码'
    return ''
  },
  college(value) {
    return String(value || '').trim() ? '' : '请填写学院'
  },
  major(value) {
    return String(value || '').trim() ? '' : '请填写专业'
  },
  className(value) {
    return String(value || '').trim() ? '' : '请填写班级'
  },
  grade(value) {
    return value ? '' : '请选择年级'
  },
  deptIds(value) {
    return Array.isArray(value) && value.length ? '' : '请选择意向部门'
  },
  skills(value) {
    return String(value || '').length > 500 ? '特长最多 500 字' : ''
  },
  intro(value) {
    return String(value || '').length > 500 ? '自我介绍最多 500 字' : ''
  }
}

const FIELD_IDS = {
  name: 'field-name',
  gender: 'field-gender',
  studentNo: 'field-studentNo',
  phone: 'field-phone',
  captchaCode: 'field-captchaCode',
  college: 'field-college',
  major: 'field-major',
  className: 'field-className',
  grade: 'field-grade',
  deptIds: 'field-deptIds'
}
const STEP1_FIELDS = ['name', 'gender', 'studentNo', 'phone', 'captchaCode']
const STEP2_FIELDS = ['college', 'major', 'className', 'grade', 'deptIds', 'skills', 'intro']
const step1Fields = computed(() => (resubmitMode.value ? ['name', 'gender', 'phone', 'captchaCode'] : STEP1_FIELDS))
const step2Fields = computed(() =>
  resubmitMode.value ? ['college', 'major', 'className', 'grade', 'skills', 'intro'] : STEP2_FIELDS
)

function validateField(field) {
  if (!VALIDATORS[field]) return true
  let value = form[field]
  if (field === 'captchaCode') value = captchaCode.value
  if (field === 'deptIds') value = form.deptIds
  errors[field] = VALIDATORS[field](value)
  return !errors[field]
}

function validateStep(fields) {
  return fields.reduce((valid, field) => {
    validateField(field)
    return valid && !errors[field]
  }, true)
}

function focusFirstError(fields) {
  const key = fields.find(field => errors[field])
  if (!key) return
  nextTick(() => {
    const element = document.getElementById(FIELD_IDS[key])
    if (!element) return
    element.scrollIntoView({ block: 'center', behavior: 'smooth' })
    if (typeof element.focus === 'function' && element.tagName !== 'DIV') {
      element.focus({ preventScroll: true })
      return
    }
    const input = element.querySelector('input, select, textarea')
    if (input) input.focus({ preventScroll: true })
  })
}

// ---------- 验证码校验 ----------
function isVerifyValid() {
  return !!verifyToken.value && Date.now() < verifyExpireAt.value
}

/**
 * 第 1 步需要校验的字段。
 * 验证成功后 captchaCode 会被清空（见 ensureVerify），若此时仍把 captchaCode 纳入校验，
 * 用户回退到第 1 步再点「下一步」会必然报「请输入图形验证码」；
 * 因此 verifyToken 仍在有效期内时跳过该字段校验。
 */
function step1FieldsToValidate() {
  if (!isVerifyValid()) return step1Fields.value
  return step1Fields.value.filter(field => field !== 'captchaCode')
}

async function ensureVerify() {
  if (!validateField('captchaCode')) return false
  verifying.value = true
  try {
    const res = await verifyApplyCaptcha({ uuid: captcha.uuid, code: captchaCode.value.trim() })
    const data = res.data !== undefined ? res.data : res || {}
    if (!data.verifyToken) throw new Error('verify failed')
    verifyToken.value = data.verifyToken
    verifyExpireAt.value = Date.now() + (Number(data.expireIn) || 1800) * 1000
    captchaCode.value = ''
    errors.captchaCode = ''
    return true
  } catch (e) {
    // 验证码错误 / 校验接口失败：给出明确提示并换一张，避免用户以为按钮没反应
    captchaCode.value = ''
    await refreshCaptcha()
    errors.captchaCode = '验证码错误，请重新输入'
    ElMessage.error('验证码错误，请重新输入')
    return false
  } finally {
    verifying.value = false
  }
}

// ---------- 步骤控制 ----------
async function handleNext() {
  if (submitting.value || verifying.value) return
  if (step.value === 1) {
    // 已验证通过（30 分钟内）时跳过 captchaCode 校验，直接进入下一步
    const fields = step1FieldsToValidate()
    if (!validateStep(fields)) {
      focusFirstError(fields)
      return
    }
    if (!isVerifyValid()) {
      const passed = await ensureVerify()
      if (!passed) {
        focusFirstError(['captchaCode'])
        return
      }
    }
    step.value = 2
    scrollToTop()
  } else if (step.value === 2) {
    if (!validateStep(step2Fields.value)) {
      focusFirstError(step2Fields.value)
      return
    }
    step.value = 3
    scrollToTop()
  } else {
    await doSubmit()
  }
}

function prevStep() {
  if (step.value > 1) {
    step.value -= 1
    scrollToTop()
  }
}

function goStep(target) {
  step.value = target
  scrollToTop()
}

function handleBack() {
  if (success.value) {
    goProgress()
    return
  }
  if (resubmitMode.value) {
    router.replace('/h5/progress')
    return
  }
  if (step.value > 1) {
    prevStep()
    return
  }
  router.replace('/h5/progress')
}

function goProgress() {
  router.push('/h5/progress')
}

function applyAnother() {
  success.value = false
  agreed.value = false
  form.deptIds = []
  errors.deptIds = ''
  if (!isVerifyValid()) {
    step.value = 1
    captchaCode.value = ''
    refreshCaptcha()
    ElMessage.info('校验已过期，请重新完成图形验证码')
  } else {
    step.value = 2
  }
  scrollToTop()
}

function scrollToTop() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = 0
  })
}

// ---------- 提交 ----------
async function doSubmit() {
  if (!agreed.value) {
    ElMessage.warning('请先阅读并同意《注册须知》')
    return
  }
  // 提交前复检第 1 步：已验证通过时同样跳过 captchaCode（验证成功后该输入已清空）
  const step1CheckFields = step1FieldsToValidate()
  if (!validateStep(step1CheckFields)) {
    step.value = 1
    focusFirstError(step1CheckFields)
    return
  }
  if (!validateStep(step2Fields.value)) {
    step.value = 2
    focusFirstError(step2Fields.value)
    return
  }
  if (!isVerifyValid()) {
    step.value = 1
    refreshCaptcha()
    ElMessage.warning('验证已过期，请重新验证')
    return
  }
  if (resubmitMode.value && !resubmitQueryToken.value) {
    ElMessage.error('验证已过期，请返回「报名进度」重新验证后再重提')
    return
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    if (resubmitMode.value) {
      await resubmitApply(payload)
      sessionStorage.removeItem(RESUBMIT_KEY + resubmitApplyId.value)
      ElMessage.success('已重新提交，请等待审批')
      router.replace('/h5/progress')
    } else {
      const res = await submitApply(payload)
      const data = res.data !== undefined ? res.data : res || {}
      result.applyIds = data.applyIds || []
      result.applyNos = data.applyNos || []
      result.progressTip = data.progressTip || ''
      success.value = true
      scrollToTop()
    }
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理，此处仅保留输入
  } finally {
    submitting.value = false
  }
}

function buildPayload() {
  if (resubmitMode.value) {
    // ApplyResubmitDTO：学号/部门不可改，手机号可更正
    return {
      applyId: Number(resubmitApplyId.value),
      queryToken: resubmitQueryToken.value,
      name: form.name.trim(),
      gender: form.gender,
      phone: form.phone.trim(),
      college: form.college.trim(),
      major: form.major.trim(),
      className: form.className.trim(),
      grade: form.grade,
      skills: form.skills.trim(),
      intro: form.intro.trim(),
      obeyAdjust: form.obeyAdjust,
      photoFileId: photo.fileId || null,
      cardFileId: card.fileId || null
    }
  }
  return {
    name: form.name.trim(),
    gender: form.gender,
    studentNo: form.studentNo.trim(),
    phone: form.phone.trim(),
    college: form.college.trim(),
    major: form.major.trim(),
    className: form.className.trim(),
    grade: form.grade,
    skills: form.skills.trim(),
    intro: form.intro.trim(),
    obeyAdjust: form.obeyAdjust,
    photoFileId: photo.fileId || null,
    cardFileId: card.fileId || null,
    applyDeptIds: [...form.deptIds],
    verifyToken: verifyToken.value,
    agreed: agreed.value
  }
}

// ---------- 部门选择 ----------
function toggleDept(deptId) {
  const index = form.deptIds.indexOf(deptId)
  if (index >= 0) {
    form.deptIds.splice(index, 1)
  } else {
    form.deptIds.push(deptId)
  }
  errors.deptIds = ''
}

// ---------- 附件上传 ----------
function pickPhoto() {
  if (photo.uploading) return
  photoInput.value && photoInput.value.click()
}

function pickCard() {
  if (card.uploading) return
  cardInput.value && cardInput.value.click()
}

function ensureCanUpload() {
  if (!isVerifyValid()) {
    ElMessage.warning('验证已过期，请返回第一步重新获取图形验证码')
    step.value = 1
    refreshCaptcha()
    return false
  }
  return true
}

function onPhotoChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  const isPhoto = /^image\/(jpeg|png)$/i.test(file.type) || /\.(jpe?g|png)$/i.test(file.name)
  if (!isPhoto) {
    ElMessage.error('仅支持 jpg / png，且不超过 2MB')
    return
  }
  if (file.size > PHOTO_MAX) {
    ElMessage.error('仅支持 jpg / png，且不超过 2MB')
    return
  }
  if (!ensureCanUpload()) return
  upload(file, 'member_photo', photo, true)
}

function onCardChange(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  const allowed =
    /^image\/(jpeg|png)$/i.test(file.type) ||
    /application\/pdf$/i.test(file.type) ||
    /\.(jpe?g|png|pdf)$/i.test(file.name)
  if (!allowed) {
    ElMessage.error('简历仅支持 pdf / jpg / png，且不超过 5MB')
    return
  }
  if (file.size > CARD_MAX) {
    ElMessage.error('简历仅支持 pdf / jpg / png，且不超过 5MB')
    return
  }
  if (!ensureCanUpload()) return
  upload(file, 'member_card', card, false)
}

async function upload(file, bizType, target, withPreview) {
  target.uploading = true
  target.progress = 0
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('bizType', bizType)
    formData.append('verifyToken', verifyToken.value)
    const res = await uploadApplyFile(formData, event => {
      if (event && event.total) {
        target.progress = Math.min(99, Math.round((event.loaded / event.total) * 100))
      }
    })
    const data = res.data !== undefined ? res.data : res || {}
    if (!data.fileId) throw new Error('上传失败')
    target.fileId = data.fileId
    target.fileName = file.name
    target.original = false
    target.progress = 100
    if (withPreview) {
      if (target.preview) URL.revokeObjectURL(target.preview)
      target.preview = URL.createObjectURL(file)
    }
    ElMessage.success('上传成功')
  } catch (e) {
    // 错误提示由 utils/request.js 统一处理
  } finally {
    target.uploading = false
  }
}

function removePhoto() {
  if (photo.preview) URL.revokeObjectURL(photo.preview)
  photo.preview = ''
  photo.progress = 0
  // 重提场景下删除“新上传”的照片 → 回退到原附件（由后端保留）
  if (resubmitMode.value && originalPhotoFileId.value) {
    photo.fileId = originalPhotoFileId.value
    photo.original = true
    photo.fileName = ''
  } else {
    photo.fileId = null
    photo.fileName = ''
    photo.original = false
  }
}

function removeCard() {
  card.progress = 0
  if (resubmitMode.value && originalCardFileId.value) {
    card.fileId = originalCardFileId.value
    card.original = true
    card.fileName = '简历（原附件）'
  } else {
    card.fileId = null
    card.fileName = ''
    card.original = false
  }
}
</script>

<style lang="scss" scoped>
/* ==========================================================================
   注册页（/h5/apply）视觉改版
   - 所有选择器都带 scoped 属性，只影响本页，不动 /h5/progress、/h5/portal
   - 设计语言沿用珍珠玻璃：浅色场景面 + 近实色卡片 + 细边线 + 单一视觉主角
   ========================================================================== */

/* ---------- 顶部导航 ---------- */
.reg-page {
  .h5-nav {
    padding: 0 14px;
    background: var(--rail-bg);
    backdrop-filter: blur(18px) saturate(1.1);
    -webkit-backdrop-filter: blur(18px) saturate(1.1);
  }

  .h5-nav__back {
    width: 32px;
    color: var(--ink);

    svg {
      width: 22px;
      height: 22px;
    }
  }

  .h5-nav__title {
    font-size: 16px;
    font-weight: 650;
    letter-spacing: -0.01em;
  }

  .h5-nav__side {
    flex: none;
    padding: 5px 11px;
    border-radius: 999px;
    background: var(--hover-bg);
    font-size: 12px;
    font-weight: 600;
    line-height: 1.4;
  }
}

/* ---------- 头图：浅色场景 + 品牌 + 三枚信息胶囊 ---------- */
.reg-hero {
  position: relative;
  padding: 22px 18px 18px;
  overflow: hidden;
  border-bottom: 1px solid var(--line);
  background:
    radial-gradient(110% 80% at 100% 0%, rgba(35, 109, 233, 0.1), transparent 62%),
    radial-gradient(90% 70% at 0% 100%, rgba(130, 221, 45, 0.12), transparent 62%),
    var(--canvas);

  &__brand {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
  }

  &__logo {
    display: flex;
    flex: 0 0 44px;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: 14px;
    background: linear-gradient(145deg, var(--blue), #3f7ff0);
    box-shadow: 0 6px 16px rgba(35, 109, 233, 0.28), var(--edge);
    color: #fff;
    font-size: 19px;
    font-weight: 700;
    line-height: 1;
    text-align: center;
  }

  &__brand-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;

    b {
      color: var(--ink);
      font-size: 15px;
      font-weight: 700;
      letter-spacing: -0.01em;
    }

    i {
      color: var(--ink-soft);
      font-size: 11.5px;
      font-style: normal;
    }
  }

  &__title {
    margin: 0 0 8px;
    color: var(--ink);
    font-size: 25px;
    font-weight: 760;
    letter-spacing: -0.03em;
    line-height: 1.26;
  }

  &__desc {
    margin: 0;
    color: var(--ink-muted);
    font-size: 13px;
    line-height: 1.7;
  }

  &__no {
    color: var(--ink);
    font-family: var(--font-mono);
    font-size: 13px;
  }

  &__meta {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
    margin: 16px 0 0;
    padding: 0;
    list-style: none;

    li {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 5px;
      min-width: 0;
      padding: 6px 8px;
      border: 1px solid var(--glass-line);
      border-radius: 12px;
      background: var(--pill-bg);
      box-shadow: var(--e1), var(--edge-strong);
      color: var(--ink-muted);
      font-size: 11px;
      white-space: nowrap;

      svg {
        flex: none;
        width: 12px;
        height: 12px;
        fill: none;
        stroke: var(--blue);
        stroke-linecap: round;
        stroke-linejoin: round;
        stroke-width: 1.8;
      }
    }
  }
}

/* 第 2/3 步收起头图，把屏幕让给表单 */
.reg-hero.is-compact {
  padding: 14px 18px 15px;

  .reg-hero__brand {
    margin-bottom: 10px;
  }

  .reg-hero__title {
    margin: 0;
    font-size: 19px;
    letter-spacing: -0.02em;
  }

  .reg-hero__desc,
  .reg-hero__meta {
    display: none;
  }
}

/* ---------- 步骤：细进度条 + 三段标签 ---------- */
.reg-steps {
  margin: 14px 16px 0;
  padding: 14px 16px 13px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);

  &__bar {
    height: 4px;
    overflow: hidden;
    border-radius: 999px;
    background: var(--groove-bg);

    i {
      display: block;
      height: 100%;
      border-radius: 999px;
      background: linear-gradient(90deg, var(--blue), var(--lime));
      transition: width 0.32s var(--ease-premium);
    }
  }

  &__list {
    display: flex;
    gap: 4px;
    margin-top: 11px;
  }
}

.reg-step {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 6px;
  color: var(--ink-soft);
  font-size: 12px;
  line-height: 1.3;

  &__idx {
    display: inline-flex;
    flex: 0 0 20px;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    border-radius: 999px;
    background: var(--groove-bg);
    color: var(--ink-soft);
    font-size: 11px;
    font-weight: 700;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.is-active {
    color: var(--ink);
    font-weight: 650;

    .reg-step__idx {
      background: var(--blue);
      box-shadow: 0 2px 8px rgba(35, 109, 233, 0.32);
      color: #fff;
    }
  }

  &.is-done {
    color: var(--ink-muted);

    .reg-step__idx {
      background: var(--lime-soft);
      color: var(--lime-text);
    }
  }
}

/* ---------- 卡片 / 表单 ---------- */
.h5-card {
  margin: 12px 16px 0;
  padding: 18px 16px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--surface);
  box-shadow: var(--edge), var(--e1);
}

.h5-sec-title {
  gap: 9px;
  margin: 0 0 16px;
  font-size: 14.5px;
  font-weight: 650;
  letter-spacing: -0.01em;

  &::before {
    width: 7px;
    height: 7px;
    border-radius: 999px;
    background: var(--lime);
    box-shadow: 0 0 0 4px var(--lime-soft);
  }
}

.h5-sec-title__tip {
  font-size: 11px;
  font-weight: 500;
}

.h5-field {
  margin-bottom: 16px;
}

.h5-field__label {
  gap: 3px;
  margin-bottom: 7px;
  color: var(--ink-muted);
  font-size: 13px;
  font-weight: 550;
}

.h5-req {
  color: var(--red-text);
}

.h5-input,
.h5-select {
  height: 46px;
  padding: 0 14px;
  border-radius: 12px;
  background: var(--input-bg);
  box-shadow: var(--edge);
  line-height: 44px;
  font-size: 16px;
}

.h5-textarea {
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--input-bg);
  box-shadow: var(--edge);
  font-size: 16px;
}

.h5-input:focus,
.h5-select:focus,
.h5-textarea:focus {
  box-shadow: 0 0 0 3px var(--blue-soft);
}

.h5-input.is-error,
.h5-select.is-error,
.h5-textarea.is-error {
  box-shadow: 0 0 0 3px var(--red-soft);
}

.h5-err {
  color: var(--red-text);
}

.h5-field__row .h5-input {
  flex: 1;
  width: auto;
  min-width: 0;
}

.h5-captcha {
  height: 46px;
  border-radius: 12px;
}

.h5-captcha--placeholder {
  padding: 0;
  font-family: inherit;
}

.h5-radio-row {
  gap: 8px;
}

.h5-radio {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 10px;
}

.h5-dept {
  min-height: 40px;
  border-radius: 10px;
}

.h5-upload {
  border-radius: 12px;
}

.h5-agree {
  color: var(--ink-muted);
  font-size: 12.5px;
  line-height: 1.7;
}

.h5-resubmit-dept {
  padding: 11px 13px;
  border: 1px solid var(--line-strong);
  border-radius: 12px;
  background: var(--blue-soft);
  color: var(--blue);
  font-size: 14px;
  font-weight: 600;
}

.h5-footer-gap {
  height: 20px;
}

/* ---------- 底部操作条 ---------- */
.h5-submit-bar {
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--line);
  background: var(--rail-bg);
  backdrop-filter: blur(18px) saturate(1.1);
  -webkit-backdrop-filter: blur(18px) saturate(1.1);
}

.h5-submit {
  height: 48px;
  border-radius: 14px;
  background: var(--blue);
  box-shadow: 0 8px 20px rgba(35, 109, 233, 0.26);
  font-size: 16px;
  font-weight: 650;
  letter-spacing: 0.02em;
  transition: transform 0.16s ease, box-shadow 0.16s ease, opacity 0.16s ease;

  &:active {
    transform: translateY(1px);
    box-shadow: none;
  }

  &.is-plain {
    border: 1px solid var(--line-strong);
    background: var(--input-bg);
    box-shadow: var(--edge);
    color: var(--ink);
  }

  &.is-sm {
    height: 38px;
    border-radius: 12px;
    font-size: 14px;
  }
}

.h5-btn-text {
  flex: 0 0 auto;
  height: 48px;
  padding: 0 16px;
  border: 1px solid var(--line-strong);
  border-radius: 14px;
  background: var(--input-bg);
  color: var(--ink-muted);
  font-size: 14px;
  font-weight: 600;
}

/* ---------- 注册成功 ---------- */
.reg-success {
  padding: 40px 20px 24px;
  text-align: center;

  &__badge {
    width: 72px;
    height: 72px;
    margin: 0 auto 16px;
    color: var(--lime-text);
    filter: drop-shadow(0 8px 18px var(--lime-soft));

    svg {
      display: block;
      width: 100%;
      height: 100%;
    }
  }

  &__title {
    margin: 0 0 8px;
    color: var(--ink);
    font-size: 21px;
    font-weight: 740;
    letter-spacing: -0.02em;
  }

  &__sub {
    margin: 0 0 20px;
    color: var(--ink-soft);
    font-size: 12.5px;
    line-height: 1.75;
  }

  &__card {
    margin-bottom: 22px;
    padding: 4px 16px;
    border: 1px solid var(--line);
    border-radius: 16px;
    background: var(--surface);
    box-shadow: var(--edge), var(--e1);
    text-align: left;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 13px 0;
    border-bottom: 1px solid var(--line);
    font-size: 13px;

    &:last-child {
      border-bottom: none;
    }

    span {
      flex: none;
      color: var(--ink-soft);
    }

    b {
      color: var(--ink);
      font-weight: 650;
      text-align: right;
      word-break: break-all;
    }

    b.is-pending {
      color: var(--warn-text);
    }
  }

  &__no {
    font-family: var(--font-mono);
    font-size: 13px;
    letter-spacing: 0.02em;
  }

  &__tip {
    margin: 14px 0 0;
    color: var(--ink-faint);
    font-size: 11.5px;
  }

  .h5-submit {
    margin-bottom: 10px;
  }
}

/* ---------- 注册须知弹窗 ---------- */
.h5-notice {
  color: var(--ink-muted);
  font-size: 13px;
  line-height: 1.8;

  p {
    margin: 0 0 8px;
  }
}
</style>
