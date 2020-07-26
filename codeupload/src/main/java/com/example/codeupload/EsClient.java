package com.example.codeupload;

import cn.hutool.db.Page;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.strings.StringUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EsClient {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(EsClient.class);

    @Autowired
    JestClient jestClient;

    /**
     * index是否存在
     *
     * @param indexname
     * @return
     * @throws IOException
     */
    public JestResult IndicesExists(String indexname) throws IOException {
        //检查索引是否存在
        JestResult result = jestClient.execute(new IndicesExists.Builder(indexname).build());
        return result;
    }

    /**
     * 创建索引
     *
     * @param indexname
     * @return
     * @throws IOException
     */
    public Boolean CreateIndex(String indexname) throws IOException {
        //检查索引是否存在
        //加shards和分片参数
        Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", 5);
        settings.put("number_of_replicas", 1);
        JestResult result = jestClient.execute(new CreateIndex.Builder(indexname).settings(settings).build());
        return result.isSucceeded();
    }

    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @return true/false
     */
    public boolean deleteIndex(String indexName) throws IOException {
        JestResult jr = null;
        boolean bool = false;
        try {
            jr = jestClient.execute(new DeleteIndex.Builder(indexName).build());
            bool = jr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 获取索引映射
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @return Mapping映射
     */
    public String getIndexMapping(String indexName, String indexType) throws IOException {
        GetMapping getMapping = new GetMapping.Builder().addIndex(indexName).addType(indexType).build();
        JestResult jr = null;
        String string = "";
        try {
            jr = jestClient.execute(getMapping);
            string = jr.getJsonString();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return string;
    }

    /**
     * 创建索引映射
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param source    索引映射
     * @return true/false
     */
    public boolean createIndexMapping(String indexName, String indexType, String source) throws IOException {
        PutMapping putMapping = new PutMapping.Builder(indexName, indexType, source).build();
        JestResult jr = null;
        boolean bool = false;
        try {
            jr = jestClient.execute(putMapping);
            bool = jr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 根据文档id查询文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param id        文档id
     * @return 文档
     */
    public Map getIndexDocById(String indexName, String indexType, String id) throws IOException {
        Get get = new Get.Builder(indexName, id).type(indexType).build();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            JestResult result = jestClient.execute(get);
            JsonObject jsonObject = result.getJsonObject().get("_source").getAsJsonObject();
            Gson gson = new Gson();
            map = gson.fromJson(jsonObject, map.getClass());
            //将索引id存入map集合
            map.put("id", id);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return map;
    }

    /**
     * 根据文档id查询文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param id        文档id
     * @return 文档
     */
    public JsonObject getIndexDocByIdForJson(String indexName, String indexType, String id) throws IOException {
        Get get = new Get.Builder(indexName, id).type(indexType).build();
        Map<String, Object> map = new HashMap<String, Object>();
        JsonObject jsonObject = null;
        try {
            JestResult result = jestClient.execute(get);
            jsonObject = result.getJsonObject().get("_source").getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return jsonObject;
    }

    /**
     * 创建文档
     *
     * @param indexname
     * @param typename
     * @param obj
     * @return
     * @throws IOException
     */
    public String createIndexDoc(String indexname, String typename, Object obj) throws IOException {
        Index index = new Index.Builder(obj).index(indexname).type(typename).build();
        DocumentResult documentResult = jestClient.execute(index);//.getId().isEmpty()
        return documentResult.getId();
    }

    /**
     * 根据文档id删除索引文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param id        文档唯一id
     */
    public boolean deleteIndexDoc(String indexName, String indexType, String id) throws IOException {
        DocumentResult dr = null;
        boolean bool = false;
        try {
            dr = jestClient.execute(new Delete.Builder(id).index(indexName).type(indexType).build());
            bool = dr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 根据文档id删除索引文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param search    检索式
     */
    public boolean deleteIndexDocBySearch(String indexName, String indexType, String search) throws IOException {
        DocumentResult dr = null;
        boolean bool = false;
        try {
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.queryStringQuery(search).defaultOperator(Operator.AND));
            Search sb = new Search.Builder(sourceBuilder.toString()).addIndex(indexName).addType(indexType).build();
            dr = jestClient.execute(new Delete.Builder(sb.toString()).index(indexName).type(indexType).build());
            bool = dr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 批量索引文档，（因为设置了唯一id，所以该方法执行时索引字段必须有id）,id存在更新，不存在添加
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param list      List集合对戏
     * @return true/false
     */
    public boolean upsertIndexDocBulk(String indexName, String indexType, List<Map<String, Object>> list) throws IOException {
        Bulk.Builder bulk = new Bulk.Builder().defaultIndex(indexName).defaultType(indexType);
        for (Map<String, Object> map : list) {
            //设置文档唯一id值，id存在执行更新，不存在执行添加
            Index index = new Index.Builder(map).id(map.get("id").toString()).build();
            bulk.addAction(index);
        }
        BulkResult br = null;
        boolean boll = false;
        try {
            br = jestClient.execute(bulk.build());
            boll = br.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return boll;
    }

    /**
     * 批量新增索引文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param list      集合
     * @return true/false
     */
    public boolean updateIndexDocBulk(String indexName, String indexType, List<Map<String, Object>> list) throws IOException {
        Bulk.Builder bulk = new Bulk.Builder().defaultIndex(indexName).defaultType(indexType);
        for (Map<String, Object> map : list) {
            //如未设置索引唯一id值，则唯一id会默认生成，索引操作为添加操作
            Index index = new Index.Builder(map).build();
            bulk.addAction(index);
        }
        BulkResult br = null;
        boolean boll = false;
        try {
            br = jestClient.execute(bulk.build());
            boll = br.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return boll;
    }

    /**
     * 单条更新索引文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param map       map集合
     * @return true/false
     */
    public boolean updateIndexDoc(String indexName, String indexType, Map<String, Object> map) throws IOException {
        Index index = new Index.Builder(map).index(indexName).type(indexType).id(map.get("id").toString()).refresh(true).build();
        JestResult jr = null;
        boolean bool = false;
        try {
            jr = jestClient.execute(index);
            bool = jr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 单条更新索引文档
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param id        索引id
     * @param map       map集合
     * @return true/false
     */
    public boolean updateIndexDoc(String indexName, String indexType, String id, Map<String, Object> map) throws IOException {
        Index index = new Index.Builder(map).index(indexName).type(indexType).id(id).refresh(true).build();
        JestResult jr = null;
        boolean bool = false;
        try {
            jr = jestClient.execute(index);
            bool = jr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 单条更新索引文档
     *
     * @param indexName  索引名称
     * @param indexType  索引类型
     * @param jsonObject jsonObject
     * @return true/false
     */
    public boolean updateIndexDoc(String indexName, String indexType, JsonObject jsonObject) throws IOException {
        Index index = new Index.Builder(jsonObject).index(indexName).type(indexType).id(jsonObject.get("id").toString()).build();
        JestResult jr = null;
        boolean bool = false;
        try {
            jr = jestClient.execute(index);
            bool = jr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 单条更新索引文档
     *
     * @param indexName  索引名称
     * @param indexType  索引类型
     * @param id         索引id
     * @param jsonObject jsonObject
     * @return true/false
     */
    public boolean updateIndexDoc(String indexName, String indexType, String id, JsonObject jsonObject) throws IOException {
        Index index = new Index.Builder(jsonObject).index(indexName).type(indexType).id(id).build();
        JestResult jr = null;
        boolean bool = false;
        try {
            jr = jestClient.execute(index);
            bool = jr.isSucceeded();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return bool;
    }

    /**
     * 聚类示例
     *
     * @param indexName   索引名称
     * @param indexType   索引类型
     * @param field       操作字段
     * @param facetsField 聚类字段
     */
    public void facetsSearch(String indexName, String indexType, String field, String facetsField) throws IOException {
        SumAggregationBuilder sumBuilder = AggregationBuilders.sum(field).field(facetsField);
        Search sb = new Search.Builder(sumBuilder.toString()).addIndex(indexName).addType(indexType).build();

        SearchResult result = null;
        try {
            result = jestClient.execute(sb);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * 统计文档总数
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param search    检索式
     * @return 文档总数
     */
    public Double count(String indexName, String indexType, String search) throws IOException {
        Count count = new Count.Builder().addIndex(indexName).addType(indexType).query(search).build();
        CountResult cr = null;
        Double db = 0d;
        try {
            cr = jestClient.execute(count);
            db = cr.getCount();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        return db;
    }

    /**
     * 返回文档唯一id，根据检索式
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param search    检索式
     * @return
     */
    public String getIndexDocIds(String indexName, String indexType, String search) throws IOException {
        //设置默认参检索引
        indexName = StringUtils.isBlank(indexName) ? "*" : indexName;
        //设置默认检索全部
        search = StringUtils.isBlank(search) ? "*" : search;
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置默认分词后AND连接，StringQuery支持通配符
        sourceBuilder.query(QueryBuilders.queryStringQuery(search).defaultOperator(Operator.AND));
        sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));

        Search sb = new Search.Builder(sourceBuilder.toString()).addIndex(indexName).addType(indexType).build();
        SearchResult result = null;
        try {
            result = jestClient.execute(sb);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        long totalCount = 0;
        StringBuilder sb1 = new StringBuilder();
        if (result != null && result.isSucceeded()) {
            //获取总记录数
            totalCount = result.getTotal();
            if (totalCount > 0) {
                Map<String, Object> map = new HashMap<String, Object>();
                JsonArray jsonArray = result.getJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    String id = jsonArray.get(i).getAsJsonObject().get("_id").toString();
                    sb1.append(id + ",");
                }
            }
        }
        return StringUtils.isNotBlank(sb1.toString()) ? sb1.toString().substring(0, sb1.toString().length() - 1) : "";
    }

    public String search(String index, String type, String field, Object value) throws Exception {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(new MatchQueryBuilder(field, value));
        log.info(searchSourceBuilder.toString());
        SearchResult result = jestClient.execute(new Search.Builder(searchSourceBuilder.toString())
                .addIndex(index)
                .addType(type)
                .build());
        return result.getJsonString();
    }

    /**
     * 精确查找
     *
     * @param index
     * @param type
     * @param field
     * @param value
     * @return
     * @throws IOException
     */
    public SearchResult MatchQuery(String index, String type, String field, Object value, int from, int size) throws IOException {
        //构建搜索对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //filter
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder(field, value);
        boolQueryBuilder.filter(termQueryBuilder);

        //must
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder(field, value);
        boolQueryBuilder.must(matchQueryBuilder);
        searchSourceBuilder.query(boolQueryBuilder);
        //分页
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        //高亮
        searchSourceBuilder.highlight();
        String dslStr = searchSourceBuilder.toString();
//    List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        Search search = new Search.Builder(dslStr).addIndex(index).addType(type).build();
        SearchResult result = jestClient.execute(search);
        return result;
    }

    /**
     * 查询，根据检索式查询
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param search    检索式
     * @param sortField 排序字段
     * @return 返回List结果集
     */
    public List baseSearch(String indexName, String indexType, String search, String sortField) throws IOException {
        //设置默认参检索引
        indexName = StringUtils.isBlank(indexName) ? "*" : indexName;
        //设置默认检索全部
        search = StringUtils.isBlank(search) ? "*" : search;
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置默认分词后AND连接，StringQuery支持通配符
        sourceBuilder.query(QueryBuilders.queryStringQuery(search).defaultOperator(Operator.AND));
        //设置排序规则
        if (StringUtils.isNotBlank(sortField)) {
            sourceBuilder.sort(new FieldSortBuilder(sortField).order(SortOrder.ASC));
        } else {
            sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        }
        Search sb = new Search.Builder(sourceBuilder.toString()).addIndex(indexName).addType(indexType).build();
        SearchResult result = null;
        try {
            result = jestClient.execute(sb);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        long totalCount = 0;
        List<Object> list = new ArrayList<Object>();
        if (result != null && result.isSucceeded()) {
            //获取总记录数
            totalCount = result.getTotal();
            if (totalCount > 0) {
                Map<String, Object> map = new HashMap<String, Object>();
                JsonArray jsonArray = result.getJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject jsonObject = jsonArray.get(i).getAsJsonObject().get("_source").getAsJsonObject();
                    Gson gson = new Gson();
                    map = gson.fromJson(jsonObject, map.getClass());
                    //将索引id存入map集合
                    String id = jsonArray.get(i).getAsJsonObject().get("_id").toString();
                    map.put("id", id);
                    list.add(map);
                }
            }
        }
        return list;
    }
//
//    /**
//     * 分页查询，通用方法
//     * @param indexName 索引名称
//     * @param indexType 索引类型
//     * @param search    检索式，即检索词
//     * @param pageNo    页码
//     * @param pageSize  每页记录数
//     * @return 返回Page结果集
//     */
//    public Page baseSearch(String indexName, String indexType, String search, Integer pageNo, Integer pageSize){
//        //设置默认参检索引
//        indexName = StringUtils.isBlank(indexName) ? "*" : indexName;
//        //设置默认检索全部
//        search = StringUtils.isBlank(search) ? "*" : search;
//        //设置默认页码第1页
//        pageNo = pageNo==null || pageNo<1 ? 1: pageNo;
//        //设置默认每页记录数20
//        pageSize = pageSize==null ? 20: pageSize;
//        //此处为自己封装的分页方法，返回每页起始记录序号
//        int startIndex = Page.getStartOfPage(pageNo, pageSize);
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        //设置默认分词后AND连接，StringQuery支持通配符
//        sourceBuilder.query(QueryBuilders.queryStringQuery(search).defaultOperator(Operator.AND));
//        //es分页从0开始
//        sourceBuilder.from(startIndex).size(pageSize);
//        sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
//
//        Search sb = new Search.Builder(sourceBuilder.toString()).addIndex(indexName).addType(indexType).build();
//        SearchResult result = null;
//        try {
//            result = jestClient.execute(sb);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        long totalCount = 0;
//        List list = new ArrayList<>();
//
//        if (result !=null && result.isSucceeded()) {
//            //获取总记录数
//            totalCount = result.getTotal();
//            if (totalCount > 0) {
//                Map<String, Object> map = new HashMap<String, Object>();
//                /*JsonArray解析结果集*/
//                JsonArray jsonArray = result.getJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
//                for (int i = 0; i < jsonArray.size(); i++) {
//                    JsonObject jsonObject =  jsonArray.get(i).getAsJsonObject().get("_source").getAsJsonObject();
//                    Gson gson = new Gson();
//                    map = gson.fromJson(jsonObject,map.getClass());
//                    //将索引id存入map集合
//                    String id = jsonArray.get(i).getAsJsonObject().get("_id").toString();
//                    map.put("id",id);
//                    list.add(map);
//                }
//            }
//        }
//        return new Page(pageNo, totalCount, pageSize, list);
//    }
//
//    /**
//     * 分页查询，通用方法
//     * @param indexName 索引名称
//     * @param indexType 索引类型
//     * @param search    检索式，即检索词
//     * @param pageNo    页码
//     * @param pageSize  每页记录数
//     * @param sortField 排序字段，多字段间逗号分隔
//     * @param sortRule 排序规则，多值每个值与sortF字段一一对应，逗号分隔（ASC升序/DESC降序）
//     * @return 返回Page结果集
//     */
//    public Page baseSearch(String indexName, String indexType, String search, Integer pageNo, Integer pageSize, String sortField , String sortRule){
//        //设置默认参检索引
//        indexName = StringUtils.isBlank(indexName) ? "*" : indexName;
//        //设置默认检索全部
//        search = StringUtils.isBlank(search) ? "*" : search;
//        //设置默认页码第1页
//        pageNo = pageNo==null || pageNo<1 ? 1: pageNo;
//        //设置默认每页记录数20
//        pageSize = pageSize==null ? 20: pageSize;
//        //调用分页，返回每页起始记录序号
//        int startIndex = Page.getStartOfPage(pageNo, pageSize);
//        //创建sourceBuilder
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        //设置默认分词后AND连接，StringQuery支持通配符
//        sourceBuilder.query(QueryBuilders.queryStringQuery(search).defaultOperator(Operator.AND));
//        //es分页从0开始
//        sourceBuilder.from(startIndex).size(pageSize);
//        //设置排序规则
//        if (StringUtils.isNotBlank(sortField)) {
//            String [] sf = sortField.split(",");
//            String [] sr = sortRule.split(",");
//            StringBuilder sb = new StringBuilder();
//            for (int i=0; i<sf.length; i++) {
//                if ("ASC".equals(sr[i])) {
//                    //按排序字段进行升序排序
//                    sourceBuilder.sort(new FieldSortBuilder(sf[i]).order(SortOrder.ASC));
//                } else {
//                    //按排序字段进行降序排序
//                    sourceBuilder.sort(new FieldSortBuilder(sf[i]).order(SortOrder.DESC));
//                }
//            }
//        } else {
//            //默认相关度降序排序
//            sourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
//        }
//        Search sb = new Search.Builder(sourceBuilder.toString()).addIndex(indexName).addType(indexType).build();
//        SearchResult result = null;
//        try {
//            result = jestClient.execute(sb);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //存储总文档数
//        long totalCount = 0;
//        //用于存储最后的结果集
//        List list = new ArrayList<>();
//
//        if (result !=null && result.isSucceeded()) {
//            //获取总记录数
//            totalCount = result.getTotal();
//            if (totalCount > 0) {
//                //用于转储数据
//                Map<String, Object> map = new HashMap<String, Object>();
//                /*JsonArray解析结果集*/
//                JsonArray jsonArray = result.getJsonObject().get("hits").getAsJsonObject().get("hits").getAsJsonArray();
//                for (int i = 0; i < jsonArray.size(); i++) {
//                    JsonObject jsonObject =  jsonArray.get(i).getAsJsonObject().get("_source").getAsJsonObject();
//                    Gson gson = new Gson();
//                    map = gson.fromJson(jsonObject,map.getClass());
//                    //将索引id存入map集合
//                    String id = jsonArray.get(i).getAsJsonObject().get("_id").toString();
//                    map.put("id",id);
//                    list.add(map);
//                }
//            }
//        }
//        return new Page(pageNo, totalCount, pageSize, list);
//    }
//
//    /**
//     * 更新es索引数据,索引中有联合索引，需调用该方法进行数据更新
//     * @param map 集合
//     * @param indexName 索引名称
//     * @param indexType 索引类型
//     */
//    public boolean updateEsData(String indexName, String indexType, Map<String, Object> map, String[] fields, String[] index ) {
//        //存储数据
//        Map<String, Object> hmap = new HashMap<String, Object>();
//        for (int j = 0; j < fields.length; j++) {
//            if (fields[j].contains(":")) {
//                //分割联合字段
//                String[] sa = fields[j].split(":");
//                StringBuffer sb = new StringBuffer();
//                for (String st : sa) {
//                    //联合字段，取值拼接
//                    sb.append(map.get(st) + ";");
//                }
//                hmap.put(index[j], sb.toString());
//            } else {
//                hmap.put(index[j], map.get(fields[j]));
//            }
//        }
//        //针对car_model索引 添加 年款style_year_suffix  默认数据全部符合规范2017格式
//        if (indexName.equals("car_model")) {
//            if (null != map.get("style_year")) {
//                hmap.put("style_year_suffix", map.get("style_year").toString().substring(2, map.get("style_year").toString().length()));
//            }
//        }
//        //调用es更新方法
//        boolean bool = updateIndexDoc(indexName,indexType,hmap);
//        return bool;
//    }


}
