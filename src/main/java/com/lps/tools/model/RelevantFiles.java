package com.lps.tools.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
public class RelevantFiles {

    /**
     * 配置文件
     */
    List<String> profiles ;

    /**
     * controller
     */
    List<String> controllers;

    /**
     * entity、dto、vo
     */
    Map<String, String> dataClasses ;

}