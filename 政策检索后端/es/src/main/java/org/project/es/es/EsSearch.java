package org.project.es.es;

import com.aliyun.core.utils.StringUtils;
import com.jfinal.aop.Inject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.pipeline.LinearModel;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.project.es.face.FaceService;
import org.project.es.policy.PolicyService;

import java.io.IOException;
import java.util.*;

/**
 * @author Administrator
 * ES的查询原则：拿我们查询的字段去倒排索引上查询匹配
 * 1.match：全局查询，如果是多个词语，会进行分词查询。字母字母默认转换成全小写，进行匹配。
 * {"query":{"match":{"name":"比尔盖茨"}}
 * }
 * 2.match_phrase：查询短语，会对短语进行分词，match_phrase的分词结果必须在text字段分词中都包含，而且顺序必须相同，而且必须都是连续的
 * {"query":{"match_phrase":{"country":"中 国"}}
 * }
 * 3.term： 单个词语查询，会精确匹配词语，会根据输入字段精确匹配。
 * {"query":{"term":{"country":"America"}}
 * }
 * 4.terms：多个词语查询，精确匹配，满足多个词语中的任何一个都会返回。
 * {"query":{"terms":{"country":["America","america"]}}
 * }
 * 5.exists：类似于SQL的ISNULL，字段不为空的会返回出来。
 * {"query":{"exists":{"field":"country"}}
 * }
 * 6.range： 类似于SQL的between and关键字，返回查询
 * {"query":{"range":{"age":{"lte":50,"gte":20}}}
 * }
 * 7.ids：一次查询多个id，批量返回。
 * {"query":{"ids":{"values":["JJpNJHgBLLjdyTtc34ag","JZp5JHgBLLjdyTtcEobD"]}}
 * }
 * 8.fuzzy：模糊查询
 * 编辑距离：就是一个词语变成另外一个词语要编辑的次数,也叫莱文斯坦距离(Levenshtein distance)，指两个字符串之间，由一个转成另一个所需的最少编辑操作次数
 * 9.query：复合查询
 */
public class EsSearch {
    @Inject
    FaceService faceService;
    @Inject
    PolicyService policyService;

    /**
     * 查询指定索引中的全部数据
     */
    public static List<Map<String,Object>> matchAllQuery(RestHighLevelClient client,String index) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 查询所有数据
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        request.source(sourceBuilder);
        return printSearch(client,request,null);
    }

    /**
     * match全局查询
     * 如果是多个词语，会进行分词查询。字母字母默认转换成全小写，进行匹配。
     */
    public static List<Map<String,Object>> matchQuery(RestHighLevelClient client,String index,String key,String value) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 查询所有数据
        sourceBuilder.query(QueryBuilders.matchQuery(key,value));
        request.source(sourceBuilder);
        return printSearch(client,request,null);
    }

    /**
     * term查询
     *
     * 精确查询指定索引中字段key的值为value的文本
     * ES中term对搜索文本不分词，直接拿去倒排索引中匹配，你输入的是什么，就去匹配倒排索引什么，相当于SQL中的单个where条件
     */
    public static List<Map<String,Object>> termQuery(RestHighLevelClient client,String index,String key,String value) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 查询所有数据
        sourceBuilder.query(QueryBuilders.termQuery(key,value));
        request.source(sourceBuilder);
        return printSearch(client,request,null);
    }

    /**
     * 分页查询
     * begin为从查询到结果的第begin+1个开始显示
     * 但是这个好像只显示第一页的结果
     */
    public static void pageQuery(RestHighLevelClient client,String index,int begin,int size) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 查询所有数据
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        // 分页查询
        // 当前页起始索引(第一条数据的顺序号)，from
        sourceBuilder.from(begin);
        // 每页显示多少条size
        sourceBuilder.size(size);
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * 数据排序
     * @param sort 需要进行排序的字段
     * @param type 排序方式 0--升序；1--降序
     */
    public static void sortOrderQuery(RestHighLevelClient client,String index,String sort,int type) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 查询所有数据
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        // 排序
        if(type==0){
            sourceBuilder.sort(sort, SortOrder.ASC);
        }
        else {
            sourceBuilder.sort(sort, SortOrder.DESC);
        }
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * 过滤字段
     * 即只显示指定字段的值，不显示其他字段
     * @param key 为需要过滤的一组字段
     */
    public static void filterFieldsQuery(RestHighLevelClient client,String index,String[] key) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 查询所有数据
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        // 查询字段过滤
        String[] excludes = {};
        String[] includes = key;
        sourceBuilder.fetchSource(includes, excludes);
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * Bool查询
     */
    public static void boolQuery(RestHighLevelClient client,String index) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 必须包含
        boolQueryBuilder.must(QueryBuilders.matchQuery("name", "小"));
        // 一定不含
        boolQueryBuilder.mustNot(QueryBuilders.matchQuery("information","我"));
        // 可能包含
        boolQueryBuilder.should(QueryBuilders.matchQuery("gender", 0));
        // 查询所有数据
        sourceBuilder.query(boolQueryBuilder);
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * 范围查询
     * 若为字符串的大小比较则按照其规则：比较每一位上字符的大小，直到最后没有字符（满足前面要求才会比较长度）
     * 例：110>10086
     */
    public static void rangeQuery(RestHighLevelClient client,String index) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("mobile");
        // 大于等于
        rangeQuery.gte("110");
        // 小于等于
        rangeQuery.lte("15927488318");
        // 查询所有数据
        sourceBuilder.query(rangeQuery);
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * 模糊匹配
     * 参考1：
     * https://blog.csdn.net/weixin_53142722/article/details/128313219
     * 表示按照指定key的指定value进行查询
     * 主要针对英文字母不匹配的问题，中文似乎不太正确
     *
     * fuzziness：最大编辑距离，[0,1,2]
     *      编辑距离是将一个术语转换为另一个术语所需的一个字符更改的次数。 这些更改可以包括：
     *      更改字符（box→fox）
     *      删除字符（black→lack）
     *      插入字符（sic→sick）
     *      转置两个相邻字符（act→cat）
     * prefixLength：要求指定前n个字符的完全匹配，后续可以不一样
     *
     * 参考2：
     * https://blog.csdn.net/qq_45443475/article/details/127359991
     * fuzziness：最大编辑距离【一个字符串要与另一个字符串相同必须更改的一个字符数】。默认为AUTO。
     * prefix_length：不会被“模糊化”的初始字符数。这有助于减少必须检查的术语数量。默认为0。
     * max_expansions：fuzzy查询将扩展到的最大术语数。默认为50。
     * transpositions：是否支持模糊转置（ab→ ba）。默认值为false。
     * 比如输入"方财兄"，这时候也要匹配到“方才兄”。
     */
    public static List<Map<String,Object>> fuzzyQuery(RestHighLevelClient client,String index,String key,String value) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);

        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 查询所有数据
        sourceBuilder.query(QueryBuilders.fuzzyQuery(key,value).fuzziness(Fuzziness.TWO).prefixLength(0));
        request.source(sourceBuilder);
        return printSearch(client,request,null);
    }

    /**
     * 聚合查询
     */
    public static void aggrQuery(RestHighLevelClient client,String index) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);

        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(AggregationBuilders.max("maxAge").field("age"));
        // 设置请求体
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * 分组统计
     */
    public static void groupQuery(RestHighLevelClient client,String index) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.aggregation(AggregationBuilders.terms("age_group_by").field("age"));
        // 设置请求体
        request.source(sourceBuilder);
        printSearch(client,request,null);
    }

    /**
     * 政策分类等的Bool查询
     * 均使用match模糊匹配
     * 查询条件：
     * policyGrade 政策级别 --直接过滤
     * province 省份 --过滤
     * label 行业分类 --过滤
     * policyType 发布类型 --过滤
     * pubAgency 发布机构
     * time 发布时间范围（表示距离当前时间的n天内）(暂时没考虑)
     * searchValue 搜索框内容,会对title和content都进行匹配并标注高亮
     */
    public List<Map<String,Object>> boolPolicyQuery(RestHighLevelClient client,String index,String policyGrade,String province, String label,String policyType,
                                       String pubAgency,int time,String searchValue,long userId,String userProvince,Map<String,Float> face) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置查询的结果条数（ES默认为10条，这里设定为20条）
        sourceBuilder.size(policyService.searchSize);
        //设置解释分数详情
        //sourceBuilder.explain(true);
        //关于filter过滤返回结果为空的问题--因为使用了text类型：
        //如果字段是text类型，存入的数据会先进行分词，然后将分完词的词组存入索引，而keyword则不会进行分词，直接存储。
        //text类型的数据被用来索引长文本，例如电子邮件主体部分或者一款产品的介绍，这些文本会被分析，在建立索引文档之前会被分词器进行分词，转化为词组。经过分词机制之后es允许检索到该文本切分而成的词语，但是text类型的数据不能用来过滤、排序和聚合等操作。
        //keyword类型的数据可以满足电子邮箱地址、主机名、状态码、邮政编码和标签等数据的要求，不进行分词，常常被用来过滤、排序和聚合。
        // https://blog.csdn.net/sfh2018/article/details/118083634
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //游客登录：对于选项框内容，直接过滤后按时间降序排序（在搜索框无内容时），因为使用过滤无法得到score，也就无法和function中的分数结合
        if(userId==-1){
            if (policyGrade !=null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("policyGrade", policyGrade));
            }
            if (province !=null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("province", province));
            }
            if (label !=null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("label", label));
            }
            if (policyType !=null){
                boolQueryBuilder.filter(QueryBuilders.termQuery("policyType", policyType));
            }
            //在没有搜索框内容时，对于其他条件按照时间排序（降序），存在搜索框内容时则按照分数排序
            if(searchValue==null){
                FieldSortBuilder sortTime= SortBuilders.fieldSort("pubTime").order(SortOrder.DESC);
                sourceBuilder.sort(sortTime);
                sourceBuilder.trackScores(true);
            }
        }
        //用户登录，选项框也使用match匹配，可以得到分数与function进行交互（主要是与label结合）
        else {
            if (policyGrade !=null){
                boolQueryBuilder.must(QueryBuilders.matchQuery("policyGrade", policyGrade));
            }
            if (province !=null){
                boolQueryBuilder.must(QueryBuilders.matchQuery("province", province));
            }
            if (label !=null){
                boolQueryBuilder.must(QueryBuilders.matchQuery("label", label));
            }
            if (policyType !=null){
                boolQueryBuilder.must(QueryBuilders.matchQuery("policyType", policyType));
            }
        }
        if (pubAgency !=null){
            boolQueryBuilder.must(QueryBuilders.matchQuery("pubAgencyFullname", pubAgency));
        }
        if (searchValue !=null){
            boolQueryBuilder.should(QueryBuilders.matchQuery("policyTitle", searchValue));
            boolQueryBuilder.must(QueryBuilders.matchQuery("textContent", searchValue));
        }
        //将boolQuery放入请求体，避免因为下方没有高亮、个性化的function而无法保证基础查询
        sourceBuilder.query(boolQueryBuilder);
        //time==-2表示为不需要高亮的方法，例如政策下方推荐
        String[] highlightKey=null;
        if(time!=-2){
            // 设置两个高亮字段
            highlightKey= new String[]{"policyTitle", "textContent"};
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            //考虑返回5X50的高亮
            highlightBuilder.numOfFragments(5);
            highlightBuilder.fragmentSize(50);
            // 设置标签前缀
            highlightBuilder.preTags("<font color='red'>");
            // 设置标签后缀
            highlightBuilder.postTags("</font>");
            // 设置标题和正文的高亮
            highlightBuilder.field(highlightKey[0]).field(highlightKey[1]);
            // 放入查询的请求体
            sourceBuilder.highlighter(highlightBuilder);
            //time==-1表示对应的不是用户个性化搜索(例如政策溯源)，不需要考虑用户画像;userId==-1表示游客登录，不需要个性化，只用根据省份
            // （因为会比较慢，主要是获取用户画像的过程慢，考虑记录用户画像，当用户添加新的政策or隔一段时间定时进行用户画像的更新）
            if(time!=-1){
                // 设置查询评分加上自定义评分规则
                FunctionScoreQueryBuilder builder=QueryBuilders.functionScoreQuery(boolQueryBuilder,changeFunction(userId,userProvince,face));
                // 设置分数=查询评分*自定义评分相加（相加得到1~2的一个数--province返回0/1，label按照权重评分返回1~2的数）
                builder.scoreMode(FunctionScoreQuery.ScoreMode.SUM);
                builder.boostMode(CombineFunction.MULTIPLY);
                sourceBuilder.query(builder);
            }
        }
        request.source(sourceBuilder);
        System.out.println(sourceBuilder);
        return printSearch(client,request,highlightKey);
    }

    /**
     * 高亮查询
     * 匹配查询方式的高亮查询
     */
    public List<Map<String,Object>> highLightQuery(RestHighLevelClient client,String index,String key,String value) throws IOException {
        // 创建搜索请求对象
        SearchRequest request = new SearchRequest();
        request.indices(index);
        // 构建查询的请求体
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //sourceBuilder.explain(true);
        // 构建匹配查询方式的高亮查询
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(key,value);
        // 设置查询方式
        sourceBuilder.query(matchQueryBuilder);
        //设置查询的结果条数（ES默认为10条，这里设定为100条）
        sourceBuilder.size(policyService.searchSize);
        // 构建高亮字段
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        // 设置标签前缀
        highlightBuilder.numOfFragments(5);
        highlightBuilder.fragmentSize(50);
        highlightBuilder.preTags("<font color='red'>");
        // 设置标签后缀
        highlightBuilder.postTags("</font>");
        // 设置高亮字段
        highlightBuilder.field(key);
        // 设置高亮构建对象
        sourceBuilder.highlighter(highlightBuilder);
        // 设置请求体
        request.source(sourceBuilder);
        System.out.println(sourceBuilder);
        return printSearch(client,request, new String[]{key});
    }

    /**
     * 联想词查询（即搜索框自动提示）
     * suggest:代表接下来的查询是一个suggest类型的查询
     * policySuggest:这次查询的名称，联想结果会显示在其中，自定义
     * prefix:用来补全的词语前缀，本例中搜索以北京开头的内容
     * completion:代表是completion类型的suggest，其它类型还有：Term、Phrase
     * field：policyTitle.suggest，要查询的字段，在policy_index索引中表现为子字段
     *
     * 后续有必要改成搜索热词（搜索次数较多的语句~~~~）
     */
    public static List<String> suggestQuery(RestHighLevelClient client,String index,String value) throws IOException {
        if (value==null){
            return null;
        }
        //1.定义suggest对象
        SuggestBuilder suggestBuilder = new SuggestBuilder();
        //2.定义本次查询的字段名字(类型为completion)
        CompletionSuggestionBuilder suggestion = SuggestBuilders
                .completionSuggestion("suggest").prefix(value).size(20).skipDuplicates(true);
        suggestBuilder.addSuggestion("policySuggest",suggestion);
        //3.indices在指定索引中进行suggest查询
        SearchRequest request = new SearchRequest().indices(index).source(new SearchSourceBuilder().suggest(suggestBuilder));
        //4.发送查询得到联想结果
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert searchResponse != null;
        Suggest suggest = searchResponse.getSuggest();
        //5.将联想结果用set集合存储并返回
        Set<String> keywords = null;
        if (suggest != null) {
            keywords = new HashSet<>();
            List<? extends Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option>> entries = suggest.getSuggestion("policySuggest").getEntries();
            for (Suggest.Suggestion.Entry<? extends Suggest.Suggestion.Entry.Option> entry: entries) {
                for (Suggest.Suggestion.Entry.Option option: entry.getOptions()) {
                    //最多返回10个推荐，每个长度最大为20
                    String keyword = option.getText().string();
                    if (!StringUtils.isEmpty(keyword) && keyword.length() <= 20) {
                        //去除输入字段
                        if (keyword.equals(value)) {
                            continue;
                        }
                            keywords.add(keyword);
                        if (keywords.size() > 10) {
                            break;
                        }
                    }
                }
            }
        }
        if (keywords!=null){
            return new ArrayList<>(keywords);
        }
        return null;
    }

    /**
     * 输出查询结果
     * 将构建好的请求request发送到客户端并将响应response打印输出
     * keys不为空表示需要高亮查询的字段，null表示其他查询，不需要添加highLight
     */
    public static List<Map<String,Object>> printSearch(RestHighLevelClient client,SearchRequest request,String[] keys) throws IOException {
        // 客户端发送请求，获取响应对象
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 查询匹配
        SearchHits hits = response.getHits();
        System.out.println("took: " + response.getTook());
        System.out.println("耗时: " + response.isTimedOut());
        System.out.println("条数: " + hits.getTotalHits());
        System.out.println("最高分数: " + hits.getMaxScore());

        List<Map<String,Object>> result=new ArrayList<>();
        float maxScore=hits.getMaxScore();
        for (SearchHit hit : hits) {
            // 记录每条查询的结果信息和得分,获取高亮字段的结果
            Map<String,Object> map=hit.getSourceAsMap();
            map.put("score",hit.getScore()/maxScore*(float) 100);
            Map<String,List<String>> highlight=new HashMap<>();
            if(keys!=null){
                //PS:这里通过设置highlight请求参数，将高亮不再分句直接返回全部内容，但是这里感觉在搜索列表中就不能突出高亮了，好像有问题？
                for (String key:keys){
                    // 循环取出高亮句存入列表
                    List<String> highlightKey=new ArrayList<>();
                    // 这里进行一个判空，因为在bool查询中出于业务考虑，policyTitle被认为是可能匹配的字段，
                    // 也就是说搜索词可以不出现在政策标题中，那么政策标题就不存在高亮，也就不存在getFragments了，因此在这里进行判空
                    if(hit.getHighlightFields().get(key)!=null){
                        Text[] high=hit.getHighlightFields().get(key).getFragments();
                        for (Text text : high) {
                            highlightKey.add(text.toString());
                        }
                        highlight.put(key,highlightKey);
                    }
                }
                map.put("highlight",highlight);
            }
            result.add(map);
        }
        System.out.println("-----------------------------");
        return result;
    }


    /**
     * 修改es的排序方式（使用权重脚本的方式实现）
     * https://blog.csdn.net/W2044377578/article/details/128636611
     * https://www.elastic.co/guide/en/elasticsearch/reference/7.16/query-dsl-function-score-query.html
     * https://www.cnblogs.com/eternityz/p/16166331.html
     */
    public FunctionScoreQueryBuilder.FilterFunctionBuilder[] changeFunction(long userId,String province,Map<String,Float> face){
        //userId==-1表示游客登录，不需要个性化，只用根据省份
        double labelNumScore=faceService.labelNum;
        double maxLabelScore= faceService.maxLabelScore;
        double minLabelScore= faceService.minLabelScore;
        List<String> labels=faceService.labels;
        String[] provinces= PolicyService.chinaProvince;
        List<String> provinceList = List.of(provinces);
        StringBuilder scoreScript= new StringBuilder();
        FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders;
        //1.游客登录，仅记录省份影响，数组长度=1（设置过长会导致function==null产生错误）
        if(userId==-1){
            filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[1];
            //若为游客登录，设定省份影响在1-1.5之间
            scoreScript.append("if(doc['provinceNum'].value==").append(provinceList.indexOf(province)).append("){return 1.5;}else {return 1.0;}");
            ScoreFunctionBuilder<ScriptScoreFunctionBuilder> labelScoreFunction = ScoreFunctionBuilders.scriptFunction(new Script(scoreScript.toString()));
            FunctionScoreQueryBuilder.FilterFunctionBuilder labelFunction=new FunctionScoreQueryBuilder.FilterFunctionBuilder(labelScoreFunction);
            filterFunctionBuilders[0]=labelFunction;
            return filterFunctionBuilders;
        }
        //2.用户登录
        //(1)添加对应用户标签画像,标签score_script脚本自定义评分
        filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[2];
        //对省份和标签的自定义结果进行求和
        Object[] label= face.keySet().toArray();
        List<Short> labelNum=new ArrayList<>();
        //将字符串标签转换为数字编号形式，用于排序规则的编写
        for (Object o : label) {
            labelNum.add((short) labels.indexOf(o));
        }
        //label在用户占比超过25%，认为这个label是有利的，此时匹配省份=0.5，标签>1，score评分升高
        //若匹配省份=0，标签>1，score评分升高也合理
        //若占比小于25%，则此标签对用户没有明显影响，此时匹配省份=0.5，标签=1，score评分升高
        //若匹配省份=0，标签=1，则score评分保持不变（也合理，如果查询评分非常高则足以超越前面的内容）
        //对标签的影响进行一定限制，避免查询结果完全由标签控制
        //如果希望搜索结果对用户标签很敏感的话就要下调这里25%的影响，也就是比如说设定只要标签占比超过10%等就会影响到搜索结果
        double labelScore=Math.min(face.get(label[0]) * labelNumScore,maxLabelScore);
        labelScore=Math.max(labelScore,minLabelScore);
        scoreScript.delete(0,scoreScript.length());
        scoreScript.append("if(doc['labelNum'].value==").append(labelNum.get(0)).append("){return ").append(labelScore).append(";}");
        for (int i=1;i<label.length;i++){
            if(face.get(label[i]) * labelNumScore<minLabelScore){
                continue;
            }
            labelScore=Math.min(face.get(label[i]) * labelNumScore,maxLabelScore);
            scoreScript.append("else if(doc['labelNum'].value==").append(labelNum.get(i)).append("){return ").append(labelScore).append(";}");
        }
        scoreScript.append("else {return 1.0;}");
        //**层层包装填充放到functions中：https://blog.csdn.net/xiaoll880214/article/details/86716393
        ScoreFunctionBuilder<ScriptScoreFunctionBuilder> labelScoreFunction = ScoreFunctionBuilders.scriptFunction(new Script(scoreScript.toString()));
        FunctionScoreQueryBuilder.FilterFunctionBuilder labelFunction=new FunctionScoreQueryBuilder.FilterFunctionBuilder(labelScoreFunction);
        filterFunctionBuilders[0]=labelFunction;
        //(2)省份num衰减评分
        //若为用户登录，利用衰减函数，设定在给定省份id（仅此id）对应的得分为0.5（以id+偏移量为原点，搜索偏移量范围得分为decay）
        LinearDecayFunctionBuilder provinceScoreFunction= ScoreFunctionBuilders.linearDecayFunction("provinceNum", provinceList.indexOf(province)+0.1, 0.1, 0, 0.5);
        FunctionScoreQueryBuilder.FilterFunctionBuilder provinceFunction=new FunctionScoreQueryBuilder.FilterFunctionBuilder(provinceScoreFunction);
        filterFunctionBuilders[1]=provinceFunction;
        return filterFunctionBuilders;
    }
}
