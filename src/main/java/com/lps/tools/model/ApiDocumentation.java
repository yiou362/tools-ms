package com.lps.tools.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author hhuang26
 */
@Data
@AllArgsConstructor
public// 自定义类表示 API 文档对象
class ApiDocumentation {
    private String apiInfo;
    private String cat;

    // toString 用于调试
    @Override
    public String toString() {
        return "ApiDocumentation{\n" +
               "api_md='" + apiInfo + "',\n" +
               "line='" + cat + "'\n}";
    }
}
