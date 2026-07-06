package com.enterprise.rag.api.controller;

import com.enterprise.rag.application.dto.QAAnswerResponse;
import com.enterprise.rag.application.dto.QAQuestionRequest;
import com.enterprise.rag.application.service.QAService;
import com.enterprise.rag.common.dto.ChatResponse;
import com.enterprise.rag.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 问答Controller
 *
 * RESTful接口规范：
 * - POST /api/qa/ask - 智能问答（模式A，原有RAG链路）
 * - POST /api/qa/ask/mode-c - 智能问答（模式C折中方案）
 * - POST /api/qa/general-ask - 通用自由问答
 * - GET /api/qa/history - 问答历史
 */
@Slf4j
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Tag(name = "智能问答", description = "基于RAG的智能问答接口")
public class QAController {

    private final QAService qaService;

    /**
     * 智能问答（模式A：原有RAG链路）
     * <p>
     * 检索命中知识库则基于文档回答，未命中则返回"无法从知识库中找到相关信息"。
     * </p>
     */
    @PostMapping("/ask")
    @Operation(
            summary = "智能问答（模式A）",
            description = "原有RAG链路：检索命中知识库则基于文档回答，未命中则直接返回无信息提示"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QAAnswerResponse.class)))
    })
    public Result<QAAnswerResponse> ask(
            @Valid @RequestBody QAQuestionRequest request) {

        log.info("接收到模式A问答请求: {}", request.getQuestion());

        QAAnswerResponse response = qaService.ask(request);
        return Result.success(response);
    }

    /**
     * 智能问答（模式C：折中方案）
     * <p>
     * 检索匹配知识库则基于文档回答；无匹配则返回询问结构，
     * 前端弹窗确认后再调用通用大模型问答。
     * </p>
     *
     * <h3>三种返回场景：</h3>
     * <ul>
     *   <li><b>RAG_ANSWER</b>：检索命中，基于文档回答</li>
     *   <li><b>NEED_CONFIRM_GENERAL</b>：未命中，返回询问弹窗提示</li>
     *   <li><b>GENERAL_ANSWER</b>：用户确认后，通用自由问答</li>
     * </ul>
     */
    @PostMapping("/ask/mode-c")
    @Operation(
            summary = "智能问答（模式C折中方案）",
            description = "检索匹配知识库则基于文档回答；无匹配则返回询问结构，前端弹窗确认后再调用通用大模型问答"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatResponse.class)))
    })
    public Result<ChatResponse> askModeC(
            @Valid @RequestBody QAQuestionRequest request) {

        log.info("接收到模式C问答请求: {}", request.getQuestion());

        ChatResponse response = qaService.askModeC(request);
        return Result.success(response);
    }

    /**
     * 通用自由问答
     * <p>
     * 跳过向量检索，直接使用大模型进行自然对话回答。
     * 适用于闲聊、常识、外部资讯类问题。
     * </p>
     */
    @PostMapping("/general-ask")
    @Operation(
            summary = "通用自由问答",
            description = "跳过向量检索，直接使用大模型进行自然对话回答，适用于闲聊、常识、外部资讯类问题"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "操作成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ChatResponse.class)))
    })
    public Result<ChatResponse> generalAsk(
            @Valid @RequestBody QAQuestionRequest request) {

        log.info("接收到通用问答请求: {}", request.getQuestion());

        ChatResponse response = qaService.generalAsk(request);
        return Result.success(response);
    }

    /**
     * 问答历史
     */
    @GetMapping("/history")
    @Operation(summary = "问答历史", description = "获取指定会话的问答历史记录")
    public Result<QAAnswerResponse> history(
            @Parameter(description = "会话ID") @RequestParam(required = false) String sessionId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {

        QAAnswerResponse response = qaService.getHistory(sessionId, pageNum, pageSize);
        return Result.success(response);
    }
}
