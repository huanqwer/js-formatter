package com.zfbgt.js.formatter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LaoQin
 * @Description TODO 启动类
 * @date 2020/03/23
 */
@Slf4j
@SpringBootApplication
public class JsFormatterApplication {

    /**
     * @Description TODO 文件路径
     * @author LaoQin
     * @date 2020/03/23
     */
    private static String path;

    public static void main(String[] args) {
        log.info("项目启动...");
        //判断是否传入文件路径
        if (args.length == 0) {
            log.info("找不到文件...");
            log.info("终止程序！");
            //退出程序
            System.exit(-1);
        }
        log.info("文件路径为：" + args[0]);
        path = args[0];
        SpringApplication.run(JsFormatterApplication.class, args);
    }

    /**
     * @Description TODO 预编译Patterns
     * @author LaoQin
     * @date 2020/03/23
     */
    private static Pattern SCRIPT_PATTERN = Pattern.compile("}$");
    private static Pattern METHOD_PATTERN = Pattern.compile("(\\s+)(@)(\\w+)(=\")(.*)(\")");
    private static Pattern INNER_METHOD_PATTERN = Pattern.compile("(.*)(\\s*)([:\\(])");
    private static Pattern METHOD_TEMPLATE_PATTERN = Pattern.compile("methods\\s*:\\s*\\{[\\s\\S]*");
    private static Pattern METHOD_NAME_PATTERN = Pattern.compile("(\\S*)\\(");

    /**
     * @Description TODO 预编译常量列表
     * @author LaoQin
     * @date 2020/03/23
     */
    private static String METHODS = "methods";
    private static String METHOD_TEMPLATE = ",methods:{}}";

    @PostConstruct
    public void formatFile() throws IOException {
        log.info("开始处理文件...");
        log.info("获得文件路径为" + path);
        File file = new File(path);
        Document document = Jsoup.parse(file, "UTF-8");
        //获取模板代码
        Element template = document.getElementsByTag("template").get(0);
        //获取js代码
        Element script = document.getElementsByTag("script").get(0);
        //没有methods节点则创建节点
        String replacedScript = script.html();
        TreeSet<String> nowMethodSet = new TreeSet<>();
        if (!script.html().contains(METHODS)) {
            Matcher matcher = SCRIPT_PATTERN.matcher(script.html());
            if (matcher.find()) {
                replacedScript = matcher.replaceAll(METHOD_TEMPLATE);
            }
        }else {
            //如果存在methods节点则维护一个现有method set
            Matcher matcher = METHOD_TEMPLATE_PATTERN.matcher(replacedScript);
            if(matcher.find()){
                String methodTemplateGroup = matcher.group();
                Matcher innerMatcher = INNER_METHOD_PATTERN.matcher(methodTemplateGroup);
                while (innerMatcher.find()){
                    String trimStr = innerMatcher.group(1).trim();
                    if(trimStr.contains(":")){
                        trimStr = trimStr.split(":")[0].trim();
                    }
                    //除了methods本身以外的都加入nowMethodSet
                    if(!METHODS.equals(trimStr)){
                        nowMethodSet.add(trimStr);
                    }
                }
            }
        }

        //匹配方法的正则表达式
        Matcher matcher = METHOD_PATTERN.matcher(template.html());
        Set methodSet = new TreeSet<String>();
        while (matcher.find()) {
            //获取方法定义
            String method = matcher.group(5);
            Matcher methodNameMatcher = METHOD_NAME_PATTERN.matcher(method);
            String methodName = null;
            if(methodNameMatcher.find()){
                methodName = methodNameMatcher.group(1);
            }
            if(nowMethodSet.contains(methodName==null?method:methodName)){
                continue;
            }
            //补全方法定义
            if(!method.contains("(")){
                method+="()";
            }
            //补全方法体
            method+="{}";
            //添加至方法列表
            methodSet.add(method);
        }
        //处理待添加队列
        String methodStr = methodSet.toString();
        String replacedMethodStr = methodStr.replace("[", "").replace("]", "");
        if(nowMethodSet.size()>0){
            replacedMethodStr = ","+replacedMethodStr;
        }
        replacedMethodStr+="}}";
        StringBuffer sb = new StringBuffer(replacedScript);
        sb.replace(sb.lastIndexOf("}"),sb.length(),"");
        sb.replace(sb.lastIndexOf("}"),sb.length(),"");
        sb.append(replacedMethodStr);
        script.text(sb.toString().replaceAll("\n","&huanhang"));
        //过滤多余标签
        String tempStr = tagFilter(document.html(), "html","head","body");
        tempStr = tempStr.replaceAll("&amp;huanhang","\n").replaceAll("&gt;",">");
        FileWriter writer = new FileWriter(file);
        writer.write(tempStr);
        writer.flush();
        writer.close();
    }
    /**
     * @Description TODO 过滤某特定标签
     * @author LaoQin
     * @date 2020/03/20
     * @param str 待过滤标签
     * @param tagNames 过滤标签集合
     * @return java.lang.String 过滤后的标签
     */
    private String tagFilter(String str,String ... tagNames){
        for(String tagName: tagNames){
            str = str.replace("<" + tagName + ">", "")
                    .replace("</" + tagName + ">", "");
        }
        return str;
    }
}
