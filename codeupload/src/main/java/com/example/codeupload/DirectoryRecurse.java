package com.example.codeupload;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @description:
 * @author: fanlin.zeng
 * @time: 2020-7-25 10:51
 */
@Component
@Slf4j
public class DirectoryRecurse {
    @Autowired
    private JestClient jestClient;


    //读取文件内容转换为字符串
    private String readToString(File file, String fileType) {
        StringBuffer result = new StringBuffer();
        switch (fileType) {
            case "text/plain":
            case "java":
            case "yml":
            case "txt":
                try (FileInputStream in = new FileInputStream(file)) {
                    Long filelength = file.length();
                    byte[] filecontent = new byte[filelength.intValue()];
                    in.read(filecontent);
                    result.append(new String(filecontent, "utf8"));
                } catch (FileNotFoundException e) {
                    log.error("{}", e.getLocalizedMessage());
                } catch (IOException e) {
                    log.error("{}", e.getLocalizedMessage());
                }
                break;

        }
        return result.toString();
    }

    //判断是否已经索引
    private JSONObject isIndex(File file) {
        JSONObject result = new JSONObject();
        //用MD5生成文件指纹,搜索该指纹是否已经索引
        String fileFingerprint = SecureUtil.md5(file);
        result.put("MD5", fileFingerprint);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("MD5", fileFingerprint));
        Search search = new Search.Builder(searchSourceBuilder.toString()).addIndex("diskfile").addType("files").build();
        try {
            //执行
            SearchResult searchResult = jestClient.execute(search);
            if (searchResult.getTotal() > 0) {
                result.put("isIndex", true);
            } else {
                result.put("isIndex", false);
            }
        } catch (IOException e) {
            log.error("{}", e.getLocalizedMessage());
        }
        return result;
    }

    //对文件目录及内容创建索引
    private void createIndex(File file, String method) {
        //忽略掉临时文件，以~$起始的文件名
        if (file.getName().startsWith("~$")) return;
        String fileType = null;

        switch (fileType) {
            case "text/plain":
            case "java":
            case "c":
            case "cpp":
            case "txt":
            case "doc":
            case "docx":
                JSONObject isIndexResult = isIndex(file);
                log.info("文件名：{}，文件类型：{}，MD5：{}，建立索引：{}", file.getPath(), fileType, isIndexResult.getString("fileFingerprint"), isIndexResult.getBoolean("isIndex"));

                if (isIndexResult.getBoolean("isIndex")) break;
                //1. 给ES中索引(保存)一个文档
                Article article = new Article();
                article.setTitle(file.getName());
                article.setPath(file.getPath());
                article.setContent(readToString(file, fileType));
                article.setMd5(isIndexResult.getString("MD5"));
                //2. 构建一个索引
                Index index = new Index.Builder(article).index("diskfile").type("files").build();
                try {
                    //3. 执行
                    if (!jestClient.execute(index).getId().isEmpty()) {
                        log.info("构建索引成功！");
                    }
                } catch (IOException e) {
                    log.error("{}", e.getLocalizedMessage());
                }
                break;
        }
    }


}
