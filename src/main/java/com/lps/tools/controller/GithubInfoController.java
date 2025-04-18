package com.lps.tools.controller;

import com.lps.tools.model.AnalysisResult;
import com.lps.tools.service.GithubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GithubInfoController {

    @Autowired
    private GithubService githubService;

    @GetMapping("/github/analyze-controllers")
    public List<AnalysisResult> analyzeControllers(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String branch,
            @RequestParam String token,
            @RequestParam String githubApiVersion) {
        HttpHeaders headers = new HttpHeaders();
        try {
            return githubService.analyzeControllers(owner, repo, branch, headers, token, githubApiVersion);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}