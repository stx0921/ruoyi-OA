#!/usr/bin/env node
/**
 * 单文件 Vue SFC 语法自检
 *
 * 用法：cd web && node scripts/check-sfc.mjs src/views/oa/leave/index.vue [...]
 *
 * 为什么要单独写这个：仓库里 views 是通过 import.meta.glob 全量加载的，
 * 多个人同时改页面时跑完整 `npm run build` 会互相干扰（别人的半成品也会一起报错）。
 * 这里只编译指定文件，能定位模板 / <script setup> 的语法错误。
 */
import fs from 'node:fs'
import path from 'node:path'
import { parse, compileScript, compileTemplate } from '@vue/compiler-sfc'

const files = process.argv.slice(2)
if (!files.length) {
  console.error('用法: node scripts/check-sfc.mjs <file.vue> [...]')
  process.exit(2)
}

let failed = 0
for (const file of files) {
  const abs = path.resolve(process.cwd(), file)
  try {
    if (!fs.existsSync(abs)) throw new Error('文件不存在: ' + abs)
    const source = fs.readFileSync(abs, 'utf8')
    const { descriptor, errors } = parse(source, { filename: abs })
    if (errors && errors.length) {
      throw new Error(errors.map(e => e.message || String(e)).join('\n'))
    }
    const id = 'oa-sfc-check'
    let script = null
    if (descriptor.script || descriptor.scriptSetup) {
      script = compileScript(descriptor, { id })
    }
    if (descriptor.template) {
      const result = compileTemplate({
        source: descriptor.template.content,
        filename: abs,
        id,
        compilerOptions: {
          bindingMetadata: script ? script.bindings : undefined
        }
      })
      if (result.errors && result.errors.length) {
        throw new Error(
          result.errors
            .map(e => (typeof e === 'string' ? e : e.message || JSON.stringify(e)))
            .join('\n')
        )
      }
    }
    console.log('OK   ' + file)
  } catch (e) {
    failed++
    const msg = String((e && e.message) || e).trim()
    console.error('FAIL ' + file + '\n     ' + msg.split('\n').join('\n     '))
  }
}

if (failed) {
  console.error(`\n${failed} 个文件未通过语法检查`)
  process.exit(1)
}
console.log('\n全部通过')
