package com.enterprise.rag.common.constant;

/**
 * RAG系统常量类
 * <p>
 * 定义系统中使用的固定常量值，包括文件类型、切片策略、状态标识等。
 * 注意：密钥、阈值等配置信息请使用RagProperties配置类，禁止在此硬编码。
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
public final class RagConstants {

    /**
     * 私有构造方法，防止实例化
     */
    private RagConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ==================== 文件相关常量 ====================

    /**
     * 支持的文件类型 - PDF
     */
    public static final String FILE_TYPE_PDF = "pdf";

    /**
     * 支持的文件类型 - Word文档
     */
    public static final String FILE_TYPE_DOCX = "docx";

    /**
     * 支持的文件类型 - Word旧版
     */
    public static final String FILE_TYPE_DOC = "doc";

    /**
     * 支持的文件类型 - Excel
     */
    public static final String FILE_TYPE_XLSX = "xlsx";

    /**
     * 支持的文件类型 - Excel旧版
     */
    public static final String FILE_TYPE_XLS = "xls";

    /**
     * 支持的文件类型 - 文本文件
     */
    public static final String FILE_TYPE_TXT = "txt";

    /**
     * 支持的文件类型 - Markdown
     */
    public static final String FILE_TYPE_MD = "md";

    /**
     * 文件存储路径分隔符
     */
    public static final String FILE_PATH_SEPARATOR = "/";

    // ==================== 切片相关常量 ====================

    /**
     * 切片策略 - 按固定字符数切片
     */
    public static final String CHUNK_STRATEGY_FIXED_SIZE = "fixed_size";

    /**
     * 切片策略 - 按段落切片
     */
    public static final String CHUNK_STRATEGY_PARAGRAPH = "paragraph";

    /**
     * 切片策略 - 按语义切片
     */
    public static final String CHUNK_STRATEGY_SEMANTIC = "semantic";

    /**
     * 切片策略 - 按句子切片
     */
    public static final String CHUNK_STRATEGY_SENTENCE = "sentence";

    /**
     * 切片重叠字符数默认值（实际值从配置读取）
     */
    public static final int CHUNK_OVERLAP_DEFAULT = 200;

    // ==================== 向量相关常量 ====================

    /**
     * 向量维度 - Jina AI Embedding
     */
    public static final int VECTOR_DIMENSION_JINA = 768;

    /**
     * 向量维度 - OpenAI Embedding (text-embedding-v3为1024维)
     */
    public static final int VECTOR_DIMENSION_OPENAI = 1024;

    /**
     * 向量相似度计算方法 - 余弦相似度
     */
    public static final String SIMILARITY_METHOD_COSINE = "cosine";

    /**
     * 向量相似度计算方法 - 欧氏距离
     */
    public static final String SIMILARITY_METHOD_EUCLIDEAN = "euclidean";

    // ==================== AI模型相关常量 ====================

    /**
     * AI模型类型 - 通义千问
     */
    public static final String AI_MODEL_QWEN = "qwen";

    /**
     * AI模型类型 - OpenAI
     */
    public static final String AI_MODEL_OPENAI = "openai";

    /**
     * AI模型类型 - 本地模型
     */
    public static final String AI_MODEL_LOCAL = "local";

    /**
     * Embedding模型类型 - Jina AI
     */
    public static final String EMBEDDING_MODEL_JINA = "jina";

    /**
     * Embedding模型类型 - OpenAI
     */
    public static final String EMBEDDING_MODEL_OPENAI = "openai-embedding";

    // ==================== 数据库相关常量 ====================

    /**
     * 向量数据库类型 - ChromaDB
     */
    public static final String VECTOR_DB_CHROMA = "chromadb";

    /**
     * 向量数据库类型 - Milvus
     */
    public static final String VECTOR_DB_MILVUS = "milvus";

    /**
     * 向量数据库类型 - Pinecone
     */
    public static final String VECTOR_DB_PINECONE = "pinecone";

    /**
     * 向量集合名称 - 文档向量
     */
    public static final String COLLECTION_NAME_DOCUMENTS = "rag_documents";

    /**
     * 向量集合名称 - 文档切片向量
     */
    public static final String COLLECTION_NAME_CHUNKS = "rag_chunks";

    // ==================== 状态相关常量 ====================

    /**
     * 状态 - 正常
     */
    public static final Integer STATUS_NORMAL = 1;

    /**
     * 状态 - 禁用
     */
    public static final Integer STATUS_DISABLED = 0;

    /**
     * 状态 - 删除
     */
    public static final Integer STATUS_DELETED = -1;

    /**
     * 是否标识 - 是
     */
    public static final Boolean YES = true;

    /**
     * 是否标识 - 否
     */
    public static final Boolean NO = false;

    // ==================== 分页相关常量 ====================

    /**
     * 默认页码
     */
    public static final Integer DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页大小
     */
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大每页大小
     */
    public static final Integer MAX_PAGE_SIZE = 100;

    // ==================== 缓存相关常量 ====================

    /**
     * 缓存键前缀 - 文档
     */
    public static final String CACHE_KEY_DOCUMENT_PREFIX = "rag:document:";

    /**
     * 缓存键前缀 - 切片
     */
    public static final String CACHE_KEY_CHUNK_PREFIX = "rag:chunk:";

    /**
     * 缓存键前缀 - 对话
     */
    public static final String CACHE_KEY_CHAT_PREFIX = "rag:chat:";

    /**
     * 缓存键前缀 - 向量
     */
    public static final String CACHE_KEY_VECTOR_PREFIX = "rag:vector:";

    /**
     * 分布式锁键前缀
     */
    public static final String LOCK_KEY_PREFIX = "rag:lock:";

    // ==================== 时间相关常量 ====================

    /**
     * 一天的毫秒数
     */
    public static final Long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L;

    /**
     * 一小时的毫秒数
     */
    public static final Long ONE_HOUR_MILLIS = 60 * 60 * 1000L;

    /**
     * 一分钟的毫秒数
     */
    public static final Long ONE_MINUTE_MILLIS = 60 * 1000L;

    // ==================== 其他常量 ====================

    /**
     * 系统名称
     */
    public static final String SYSTEM_NAME = "RAG Enterprise Doc QA";

    /**
     * 系统版本
     */
    public static final String SYSTEM_VERSION = "1.0.0";

    /**
     * 默认字符编码
     */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * JSON内容类型
     */
    public static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * 文件上传内容类型
     */
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
}