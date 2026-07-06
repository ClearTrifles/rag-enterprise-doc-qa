## Skill元数据
skill_name: 数据库设计技能
skill_state: STATE_3_DB
skill_type: 数据设计技能
version: 1.0
description: 三表极简设计，适配RAG全业务，带索引、字段注释、审计字段，符合企业MySQL规范

## 强制库表规则
### 固定三张业务表（不冗余建表）
1. 文档表：rag_document（文档基础信息、状态、原始文件名、存储路径、创建时间、启用状态）
2. 文档切片表：rag_document_chunk（切片内容、向量关联ID、所属文档ID、相似度分值、切片序号）
3. 问答记录表：rag_chat_record（用户提问、AI回答、引用切片ID、消耗Token、问答时间、状态）
### 强制数据库规范
1. 所有表自带审计字段：create_by、create_time、update_by、update_time、is_delete逻辑删除
2. 业务查询字段建立B+索引，优化关联查询
3. 状态全部使用枚举类，禁止硬编码数字
4. 编写MySQL建表语句、MyBatis-Plus实体、DTO、枚举全量代码

## 本状态交付物
1. docs/03-数据库设计文档.md
2. 完整建表SQL脚本
3. 全量业务枚举、实体类代码
4. 完成后输出【STATE_3_COMPLETE】