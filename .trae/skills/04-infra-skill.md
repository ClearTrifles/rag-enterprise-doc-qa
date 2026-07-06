## Skill元数据
skill_name: 项目基建开发技能
skill_state: STATE_4_INFRA
skill_type: 基建编码技能
version: 1.0
description: 开发项目底层基建能力，公共模块全部完工，业务零耦合，直接供业务层调用

## 强制基建开发清单
### rag-common公共模块必做内容
1. 全局统一返回体Result、分页封装类
2. 全局自定义业务异常、异常枚举、全局异常拦截器
3. 统一日志切面、请求入参日志打印
4. 全局参数校验工具、常量类（AI密钥、阈值、切片常量）
### rag-core AI中台模块必做内容
1. 通义千问LLM/Embedding双接口统一封装SDK
2. 本地ChromaDB向量库工具类封装
3. 自定义业务隔离线程池配置类
4. Redis工具类、限流工具、防重工具类
### 全局配置
1. SpringDoc接口文档自动配置
2. MyBatis-Plus全局配置、自动填充审计字段
3. Resilience4j熔断、Guava重试全局配置

## 编码强制规则
1. 所有工具类添加单元测试
2. 配置化绑定yml，禁止代码硬编码密钥、阈值
3. 基建代码高可用、可复用，满足开闭原则

## 本状态交付物
1. 全模块基建代码完工
2. 基建单元测试用例
3. yml多环境配置（dev本地环境）
4. 完成后输出【STATE_4_COMPLETE】