package com.lps.tools.model;

import lombok.Data;

import java.util.List;

@Data
public class ProjectOverviewResult {
    private List<String> controllers;
    private List<String> profiles;
}