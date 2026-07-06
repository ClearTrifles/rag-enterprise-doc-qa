~~~markdown
## Skill元数据 
skill_name: rag-mode-c-transform-skill 
skill_state: RAG_TRANSFORM_MODE_C
skill_type: 需求升级与变更
version: 1.0
description: 改造原有RAG问答逻辑为模式C折中方案：检索匹配知识库则基于文档回答；无匹配则返回询问结构，前端弹窗确认后再调用通用大模型问答；不修改原有向量、线程池、熔断、数据库、环境变量配置 
 

## 一、全局前置强制约束（必须遵守，禁止改动） 
1. 严格读取项目 `.trae/global-rule.md` 全部约束，原有技术栈、多模块结构、包名、Chroma向量配置（1024维、切片512、重叠120、相似度阈值0.75、Top5召回）完全不变 
2. 通义千问模型固定 `qwen3.6-flash`，Embedding固定 `text-embedding-v3`，密钥读取系统环境变量 `aliQwen-api`，禁止写明文密钥 
3. 原有异步线程池、Resilience4j熔断、Guava重试、Redis限流、全局异常、统一Result返回体全部保留，不得删除重构 
4. 不新增数据库表、不修改原有建表结构、不引入云服务/OSS/独立向量库 
5. 仅修改问答链路、DTO、枚举、Controller、Prompt逻辑，其余业务代码不动 
6. 改造完成必须输出固定标识：`RAG_MODE_C_TRANSFORM_COMPLETE`，等待人工核验 
## 二、详细分步改造指令 
### 步骤1：rag-common 模块新增应答类型枚举 ChatReplyTypeEnum 
包路径：`com.enterprise.rag.common.enums` 枚举三个值： 
1. `RAG_ANSWER`：检索命中知识库，基于文档回答 
2. `NEED_CONFIRM_GENERAL`：未检索到知识库内容，需要前端弹窗询问是否开启通用问答 
3. `GENERAL_ANSWER`：用户确认后，大模型通用自由问答 
### 步骤2：重构ChatResponse返回DTO（rag-common模块） 
包路径：`com.enterprise.rag.common.dto` 新增/修改字段： 
- `replyType`：ChatReplyTypeEnum 应答类型 
- `question`：原始提问字符串 
- `answer`：回答内容（无答案为空字符串） 
- `referenceList`：切片溯源引用列表（存储文档ID、文档名称、切片内容） 
- `promptTip`：提示文案（仅未匹配知识库时填充询问话术） 外层依然使用项目原有 `Result<T>` 统一包装，接口兼容性不变 
### 步骤3：改造rag-core RAG编排核心业务逻辑 
原有逻辑：检索无匹配直接返回无信息 新分支完整逻辑： 
1. 用户问题 → 问题向量化 → Chroma相似度检索 
2. 分支A：存在≥阈值有效切片   
- 组装**RAG约束Prompt**：强制模型只能依赖参考文档作答、禁止编造幻觉、末尾标注引用来源   
- 调用通义千问生成答案   
- 封装返回 `replyType=RAG_ANSWER`，填充answer+referenceList，promptTip置空 
3. 分支B：无有效匹配切片   
- **不调用LLM、不消耗Token**   
- `replyType=NEED_CONFIRM_GENERAL`   
- promptTip固定文案：`未在内部私有知识库查询到该问题相关资料，请问您是否需要AI基于通用公开知识为您解答？`   
- answer、referenceList为空 
### 步骤4：新增通用问答接口能力 
1. 在ChatController新增接口：`POST /api/v1/chat/generalAsk`   
入参：question（提问内容）、confirmGeneral（布尔值固定true） 
2. 内部逻辑：跳过向量检索，组装**无约束通用Prompt**，允许大模型回答闲聊、常识、外部资讯类问题 
3. 返回 `replyType=GENERAL_ANSWER`，填充回答内容，无溯源列表 
### 步骤5：修改原有问答接口 /api/v1/chat/ask 
1. 适配新ChatResponse结构体返回 
2. SpringDoc接口注释同步更新，标注三种返回类型说明 
3. 入参校验、限流、防重逻辑保持原有不变 
### 步骤6：两套Prompt隔离定义（配置类/常量类统一管理） 
1. RAG专用Prompt：强约束，必须依托参考文档，杜绝幻觉，标注来源 
2. 通用问答Prompt：无知识库限制，自然对话回答各类问题 
### 步骤7：编译校验要求 
1. 全项目Maven clean+compile 无编译报错、无红色依赖、无类找不到异常 
2. 原有文档上传、切片、向量化、删除联动清理向量功能完全不受影响 
3. 原有问答历史入库逻辑正常兼容新返回结构 
## 三、改造后三种场景返回示例（用于自检） 
### 场景1：命中知识库 
```json {  "code": 200,  "success": true,  "msg": "操作成功",  "data": {    "replyType": "RAG_ANSWER",    "question": "内部制度相关问题",    "answer": "基于文档生成的回答...",    "referenceList": [...],    "promptTip": ""  } }
### 场景 2：无匹配，待用户确认
```json {"code": 200,"success": true,"msg": "操作成功","data": {
    "replyType": "NEED_CONFIRM_GENERAL",
    "question": "今天天气怎么样？",
    "answer": "",
    "referenceList": [],
    "promptTip": "未在内部私有知识库查询到该问题相关资料，请问您是否需要AI基于通用公开知识为您解答？" } } 
### 场景 3：用户确认通用问答
``` json {"code": 200, "success": true, "msg": "操作成功", "data": {
    "replyType": "GENERAL_ANSWER",
    "question": "吃了吗您？",
    "answer": "通用闲聊回答内容",
    "referenceList": [],
    "promptTip": "" } }
## 四、交付完成标识
全部代码改造、编译校验完成后，末尾输出：RAG_MODE_C_TRANSFORM_COMPLETE
~~~

