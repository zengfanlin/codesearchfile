package com.example.codeupload;

import com.alibaba.fastjson.JSONObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import lombok.extern.slf4j.Slf4j;
import net.sf.jmimemagic.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@Slf4j
public class CodeuploadApplication {

    public static void main(String[] args) throws IOException, MagicParseException, MagicException, MagicMatchNotFoundException {
        SpringApplication.run(CodeuploadApplication.class, args);
        ApplicationContext context = SpringUtil.getApplicationContext();
        JestClient jestClient = context.getBean(JestClient.class);// 注意是UserServiceI
//        Operator(jestClient);
        Print(jestClient);
    }

    private static void Operator(JestClient jestClient) throws IOException {
        //检查索引是否存在
//        JestResult result = jestClient.execute(new IndicesExists.Builder("ros-logs-db").build());
        //创建索引
        JestResult result = jestClient.execute(new CreateIndex.Builder("rosdb").build());
        //加shards和分片参数
//        Map<String, Object> settings = new HashMap<>();
//        settings.put("number_of_shards", 11);
//        settings.put("number_of_replicas", 2);
//        jestClient.execute(new CreateIndex.Builder("employees").settings(settings).build());

        System.out.println(result);

    }


    private static void Print(JestClient jestClient) throws FileNotFoundException, MagicParseException, MagicException, MagicMatchNotFoundException {
        //第五种
        ApplicationHome h = new ApplicationHome(CodeuploadApplication.class);
        File jarF = h.getSource();
        String path = jarF.getParentFile().toString();
        System.out.println("--------------");
        System.out.println("开始遍历当前目录:" + path);
        System.out.println("--------------");
        path = "C:\\001-workspace\\test\\mongodemo";

        traverseFolder(path, path,jestClient);
    }

    private static void traverseFolder(String rootpath, String path,JestClient jestClient) throws MagicParseException, MagicException, MagicMatchNotFoundException {
        File file1 = new File(path);
        if (file1.exists() && !file1.getName().startsWith(".") && !file1.getName().contains("target")) {
            File[] files = file1.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        traverseFolder(rootpath, file.getAbsolutePath(),jestClient);
                    } else {
                        CreateIndex(file, rootpath,jestClient);
                    }
                }
            }
        }
    }

    private static void CreateIndex(File file, String rootpath,JestClient jestClient) throws MagicParseException, MagicException, MagicMatchNotFoundException {
        if (file.getName().startsWith("~$")) return;
        if (file.getName().startsWith(".")) return;

        Magic parser = new Magic();
        MagicMatch match = parser.getMagicMatch(file, false);
        String fileType = match.getMimeType();
        String md5 = Md5CaculateUtil.getMD5(file);
        String filename = file.getAbsolutePath().replace(rootpath, "");
        log.info("文件名：{}，文件类型：{}，MD5：{}，建立索引：{}", filename, fileType, md5, "false");

//        1. 给ES中索引(保存)一个文档
        Article article = new Article();
        article.setTitle(file.getName());
        article.setAuthor(file.getParent());
        article.setPath(file.getPath());
        article.setContent(readToString(file, fileType));
        article.setMD5(md5);
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
    }

    //读取文件内容转换为字符串
    private static String readToString(File file, String fileType) {
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
}
