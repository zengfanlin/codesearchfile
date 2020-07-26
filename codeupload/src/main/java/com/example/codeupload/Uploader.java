package com.example.codeupload;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.crypto.SecureUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.core.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


import java.io.File;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public class Uploader {
    @Value("${file.index.allowfiletype}")
    String allowfiletyps;
    @Value("${file.index.rootpath}")
    String rootpath;
    @Autowired
    EsClient esclient;
    @Value("${file.index.name}")
    String indexname;
    @Value("${file.index.type}")
    String indextype;

    public void run() throws Exception {
        //第五种
//        ApplicationHome h = new ApplicationHome(CodeuploadApplication.class);
//        File jarF = h.getSource();
//        String path = jarF.getParentFile().toString();
        System.out.println("--------------");
        System.out.println("开始遍历当前目录:" + rootpath);
        System.out.println("--------------");
        try {
            traverseFolder(rootpath);
//            esclient.deleteIndex(indexname);

        } catch (Exception e) {
            throw e;
        }
    }

    private void CreateIndex(File file) throws Exception {
        if (file.getName().startsWith("~$")) return;
        if (file.getName().startsWith(".")) return;
        String fileType = FileTypeUtil.getType(file).toLowerCase();
        if (allowfiletyps.indexOf(fileType) <= 0) {
            return;
        }

        String md5 = SecureUtil.md5(file);
        String filename = file.getAbsolutePath().replace(rootpath, "");
//        log.info("文件名：{}，文件类型：{}，MD5：{}，建立索引：{}", filename, fileType, md5, "false");
        //默认UTF-8编码，可以在构造中传入第二个参数做为编码
        FileReader fileReader = new FileReader(file);

//       1. 给ES中索引(保存)一个文档
        Article article = new Article();
        article.setTitle(file.getName());
        article.setPath(filename);
        article.setContent(fileReader.readString());
        article.setMd5(md5);


//        log.info("文件名：{}，路径：{}，MD5：{}，建立索引：{}", article.getTitle(), article.getPath(), article.getMd5(), "false");
//        log.info(article.getContent());
//        2. 构建一个索引

        SearchResult result = esclient.MatchQuery(indexname, indextype, "path", article.getPath(), 0, 1);
        if (result.isSucceeded()) {
            JsonElement jsonElement = result.getJsonObject().getAsJsonObject("hits").get("total");
            if (Convert.toInt(jsonElement) > 0) {
                SearchResult.Hit<Article, Void> hit = result.getFirstHit(Article.class);
                Article asource = hit.source;
                if (article.getMd5().equals(asource.getMd5())) {
                    log.info(article.getTitle() + "----重复");
                } else {
                    log.info(article.getTitle() + "----不重复，更新文件");
                    asource.setMd5(article.getMd5());
                    asource.setTitle(article.getTitle());
                    asource.setContent(article.getContent());

                    Map<String,Object> map=new HashMap<String,Object>();
                    map.put("id",asource.getId());
                    map.put("title",asource.getTitle());
                    map.put("path",asource.getPath());
                    map.put("md5",asource.getMd5());
                    map.put("content",asource.getContent());
                    boolean b = esclient.updateIndexDoc(indexname, indextype, map);
                    if(b){
                        log.info(article.getPath()+"更新成功！");
                    }
                    else {
                        log.info(article.getPath()+"更新失败！");
                    }
                }
            } else {
                log.info(article.getTitle() + "----文件不存在 ，新增");
                String id = esclient.createIndexDoc(indexname, indextype, article);
                log.info("创建成功:" + id);
            }

        } else {
            log.info(result.getErrorMessage());
        }
    }

    public void traverseFolder(String path) throws Exception {
        File file1 = new File(path);
        if (file1.exists() && !file1.getName().startsWith(".") && !file1.getName().contains("target")) {
            File[] files = file1.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        traverseFolder(file.getAbsolutePath());
                    } else {
                        CreateIndex(file);
                    }
                }
            }
        }
    }

}
