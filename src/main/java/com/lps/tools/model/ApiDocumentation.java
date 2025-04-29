package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hhuang26
 * @description 存放每一个LLM生成的接口信息
 */
@Data
@AllArgsConstructor
public class ApiDocumentation {
    private String apiInfo;
    private String cat;

    // toString 用于调试
    @Override
    public String toString() {
        return "ApiDocumentation{\n" +
               "apiInfo='" + apiInfo + "',\n" +
               "cat='" + cat + "'\n}";
    }
}
