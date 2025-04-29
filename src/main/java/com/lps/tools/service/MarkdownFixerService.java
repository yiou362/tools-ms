package com.lps.tools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lps.tools.model.ApiDocumentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author hhuang26
 */
@Service
public class MarkdownFixerService {
    public List<ApiDocumentation> fixMarkdown(List<String> arg1) throws Exception {
        // 存储结果
        List<ApiDocumentation> result = new ArrayList<>();

        // 解析每个 JSON 对象字符串
        ObjectMapper mapper = new ObjectMapper();
        for (String jsonStr : arg1) {
            // 清理代码块标识（如 ```json 和 ```）
            String cleanedJson = jsonStr.replaceAll("```json\\n|```", "").trim();

            // 解析 JSON 对象
            Map<String, String> jsonObj = mapper.readValue(cleanedJson, Map.class);

            // 获取 apiInfo 和 cat
            String apiMd = jsonObj.get("apiInfo");
            String line = jsonObj.get("cat");

            // 修复转义符号
            String fixedApiMd = fixEscapedChars(apiMd);
            String fixedLine = fixEscapedChars(line);

            // 构造 ApiDocumentation 对象
            ApiDocumentation doc = new ApiDocumentation(fixedApiMd, fixedLine);

            // 添加到结果
            result.add(doc);
        }

        return result;
    }

    // 修复转义符号
    private String fixEscapedChars(String input) {
        if (input == null) {
            return null;
        }
        // 替换 \\n 为 \n，\\\" 为 "，\\t 为 \t
        return input.replace("\\n", "\n")
                   .replace("\\\"", "\"")
                   .replace("\\t", "\t");
    }

    /**
     * 将 LLM生成的MarkDown内容处理，并 cat 和 apiInfo 字段拼接为一个整体字符串
     * @param args String[] MarkDown内容
     * @return 拼接后的字符串
     */
    public String subMdToStr(List<String> args) throws Exception {
        List<ApiDocumentation> apiDocs = fixMarkdown(args);

        // 检查输入是否为空
        if (apiDocs == null || apiDocs.isEmpty()) {
            return "";
        }

        // 初始化 StringBuilder 用于拼接 cat 和 apiInfo
        StringBuilder catMd = new StringBuilder();
        StringBuilder apiMd = new StringBuilder();

        catMd.append("# API Documentation\n");
        catMd.append("# Endpoint List\n");
        //为目录添加标题
        catMd.append("| Endpoint URL | HTTP Method | Description |\n");
        catMd.append("| --- | --- | --- |\n");

        // 遍历 ApiDocumentation 列表
        for (ApiDocumentation doc : apiDocs) {
            // 拼接 cat 字段
            if (doc.getCat() != null && !doc.getCat().isEmpty()) {
                catMd.append(doc.getCat()).append("\n");
            }

            // 拼接 apiInfo 字段
            if (doc.getApiInfo() != null && !doc.getApiInfo().isEmpty()) {
                apiMd.append(doc.getApiInfo()).append("\n");
            }
        }

        // 拼接 catMd 和 apiMd
        StringBuilder output = new StringBuilder();
        if (!catMd.isEmpty()) {
            output.append(catMd);
        }
        if (!apiMd.isEmpty()) {
            output.append(apiMd);
        }

        return output.toString();
    }
}