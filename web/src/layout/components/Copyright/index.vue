<template>
  <footer v-if="visible" class="copyright">
    <span>{{ content }}</span>
    <span v-if="icpNumber" class="copyright-icp">
      <a :href="icpLink" target="_blank" rel="noopener noreferrer">{{ icpNumber }}</a>
    </span>
  </footer>
</template>

<script setup>
import defaultSettings from '@/settings'
import useSettingsStore from '@/store/modules/settings'

const settingsStore = useSettingsStore()

const icpNumber = defaultSettings.icpNumber
const icpLink = defaultSettings.icpLink
// 配置了备案号就始终显示底部，不受本地布局设置影响
const visible = computed(() => settingsStore.footerVisible || !!icpNumber)
const content = computed(() => settingsStore.footerContent)
</script>

<style scoped>
.copyright {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  height: 36px;
  padding: 10px 20px;
  text-align: right;
  background-color: #f8f8f8;
  color: #666;
  font-size: 14px;
  border-top: 1px solid #e7e7e7;
  z-index: 999;
}

.copyright-icp a {
  color: #666;
  text-decoration: none;
}

.copyright-icp a:hover {
  color: #236de9;
  text-decoration: underline;
}
</style>
