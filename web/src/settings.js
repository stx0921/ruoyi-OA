export default {
  /**
   * 网页标题
   */
  title: import.meta.env.VITE_APP_TITLE,

  /**
   * 侧边栏主题：
   * 珍珠玻璃主题下导航轨统一为浅色玻璃面（见 assets/styles/ui.scss 的 .m-rail 段），
   * 因此默认值改为 theme-light；theme-dark 仅作为历史配置兼容，不再对外提供切换入口。
   */
  sideTheme: 'theme-light',

  /**
   * 是否系统布局配置
   */
  showSettings: true,

  /**
   * 菜单导航模式 1、纯左侧 2、混合（左侧+顶部） 3、纯顶部
   */
  navType: 1,

  /**
   * 是否显示 tagsView
   */
  tagsView: true,

  /**
   * 持久化标签页
   */
  tagsViewPersist: false,

  /**
   * 显示页签图标
   */
  tagsIcon: false,

  /**
   * 标签页样式：card 卡片（默认）、chrome 谷歌浏览器风格
   */
  tagsViewStyle: 'card',

  /**
   * 是否固定头部
   */
  fixedHeader: true,

  /**
   * 是否显示logo
   */
  sidebarLogo: true,

  /**
   * 是否显示动态标题
   */
  dynamicTitle: false,

  /**
   * 是否显示底部版权
   */
  footerVisible: false,

  /**
   * 底部版权文本内容
   */
  footerContent: '© 2026 轻量 OA · 企业协同办公平台'
}

