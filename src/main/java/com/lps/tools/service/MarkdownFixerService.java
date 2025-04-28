package com.lps.tools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lps.tools.model.ApiDocumentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MarkdownFixerService {
    public List<ApiDocumentation> fixMarkdown(String[] arg1) throws Exception {
        // 存储结果
        List<ApiDocumentation> result = new ArrayList<>();

        // 解析每个 JSON 对象字符串
        ObjectMapper mapper = new ObjectMapper();
        for (String jsonStr : arg1) {
            // 清理代码块标识（如 ```json 和 ```）
            String cleanedJson = jsonStr.replaceAll("```json\\n|```", "").trim();

            // 解析 JSON 对象
            Map<String, String> jsonObj = mapper.readValue(cleanedJson, Map.class);

            // 获取 api_md 和 line
            String apiMd = jsonObj.get("api_md");
            String line = jsonObj.get("line");

            // 修复转义符号
            String fixedApiMd = fixEscapedChars(apiMd);
            String fixedLine = fixEscapedChars(line);

            // 构造 ApiDocumentation 对象
            ApiDocumentation doc = new ApiDocumentation(fixedApiMd, fixedLine);

            // 添加到结果
            result.add(doc);
        }

        return result;
    }

    // 修复转义符号
    private String fixEscapedChars(String input) {
        if (input == null) {
            return null;
        }
        // 替换 \\n 为 \n，\\\" 为 "，\\t 为 \t
        return input.replace("\\n", "\n")
                   .replace("\\\"", "\"")
                   .replace("\\t", "\t");
    }

    /**
     * 将 LLM生成的MarkDown内容处理，并 cat 和 apiInfo 字段拼接为一个整体字符串
     * @param args String[] MarkDown内容
     * @return 拼接后的字符串
     */
    public String subMdToStr(String[] args) throws Exception {
        List<ApiDocumentation> apiDocs = fixMarkdown(args);

        // 检查输入是否为空
        if (apiDocs == null || apiDocs.isEmpty()) {
            return "";
        }

        // 初始化 StringBuilder 用于拼接 cat 和 apiInfo
        StringBuilder catMd = new StringBuilder();
        StringBuilder apiMd = new StringBuilder();

        // 遍历 ApiDocumentation 列表
        for (ApiDocumentation doc : apiDocs) {
            // 拼接 cat 字段
            if (doc.getCat() != null && !doc.getCat().isEmpty()) {
                catMd.append(doc.getCat()).append("\n");
            }

            // 拼接 apiInfo 字段
            if (doc.getApiInfo() != null && !doc.getApiInfo().isEmpty()) {
                apiMd.append(doc.getApiInfo()).append("\n");
            }
        }

        // 拼接 catMd 和 apiMd
        StringBuilder output = new StringBuilder();
        if (catMd.length() > 0) {
            output.append(catMd.toString());
        }
        if (apiMd.length() > 0) {
            output.append(apiMd.toString());
        }

        return output.toString();
    }

//    // 测试代码
//    public static void main(String[] args) throws Exception {
//        String[] arg1 = new String[] {
//            "```json\n{\n  \"api_md\": \"## 查询生产地址\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/area/findAllArea\\n- 描述：查询生产地址\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| Area | object | 地址信息 | 是 |\\n| page | integer | 页码 | 是 |\\n| limit | integer | 每页条数 | 是 |\\n\\n### Area 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| areaId | integer | 地址ID |\\n| areaName | string | 地址名称 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| code | integer | 状态码 |\\n| msg | string | 消息 |\\n| count | integer | 总条数 |\\n| data | array | 数据列表 |\\n\\n### Data 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| areaId | integer | 地址ID |\\n| areaName | string | 地址名称 |\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/area/findAllArea?page=1&limit=10\\\"\\n\\n响应（200 OK）:\\njson\\n{\\n  \\\"code\\\": 0,\\n  \\\"msg\\\": \\\"\\\",\\n  \\\"count\\\": 100,\\n  \\\"data\\\": [{\\n    \\\"areaId\\\": 1,\\n    \\\"areaName\\\": \\\"地址1\\\"\\n  }]\\n}\\n\\n响应（400 Bad Request）:\\njson\\n{\\n  \\\"code\\\": 400,\\n  \\\"msg\\\": \\\"参数错误\\\"\\n}\\n\\n## 添加生产地址\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/area/addArea\\n- 描述：添加生产地址\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| Area | object | 地址信息 | 是 |\\n\\n### Area 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| areaId | integer | 地址ID |\\n| areaName | string | 地址名称 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| 结果 | string | 操作结果 |\\n\\n### 使用示例\\ncurl -X POST -d '{\\\"areaName\\\":\\\"新地址\\\"}' \\\"http://example.com/area/addArea\\\"\\n\\n响应（200 OK）:\\njson\\n\\\"添加成功\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"地址已存在\\\"\\n\\n## 删除生产地址\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/area/deleteArea\\n- 描述：删除生产地址\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| areaId | integer | 地址ID | 是 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| 结果 | string | 操作结果 |\\n\\n### 使用示例\\ncurl -X POST -d 'areaId=1' \\\"http://example.com/area/deleteArea\\\"\\n\\n响应（200 OK）:\\njson\\n\\\"删除成功\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"删除失败\\\"\",\n  \"line\": \"| /area/findAllArea | GET | 查询生产地址 |\\n| /area/addArea | POST | 添加生产地址 |\\n| /area/deleteArea | POST | 删除生产地址 |\"\n}\n```",
//            "```json\n{\n  \"api_md\": \"## 进入处方划价页面\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/caoout/xiang\\n- 描述：进入处方划价页面\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| model | object | 模型 | 是 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/caoout/xiang\\\"\\n\\n响应（200 OK）:\\njson\\n\\\"cao/Cxiangmu\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\\n\\n## 进入收费页面\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/caoout/out\\n- 描述：进入收费页面\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| model | object | 模型 | 是 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/caoout/out\\\"\\n\\n响应（200 OK）:\\njson\\n\\\"cao/Ctoll\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\\n\\n## 查询药品所有信息\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/caoout/selout\\n- 描述：查询药品所有信息\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| coutpatienttype | object | 费用类型 | 是 |\\n| page | integer | 页码 | 是 |\\n| limit | integer | 每页数量 | 是 |\\n| projectName | string | 项目名称 | 否 |\\n\\n### Coutpatienttype 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| outpatientId | integer | 序号 |\\n| projectName | string | 项目名称 |\\n| unit | integer | 单位 |\\n| bigprojectId | integer | 项目分类 |\\n| price | double | 价格 |\\n| unitName | string | 单位 |\\n| ostate | integer | 项目状态 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| code | integer | 状态码 |\\n| msg | string | 消息 |\\n| count | integer | 总条数 |\\n| data | array | 数据列表 |\\n\\n### CPharmacy 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| pharmacyId | integer | 药品id |\\n| pharmacyName | string | 药品名称 |\\n| drugstoreId | integer | 领货单位外键 |\\n| skullId | integer | 经办人Id |\\n| warehouseId | integer | 库房Id |\\n| unit | integer | 库房 |\\n| sellingPrice | double | 售价 |\\n| area | integer | 产地 |\\n| type | integer | 类型 |\\n| produceDate | date | 生产日期 |\\n| validDate | date | 保质期 |\\n| drugstorenum | integer | 数量 |\\n| skullbatch | integer | 批号 |\\n| unitname | string | 单位名称 |\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/caoout/selout?coutpatienttype={}&page=1&limit=10&projectName=test\\\"\\n\\n响应（200 OK）:\\njson\\n{\\n  \\\"code\\\": 0,\\n  \\\"msg\\\": \\\"\\\",\\n  \\\"count\\\": 100,\\n  \\\"data\\\": [\\n    {\\n      \\\"pharmacyId\\\": 1,\\n      \\\"pharmacyName\\\": \\\"药1\\\",\\n      \\\"drugstoreId\\\": 1,\\n      \\\"skullId\\\": 1,\\n      \\\"warehouseId\\\": 1,\\n      \\\"unit\\\": 1,\\n      \\\"sellingPrice\\\": 10.5,\\n      \\\"area\\\": 1,\\n      \\\"type\\\": 1,\\n      \\\"produceDate\\\": \\\"2022-01-01\\\",\\n      \\\"validDate\\\": \\\"2023-01-01\\\",\\n      \\\"drugstorenum\\\": 100,\\n      \\\"skullbatch\\\": 1,\\n      \\\"unitname\\\": \\\"盒\\\"\\n    }\\n  ]\\n}\\n\\n响应（400 Bad Request）:\\njson\\n{\\n  \\\"code\\\": 400,\\n  \\\"msg\\\": \\\"参数错误\\\"\\n}\\n\\n## 查询处方中是否有这个药\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/caoout/selchuo\\n- 描述：查询处方中是否有这个药\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| cCashier | object | 收费信息 | 是 |\\n| reid | integer | 用户id | 是 |\\n| mename | string | 药品名称 | 是 |\\n\\n### CCashier 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| cashier | integer | 处方id |\\n| reportId | integer | 挂号id |\\n| durgname | string | 药品名称 |\\n| durgnum | integer | 药品数量 |\\n| repiceprice | double | 价格 |\\n| repicetotal | double | 小计 |\\n| state | integer | 状态 |\\n| ostate | integer | 项目状态 |\\n| jie | string | 结 |\\n| mstate | integer | 状态 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/caoout/selchuo?cCashier={}&reid=1&mename=药1\\\"\\n\\n响应（200 OK）:\\njson\\n1\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\\n\\n## 添加处方药品\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/caoout/addchuo\\n- 描述：添加处方药品\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| cCashier | object | 收费信息 | 是 |\\n| ostate | integer | 项目状态 | 是 |\\n\\n### CCashier 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| cashier | integer | 处方id |\\n| reportId | integer | 挂号id |\\n| durgname | string | 药品名称 |\\n| durgnum | integer | 药品数量 |\\n| repiceprice | double | 价格 |\\n| repicetotal | double | 小计 |\\n| state | integer | 状态 |\\n| ostate | integer | 项目状态 |\\n| jie | string | 结 |\\n| mstate | integer | 状态 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X POST \\\"http://example.com/caoout/addchuo\\\" -d '{\\\"cCashier\\\":{}, \\\"ostate\\\":1}'\\n\\n响应（200 OK）:\\njson\\n1\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\\n\\n## 如果处方中有该药品则修改该药品的数量和价钱\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/caoout/updchuo\\n- 描述：如果处方中有该药品则修改该药品的数量和价钱\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| cCashier | object | 收费信息 | 是 |\\n| cPharmacy | object | 药品信息 | 是 |\\n\\n### CCashier 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| cashier | integer | 处方id |\\n| reportId | integer | 挂号id |\\n| durgname | string | 药品名称 |\\n| durgnum | integer | 药品数量 |\\n| repiceprice | double | 价格 |\\n| repicetotal | double | 小计 |\\n| state | integer | 状态 |\\n| ostate | integer | 项目状态 |\\n| jie | string | 结 |\\n| mstate | integer | 状态 |\\n\\n### CPharmacy 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| pharmacyId | integer | 药品id |\\n| pharmacyName | string | 药品名称 |\\n| drugstoreId | integer | 领货单位外键 |\\n| skullId | integer | 经办人Id |\\n| warehouseId | integer | 库房Id |\\n| unit | integer | 库房 |\\n| sellingPrice | double | 售价 |\\n| area | integer | 产地 |\\n| type | integer | 类型 |\\n| produceDate | date | 生产日期 |\\n| validDate | date | 保质期 |\\n| drugstorenum | integer | 数量 |\\n| skullbatch | integer | 批号 |\\n| unitname | string | 单位名称 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X POST \\\"http://example.com/caoout/updchuo\\\" -d '{\\\"cCashier\\\":{}, \\\"cPharmacy\\\":{}}'\\n\\n响应（200 OK）:\\njson\\n1\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\\n\\n## 删除处方中的药品\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/caoout/delo\\n- 描述：删除处方中的药品\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| cCashier | object | 收费信息 | 是 |\\n\\n### CCashier 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| cashier | integer | 处方id |\\n| reportId | integer | 挂号id |\\n| durgname | string | 药品名称 |\\n| durgnum | integer | 药品数量 |\\n| repiceprice | double | 价格 |\\n| repicetotal | double | 小计 |\\n| state | integer | 状态 |\\n| ostate | integer | 项目状态 |\\n| jie | string | 结 |\\n| mstate | integer | 状态 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X POST \\\"http://example.com/caoout/delo\\\" -d '{\\\"cCashier\\\":{}}'\\n\\n响应（200 OK）:\\njson\\n\\\"删除成功\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"删除失败\\\"\\n\\n## 查询处方的总价钱\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/caoout/selch\\n- 描述：查询处方的总价钱\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| cCashier | object | 收费信息 | 是 |\\n\\n### CCashier 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| cashier | integer | 处方id |\\n| reportId | integer | 挂号id |\\n| durgname | string | 药品名称 |\\n| durgnum | integer | 药品数量 |\\n| repiceprice | double | 价格 |\\n| repicetotal | double | 小计 |\\n| state | integer | 状态 |\\n| ostate | integer | 项目状态 |\\n| jie | string | 结 |\\n| mstate | integer | 状态 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/caoout/selch?cCashier={}\\\"\\n\\n响应（200 OK）:\\njson\\n100.5\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\\n\\n## 收费\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/caoout/shoufei\\n- 描述：收费\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| reportVo | object | 报告信息 | 是 |\\n\\n### ReportVo 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| reportId | integer | 挂号id |\\n| reportName | string | 患者姓名 |\\n| sex | string | 性别 |\\n| age | integer | 年龄 |\\n| price | double | 价钱 |\\n| time | date | 时间 |\\n| users | string | 操作员 |\\n| state | integer | 状态 |\\n| ddepartmentid | integer | 科室id |\\n| ddoctorid | integer | 医生id |\\n| dredisteredid | integer | 挂号类型id |\\n| department | string | 科室 |\\n| doctorName | string | 医生姓名 |\\n| type | string | 挂号类型 |\\n| carid | string | 身份证号 |\\n| phone | string | 电话 |\\n| carido | integer | 身份证前 |\\n| caridt | integer | 身份证后 |\\n| cc | integer | 判断日期 |\\n| datime | string | 时间 |\\n| zhuan | string | 转 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n\\n### 使用示例\\ncurl -X POST \\\"http://example.com/caoout/shoufei\\\" -d '{\\\"reportVo\\\":{}}'\\n\\n响应（200 OK）:\\njson\\n1\\n\\n响应（400 Bad Request）:\\njson\\n\\\"\\\"\",\n  \"line\": \"| /caoout/xiang | GET | 进入处方划价页面 |\\n| /caoout/out | GET | 进入收费页面 |\\n| /caoout/selout | GET | 查询药品所有信息 |\\n| /caoout/selchuo | GET | 查询处方中是否有这个药 |\\n| /caoout/addchuo | POST | 添加处方药品 |\\n| /caoout/updchuo | POST | 如果处方中有该药品则修改该药品的数量和价钱 |\\n| /caoout/delo | POST | 删除处方中的药品 |\\n| /caoout/selch | GET | 查询处方的总价钱 |\\n| /caoout/shoufei | POST | 收费 |\"\n}\n```"
//            ,"```json\n{\n  \"api_md\": \"## 查询生产地址\\n\\n### 端点详情\\n- 方法：GET\\n- URL：/area/findAllArea\\n- 描述：查询生产地址\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| Area | object | 地址信息 | 是 |\\n| page | integer | 页码 | 是 |\\n| limit | integer | 每页条数 | 是 |\\n\\n### Area 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| areaId | integer | 地址ID |\\n| areaName | string | 地址名称 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| code | integer | 状态码 |\\n| msg | string | 消息 |\\n| count | integer | 总条数 |\\n| data | array | 数据列表 |\\n\\n### Data 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| areaId | integer | 地址ID |\\n| areaName | string | 地址名称 |\\n\\n### 使用示例\\ncurl -X GET \\\"http://example.com/area/findAllArea?page=1&limit=10\\\"\\n\\n响应（200 OK）:\\njson\\n{\\n  \\\"code\\\": 0,\\n  \\\"msg\\\": \\\"\\\",\\n  \\\"count\\\": 100,\\n  \\\"data\\\": [{\\n    \\\"areaId\\\": 1,\\n    \\\"areaName\\\": \\\"地址1\\\"\\n  }]\\n}\\n\\n响应（400 Bad Request）:\\njson\\n{\\n  \\\"code\\\": 400,\\n  \\\"msg\\\": \\\"参数错误\\\"\\n}\\n\\n## 添加生产地址\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/area/addArea\\n- 描述：添加生产地址\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| Area | object | 地址信息 | 是 |\\n\\n### Area 结构\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| areaId | integer | 地址ID |\\n| areaName | string | 地址名称 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| 结果 | string | 操作结果 |\\n\\n### 使用示例\\ncurl -X POST -d '{\\\"areaName\\\":\\\"新地址\\\"}' \\\"http://example.com/area/addArea\\\"\\n\\n响应（200 OK）:\\njson\\n\\\"添加成功\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"地址已存在\\\"\\n\\n## 删除生产地址\\n\\n### 端点详情\\n- 方法：POST\\n- URL：/area/deleteArea\\n- 描述：删除生产地址\\n\\n### 请求参数\\n| 参数名称 | 类型 | 描述 | 是否必填 |\\n|----------|------|------|----------|\\n| areaId | integer | 地址ID | 是 |\\n\\n### 响应参数\\n| 字段名称 | 类型 | 描述 |\\n|----------|------|------|\\n| 结果 | string | 操作结果 |\\n\\n### 使用示例\\ncurl -X POST -d 'areaId=1' \\\"http://example.com/area/deleteArea\\\"\\n\\n响应（200 OK）:\\njson\\n\\\"删除成功\\\"\\n\\n响应（400 Bad Request）:\\njson\\n\\\"删除失败\\\"\",\n  \"line\": \"| /area/findAllArea | GET | 查询生产地址 |\\n| /area/addArea | POST | 添加生产地址 |\\n| /area/deleteArea | POST | 删除生产地址 |\"\n}\n```"
//        };
//
//        List<ApiDocumentation> result = fixMarkdown(arg1);
//
//        // 打印结果
//        System.out.println("Fixed Markdown:");
//        for (ApiDocumentation doc : result) {
//            System.out.println(doc);
//            System.out.println("---");
//        }
//    }


}