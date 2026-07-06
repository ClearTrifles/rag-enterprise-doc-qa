```markdown
## Skill元数据 
skill_name: 前端适配后端RAG模式C应答结构改造技能 
skill_state: FRONT_ADAPT_RAG_MODE_C 
skill_type: 前端业务适配改造 
version: 1.0 
description: 对齐后端新ChatResponse三层应答类型，改造聊天页面逻辑，实现：知识库命中直接展示回答；无匹配弹出确认框选择是否启用通用问答；确认后调用/generalAsk接口获取闲聊答案，整体不改动前端底层架构、Pinia、Axios封装、原有页面布局，适配已规划一步到位前端架构 
## 前置强制约束 
1. 严格遵守.trae/global-rule.md前端全部开发约束，技术栈Vue3+TS+Vite5+ElementPlus+Pinia不变 
2. 不重构Axios拦截器、全局状态、路由、Layout布局，仅修改聊天业务逻辑、类型定义、接口调用 
3. 对齐后端两个接口：   POST /api/v1/chat/ask  主问答入口   POST /api/v1/chat/generalAsk 用户确认后通用问答入口 
4. TS类型严格对齐后端ChatResponse、枚举replyType三种取值，杜绝类型警告 
5. 交互符合原有企业级交互规范：loading、防抖、重复请求拦截、二次确认、空状态处理 
6. 改造完成输出状态码：FRONT_ADAPT_RAG_MODE_C_COMPLETE，等待人工核验 
## 分步改造要求 
### 1、补充全局TS类型定义 src/types/chat.ts 
定义枚举字符串类型别名： type ChatReplyType = 'RAG_ANSWER' | 'NEED_CONFIRM_GENERAL' | 'GENERAL_ANSWER' 
定义完整接口结构： interface ChatReferenceItem {  
	docId: string  
	docName: string  
	chunkContent: string 
} 
interface ChatResponse {  
	replyType: ChatReplyType  
	question: string  
	answer: string  
	referenceList: ChatReferenceItem[]  
	promptTip: string 
} 
保证全局接口返回类型统一，匹配后端DTO 
### 2、更新 src/api/chatApi.ts 
1. 修改原有 sendQuestion 接口，返回 Promise<Result<ChatResponse>> 
2. 新增函数 generalAskQuestion(question:string) 调用 /generalAsk 接口 
3. 请求入参、返回类型全部标注TS，禁止any类型 
### 3、改造聊天页面核心业务逻辑（问答首页） 
原有单一接收回答逻辑改为分支判断： 
1. 调用 /ask 拿到返回 res.data 
2. 分支一：replyType === 'RAG_ANSWER'   
- 正常渲染回答气泡，遍历 referenceList 展示引用溯源文档 
3. 分支二：replyType === 'NEED_CONFIRM_GENERAL'   
- 立刻弹出 ElementPlus MessageBox 确认弹窗，提示文案取自 promptTip   
- 【确定】：调用 generalAskQuestion，拿到结果后以 replyType=GENERAL_ANSWER 渲染对话   
- 【取消】：终止流程，不新增AI回答气泡 
4. 分支三：replyType === 'GENERAL_ANSWER'   
- 直接渲染普通回答气泡，不展示文档溯源 
### 4、适配Pinia chatStore 
1. 存储结构兼容三种应答类型数据 
2. 会话本地持久化存储完整ChatResponse结构，刷新页面类型不丢失 
3. 新建会话、清空会话逻辑不受改动影响 
### 5、交互细节补齐 
1. 弹窗居中、取消/确认文案适配业务语义 
2. 发起请求全程loading，防止重复点击多发请求 
3. 异常捕获：通用问答接口报错统一全局提示 
4. 长文本、引用列表样式保持原有设计风格，不整体改版UI 
### 6、自检要求 
1. ESLint无告警、TS无类型错误 
2. 前后端联调三种场景均可闭环跑通   
① 知识库命中回答 
② 无匹配弹窗询问 
③ 用户同意后通用闲聊回答 
3. 知识库管理、问答记录、系统配置页面代码完全不受本次修改影响 
## 交付完成标识 全部代码修改、类型补齐、逻辑调试完毕，输出：FRONT_ADAPT_RAG_MODE_C_COMPLETE
```

