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
### 3.接口API模块化拆分 新建api文件夹，按业务拆分：chatApi.ts、documentApi.ts、recordApi.ts、settingApi.ts，所有请求统一归类，禁止页面内直接写axios请求 
### 4.公共工具hooks封装 封装常用钩子：useLoading、useDebounce、useFileUpload、usePagination，全局复用 完成后输出 STATE_FRONT_2_COMPLETE 
## 交付物 
1. Axios完整封装类 
2. 全部Pinia状态模块 
3. 全业务API请求函数 
4. 通用hooks工具库
```

