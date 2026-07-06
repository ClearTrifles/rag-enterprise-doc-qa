package com.enterprise.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问答请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAQuestionRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过500个字符")
    private String question;

    private String sessionId;
}