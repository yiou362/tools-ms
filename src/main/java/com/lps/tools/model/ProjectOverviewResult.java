package com.lps.tools.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author hhuang26
 */
@Data
public class ProjectOverviewResult {
    private List<String> controllers;
    private List<GitHubFileItem> profiles;

}