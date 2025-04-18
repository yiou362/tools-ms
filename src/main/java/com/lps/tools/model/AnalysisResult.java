package com.lps.tools.model;

import java.util.List;

public class AnalysisResult {
    private String content;
    private List<String> params;
    private List<String> returns;

    public AnalysisResult( String content, List<String> params, List<String> returns) {
        this.content = content;
        this.params = params;
        this.returns = returns;
    }

    public String getContent() {
        return content;
    }

    public List<String> getParams() {
        return params;
    }

    public List<String> getReturns() {
        return returns;
    }
}