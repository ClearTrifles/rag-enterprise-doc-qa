package com.enterprise.rag.application.service.impl;

import lombok.experimental.UtilityClass;

/**
 * 问答Prompt配置类
 * <p>
 * 包含两套Prompt模板：
 * 1. RAG专用Prompt：强约束，必须依托参考文档，杜绝幻觉，标注来源
 * 2. 通用问答Prompt：无知识库限制，自然对话回答各类问题
 * </p>
 *
 * @author Enterprise RAG Team
 * @version 1.0.0
 */
@UtilityClass
public class ChatPromptConfig {

    /**
     * RAG专用Prompt（知识库问答）
     * <p>
     * 特点：
     * 1. 强约束，必须依托参考文档作答
     * 2. 禁止编造幻觉
     * 3. 末尾标注引用来源
     * </p>
     */
    public static final String RAG_PROMPT_TEMPLATE = """
            你是一个专业的文档问答助手。请根据以下提供的参考文档内容，回答用户的问题。

            参考文档：
            {context}

            请遵循以下规则：
            1. 只根据提供的参考文档内容进行回答，不要添加任何外部知识
            2. 如果参考文档中没有相关内容，请明确告知用户
            3. 回答要简洁明了，不要冗长
            4. 如果用户的问题与参考文档无关，请说明这一点
            5. 在回答的末尾，可以标注参考来源（如"根据《文档名称》"）

            用户问题：
            {question}
            """;

    /**
     * 通用问答Prompt（无知识库限制）
     * <p>
     * 特点：
     * 1. 无知识库限制，自然对话回答
     * 2. 可以回答闲聊、常识、外部资讯类问题
     * 3. 回答友好、专业
     * </p>
     */
    public static final String GENERAL_PROMPT_TEMPLATE = """
            你是一个友好的AI助手，可以帮助用户回答各类问题。

            请遵循以下规则：
            1. 回答要友好、专业、简洁
            2. 如果不确定答案，可以诚实告知用户
            3. 对于需要外部资讯的问题（如天气、新闻等），可以说明你需要最新数据
            4. 避免生成有害、虚假或误导性内容

            用户问题：
            {question}
            """;

    /**
     * 知识库未命中提示文案
     */
    public static final String NO_MATCH_PROMPT_TIP = "未在内部私有知识库查询到该问题相关资料，请问您是否需要AI基于通用公开知识为您解答？";

    /**
     * 构建RAG Prompt
     *
     * @param context 上下文（检索到的文档内容）
     * @param question 问题
     * @return 格式化后的Prompt
     */
    public static String buildRAGPrompt(String context, String question) {
        return RAG_PROMPT_TEMPLATE
                .replace("{context}", context)
                .replace("{question}", question);
    }

    /**
     * 构建通用问答Prompt
     *
     * @param question 问题
     * @return 格式化后的Prompt
     */
    public static String buildGeneralPrompt(String question) {
        return GENERAL_PROMPT_TEMPLATE.replace("{question}", question);
    }
}
