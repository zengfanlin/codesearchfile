package com.example.codeupload;

import io.searchbox.annotations.JestId;
import lombok.Data;

/**
 * @description:
 * @author: fanlin.zeng
 * @time: 2020-7-25 10:49
 */
@Data
public class Article {
    @JestId
    private String id;
    private String title;
    private String path;
    private String md5;
    private String content;

}
