package com.enterprise.rag.infrastructure.ai;

import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.enterprise.rag.infrastructure.config.QwenProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通义千问LLM服务封装类
 * 使用OpenAI兼容格式调用API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QwenLLMService {

    private static final String CIRCUIT_BREAKER_NAME = "qwenApi";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个专业的企业文档问答助手，请根据用户的问题提供准确、专业的回答。";
    private static final String OPENAI_COMPATIBLE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private final QwenProperties qwenProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "chatFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public String chat(String userMessage) {
        return chat(userMessage, DEFAULT_SYSTEM_PROMPT);
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "chatWithPromptFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public String chat(String userMessage, String systemPrompt) {
        log.debug("开始调用通义千问LLM - 模型: {}, 消息长度: {}",
                qwenProperties.getChatModel(), userMessage.length());

        try {
            String response = callQwenApi(systemPrompt, userMessage);
            log.debug("通义千问LLM调用成功 - 响应长度: {}", response.length());
            return response;

        } catch (NoApiKeyException e) {
            log.error("通义千问API密钥未配置，请检查环境变量aliQwen-api");
            throw new RuntimeException("API密钥未配置", e);
        } catch (InputRequiredException e) {
            log.error("通义千问API调用参数缺失: {}", e.getMessage());
            throw new RuntimeException("API调用参数缺失", e);
        } catch (ApiException e) {
            log.error("通义千问API调用异常: {}", e.getMessage());
            throw new RuntimeException("API调用异常", e);
        } catch (Exception e) {
            log.error("通义千问API调用异常: {}", e.getMessage());
            throw new RuntimeException("API调用异常", e);
        }
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "chatMultiTurnFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public String chat(List<Message> messages) {
        log.debug("开始调用通义千问LLM多轮对话 - 消息数量: {}", messages.size());

        try {
            List<Map<String, String>> messageList = new ArrayList<>();
            
            for (Message msg : messages) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", msg.getRole());
                msgMap.put("content", msg.getContent().toString());
                messageList.add(msgMap);
            }

            String response = callQwenApiWithMessages(messageList);
            log.debug("通义千问LLM多轮对话调用成功 - 响应长度: {}", response.length());
            return response;

        } catch (Exception e) {
            log.error("通义千问API调用异常: {}", e.getMessage());
            throw new RuntimeException("API调用异常", e);
        }
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "chatRagFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public String chatWithRag(String userQuestion, String context) {
        log.debug("开始RAG增强对话 - 问题: {}, 上下文长度: {}",
                userQuestion, context != null ? context.length() : 0);

        String ragSystemPrompt = buildRagSystemPrompt(context);
        return chat(userQuestion, ragSystemPrompt);
    }

    private String buildRagSystemPrompt(String context) {
        return String.format("""
                你是一个专业的企业文档问答助手。请根据以下检索到的文档内容回答用户问题。

                要求：
                1. 优先使用文档内容回答问题
                2. 如果文档中没有相关信息，请明确告知用户
                3. 回答要准确、专业、简洁
                4. 如果需要引用文档内容，请标注来源

                参考文档：
                %s

                """, context != null ? context : "");
    }

    /**
     * 使用RestTemplate直接调用通义千问API（OpenAI兼容格式）
     */
    private String callQwenApi(String systemPrompt, String userMessage) throws Exception {
        if (qwenProperties.getApiKey() == null || qwenProperties.getApiKey().isEmpty()) {
            throw new NoApiKeyException();
        }

        // 构建请求体（OpenAI兼容格式）
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenProperties.getChatModel());

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("result_format", "message");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + qwenProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // 使用OpenAI兼容格式的API端点
        String apiUrl = OPENAI_COMPATIBLE_URL + "/chat/completions";
        log.debug("调用通义千问API - URL: {}, 模型: {}", apiUrl, qwenProperties.getChatModel());

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new ApiException(new RuntimeException("API调用失败，状态码: " + response.getStatusCode() + ", 响应: " + response.getBody()));
        }

        return parseOpenAIResponse(response.getBody());
    }

    /**
     * 调用API（多轮对话，OpenAI兼容格式）
     */
    private String callQwenApiWithMessages(List<Map<String, String>> messages) throws Exception {
        if (qwenProperties.getApiKey() == null || qwenProperties.getApiKey().isEmpty()) {
            throw new NoApiKeyException();
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", qwenProperties.getChatModel());
        requestBody.put("messages", messages);
        requestBody.put("result_format", "message");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + qwenProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String apiUrl = OPENAI_COMPATIBLE_URL + "/chat/completions";
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new ApiException(new RuntimeException("API调用失败，状态码: " + response.getStatusCode()));
        }

        return parseOpenAIResponse(response.getBody());
    }

    /**
     * 解析OpenAI兼容格式的API响应
     * 响应格式: {"choices":[{"message":{"content":"..."}}]}
     */
    private String parseOpenAIResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        
        JsonNode choices = root.get("choices");
        if (choices == null || choices.size() == 0) {
            throw new ApiException(new RuntimeException("API响应格式异常，缺少choices字段"));
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null) {
            throw new ApiException(new RuntimeException("API响应格式异常，缺少message字段"));
        }

        JsonNode content = message.get("content");
        if (content == null) {
            throw new ApiException(new RuntimeException("API响应格式异常，缺少content字段"));
        }

        return content.asText();
    }

    // ==================== 熔断降级方法 ====================

    private String chatFallback(String userMessage, Throwable t) {
        log.error("通义千问LLM调用熔断降级 - 消息: {}, 错误: {}", userMessage, t.getMessage());
        return "抱歉，AI服务暂时不可用，请稍后重试。";
    }

    private String chatWithPromptFallback(String userMessage, String systemPrompt, Throwable t) {
        log.error("通义千问LLM调用熔断降级 - 消息: {}, 错误: {}", userMessage, t.getMessage());
        return "抱歉，AI服务暂时不可用，请稍后重试。";
    }

    private String chatMultiTurnFallback(List<Message> messages, Throwable t) {
        log.error("通义千问LLM多轮对话调用熔断降级 - 消息数量: {}, 错误: {}", messages.size(), t.getMessage());
        return "抱歉，AI服务暂时不可用，请稍后重试。";
    }

    private String chatRagFallback(String userQuestion, String context, Throwable t) {
        log.error("通义千问RAG对话调用熔断降级 - 问题: {}, 错误: {}", userQuestion, t.getMessage());
        return "抱歉，AI服务暂时不可用，请稍后重试。";
    }
}