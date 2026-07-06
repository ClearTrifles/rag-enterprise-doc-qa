package com.enterprise.rag.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档上传请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    @NotBlank(message = "文档名称不能为空")
    @Size(max = 255, message = "文档名称不能超过255个字符")
    private String documentName;

    @Size(max = 500, message = "存储路径不能超过500个字符")
    private String storagePath;
}