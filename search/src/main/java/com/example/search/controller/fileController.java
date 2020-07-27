package com.example.search.controller;

import com.example.search.model.Article;
import com.example.search.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: fanlin.zeng
 * @time: 2020-7-27 8:53
 */
@RestController
public class fileController {
    @Autowired
    SearchService service;

    @GetMapping("/api/file")
    public ModelAndView index() throws IOException {
        List<Article> list = service.getList("Uploader", 0, 10);
        return new ModelAndView("file/article", "articlelist", list);
    }

}
