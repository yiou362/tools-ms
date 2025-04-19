package com.lps.tools.model;

import lombok.Data;

import java.util.List;

/**
 * @author hhuang26
 */
@Data
public class ProjectOverviewResult {
    private List<String> controllers;
    private List<String> profiles;
}