<template>
  <el-drawer v-model="showSettings" :withHeader="false" :lock-scroll="false" direction="rtl" size="300px">
    <div class="setting-drawer-title">
      <h3 class="drawer-title">菜单导航设置</h3>
    </div>
    <div class="nav-wrap">
      <el-tooltip content="左侧菜单" placement="bottom">
        <div class="item left" @click="handleNavType(1)" :class="{ activeItem: navType == 1 }">
          <b></b><b></b>
        </div>
      </el-tooltip>

      <el-tooltip content="混合菜单" placement="bottom">
        <div class="item mix" @click="handleNavType(2)" :class="{ activeItem: navType == 2 }">
          <b></b><b></b>
        </div>
      </el-tooltip>
      <el-tooltip content="顶部菜单" placement="bottom">
        <div class="item top" @click="handleNavType(3)" :class="{ activeItem: navType == 3 }">
          <b></b><b></b>
        </div>
      </el-tooltip>
    </div>
    <div class="drawer-item">
      <span>主题颜色</span>
      <span class="comp-style">
        <el-color-picker v-model="theme" :predefine="predefineColors" @change="themeChange"/>
      </span>
    </div>
    <el-divider />

    <h3 class="drawer-title">系统布局配置</h3>

    <div class="drawer-item">
      <span>开启页签</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>持久化标签页</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsViewPersist" :disabled="!settingsStore.tagsView" @change="tagsViewPersistChange" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示页签图标</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.tagsIcon" :disabled="!settingsStore.tagsView" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>标签页样式</span>
      <span class="comp-style">
        <el-radio-group v-model="settingsStore.tagsViewStyle" :disabled="!settingsStore.tagsView" size="small">
          <el-radio-button label="card">卡片</el-radio-button>
          <el-radio-button label="chrome">谷歌</el-radio-button>
        </el-radio-group>
      </span>
    </div>

    <div class="drawer-item">
      <span>固定 Header</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.fixedHeader" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>显示 Logo</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.sidebarLogo" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>动态标题</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.dynamicTitle" @change="dynamicTitleChange" class="drawer-switch" />
      </span>
    </div>

    <div class="drawer-item">
      <span>底部版权</span>
      <span class="comp-style">
        <el-switch v-model="settingsStore.footerVisible" class="drawer-switch" />
      </span>
    </div>

    <el-divider />

    <el-button type="primary" plain icon="DocumentAdd" @click="saveSetting">保存配置</el-button>
    <el-button plain icon="Refresh" @click="resetSetting">重置配置</el-button>
  </el-drawer>

</template>

<script setup>
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import { handleThemeStyle } from '@/utils/theme'

const { proxy } = getCurrentInstance()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const showSettings = ref(false)
const navType = ref(settingsStore.navType)
const theme = ref(settingsStore.theme)
const tagsViewPersist = ref(settingsStore.tagsViewPersist)
const storeSettings = computed(() => settingsStore)
const predefineColors = ref(["#236de9", "#ff4500", "#ff8c00", "#ffd700", "#90ee90", "#00ced1", "#1e90ff", "#c71585"])

/** 是否需要dynamicTitle */
function dynamicTitleChange() {
  useSettingsStore().setTitle(useSettingsStore().title)
}

function tagsViewPersistChange(val) {
  settingsStore.tagsViewPersist = val
  tagsViewPersist.value = val
}

function themeChange(val) {
  settingsStore.theme = val
  handleThemeStyle(val)
}

function handleNavType(val) {
  settingsStore.navType = val
  navType.value = val
}

/** 菜单导航设置：navType 是 ref，回调参数就是新值（原写法 val.value 恒为 undefined） */
watch(navType, val => {
  if (val == 1) {
    appStore.sidebar.opened = true
    appStore.toggleSideBarHide(false)
  }
  if (val == 2) {
    appStore.sidebar.opened = true
  }
  if (val == 3) {
    appStore.sidebar.opened = false
    appStore.toggleSideBarHide(true)
  }
  if ([1, 3].includes(val)) {
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes)
  }
}, { immediate: true }
)

function saveSetting() {
  proxy.$modal.loading("正在保存到本地，请稍候...")
  if (!tagsViewPersist.value) {
    proxy.$cache.local.remove('tags-view-visited')
  }
  let layoutSetting = {
    "navType": storeSettings.value.navType,
    "tagsView": storeSettings.value.tagsView,
    "tagsIcon": storeSettings.value.tagsIcon,
    "tagsViewStyle": storeSettings.value.tagsViewStyle,
    "tagsViewPersist": storeSettings.value.tagsViewPersist,
    "fixedHeader": storeSettings.value.fixedHeader,
    "sidebarLogo": storeSettings.value.sidebarLogo,
    "dynamicTitle": storeSettings.value.dynamicTitle,
    "footerVisible": storeSettings.value.footerVisible,
    "sideTheme": storeSettings.value.sideTheme,
    "theme": storeSettings.value.theme
  }
  localStorage.setItem("layout-setting", JSON.stringify(layoutSetting))
  setTimeout(() => proxy.$modal.closeLoading(), 1000)
}

function resetSetting() {
  proxy.$cache.local.remove('tags-view-visited')
  proxy.$modal.loading("正在清除设置缓存并刷新，请稍候...")
  localStorage.removeItem("layout-setting")
  setTimeout(() => window.location.reload(), 1000)
}

function openSetting() {
  showSettings.value = true
}

defineExpose({
  openSetting
})
</script>

<style lang='scss' scoped>
.setting-drawer-title {
  margin-bottom: 12px;
  color: var(--el-text-color-primary, rgba(0, 0, 0, 0.85));
  line-height: 22px;
  font-weight: bold;

  .drawer-title {
    font-size: 14px;
  }
}

.drawer-item {
  color: var(--el-text-color-regular, rgba(0, 0, 0, 0.65));
  padding: 12px 0;
  font-size: 14px;

  .comp-style {
    float: right;
    margin: -3px 8px 0px 0px;
  }
}

// 导航模式
.nav-wrap {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 10px;
  margin-bottom: 20px;

  .activeItem {
    border: 2px solid var(--blue) !important;
  }

  .item {
    position: relative;
    margin-right: 16px;
    cursor: pointer;
    width: 56px;
    height: 48px;
    border-radius: var(--r-xs);
    background: var(--groove-bg);
    border: 2px solid transparent;
    overflow: hidden;
  }

  .left {
    b:first-child {
      display: block;
      height: 100%;
      background: var(--surface-solid);
    }
    b:last-child {
      width: 30%;
      background: var(--ink-faint);
      position: absolute;
      height: 100%;
      top: 0;
      border-radius: var(--r-xs) 0 0 var(--r-xs);
    }
  }
  .mix {
    b:first-child {
      border-radius: var(--r-xs) var(--r-xs) 0 0;
      display: block;
      height: 30%;
      background: var(--ink-faint);
    }
    b:last-child {
      width: 30%;
      background: var(--ink-faint);
      position: absolute;
      height: 70%;
      border-radius: 0 0 0 var(--r-xs);
    }
  }
  .top {
    b:first-child {
      display: block;
      height: 30%;
      background: var(--ink-faint);
      border-radius: var(--r-xs) var(--r-xs) 0 0;
    }
  }
}
</style>