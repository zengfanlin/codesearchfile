package com.example.codeupload;

import cn.hutool.extra.spring.SpringUtil;
import io.searchbox.client.JestClient;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.io.IOException;


@SpringBootApplication
@ComponentScan(basePackages = {"cn.hutool.extra.spring", "com.example.codeupload"})
//@Import(cn.hutool.extra.spring.SpringUtil.class)
public class CodeuploadApplication {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CodeuploadApplication.class);

    public static void main(String[] args) throws Exception {
        SpringApplication.run(CodeuploadApplication.class, args);
        Uploader uploader = SpringUtil.getBean(Uploader.class);
        uploader.run();

    }
}
