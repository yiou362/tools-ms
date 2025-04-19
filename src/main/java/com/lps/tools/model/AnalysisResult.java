package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @author hhuang26
 */
@Data
@AllArgsConstructor
public class AnalysisResult {
    private String content;
    private List<String> params;
    private List<String> returns;

}