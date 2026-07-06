# 企业级 RAG 文档问答系统 前端整体规划方案

# 企业级 RAG 文档问答系统 前端整体规划方案（对齐后端状态流、一步到位架构、可长期迭代不重构）

## 前置总纲（对齐你后端整套规范）

1. **后端现状**：SpringBoot3 多模块、RESTful \+ SpringDoc、Chroma 本地向量库、通义千问 `qwen3.6-flash`、环境变量密钥、统一 `Result` 返回体、全局异常、限流 / 熔断 / 异步 RAG 链路

2. **前端设计原则**

    - **一步到位架构**：模块化、路由权限隔离、全局状态、请求拦截封装、样式主题、错误兜底、多页面预留路由，后续新增功能只增页面，不重构底层

    - **前后端强兼容**：接口格式、入参出参、分页结构、错误码体系完全对齐后端

    - **本地运行即可**：不用域名、不用云服务器、仅本地前后端联调，后续打包部署 Nginx 零改造

    - **企业级规范**：ESLint \+ Prettier、Git 提交规范、环境分配置、全局异常捕获、埋点、防抖节流、文件上传分片、大文件断点续传预留

3. **选定技术栈（成熟稳定、中厂主流、长期维护、适配 Java 后端管理系统）**

### 前端最终定型技术栈（不可随意改动，写入 `.trae/global-rule.md` 约束）

```Plain Text
框架：Vue3 + TypeScript（强类型，避免后期逻辑混乱，便于长期迭代）
构建工具：Vite5（启动快、打包高效）
UI组件库：Element Plus（后端管理系统行业标配，生态完善，表单/上传/弹窗/表格开箱即用）
路由：Vue Router 4
状态管理：Pinia（替代Vuex，轻量化易维护，全局用户配置、问答全局缓存、全局加载状态统一管理）
网络请求：Axios 二次深度封装（请求拦截、响应拦截、统一错误码处理、token占位预留、重复请求取消、超时重试，对齐后端全局异常）
样式方案：SCSS + CSS变量全局主题配置（一键换色、统一间距/字号规范）
工具库：
  - lodash-es：防抖、节流、数据处理
  - nprogress：顶部进度条
  - js-md5：文件哈希（用于后端文件去重校验）
  - xlsx：后续支持问答记录导出Excel
  - pdfjs-dist：前端PDF预览（不依赖后端预览接口）
代码规范：ESLint + Prettier
环境配置：.env.development / .env.production 多环境区分
```

4. **前后端约定契约（提前定死，杜绝后期联调改结构）**

- 后端基础前缀：`/api/v1`

- 统一响应结构（前端全局拦截适配）

```json
{
  "code": 数字,
  "msg": "提示信息",
  "data": 任意业务对象,
  "success": true/false
}
```

- 分页统一结构体、文件上传入参、问答提交入参、向量查询出参提前对齐

- 后端错误码文档同步给前端，统一弹窗提示逻辑

---

# 一、新增前端专属状态流（承接后端 STATE\_6 完结，新增 STATE\_FRONT\_0 \~ STATE\_FRONT\_5）

沿用原有 Trae Skill 管控模式，新建前端专属 Skill 存放目录，严格串行执行、人工卡点核验、完成输出状态码，和后端管控逻辑完全一致。

## 目录整体扩容（项目根目录 `rag-enterprise-doc-qa`）

```Plain Text
rag-enterprise-doc-qa/
├── .trae/
│   ├── global-rule.md
│   └── skills/
│       ├── 00-init-skill.md
│       ├── ...原有7个后端skill
│       ├── front-00-init-skill.md        # 前端初始化
│       ├── front-01-arch-skill.md       # 前端架构&路由规划
│       ├── front-02-request-skill.md    # 请求封装&全局拦截
│       ├── front-03-page-dev-skill.md   # 页面完整开发
│       ├── front-04-opt-skill.md        # 交互优化&边界兼容
│       └── front-05-spec-skill.md       # 规范整理&打包部署方案
├── docs/
│   ├── 01-需求文档
│   ├── 02-架构文档
│   ├── 03-数据库设计
│   ├── 04-前后端接口契约文档【新增】
│   └── 05-面试话术
├── rag-parent/           # 原有后端多模块
├── rag-front/            # 【新建前端根目录】
├── temp-file/
└── README.md
```

## 修改 `.trae/global-rule.md` 追加前端约束（末尾粘贴）

```markdown
## 7. 前端项目强制开发约束
1. 前端技术栈固定：Vue3 + TypeScript + Vite5 + Element Plus + Pinia + VueRouter4，禁止更换框架
2. 架构采用分层解耦设计，全局请求、路由、状态、样式、工具完全抽离，满足一步到位长期迭代，禁止零散式页面代码堆砌
3. 严格对齐后端/api/v1接口前缀、统一Result返回体、后端错误码体系，前后端契约先行
4. 内置开发/生产双环境配置，本地联调后端地址可配置，后续打包部署Nginx无需大规模修改
5. 预留登录、权限、菜单动态渲染骨架，当前版本可先隐藏，后续做RBAC迭代无需重构路由架构
6. 文件上传逻辑兼容普通上传、预留分片上传、断点续传架构，适配后端大文件解析防OOM设计
7. 所有交互增加防抖、节流、loading、异常兜底、空状态、加载状态、错误重试，符合企业后台UI交互标准
8. 必须配套ESLint+Prettier代码规范，产出README、环境说明、打包部署文档
9. 前端开发遵循独立状态流：STATE_FRONT_0 → STATE_FRONT_1 → STATE_FRONT_2 → STATE_FRONT_3 → STATE_FRONT_4 → STATE_FRONT_5，不可跨步骤开发，完成输出对应状态码
```

---

# 二、6 份前端专属 Skill 完整内容 \+ Trae 执行提示词 \+ 人工核验卡点

## STATE\_FRONT\_0：前端项目初始化 \[front\-00\-init\-skill\.md\]\(front\-00\-init\-skill\.md\)

```markdown
## Skill元数据
skill_name: 前端项目初始化技能
skill_state: STATE_FRONT_0
skill_type: 前端基建初始化
version: 1.0
description: 初始化Vue3+TS前端项目，安装固定依赖，配置ESLint、Prettier、环境变量文件，搭建基础目录骨架，对齐后端项目规范

## 强制执行指令
1. 在项目根目录新建 rag-front 文件夹，Vite创建Vue3+TS项目，安装预设全套依赖
2. 自动配置 .env.development、.env.production，配置后端基础地址 VITE_API_BASE_URL = http://localhost:8080/api/v1
3. 配置 ESLint + Prettier，统一代码格式化规则，配置.gitignore忽略前端产物、依赖包、环境密钥文件
4. 规划前端标准分层目录结构，抽离api、stores、router、utils、hooks、styles、components公共文件夹
5. 编写 rag-front/README.md，记录启动命令、依赖说明、联调地址
6. 初始化完成输出 STATE_FRONT_0_COMPLETE

## 交付物
1. rag-front 完整初始化项目
2. 依赖清单、格式化配置文件
3. 环境配置文件
4. 前端启动说明文档
```

### Trae 执行提示词

```Plain Text
加载.trae/skills/front-00-init-skill.md技能，执行STATE_FRONT_0前端项目初始化，创建rag-front前端工程，安装约定技术栈依赖，配置代码规范与多环境配置，搭建标准目录结构，完成后输出STATE_FRONT_0_COMPLETE，等待我人工核验
```

### 核验卡点

✅ 目录分层清晰、无混乱代码
✅ 开发环境后端接口地址配置正确
✅ ESLint 格式化可正常校验代码
✅ 前端可独立 `npm run dev` 启动空白首页

---

## STATE\_FRONT\_1：前端架构 \+ 路由设计 \+ 前后端接口契约 \[front\-01\-arch\-skill\.md\]\(front\-01\-arch\-skill\.md\)

```markdown
## Skill元数据
skill_name: 前端架构&路由&接口契约设计技能
skill_state: STATE_FRONT_1
skill_type: 架构设计技能
version: 1.0
description: 设计整体页面布局、路由结构、菜单规划，输出前后端接口对照表，预留迭代扩展路由，实现一步到位路由架构

## 页面整体规划（当前版本必做页面）
1. 首页（智能问答对话页）：聊天对话框、提问输入框、引用文档溯源展示、清空会话、历史问答切换
2. 知识库管理页：文档列表、上传文件按钮、启用/禁用/删除文档、批量向量化重试
3. 问答记录管理页：分页查询历史问答、查看详情、导出记录
4. 个人设置页：模型参数配置（相似度阈值、召回条数、切片参数前端可视化配置）

## 预留扩展路由（写进路由配置，隐藏菜单，后续迭代直接启用，避免重构）
- 用户登录页
- 用户权限管理页
- 系统操作日志页
- 知识库分类管理页
- 系统参数配置中心页

## 强制架构约束
1. 布局采用 Layout 布局组件（侧边菜单 + 顶部导航 + 内容区域）全局复用
2. VueRouter 配置路由守卫骨架（预留登录校验逻辑，当前临时放行）
3. 在docs目录产出《前后端接口契约文档.md》，整理全部请求地址、入参、出参、分页结构、错误码对照表
4. 路由拆分模块化，避免单一路由文件臃肿
5. 完成输出 STATE_FRONT_1_COMPLETE

## 交付物
1. 全局Layout布局组件、路由完整配置
2. 菜单配置文件、路由守卫基础代码
3. docs/前后端接口契约文档.md
```

### Trae 执行提示词

```Plain Text
加载.trae/skills/front-01-arch-skill.md技能，承接前端初始化成果，完成整体布局、路由架构设计，划分现有页面与预留迭代路由，编写前后端完整接口契约文档，路由模块化拆分，完成后输出STATE_FRONT_1_COMPLETE等待核验
```

### 核验卡点

✅ 路由分层、预留后续权限扩展，无需重构
✅ 接口契约和后端 SpringDoc 完全匹配
✅ Layout 全局布局复用成型

---

## STATE\_FRONT\_2：Axios 深度封装 \+ 全局拦截 \+ Pinia 全局状态 \[front\-02\-request\-skill\.md\]\(front\-02\-request\-skill\.md\)

```markdown
## Skill元数据
skill_name: 网络请求&全局状态封装技能
skill_state: STATE_FRONT_2
skill_type: 前端基建编码
version: 1.0
description: 完成Axios二次封装、请求/响应拦截、错误统一处理、全局Pinia状态管理，是整个前端底层核心，一步到位不再改动

## 强制封装需求
### 1.Axios拦截器
1. 请求拦截：统一添加请求头、开启loading、防抖防重复请求
2. 响应拦截：自动解析后端Result结构，统一处理业务成功逻辑
3. 全局错误统一处理：
   - 网络超时、跨域、服务不可用统一提示
   - 后端自定义错误码弹窗提示
   - 全局异常兜底弹窗，防止页面崩溃
4. 配置请求超时、重复请求取消、重试机制，对齐后端熔断设计

### 2.Pinia全局状态拆分（解耦，便于迭代）
- appStore：全局loading、主题、页面布局配置、全局弹窗控制
- chatStore：当前会话内容、问答缓存、会话列表
- documentStore：知识库全局缓存、文档全局状态
- userStore：预留登录、token、用户信息（当前占位，后续迭代启用）

### 3.接口API模块化拆分
新建api文件夹，按业务拆分：chatApi.ts、documentApi.ts、recordApi.ts、settingApi.ts，所有请求统一归类，禁止页面内直接写axios请求

### 4.公共工具hooks封装
封装常用钩子：useLoading、useDebounce、useFileUpload、usePagination，全局复用

完成后输出 STATE_FRONT_2_COMPLETE
## 交付物
1. Axios完整封装类
2. 全部Pinia状态模块
3. 全业务API请求函数
4. 通用hooks工具库
```

### Trae 执行提示词

```Plain Text
加载.trae/skills/front-02-request-skill.md技能，完成Axios深度封装、全局拦截、错误统一处理，搭建模块化Pinia全局状态，拆分所有业务接口API，封装通用业务hooks，底层架构一次性定型，后续业务页面直接调用，完成输出STATE_FRONT_2_COMPLETE
```

### 核验卡点

✅ 页面无需处理判断 code，拦截器自动封装成功 / 失败逻辑
✅ 接口全部统一归口管理，无散落 axios 代码
✅ 全局 loading、重复请求拦截生效

---

## STATE\_FRONT\_3：四大页面完整业务开发 \[front\-03\-page\-dev\-skill\.md\]\(front\-03\-page\-dev\-skill\.md\)

```markdown
## Skill元数据
skill_name: 前端业务页面开发技能
skill_state: STATE_FRONT_3
skill_type: 业务编码
version: 1.0
description: 基于已封装底层，完整开发全部4个核心页面，交互完善，一步到位满足企业使用，预留扩展点位

## 页面详细开发要求
### 页面1：智能问答主页（核心）
1. 对话气泡布局，区分用户提问、AI回答
2. AI回答自动展示引用文档来源、切片段落溯源
3. 输入框防抖、超长文本限制、回车发送、清空会话
4. 思考中loading动画、回答失败重试按钮
5. 会话列表侧边栏，新建会话、删除历史会话
6. 适配后端RAG全链路接口：提问接口、会话历史接口

### 页面2：知识库管理
1. 文档表格分页展示：名称、大小、上传时间、状态、操作
2. 文件上传组件：支持多选PDF/TXT/Word、格式校验、大小校验、上传进度条
3. 单个/批量删除文档（调用后端联动清理向量接口）
4. 启用/禁用文档开关、批量重新向量化按钮
5. 前端PDF在线预览弹窗

### 页面3：问答记录管理
1. 分页查询、条件筛选（时间范围、关键词搜索）
2. 查看问答详情弹窗
3. 批量导出Excel问答记录
4. 单条删除、批量清理无用记录

### 页面4：系统参数配置页
1. 可视化修改RAG参数：切片大小、重叠值、相似度阈值、召回TopN数量
2. 提交保存到后端配置接口，实时生效
3. 模型切换下拉（当前通义千问，预留DeepSeek切换选项，前端UI提前做好）

## 交互硬性规范
1. 所有异步操作增加loading、禁用重复点击
2. 空数据状态、加载中、异常错误占位图
3. 删除、批量操作增加二次确认弹窗，防止误操作
4. 表单校验、参数合法性校验
完成输出 STATE_FRONT_3_COMPLETE

## 交付物
四个完整页面代码，联调后端全部接口可跑通
```

### Trae 执行提示词

```Plain Text
加载.trae/skills/front-03-page-dev-skill.md技能，基于已完成底层封装，完整开发问答首页、知识库管理、问答记录、系统配置四大页面，交互补齐各类边界状态，对接全部后端接口，本地前后端联调可完整跑通RAG问答全流程，完成输出STATE_FRONT_3_COMPLETE
```

### 核验卡点

✅ 上传、问答、删除、参数配置全流程通调成功
✅ 各种空状态、加载、异常交互齐全
✅ 代码复用度高，无重复冗余逻辑

---

## STATE\_FRONT\_4：交互深度优化、兼容性、边界问题完善 \[front\-04\-opt\-skill\.md\]\(front\-04\-opt\-skill\.md\)

```markdown
## Skill元数据
skill_name: 前端优化&边界兼容技能
skill_state: STATE_FRONT_4
skill_type: 优化迭代
version: 1.0
description: 补齐企业级细节，解决边缘场景，做健壮性加固，实现版本一步到位，减少后期重构

## 优化清单
1. 大文件上传优化：前端预留分片上传逻辑（UI+封装函数写好，后端后续迭代开发对应接口即可）
2. 聊天窗口滚动自动到底、长文本折叠/展开
3. 浏览器本地缓存：会话记录本地持久化，刷新页面不丢失对话
4. 移动端基础自适应布局（后台常用最小适配）
5. 全局全局错误捕获 window.onerror、未捕获Promise异常兜底，防止页面白屏崩溃
6. 增加页面访问埋点函数封装（预留统计能力）
7. 清理冗余变量、消除TS类型警告、补齐全部接口类型定义（interface严格对齐后端VO/DTO）
8. 全局SCSS主题变量统一整理，统一字号、间距、圆角、配色规范

## 性能优化
1. 路由懒加载配置
2. 大列表虚拟滚动预留方案（文档量大后直接启用）
3. 图片/资源懒加载配置
完成输出 STATE_FRONT_4_COMPLETE

## 交付物
优化后完整前端工程、完善TS类型定义、健壮性加固代码
```

### Trae 执行提示词

```Plain Text
加载.trae/skills/front-04-opt-skill.md技能，对前端项目做企业级健壮性优化，补齐边界场景、TS类型完善、本地缓存、异常兜底、性能优化，预留分片上传、虚拟滚动扩展能力，保证当前版本长期稳定无需大规模重构，完成输出STATE_FRONT_4_COMPLETE
```

### 核验卡点

✅ TS 无类型警告，前后端数据类型一一对应
✅ 异常场景不会页面卡死
✅ 预留扩展能力全部预埋完成

---

## STATE\_FRONT\_5：文档整理、打包部署方案、面试包装 \[front\-05\-spec\-skill\.md\]\(front\-05\-spec\-skill\.md\)

```markdown
## Skill元数据
skill_name: 前端收尾&部署&面试包装技能
skill_state: STATE_FRONT_5
skill_type: 复盘归档
version: 1.0
description: 整理全套前端文档、打包配置、前后端联调部署方案、面试讲解亮点，整体项目闭环

## 工作内容
1. 完善 rag-front/README.md
   - 环境依赖、启动步骤、联调配置修改方法
   - 目录结构说明、架构设计说明
   - 打包生产命令、部署方案
2. 编写 Nginx 部署配置示例（后续上云直接使用，本地不用）
3. 整理前端亮点文档（写入docs），用于面试口述
4. 整理已知优化点、后续迭代规划（登录权限、分类管理、分片上传、DeepSeek模型切换等）
5. 检查ESLint全部告警修复，代码风格统一
完成最终状态码 PROJECT_FRONT_ALL_FINISH

## 交付物
1. 完整前端项目最终版
2. 前端部署文档、README
3. 前端面试亮点说明文档
```

### Trae 执行提示词

```Plain Text
加载.trae/skills/front-05-spec-skill.md技能，完成前端项目收尾整理，完善项目说明文档、打包部署方案、面试亮点总结，修复全部代码规范告警，整体前端版本定型可长期迭代，输出PROJECT_FRONT_ALL_FINISH
```

### 核验卡点

✅ 文档齐全，新人可快速看懂架构
✅ 生产打包配置无误
✅ 有明确迭代路线，证明设计前瞻性

---

# 三、「一步到位防重构」核心设计亮点（面试重点，提前预埋）

1. **路由架构预埋权限体系**
当前只是放行路由守卫，但菜单、路由表抽离配置文件，后续做 RBAC 账号体系，只新增接口、修改守卫逻辑，页面结构完全不动。

2. **接口全部模块化抽离**
不会出现页面散写 axios，新增功能只新增 api 文件，页面逻辑清爽。

3. **Pinia 模块化拆分全局状态**
避免全局一个大 store 臃肿，后期新增业务模块新增 store 即可，不改动原有逻辑。

4. **模型切换 UI 提前预留**
设置页内置模型下拉选项，后端切换 DeepSeek 仅改配置与接口地址，前端页面零改动。

5. **大文件分片上传骨架预埋**
前端封装好分片上传函数、UI 交互，后端后续迭代开发分片接口，前端不用重写上传页面。

6. **TS 严格类型约束前后端结构体**
所有入参、出参提前定义 Interface，对接后端 DTO/VO，后期接口字段调整编译器直接报错，减少联调 Bug。

7. **全局异常、请求拦截统一收口**
后期新增业务接口，不用重复处理报错、loading、重复请求。

# 四、前后端本地联调最简方式（当前不用 Nginx）

1. 后端启动端口：`8080`

2. 前端开发环境代理配置，Vite 配置跨域反向代理，避免跨域报错

3. 前端环境变量：`VITE_API_BASE_URL=/api/v1`，Vite 代理转发到 `http://localhost:8080/api/v1`

4. 前后端各自独立启动，完全本地运行，满足你不上云的要求

# 五、整体推进节奏预估（AI 辅助 Trae 快速开发）

1. 1 天：STATE\_FRONT\_0 \+ STATE\_FRONT\_1 初始化 \+ 架构契约

2. 1\~1\.5 天：STATE\_FRONT\_2 底层请求 \+ 状态封装（核心底层定型）

3. 2\~2\.5 天：STATE\_FRONT\_3 四大页面完整开发联调

4. 1 天：STATE\_FRONT\_4 优化加固、边界补齐

5. 半天：STATE\_FRONT\_5 文档收尾、打包方案整理
**总周期：5\~6 天完成高质量企业级前端，一步到位，长期迭代无需重构**

需要我现在直接给你：

1. 前端 6 个 \[skill\.md\]\(skill\.md\) 完整文本，你一键粘贴放入 `.trae/skills/`

2. 第一条启动前端初始化的完整发送指令，你直接复制丢 Trae 开始开发吗？

> （注：部分内容可能由 AI 生成）
