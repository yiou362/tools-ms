package com.lps.tools.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class GitHubFileController {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @GetMapping("/github/files")
    public Mono<List<String>> getGitHubFiles(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String token,
            @RequestParam String githubApiVersion,
            @RequestParam(required = false, defaultValue = "main") String branch) {

        WebClient webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-GitHub-Api-Version", githubApiVersion)
                .build();

        return fetchFilesRecursively(webClient, owner, repo, branch, "").collectList();
    }

    private Flux<String> fetchFilesRecursively(WebClient webClient, String owner, String repo, String branch, String path) {
        return webClient.get()
                .uri(uriBuilder -> {
                    UriBuilder builder = uriBuilder
                            .path("/repos/{owner}/{repo}/contents/{path}")
                            .queryParam("ref", branch);
                    return path.isEmpty()
                            ? builder.build(owner, repo, "")
                            : builder.build(owner, repo, path);
                })
                .retrieve()
                .bodyToMono(Map[].class)
                .flatMapMany(Flux::fromArray)
                .flatMap(file -> {
                    String type = (String) file.get("type");
                    String filePath = (String) file.get("path");
                    if ("file".equals(type)) {
                        return Mono.just((String) file.get("download_url"));
                    } else if ("dir".equals(type)) {
                        return fetchFilesRecursively(webClient, owner, repo, branch, filePath);
                    } else {
                        return Mono.empty();
                    }
                });
    }
}