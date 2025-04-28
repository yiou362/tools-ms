package com.lps.tools.controller;

import com.lps.tools.model.GitHubRequestInfo;
import com.lps.tools.model.ProjectOverviewResult;
import com.lps.tools.service.GithubService;
import com.lps.tools.service.MarkdownFixerService;
import com.lps.tools.model.ApiDocumentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author hhuang26
 */
@RestController
@RequestMapping("/github")
public class GithubInfoController {

    @Autowired
    private GithubService githubService;

    @Autowired
    private MarkdownFixerService markdownFixerService;

    /**
     * 获取API文档相关代码（controller、entity）
     * @param owner
     * @param repo
     * @param branch
     * @param token
     * @param githubApiVersion
     * @return
     */
    @GetMapping("/analyze-controllers")
    public List<String> analyzeControllers(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String branch,
            @RequestParam String token,
            @RequestParam String githubApiVersion) {
        HttpHeaders headers = new HttpHeaders();
        try {

            return githubService.analyzeControllers(new GitHubRequestInfo(owner, repo, branch, token, githubApiVersion), headers);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取项目概览相关代码（controller、pom.xml、application.yml、Application.java、logback-spring.xml）
     * @param owner
     * @param repo
     * @param branch
     * @param token
     * @param githubApiVersion
     * @return
     */
    @GetMapping("/project-overview")
    public ProjectOverviewResult projectOverview(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String branch,
            @RequestParam String token,
            @RequestParam String githubApiVersion) {
        HttpHeaders headers = new HttpHeaders();
        try {
            return githubService.analyzeProjectOverview(new GitHubRequestInfo(owner, repo, branch, token, githubApiVersion), headers);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/fix-markdown")
    public String fixMarkdown(@RequestBody String[] output) throws Exception {
        // 调用 MarkdownFixer 处理入参，将处理后的结果转换为字符串并返回
        return markdownFixerService.subMdToStr(output);
    }
}