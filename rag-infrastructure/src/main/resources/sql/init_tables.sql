-- ===========================================
-- 企业级RAG文档问答系统 - 建表SQL脚本
-- ===========================================
-- 版本: v1.0
-- 创建日期: 2026-06-18
-- 说明: 三张核心业务表，包含审计字段和业务索引
-- ===========================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS rag_doc_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE rag_doc_db;

-- ===========================================
-- 1. 文档表（rag_document）
-- ===========================================
DROP TABLE IF EXISTS rag_document;

CREATE TABLE rag_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    document_name VARCHAR(255) NOT NULL COMMENT '文档名称',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型(PDF/TXT/DOCX)',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    storage_path VARCHAR(500) NOT NULL COMMENT '本地存储路径',
    chunk_count INT DEFAULT 0 COMMENT '切片数量',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '文档状态(0-上传中 1-处理中 2-已启用 3-已禁用 4-处理失败)',
    create_by VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '逻辑删除标记(0-正常 1-删除)',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_document_name (document_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ===========================================
-- 2. 文档切片表（rag_document_chunk）
-- ===========================================
DROP TABLE IF EXISTS rag_document_chunk;

CREATE TABLE rag_document_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    document_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_index INT NOT NULL COMMENT '切片序号(从0开始)',
    chunk_content TEXT NOT NULL COMMENT '切片内容',
    vector_id VARCHAR(64) DEFAULT NULL COMMENT 'Chroma向量库ID',
    chunk_size INT NOT NULL COMMENT '切片长度(字符数)',
    similarity_score DECIMAL(5,4) DEFAULT 0.0000 COMMENT '相似度分值',
    create_by VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '逻辑删除标记(0-正常 1-删除)',
    PRIMARY KEY (id),
    INDEX idx_document_id (document_id),
    INDEX idx_vector_id (vector_id),
    INDEX idx_chunk_index (document_id, chunk_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档切片表';

-- ===========================================
-- 3. 问答记录表（rag_chat_record）
-- ===========================================
DROP TABLE IF EXISTS rag_chat_record;

CREATE TABLE rag_chat_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_question TEXT NOT NULL COMMENT '用户提问内容',
    ai_answer TEXT DEFAULT NULL COMMENT 'AI回答内容',
    referenced_chunks VARCHAR(500) DEFAULT NULL COMMENT '引用切片ID列表(JSON)',
    referenced_documents VARCHAR(500) DEFAULT NULL COMMENT '引用文档ID列表(JSON)',
    token_count INT DEFAULT 0 COMMENT '消耗Token数量',
    response_time INT DEFAULT 0 COMMENT '响应时间(毫秒)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '问答状态(0-待处理 1-成功 2-无匹配 3-已过滤 4-限流 5-失败)',
    create_by VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete TINYINT DEFAULT 0 COMMENT '逻辑删除标记(0-正常 1-删除)',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问答记录表';

-- ===========================================
-- 初始化测试数据（可选）
-- ===========================================
-- 插入测试文档
INSERT INTO rag_document (document_name, original_filename, file_type, file_size, storage_path, chunk_count, status)
VALUES ('测试文档1', 'test1.pdf', 'PDF', 1024000, '/data/documents/test1.pdf', 0, 2);

INSERT INTO rag_document (document_name, original_filename, file_type, file_size, storage_path, chunk_count, status)
VALUES ('测试文档2', 'test2.txt', 'TXT', 512000, '/data/documents/test2.txt', 0, 2);

-- 插入测试切片
INSERT INTO rag_document_chunk (document_id, chunk_index, chunk_content, vector_id, chunk_size, similarity_score)
VALUES (1, 0, '这是测试切片内容1，用于验证向量召回功能。', 'vec_001', 50, 0.8500);

INSERT INTO rag_document_chunk (document_id, chunk_index, chunk_content, vector_id, chunk_size, similarity_score)
VALUES (1, 1, '这是测试切片内容2，用于验证向量召回功能。', 'vec_002', 50, 0.9000);

-- 插入测试问答记录
INSERT INTO rag_chat_record (session_id, user_question, ai_answer, referenced_chunks, referenced_documents, token_count, response_time, status)
VALUES ('session_001', '什么是RAG架构？', 'RAG是检索增强生成架构...', '[1,2]', '[1]', 150, 2000, 1);

-- ===========================================
-- 完成建表
-- ===========================================
SELECT '数据库初始化完成！' AS message;