package com.example.search.controller;

import com.example.search.model.CodeDocment;
import com.example.search.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
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
    public List index(@RequestParam(value = "word") String word) throws IOException {
        List<CodeDocment> list = service.getList(word, 0, 999);
        return list;
    }

}
