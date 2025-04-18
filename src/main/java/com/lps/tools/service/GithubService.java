package com.lps.tools.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import com.lps.tools.util.HttpUtil;
import com.lps.tools.model.AnalysisResult;
import com.lps.tools.model.GitHubTreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class GithubService {
    private static final Logger logger = LoggerFactory.getLogger(GithubService.class);
    @Autowired
    private RestTemplate restTemplate;

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "string", "int", "long", "double", "boolean", "list", "map", "void", "Object", "Integer"
    );
    private static final Set<String> EXTERNAL_PACKAGES = Set.of(
            "org.", "com.fasterxml.", "java.", "javax.", "com.google.", "io."
    );
    private static final Map<String, String> FILE_CACHE = new HashMap<>();
    private String lastAnalysisFilePath;

    public List<AnalysisResult> analyzeControllers(String owner, String repo, String branch, HttpHeaders headers, String token, String githubApiVersion) throws IOException, URISyntaxException {
        String sha = getDefaultBranchSha(owner, repo, branch, headers);
        List<GitHubTreeItem> tree = getRepoTree(sha, token, owner, repo,githubApiVersion);
        Map<String, Object> relevantFiles = findRelevantFiles(tree);

        List<String> controllerFiles = (List<String>) relevantFiles.get("controllers");
        // TODO 提取前十个元素用于测试，以免调用github次数太多
        controllerFiles = new ArrayList<>(
                controllerFiles.subList(0, Math.min(10, controllerFiles.size()))
        );

        List<AnalysisResult> results = new ArrayList<>();
        for (String path : controllerFiles) {
            String content = getFileContent(path, owner, repo, branch, token,githubApiVersion);
            if (content != null) {
                AnalysisResult result = parseController(content, path, relevantFiles, owner, repo, branch, token, githubApiVersion);
                results.add(new AnalysisResult(result.getContent(), result.getParams(), result.getReturns()));
            }
        }
        return results;
    }

    public String getLastAnalysisFilePath() {
        return lastAnalysisFilePath;
    }

    public String getDefaultBranchSha(String owner, String repo, String branch, HttpHeaders headers) throws URISyntaxException {
        String url = String.format("https://api.github.com/repos/%s/%s/branches/%s", owner, repo, branch);
        URI uri = new URI(url);
        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, uri);
        ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            return parseShaFromResponse(response.getBody());
        }
        throw new RuntimeException("获取分支 SHA 失败: " + response.getBody());
    }

    private String parseShaFromResponse(String responseBody) {
        int startIndex = responseBody.indexOf("\"sha\":\"") + 7;
        int endIndex = responseBody.indexOf("\"", startIndex);
        return responseBody.substring(startIndex, endIndex);
    }


    private List<GitHubTreeItem> getRepoTree(String sha, String token, String owner, String repo, String githubApiVersion) throws IOException {
        String url = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", owner, repo, sha);
        JsonNode response = HttpUtil.get(url, token, githubApiVersion);
        List<GitHubTreeItem> tree = new ArrayList<>();
        for (JsonNode item : response.get("tree")) {
            tree.add(new GitHubTreeItem(
                    item.get("path").asText(),
                    item.get("type").asText()
            ));
        }
        return tree;
    }

    private Map<String, Object> findRelevantFiles(List<GitHubTreeItem> tree) {
        List<String> controllers = new ArrayList<>();
        Map<String, String> dataClasses = new HashMap<>();
        for (GitHubTreeItem item : tree) {
            if ("blob".equals(item.getType()) && item.getPath().endsWith(".java")) {
                String pathLower = item.getPath().toLowerCase();
                if (pathLower.contains("controller")) {
                    controllers.add(item.getPath());
                    logger.info("找到 Controller: {}", item.getPath());
                } else if (pathLower.contains("/entity/") || pathLower.contains("/model/") ||
                        pathLower.contains("/dto/") || pathLower.contains("/bo/") || pathLower.contains("/po/")) {
                    String className = item.getPath().substring(item.getPath().lastIndexOf('/') + 1).replace(".java", "");
                    dataClasses.put(className, item.getPath());
                    logger.info("找到数据类: {}, 路径: {}", className, item.getPath());
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("controllers", controllers);
        result.put("dataClasses", dataClasses);
        return result;
    }

    public String getFileContent(String path, String owner, String repo, String branch, String token, String githubApiVersion) throws IOException {
        if (FILE_CACHE.containsKey(path)) {
            return FILE_CACHE.get(path);
        }
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", owner, repo, path, branch);
        JsonNode response = HttpUtil.get(url, token, githubApiVersion);
        String content = response.get("content").asText();
        String cleanedContent = content.replaceAll("\\n|\\r", "").trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(cleanedContent), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            logger.error("Base64 解码失败: {}, 路径: {}", e.getMessage(), path);
            return null;
        }
        FILE_CACHE.put(path, decoded);
        return decoded;
    }

    public AnalysisResult parseController(String content, String controllerPath, Map<String, Object> relevantFiles, String owner, String repo, String branch, String token,String githubApiVersion) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(content);
            Set<String> paramClasses = new HashSet<>();
            Set<String> returnClasses = new HashSet<>();
            List<String> skippedClasses = new ArrayList<>();

            // 解析方法
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                // 入参
                for (Parameter param : method.getParameters()) {
                    String typeName = param.getTypeAsString();
                    if (!isPrimitiveType(typeName)) {
                        paramClasses.add(typeName);
                    }
                }

                // 出参
                Type returnType = method.getType();
                String returnTypeName = returnType.asString();
                if (!returnTypeName.equals("void") && !isPrimitiveType(returnTypeName)) {
                    returnClasses.add(returnTypeName);
                }
            }

//            // 构建 import 映射
//            Map<String, String> importMap = new HashMap<>();
//            cu.getImports().forEach(importDecl -> {
//                String packagePath = importDecl.getNameAsString();
//                String className = packagePath.substring(packagePath.lastIndexOf('.') + 1);
//                String javaPath = packagePath.replace('.', '/') + ".java";
//                importMap.put(className, javaPath);
//            });

            // 获取 dataClasses
            Map<String, String> dataClasses = (Map<String, String>) relevantFiles.get("dataClasses");

            // 获取入参源代码
            List<String> params = new ArrayList<>();
            for (String className : paramClasses) {
                // 跳过原始类型
                if (isPrimitiveType(className)) {
                    logger.info("跳过原始类型: {}", className);
                    continue;
                }

                // 查找 dataClasses（精确或模糊匹配）
                String path = findMatchingClass(className, dataClasses);
                if (path != null) {
                    String code = getFileContent(path, owner, repo, branch, token,githubApiVersion);
                    if (code != null) {
                        params.add(cleanCode(code));
                        logger.info("找到入参类: {}, 路径: {}", className, path);
                    }
                } else {
                    logger.info("未找到入参类: {}，可能是跨模块", className);
                    skippedClasses.add(className + " (未找到，可能跨模块)");
                }
            }

            // 获取出参源代码
            List<String> returns = new ArrayList<>();
            for (String className : returnClasses) {
                // 跳过原始类型
                if (isPrimitiveType(className)) {
                    logger.info("跳过原始类型: {}", className);
                    continue;
                }

                // 查找 dataClasses
                String path = findMatchingClass(className, dataClasses);
                if (path != null) {
                    String code = getFileContent(path, owner, repo, branch, token,githubApiVersion);
                    if (code != null) {
                        returns.add(cleanCode(code));
                        logger.info("找到出参类: {}, 路径: {}", className, path);
                    }
                } else {
                    logger.info("未找到出参类: {}，可能是跨模块", className);
                    skippedClasses.add(className + " (未找到，可能跨模块)");
                }
            }

            // 记录跳过的类
//            if (!skippedClasses.isEmpty()) {
//                try (FileWriter writer = new FileWriter(getLastAnalysisFilePath(), StandardCharsets.UTF_8, true)) {
//                    writer.write("跳过的类:\n");
//                    for (String skipped : skippedClasses) {
//                        writer.write("- " + skipped + "\n");
//                    }
//                    writer.write("\n");
//                }
//            }

            return new AnalysisResult(content, params, returns);

        } catch (Exception e) {
            logger.error("解析错误: {}", e.getMessage());
            return new AnalysisResult(content, Collections.emptyList(), Collections.emptyList());
        }
    }

    private String findMatchingClass(String className, Map<String, String> dataClasses) {
        // 精确匹配
        if (dataClasses.containsKey(className)) {
            return dataClasses.get(className);
        }

        // 模糊匹配
        for (String dataClassName : dataClasses.keySet()) {
            if (dataClassName.toLowerCase().contains(className.toLowerCase())) {
                logger.info("模糊匹配类: {} -> {}", className, dataClassName);
                return dataClasses.get(dataClassName);
            }
        }

        return null;
    }

    private boolean isPrimitiveType(String type) {
        return PRIMITIVE_TYPES.contains(type.toLowerCase());
    }

    private String cleanCode(String code) {
//        if (code == null) return "";
//        return WHITESPACE.matcher(code).replaceAll("");
        if (code == null) {return "";}
        // 移除换行和制表符
        String cleaned = code.replaceAll("\\n|\\r|\\t", "");
        // 压缩多个空格为一个
        cleaned = cleaned.replaceAll("\\s+", " ");
        // 移除首尾空格
        return cleaned.trim();

    }
}