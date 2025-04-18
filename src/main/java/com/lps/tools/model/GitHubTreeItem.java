package com.lps.tools.model;

public class GitHubTreeItem {
    private String path;
    private String type;

    public GitHubTreeItem(String path, String type) {
        this.path = path;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public String getType() {
        return type;
    }
}