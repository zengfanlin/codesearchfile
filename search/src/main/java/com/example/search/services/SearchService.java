package com.example.search.services;

import cn.hutool.core.convert.Convert;
import com.example.search.model.CodeDocment;
import com.google.gson.JsonElement;
import io.searchbox.core.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public List<CodeDocment> getList(String keyword, int from, int size) throws IOException {
        SearchResult result = esclient.MultiMatchQuery(indexname, indextype,  keyword, "content","title");
        List<CodeDocment> list = new ArrayList<>();
        if (result.isSucceeded()) {
            JsonElement jsonElement = result.getJsonObject().getAsJsonObject("hits").get("total");
            if (Convert.toInt(jsonElement) > 0) {
                List<SearchResult.Hit<CodeDocment, Void>> hits = result.getHits(CodeDocment.class);
                for (SearchResult.Hit<CodeDocment, Void> hit : hits) {
                    CodeDocment talk = hit.source;
                    list.add(hit.source);
                }
            }
        }
        return list;
    }
}
