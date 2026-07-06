package com.enterprise.rag.application.service;

import com.enterprise.rag.application.dto.QAAnswerResponse;
import com.enterprise.rag.application.dto.QAQuestionRequest;
import com.enterprise.rag.common.dto.ChatResponse;

/**
 * 问答服务接口
 */
public interface QAService {

    /**
     * 智能问答（完整RAG链路）
     *
     * 【面试优化点说明】
     * 1. 大模型返回超时自动降级，直接返回知识库原文兜底
     * 2. 向量召回阈值动态配置，支持后台微调
     * 3. 删除文档联动删除向量库脏数据，保证数据一致性
     * 4. Prompt模板解耦，配置化修改问答约束话术
     *
     * @param request 问答请求
     * @return 问答响应
     */
    QAAnswerResponse ask(QAQuestionRequest request);

    /**
     * 智能问答（模式C折中方案）
     * <p>
     * 检索匹配知识库则基于文档回答；无匹配则返回询问结构，
     * 前端弹窗确认后再调用通用大模型问答。
     * </p>
     *
     * @param request 问答请求
     * @return 模式C问答响应（包含应答类型）
     */
    ChatResponse askModeC(QAQuestionRequest request);

    /**
     * 通用自由问答（无知识库限制）
     * <p>
     * 跳过向量检索，直接使用大模型进行自然对话回答。
     * 适用于闲聊、常识、外部资讯类问题。
     * </p>
     *
     * @param request 问答请求
     * @return 通用问答响应
     */
    ChatResponse generalAsk(QAQuestionRequest request);

    /**
     * 获取问答历史
     *
     * @param sessionId 会话ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 问答历史列表
     */
    QAAnswerResponse getHistory(String sessionId, Integer pageNum, Integer pageSize);
}
