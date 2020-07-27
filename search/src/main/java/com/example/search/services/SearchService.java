package com.example.search.services;

import cn.hutool.core.convert.Convert;
import com.example.search.model.Article;
import com.google.gson.JsonElement;
import io.searchbox.core.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: fanlin.zeng
 * @time: 2020-7-27 8:58
 */
@Component
@Slf4j
public class SearchService {
    @Autowired
    EsClient esclient;

    @Value("${file.index.name}")
    String indexname;
    @Value("${file.index.type}")
    String indextype;

    public List<Article> getList(String keyword, int from, int size) throws IOException {
        SearchResult result = esclient.MultiMatchQuery(indexname, indextype,  keyword, "content","title");
        List<Article> list = new ArrayList<>();
        if (result.isSucceeded()) {
            JsonElement jsonElement = result.getJsonObject().getAsJsonObject("hits").get("total");
            if (Convert.toInt(jsonElement) > 0) {
                List<SearchResult.Hit<Article, Void>> hits = result.getHits(Article.class);
                for (SearchResult.Hit<Article, Void> hit : hits) {
                    Article talk = hit.source;
                    list.add(hit.source);
                }
            }
        }
        return list;
    }
}
