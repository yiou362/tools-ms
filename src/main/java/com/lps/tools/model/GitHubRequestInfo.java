package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hhuang26
 */
@Data
@AllArgsConstructor
public class GitHubRequestInfo {
    private String owner;
    private String repo;
    private String branch;
    private String token;
    private String githubApiVersion;
}