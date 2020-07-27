package com.example.search.model;

import io.searchbox.annotations.JestId;
import lombok.Data;

/**
 * @description:
 * @author: fanlin.zeng
 * @time: 2020-7-25 10:49
 */
@Data
public class CodeDocment {
    @JestId
    private String id;
    private String title;
    private String path;
    private String md5;
    private String content;

}
