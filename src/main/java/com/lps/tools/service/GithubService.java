package com.lps.tools.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import com.lps.tools.model.*;
import com.lps.tools.util.HttpUtil;
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

@Service
public class GithubService {
    private static final Logger logger = LoggerFactory.getLogger(GithubService.class);
    @Autowired
    private RestTemplate restTemplate;

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "string", "int", "long", "double", "boolean", "list", "map", "void", "object", "integer"
    );
    private static final Map<String, String> FILE_CACHE = new HashMap<>();

    public List<AnalysisResult> analyzeControllers(GitHubRequestInfo gitHubRequestInfo, HttpHeaders headers) throws IOException, URISyntaxException {
        try {
            // 获取默认分支的 SHA 值
            String sha = getDefaultBranchSha(gitHubRequestInfo, headers);

            // 获取仓库的树结构
            List<GitHubTreeItem> tree = getRepoTree(gitHubRequestInfo, sha);

            // 查找相关文件
            RelevantFiles relevantFiles = findRelevantFiles(tree);

            // 获取控制器文件列表
            List<String> controllerFiles = relevantFiles.getControllers();
            if (controllerFiles == null || controllerFiles.isEmpty()) {
                return Collections.emptyList(); // 如果没有控制器文件，直接返回空列表
            }

            // 提取前十个元素用于测试（可配置）
            int maxFilesToProcess = Math.min(10, controllerFiles.size());
            List<String> limitedControllerFiles = new ArrayList<>(controllerFiles.subList(0, maxFilesToProcess));

            // 分析每个控制器文件
            List<AnalysisResult> results = new ArrayList<>();
            for (String path : limitedControllerFiles) {
                try {
                    // 获取文件内容
                    String content = getFileContent(path, gitHubRequestInfo);
                    if (content != null) {
                        // 解析控制器内容
                        AnalysisResult result = parseController(content, relevantFiles, gitHubRequestInfo);
                        results.add(new AnalysisResult(cleanCode(result.getContent()), result.getParams(), result.getReturns()));
                    }
                } catch (Exception e) {
                    // 捕获单个文件处理中的异常，避免中断整个流程
                    logger.error("Error processing file: {}. Error: {}" ,path, e.getMessage());
                }
            }

            return results;
        } catch (Exception e) {
            // 捕获主流程中的异常，避免方法完全失败
            logger.error("Error during analysis: {}" , e.getMessage());
            throw e; // 重新抛出异常，确保调用方可以感知到错误
        }
    }


    public ProjectOverviewResult analyzeProjectOverview(GitHubRequestInfo gitHubRequestInfo, HttpHeaders headers) throws IOException {
        try {
            // 获取默认分支的 SHA
            String sha = getDefaultBranchSha(gitHubRequestInfo, headers);

            // 获取仓库树结构
            List<GitHubTreeItem> tree = getRepoTree(gitHubRequestInfo,sha);

            // 查找相关文件
            RelevantFiles relevantFiles = findProjectOverviewFiles(tree);

            // 初始化结果对象
            ProjectOverviewResult results = new ProjectOverviewResult();

            // 处理 controllers 和 profiles 文件内容
            results.setControllers(getFileContents(relevantFiles.getControllers(), gitHubRequestInfo));
            results.setProfiles(getFilePathAndContents(relevantFiles.getProfiles(), gitHubRequestInfo));

            return results;
        } catch (Exception e) {
            // 记录异常日志并抛出自定义异常（可根据需求调整）
            logger.info("Error occurred while analyzing project overview: {}" , e.getMessage());
            throw new IOException("Failed to analyze project overview", e);
        }
    }

    // 提取通用方法处理文件内容获取逻辑
    private List<String> getFileContents(List<String> paths, GitHubRequestInfo gitHubRequestInfo){
        List<String> contents = new ArrayList<>();
        if (paths == null || paths.isEmpty()) {
            return contents; // 如果路径列表为空，直接返回空列表
        }
        for (String path : paths) {
            try {
                String content = getFileContent(path, gitHubRequestInfo);
                contents.add(cleanCode(content));
            } catch (Exception e) {
                // 单个文件获取失败时记录日志并继续处理其他文件
                logger.info("Failed to retrieve content for file: {}. Error: {}" , path, e.getMessage());
            }
        }
        return contents;
    }

    // 提取通用方法处理文件内容和文件路径获取逻辑
    private List<GitHubFileItem> getFilePathAndContents(List<String> paths, GitHubRequestInfo gitHubRequestInfo){
        List<GitHubFileItem> contents = new ArrayList<>();
        if (paths == null || paths.isEmpty()) {
            // 如果路径列表为空，直接返回空列表
            return contents;
        }
        for (String path : paths) {
            try {
                String content = getFileContent(path, gitHubRequestInfo);
                // 提取文件名
                String fileName = extractFileName(path);
                contents.add(new GitHubFileItem(fileName, cleanCode(content)));
            } catch (Exception e) {
                // 单个文件获取失败时记录日志并继续处理其他文件
                logger.info("Failed to retrieve content for file: {}. Error: {}" , path, e.getMessage());
            }
        }
        return contents;
    }

    // 辅助方法：从路径中提取文件名
    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex + 1 < path.length()) {
            return path.substring(lastSlashIndex + 1);
        }
        return path;
    }

//    public String getLastAnalysisFilePath() {
//        return lastAnalysisFilePath;
//    }

    public String getDefaultBranchSha(GitHubRequestInfo gitHubRequestInfo, HttpHeaders headers) throws URISyntaxException {
        String url = String.format("https://api.github.com/repos/%s/%s/branches/%s", gitHubRequestInfo.getOwner(), gitHubRequestInfo.getRepo(), gitHubRequestInfo.getBranch());
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


    private List<GitHubTreeItem> getRepoTree(GitHubRequestInfo gitHubRequestInfo,String sha) throws IOException {
        String url = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", gitHubRequestInfo.getOwner(),gitHubRequestInfo.getRepo(), sha);
        JsonNode response = HttpUtil.get(url, gitHubRequestInfo.getToken(), gitHubRequestInfo.getGithubApiVersion());
        List<GitHubTreeItem> tree = new ArrayList<>();
        for (JsonNode item : response.get("tree")) {
            tree.add(new GitHubTreeItem(
                    item.get("path").asText(),
                    item.get("type").asText()
            ));
        }

        return tree;
    }

    private RelevantFiles findRelevantFiles(List<GitHubTreeItem> tree) {
        List<String> controllers = new ArrayList<>();
        Map<String, String> dataClasses = new HashMap<>();

        for (GitHubTreeItem item : tree) {
            if ("blob".equals(item.getType()) && item.getPath() != null && item.getPath().endsWith(".java")) {
                String pathLower = item.getPath().toLowerCase();

                // 判断是否为 Controller
                if (isControllerPath(pathLower)) {
                    controllers.add(item.getPath());
                    if (logger.isDebugEnabled()) { // 仅在调试模式下记录日志
                        logger.debug("找到 Controller: {}", item.getPath());
                    }
                }
                // 判断是否为数据类
                else if (isDataClassPath(pathLower)) {
                    try {
                        String className = extractClassName(item.getPath());
                        dataClasses.put(className, item.getPath());
                        if (logger.isDebugEnabled()) { // 仅在调试模式下记录日志
                            logger.debug("找到数据类: {}, 路径: {}", className, item.getPath());
                        }
                    } catch (IllegalArgumentException e) {
                        logger.warn("无法解析文件路径: {}, 错误信息: {}", item.getPath(), e.getMessage());
                    }
                }
            }
        }

        RelevantFiles result = new RelevantFiles();
        result.setControllers(controllers);
        result.setDataClasses(dataClasses);
        return result;
    }

    // 辅助方法：判断路径是否为 Controller
    private boolean isControllerPath(String pathLower) {
        return pathLower.contains("/controller/") || pathLower.endsWith("controller.java");
    }

    // 辅助方法：判断路径是否为数据类
    private boolean isDataClassPath(String pathLower) {
        return pathLower.contains("/entity/") || pathLower.contains("/model/") ||
                pathLower.contains("/dto/") || pathLower.contains("/bo/") || pathLower.contains("/po/");
    }

    // 辅助方法：从路径中提取类名
    private String extractClassName(String path) {
        if (path == null || !path.contains("/")) {
            throw new IllegalArgumentException("无效的路径格式");
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex < 0 || lastSlashIndex + 1 >= path.length()) {
            throw new IllegalArgumentException("路径中缺少有效的类名");
        }
        return path.substring(lastSlashIndex + 1).replace(".java", "");
    }


    private RelevantFiles findProjectOverviewFiles(List<GitHubTreeItem> tree) {
        // 配置文件列表：pom.xml、application.yml、Application.java、logback-spring.xml
        final Set<String> profileFileNames = new HashSet<>(Arrays.asList(
                "pom.xml", "application", "logback-spring.xml", ".md"
        ));

        List<String> profiles = new ArrayList<>();
        List<String> controllers = new ArrayList<>();

        if (tree == null || tree.isEmpty()) {
            logger.warn("GitHubTreeItem 列表为空或未提供");
            return new RelevantFiles(); // 返回空结果
        }

        for (GitHubTreeItem item : tree) {
            if ("blob".equals(item.getType()) && item.getPath() != null) {
                String fileName = item.getPath().toLowerCase();

                // 精确匹配控制器文件
                if (fileName.contains("/controller/") || fileName.endsWith("controller.java")) {
                    controllers.add(item.getPath());
                } else if (profileFileNames.stream().anyMatch(fileName::contains)) {
                    // 精确匹配配置文件
                    profiles.add(item.getPath());
                }
            }
        }

        RelevantFiles result = new RelevantFiles();
        result.setProfiles(profiles);
        result.setControllers(controllers);

        // 集中输出日志
        if (!profiles.isEmpty()) {
            logger.info("找到 profiles: {}", profiles);
        }
        if (!controllers.isEmpty()) {
            logger.info("找到 Controllers: {}", controllers);
        }

        return result;
    }

    public String getFileContent(String path, GitHubRequestInfo gitHubRequestInfo) throws IOException {
        if (FILE_CACHE.containsKey(path)) {
            return FILE_CACHE.get(path);
        }
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", gitHubRequestInfo.getOwner(), gitHubRequestInfo.getRepo(), path, gitHubRequestInfo.getBranch());
        JsonNode response = HttpUtil.get(url, gitHubRequestInfo.getToken(), gitHubRequestInfo.getGithubApiVersion());
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

    public AnalysisResult parseController(String content, RelevantFiles relevantFiles, GitHubRequestInfo gitHubRequestInfo) {
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
                if (!"void".equals(returnTypeName) && !isPrimitiveType(returnTypeName)) {
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
            Map<String, String> dataClasses = relevantFiles.getDataClasses();

            // 获取入参源代码
            List<String> params = new ArrayList<>();
            for (String className : paramClasses) {
                // 跳过原始类型
                if (isPrimitiveType(className)) {
//                    logger.info("跳过原始类型: {}", className);
                    continue;
                }

                // 查找 dataClasses（精确或模糊匹配）
                String path = findMatchingClass(className, dataClasses);
                if (path != null) {
                    String code = getFileContent(path, gitHubRequestInfo);
                    if (code != null) {
                        params.add(cleanCode(code));
//                        logger.info("找到入参类: {}, 路径: {}", className, path);
                    }
                } else {
//                    logger.info("未找到入参类: {}，可能是跨模块", className);
                    skippedClasses.add(className + " (未找到，可能跨模块)");
                }
            }

            // 获取出参源代码
            List<String> returns = new ArrayList<>();
            for (String className : returnClasses) {
                // 跳过原始类型
                if (isPrimitiveType(className)) {
//                    logger.info("跳过原始类型: {}", className);
                    continue;
                }

                // 查找 dataClasses
                String path = findMatchingClass(className, dataClasses);
                if (path != null) {
                    String code = getFileContent(path, gitHubRequestInfo);
                    if (code != null) {
                        returns.add(cleanCode(code));
//                        logger.info("找到出参类: {}, 路径: {}", className, path);
                    }
                } else {
//                    logger.info("未找到出参类: {}，可能是跨模块", className);
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
//                logger.info("模糊匹配类: {} -> {}", className, dataClassName);
                return dataClasses.get(dataClassName);
            }
        }

        return null;
    }

    private boolean isPrimitiveType(String type) {
        return PRIMITIVE_TYPES.contains(type.toLowerCase());
    }

    private String cleanCode(String code) {
        if (code == null) {
            return "";
        }
        try {
//            // 合并正则表达式，减少多次调用 replaceAll 的开销
//            // 1. 移除 package 和 import 语句
//            // 2. 移除换行符、回车符和制表符，并压缩多余空格
            String cleaned = code.replaceAll(
                    "(package\\s+[^;]+;|import\\s+[^;]+;|\\n|\\r|\\t|\\s{2,})", " "
            );

            // 3. 移除所有注释（单行、多行、文档注释）TODO 可能会误删内容
//            cleaned = cleaned.replaceAll(
//                    "(?s)/\\*\\*?.*?\\*/|//.*", " "
//            );

            // 4. 移除 log.info(XXX);、log.error(XXX); 和 System.out.println(XXX); 语句

            // 移除首尾空格并返回结果
            return cleaned.trim();
        } catch (Exception e) {
            // 捕获异常并记录日志（可根据实际需求调整）
            logger.error("Error during code cleaning: {}" , e.getMessage());
            return ""; // 返回空字符串以保证函数的稳定性
        }
    }
}