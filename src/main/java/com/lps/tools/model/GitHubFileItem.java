package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hhuang26
 */
@Data
@AllArgsConstructor
public class GitHubFileItem {
    private String fileName;
    private String content;
}