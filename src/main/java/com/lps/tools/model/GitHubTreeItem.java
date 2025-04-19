package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hhuang26
 */
@Data
@AllArgsConstructor
public class GitHubTreeItem {
    private String path;
    private String type;
}