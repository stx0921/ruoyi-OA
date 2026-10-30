# 轻量 OA 系统 · 项目规划 v0.1

> 状态：P0/P1 部分完成 · 记录人：DSH Lead · 日期：2026-10-30

## 进度记录

### 2026-10-30 · P2 请假流程后端完成

- ✅ 审批语义设计定稿：[OA-审批语义设计.md](OA-审批语义设计.md)（状态机、按天动态路由、驳回同实例退回、审批人解析）
- ✅ BPMN：`ruoyi-oa/src/main/resources/processes/leave_approval.bpmn20.xml`，按天数路由 + 总经理分支
- ✅ 后端：域模型/Mapper/Service/Controller/待办已办/流程跟踪，全工程编译通过
- ✅ 流程定义部署成功（`OaProcessDeployer` 显式部署）
- ⚠️ 踩坑 1：Flowable 自动扫流程定义在 Spring Boot fat jar 里扫不到嵌套 jar，改显式部署
- ⚠️ 踩坑 2：`mvn package` 时 jar 插件因"自身源码未变"跳过重打包，只改依赖 jar 会导致跑旧代码；**统一改用 `mvn clean package`**
- ⏳ 待办：`oa_leave` 表尚未创建（`sql/oa/01_oa_leave.sql`），阻塞端到端联调



- ✅ 新增 `ruoyi-oa` 模块，挂入父 pom 与 `ruoyi-admin` 依赖
- ✅ Flowable 6.7.2 接入成功，排除自带 IDM（用户体系复用若依）
- ✅ `OaFlowableConfig`：关闭 IDM、关异步执行器、设中文字体
- ✅ `application.yml`：`database-schema-update=true`、历史级别 audit、自动部署 `processes/`
- ✅ 全工程 `mvn package` 通过
- ✅ 8081 临时实例启动成功（22s），Flowable 自动创建 38 张 `ACT_*` / `FLW_*` 表，引擎初始化完成
- ⚠️ 数据库 `ry-vue` 缺 `QRTZ_*` 表（定时任务），需单独导入 `sql/quartz.sql`
- ⚠️ Flowable 顺带建了事件注册表 `FLW_EV_*`，如不需要可后续排除
- ⚠️ IDEA 里的 8080 实例仍是旧构建，需 reimport Maven 后才能加载 OA 模块

## 0. 一句话

在 RuoYi-Vue 3.9.2 骨架之上，基于 Flowable 6.7.2 构建一套「组织人事 + 自定义审批流 + 待办中心 + 数据看板」的轻量企业协同办公平台，按**作品集 / 可交付**标准打磨。

## 1. 目标与取舍

| 维度 | 结论 |
|---|---|
| 主目标 | 作品集展示 + 可作为客户交付底座 |
| 次目标 | 个人技术磨练：真正掌握流程引擎接入与 BPMN，而非套模板 |
| 明确不做（v1） | 多租户、微服务拆分、移动端 App、即时通讯 |
| 成功标准 | 一条完整流程（请假）端到端跑通 → 三条业务流可演示 → 有看板与在线流程图 → 一条命令部署 |

## 2. 现有资产

| 资产 | 位置 | 状态 |
|---|---|---|
| 后端 | `D:\coder_data\RuoYi-Vue` | RuoYi-Vue 3.9.2 / Boot 2.5.15 / Java 8，已编译出 `ruoyi-admin.jar` |
| 前端 | `D:\coder_data\RuoYi-Vue3` | RuoYi-Vue3 3.9.2 / Vue 3.5.26 / Element Plus 2.13.1 / Vite 6.4.1，`node_modules` 已装 |
| 数据库脚本 | `sql/ry_20260417.sql`、`sql/quartz.sql` | 未确认是否已导入 `ry-vue` |
| 代码生成器 | `ruoyi-generator` | 自带 `vm/vue/v3`、`vm/vue/v3ts` 模板，可直接产出 Vue3 页面 |
| 版本控制 | — | ❗两个仓库均无 `.git`，需 P0 初始化 |

## 3. 技术选型

| 层 | 选型 | 依据 |
|---|---|---|
| 基础框架 | RuoYi-Vue 3.9.2 | 现成，权限/菜单/字典/日志/定时任务开箱可用 |
| 流程引擎 | Flowable 6.7.2 | 与 Spring Boot 2.5 匹配；6.8.x 作为备选 |
| 流程建模 | BPMN 2.0 + bpmn-js | 在线设计器是作品集亮点，P4 引入 |
| 持久层 | MyBatis + Druid | 沿用 |
| 缓存/会话 | Redis | 沿用 |
| 前端 | Vue3 + Element Plus + Pinia + Vite | 已就绪，且生成器支持 v3 模板 |
| 图表 | ECharts 5.6 | 已在前端依赖中 |
| 部署 | Docker Compose（MySQL + Redis + Nginx + jar） | 交付形态 |

参考实现（仅借鉴思路，不 fork）：[RuoYi-flowable](https://github.com/tony2y/RuoYi-flowable)

## 4. 架构

```
[ Vue3 前端 :80 /dev-api 代理 ]
              │
        [ Nginx / Vite ]
              │
[ ruoyi-admin :8080  RuoYi + Spring Security + JWT ]
   ├── ruoyi-system     组织/角色/菜单/字典（复用）
   ├── ruoyi-framework  安全、数据权限、多数据源
   ├── ruoyi-quartz     定时任务（合同到期提醒等）
   ├── ruoyi-generator  代码生成
   └── ruoyi-oa  ★新增  OA 业务 + Flowable 门面
              │
   [ Flowable 引擎 + ACT_* 表 ]
              │
        [ MySQL ry-vue ]  [ Redis ]
```

新增模块 `ruoyi-oa`，依赖 `flowable-spring-boot-starter-process`；不对 Flowable 做深度二次封装，业务侧通过 `RuntimeService / TaskService / HistoryService` 门面调用，保持可读性（这也是技术磨练点）。

## 5. 模块与优先级

| 优先级 | 模块 | 内容 |
|---|---|---|
| P0 | 环境打通 | 数据库导入、后端启动、前端启动、登录 |
| P1 | 组织人事 | 员工档案（入职/转正/调岗/离职）、岗位、合同到期提醒 |
| P2 | 审批内核 | Flowable 接入、请假流程端到端、待办/已办、审批意见、流程图跟踪 |
| P3 | 业务铺开 | 报销（含明细）、出差、加班；待办中心；站内消息 + 附件 |
| P4 | 打磨交付 | ECharts 看板、bpmn-js 在线设计器、演示数据、Docker Compose、README/截图 |

## 6. 数据模型草案

Flowable 自带 `ACT_*` 表由引擎初始化，业务侧只建：

| 表 | 说明 |
|---|---|
| `oa_employee` | 员工档案，关联 `sys_user`、`sys_dept`、`sys_post` |
| `oa_leave` | 请假单：类型、起止时间、时长、事由、`process_instance_id`、状态 |
| `oa_expense` / `oa_expense_item` | 报销主表 / 明细（主从表，用来练生成器） |
| `oa_travel` | 出差申请 |
| `oa_overtime` | 加班申请 |
| `oa_file` | 附件（业务类型 + 业务 id + 存储路径） |
| `oa_notice_read` | 公告已读（RuoYi 自带 `sys_notice`） |
| `oa_flow_form` | 自定义表单定义（P4 表单设计器用） |

统一约定：业务表含 `process_instance_id`、`status`、`apply_user_id`、`apply_time`；流程结束后异步回写 `status`。

## 7. 阶段与验收

| 阶段 | 交付 | 验收（可演示） |
|---|---|---|
| P0 | 环境 | 浏览器登录 `admin/admin123` 进入首页 |
| P1 | 员工档案 | 新增员工 → 列表/详情/导出 Excel 正常 |
| P2 | 请假闭环 | 员工发起 → 主管审批 → 会签 → 归档；流程图可追溯 |
| P3 | 三流齐备 | 报销/出差/请假均在待办中心可见，消息能收到 |
| P4 | 作品集 | 看板有数据、可在线画流程、`docker compose up` 一键起 |

## 8. 风险与对策

| 风险 | 对策 |
|---|---|
| Flowable 与 Boot 2.5.15 版本冲突 | 先 6.7.2；不行降到 6.6.0 或升 6.8.0，P2 首日验证 |
| 在线设计器工作量失控 | 严格放 P4，P2/P3 用预置 BPMN 文件部署 |
| 前端仓库在当前工作区外，无法写入 | 需把会话工作区调到 `D:\coder_data`（待用户确认） |
| Windows 文件权限导致命令执行器无法启动 | 需一次授权修复（脚本附回滚） |
| 业务铺太开导致每块都半成品 | 严格按 P2 单流程跑通再铺开 |

## 9. 开工清单（P0）

1. 修复执行环境权限，能跑 `mvn` / `node` / `mysql`
2. 两个仓库 `git init` + `.gitignore` + 首次提交
3. 导入 `sql/ry_20260417.sql`、`sql/quartz.sql` 到 `ry-vue`
4. 启动 MySQL / Redis，后端 8080 起来
5. 前端 `npm run dev`，登录验证
6. 新建 `ruoyi-oa` 模块骨架 + 菜单 + 占位页
7. 确认项目名（暂定 **FlowOA**）
