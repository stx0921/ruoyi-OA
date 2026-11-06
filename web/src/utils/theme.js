// 处理主题样式
export function handleThemeStyle(theme) {
  const isDark = typeof document !== 'undefined' && document.documentElement.classList.contains('dark')
  // 先做可读性归一化：过亮的主题色会让「白字 + 主色底」的主按钮、以及
  // 「主色字 + 白底」的链接/朴素按钮都糊成一片（用户反馈过按钮文字看不见）
  const base = normalizePrimary(theme)
  const primary = isDark ? softenPrimaryForDark(base) : base
  const root = document.documentElement
  root.style.setProperty('--el-color-primary', primary)
  for (let i = 1; i <= 9; i++) {
    root.style.setProperty(`--el-color-primary-light-${i}`, `${getLightColor(primary, i / 10)}`)
  }
  for (let i = 1; i <= 9; i++) {
    root.style.setProperty(`--el-color-primary-dark-${i}`, `${getDarkColor(primary, i / 10)}`)
  }
  // 珍珠玻璃主题令牌同步：页面自绘控件 / 材质类用的是 --blue，若不跟随会与
  // Element Plus 组件出现「两种蓝」（主按钮不变色、勾选框变色）
  root.style.setProperty('--blue', primary)
  root.style.setProperty('--blue-hover', getDarkColor(primary, 0.08))
  const [r, g, b] = hexToRgb(primary)
  root.style.setProperty('--blue-soft', `rgba(${r}, ${g}, ${b}, ${isDark ? 0.16 : 0.1})`)
  root.style.setProperty('--focus', `rgba(${r}, ${g}, ${b}, ${isDark ? 0.85 : 0.8})`)
}

/** 相对亮度（0–1），只用于判断颜色深浅，不做 WCAG 精确换算 */
export function relativeLuminance(color) {
  const [r, g, b] = hexToRgb(String(color).replace('#', ''))
  return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255
}

/**
 * 主题色可读性归一化
 *
 * 主按钮是「白字 + 主色底」，链接/朴素按钮是「主色字 + 白底」。
 * 用户在「布局设置 → 主题颜色」里选了过亮的颜色（黄、浅绿、浅青等）时，
 * 两种用法都会失去对比度，表现为「按钮上的字和背景糊在一起」。
 * 这里把亮度超过阈值的颜色逐级压暗，直到回到可读区间；
 * 默认珍珠蓝 #236de9（亮度约 0.40）不受影响。
 *
 * @param {string} color 用户选择的主色
 * @param {string} fallback 非法值兜底
 */
export function normalizePrimary(color, fallback = '#236de9') {
  let value = String(color || '').trim()
  if (!/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(value)) return fallback
  if (value.length === 4) {
    value = '#' + value.slice(1).split('').map(ch => ch + ch).join('')
  }
  // 白字需要底足够深：亮度超过 0.58 就压暗
  const MAX_LUMINANCE = 0.58
  if (relativeLuminance(value) <= MAX_LUMINANCE) return value.toLowerCase()
  let ratio = 0.12
  while (ratio <= 0.9) {
    const next = mixHexColors(value, '#0b1220', ratio)
    if (relativeLuminance(next) <= MAX_LUMINANCE) return next
    ratio += 0.1
  }
  return fallback
}

/** 混合两种十六进制颜色 */
export function mixHexColors(fg, bg, t) {
  const a = hexToRgb(String(fg).replace('#', ''))
  const b = hexToRgb(String(bg).replace('#', ''))
  const out = [0, 1, 2].map((i) => Math.round(a[i] * (1 - t) + b[i] * t))
  return rgbToHex(out[0], out[1], out[2])
}

/** 暗色模式下柔化主题色 */
export function softenPrimaryForDark(theme) {
  return mixHexColors(theme, '#2d3036', 0.34)
}

// hex颜色转rgb颜色
export function hexToRgb(str) {
  str = str.replace('#', '')
  let hexs = str.match(/../g)
  for (let i = 0; i < 3; i++) {
    hexs[i] = parseInt(hexs[i], 16)
  }
  return hexs
}

// rgb颜色转Hex颜色
export function rgbToHex(r, g, b) {
  let hexs = [r.toString(16), g.toString(16), b.toString(16)]
  for (let i = 0; i < 3; i++) {
    if (hexs[i].length == 1) {
      hexs[i] = `0${hexs[i]}`
    }
  }
  return `#${hexs.join('')}`
}

// 变浅颜色值
export function getLightColor(color, level) {
  let rgb = hexToRgb(color)
  for (let i = 0; i < 3; i++) {
    rgb[i] = Math.floor((255 - rgb[i]) * level + rgb[i])
  }
  return rgbToHex(rgb[0], rgb[1], rgb[2])
}

// 变深颜色值
export function getDarkColor(color, level) {
  let rgb = hexToRgb(color)
  for (let i = 0; i < 3; i++) {
    rgb[i] = Math.floor(rgb[i] * (1 - level))
  }
  return rgbToHex(rgb[0], rgb[1], rgb[2])
}
