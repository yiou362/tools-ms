package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author hhuang26
 * @description 存储LLM生成的md格式的内容，每个String类型的Item包含apiInfo、cat的md内容
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiMdRequest {
    private List<String> output;
}