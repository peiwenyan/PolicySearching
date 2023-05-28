package org.project.es.policy;

import cn.fabrice.common.pojo.BaseResult;
import cn.fabrice.common.pojo.DataResult;
import cn.fabrice.jfinal.service.BaseService;
import com.jfinal.aop.Inject;
import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.Word;
import org.elasticsearch.client.RestHighLevelClient;
import org.project.es.common.module.Policy;
import org.project.es.common.util.Neo4jUtil;
import org.project.es.es.EsDoc;
import org.project.es.es.EsIndex;
import org.project.es.es.EsSearch;
import org.project.es.face.FaceService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化搜索
 * 实际搜索语句=用户输入搜索语句+知识图谱对应的管理词句
 * 完成搜索后，根据用户画像给搜索结果进行重新评分--原始相似评分+结果与画像的匹配评分，并按照新的评分进行排序
 * 用户画像--根据用户订阅、（用户收藏、用户浏览记录）按照权重完成用户画像的抽取（类似用户关键词）
 * @author Administrator
 */
public class PolicyService extends BaseService<Policy> {
    RestHighLevelClient client = EsIndex.setTimeOut();
    String policyIndex ="policy_index";
    String suggestIndex="suggest_index";
    int suggestLimit=5;
    int highLimit=1000;
    int pushLimit=10;
    int userLimit=50;
    int expandLimit=5;
    public int searchSize=20;
    /**
     * 标识为不需要高亮的方法字段
     */
    int notHighNum=-2;
    /**
     * 标识为不需要个性化的方法字段
     */
    int notUserNum=-1;
    /**
     * 标识为需要个性化的方法字段
     */
    int needUserNum=0;
    public static String[] chinaProvince=("河北省、山西省、辽宁省、吉林省、黑龙江省、江苏省、浙江省、安徽省、福建省、江西省、山东省、河南省、湖北省、" +
            "湖南省、广东省、海南省、四川省、贵州省、云南省、陕西省、甘肃省、青海省、台湾省、内蒙古自治区、广西壮族自治区、西藏自治区、宁夏回族自治区、" +
            "新疆维吾尔自治区、北京市、天津市、上海市、重庆市、香港特别行政区、澳门特别行政区").split("、");

    /**
     * 形成用户id-用户画像、用户关键词的映射关系，便于更新和取用
     */
    Map<Long,Map<String,Float>> allFace=new HashMap<>();
    Map<Long,Map<String,Integer>> allKey=new HashMap<>();

    @Inject
    FaceService faceService;
    @Inject
    EsSearch esSearch;
    @Inject
    SearchHistoryService searchHistoryService;
    @Inject
    PolicyKeyService policyKeyService;
    @Inject
    Neo4jUtil neo4jUtil;


    /**
     * 设置用户画像和关键词
     * 在登录时进行更新，更合理的操作应该是在用户浏览、收藏等行为时更新，但因为接口不一样暂时不考虑
     * 可以通过设置定时触发来实现更新？对map中的每一个userId定时更新其画像？
     * 或者维护一个用户画像表，形成这样的映射关系，取出来的速度应该不会很慢，不然一直放在java上时间久了会有问题
     */
    public DataResult setFace(long userId){
        Map<String, Float> userFace = faceService.getUserLabel(userId);
        Map<String, Integer> userKey = policyKeyService.getUserKey(userId);
        allFace.put(userId,userFace);
        allKey.put(userId,userKey);
        Map<String,Object> result=new HashMap<>();
        result.put("当前用户画像",userFace);
        result.put("当前用户关键词",userKey);
        System.out.println("全部用户画像："+allFace);
        System.out.println("全部用户关键词："+allKey);
        return DataResult.data(result);
    }

    public PolicyService() {
        super("policy.", Policy.class, "policy");
    }

    public Policy getPolicy(long id){
        return get(id);
    }

    /**
     * 将policyId更新到对应行（id）对应的policy实体中
     * @param row 第几行
     * @param policyId 对应的policyId
     * @return true为更新成功
     */
    public boolean setPolicyId(long row,long policyId){
        System.out.println(row);
        System.out.println(policyId);
        Policy policy = getPolicy(row);
        policy.setPolicyId(policyId);
        return policy.update();
    }

    /**
     * 添加标签编号，用于自定义排序规则（针对数字计算）
     * 编号从0开始
     */
    public BaseResult setLabelNum(){
        List<Policy> policyList=list();
        List<String> labels=faceService.labels;
        for (Policy policy:policyList){
            int labelNum=labels.indexOf(policy.getLabel());
            policy.setLabelNum(labelNum);
            if(!policy.update()){
                return BaseResult.fail();
            }
        }
        return BaseResult.ok();
    }

    /**
     * 将MySQL中的数据导入到es中，便于es的快速检索（request一次性发送）
     * 由于数据太多，request过长，因此使用下面的单次发送的方法
     */
    public BaseResult addPolicyToEs(){
        List<Policy> policyList=list();
        try {
            EsDoc.bulkCreateDoc(client, policyIndex,policyList);
        } catch (IOException e) {
            e.printStackTrace();
            return BaseResult.fail();
        }
        return BaseResult.ok();
    }

    /**
     * 将MySQL中的数据导入到es中，便于es的快速检索（request多次发送，每次只发送一条）
     */
    public BaseResult addPolicyToEsOne(){
        List<Policy> policyList=list();
        for (Policy policy:policyList){
            try {
                System.out.println(policy);
                EsDoc.createDoc(client, policyIndex, String.valueOf(policy.getId()),policy);
            } catch (IOException e) {
                e.printStackTrace();
                return BaseResult.fail();
            }
        }
        return BaseResult.ok();
    }

    /**
     * 添加城市编号，用于自定义排序规则（针对数字计算）
     * 编号从0开始
     */
    public BaseResult setProvinceNum(){
        List<Policy> policyList=list();
        List<String> province = List.of(PolicyService.chinaProvince);
        System.out.println(province);
        for (Policy policy:policyList){
            int provinceNum=100;
            //如果政策省份为空，设为100，避免缺失值，用于ES的衰减函数
            if(policy.getProvince()==null){
                System.out.println("填充100");
            }
            else {
                provinceNum=province.indexOf(policy.getProvince());
            }
            policy.setProvinceNum(provinceNum);
            if(!policy.update()){
                return BaseResult.fail();
            }
        }
        return BaseResult.ok();
    }

    /**
     * 检索对应字段的指定值(精确匹配)
     */
    public DataResult termSearch(String key,String value){
        try {
            return DataResult.data(EsSearch.termQuery(client, policyIndex,key,value));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 全局匹配查询语句的分词结果
     */
    public DataResult matchSearch(String key,String value){
        try {
            return DataResult.data(EsSearch.matchQuery(client, policyIndex,key,value));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 对待匹配值模糊查询处理
     */
    public DataResult fuzzySearch(String key,String value){
        try {
            return DataResult.data(EsSearch.fuzzyQuery(client, policyIndex,key,value));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 高亮查询
     */
    public DataResult lightSearch(String key,String value){
        try {
            return DataResult.data(esSearch.highLightQuery(client, policyIndex,key,value));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 多条件查询
     */
    public DataResult boolSearch(String policyGrade,String province, String label,String policyType,
                                 String pubAgency,int time,String searchValue,long userId,int pageNo,int pageSize,String userProvince){
        Map<String, Float> userFace = allFace.get(userId);
        System.out.println("用户id"+userId+",标签画像"+userFace);
        //如果用户画像为空，说明用户暂无浏览、收藏记录，使用用户省份进行个性化（相当于游客）--过滤后按时间排序
        //==null表示游客，=={}表示无画像
        if(userFace==null||userFace.isEmpty()){
            try {
                List<Map<String, Object>> search = esSearch.boolPolicyQuery(client, policyIndex, policyGrade, province, label, policyType,
                        pubAgency, time, searchValue, -1,userProvince,null);
                List<Map<String, Object>> page = getPage(pageNo, pageSize, search,highLimit);
                return DataResult.data(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                List<Map<String, Object>> search = esSearch.boolPolicyQuery(client, policyIndex, policyGrade, province, label, policyType,
                        pubAgency, time, searchValue, userId,userProvince,userFace);
                List<Map<String, Object>> page = getPage(pageNo, pageSize, search,highLimit);
                return DataResult.data(page);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return DataResult.data(null);
    }

    /**
     * 联想词搜索
     */
    public DataResult suggestSearch(long userId,String value){
        try {
            List<String> suggest = EsSearch.suggestQuery(client, suggestIndex, value);
            //游客搜索，仅推荐es中联想词
            if(userId==-1){
                if(suggest==null){
                    return DataResult.data(null);
                }
                //有序去重
                removeDuplicateWithOrder(suggest);
                //截取一定数量的用于显示：https://blog.csdn.net/ABCAA1024/article/details/125300790
                if(suggest.size()>suggestLimit){
                    suggest=suggest.subList(0,suggestLimit);
                }
                return DataResult.data(suggest);
            }
            List<String> history = searchHistoryService.getUserSearch(userId, value);
            //用户空搜索，显示用户历史记录
            if(suggest==null){
                removeDuplicateWithOrder(history);
                if(history.size()>suggestLimit){
                    history=history.subList(0,suggestLimit);
                }
                return DataResult.data(history);
            }
            history.addAll(suggest);
            removeDuplicateWithOrder(history);
            if(history.size()>suggestLimit){
                history=history.subList(0,suggestLimit);
            }
            return DataResult.data(history);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 搜索框内容的扩展词
     */
    public DataResult expandSearch(String value){
        List<String> expand = neo4jUtil.listQueryWords(value);
        if(expand.size()>expandLimit){
            expand=expand.subList(0,suggestLimit);
        }
        return DataResult.data(expand);
    }

    /**
     * 创建policy_index索引
     */
    public BaseResult createPolicyIndex(){
        try {
            EsIndex.createPolicyIndex(client);
            return BaseResult.ok();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BaseResult.fail();
    }

    /**
     * 按照索引名删除索引
     */
    public BaseResult deleteIndex(String index){
        try {
            EsIndex.deleteIndex(client,index);
            return BaseResult.ok();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BaseResult.fail();
    }

    /**
     * 查看索引名对应的索引
     */
    public DataResult getIndex(String index){
        try {
            return DataResult.data(EsIndex.getIndex(client,index));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 创建联想搜索索引（同时也可以用于热搜）
     */
    public BaseResult createSuggestIndex(){
        try {
            EsIndex.createSuggestIndex(client);
            return BaseResult.ok();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BaseResult.fail();
    }

    /**
     * 将用户搜索记录存入ES的suggest_index索引，用于搜索框的联想词
     * 在用户点击搜索时完成
     */
    public void addSuggestToEs(String search){
        Map<String,String> map=new HashMap<>();
        map.put("suggest",search);
        try {
            EsDoc.createDoc(client,suggestIndex, "",map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 政策溯源
     */
    public DataResult policyResource(int pageNo,int pageSize,String content){
        try {
            List<Map<String, Object>> search = esSearch.highLightQuery(client, policyIndex, "textContent", content);
            System.out.println(search.size());
            List<Map<String, Object>> page = getPage(pageNo, pageSize, search,highLimit);
            return DataResult.data(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return DataResult.data(null);
    }

    /**
     * 对于某一篇政策，在下面进行相关推荐
     * 目前直接使用分类推荐，后面可以考虑从文章中提取关键词或者级别等信息，实现更精确的推荐
     * （推荐前pushNum条）
     */
    public DataResult pushUnderPolicy(long policyId){
        Policy policy = getPolicy(policyId);
        String label=policy.getLabel();
        String province=policy.getProvince();
        String key= policyKeyService.getPolicyKey(policyId);
        List<Map<String,Object>> push=new ArrayList<>();
        System.out.println("政策关键词："+key);
        if(key==null){
            try {
                push=esSearch.boolPolicyQuery(client, policyIndex,null,province, label, null,
                        null, notHighNum, null,-1,province,null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                // time=-2，不考虑高亮，不考虑用户画像
                push=esSearch.boolPolicyQuery(client, policyIndex,null,province, label, null,
                        null, notHighNum, key,-1,province,null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 避免出现政策分类总数少于推荐分类数的情况
        push.removeIf(onePush -> Integer.parseInt(String.valueOf(onePush.get("policyId")))==policyId);
        int realPush=Math.min(pushLimit,push.size());
        push=push.subList(0,realPush);
        return DataResult.data(push);
    }

    /**
     * 根据用户画像（标签）推荐用户政策
     * 可以考虑结合用户的位置和搜索关键词等实现更精准的推荐
     */
    public DataResult pushUserPolicy(long userId,String userProvince){
        Map<String, Float> userFace = allFace.get(userId);
        Map<String, Integer> userKey = allKey.get(userId);
        System.out.println("用户id"+userId+"用户画像"+userFace);
        List<Map<String,Object>> push=new ArrayList<>();
        System.out.println("用户关键词"+ userKey);
        //政策关键词为空--用户还没有浏览等记录或用户的浏览范围很广，没什么代表性词汇，则根据标签或省份推荐
        if(userKey ==null|| userKey.isEmpty()){
            //暂无浏览记录，按照省份推荐，时间排序
            if(userFace ==null||userFace.isEmpty()){
                try {
                    push=esSearch.boolPolicyQuery(client,policyIndex,null,userProvince,null,null,null,notUserNum,null,userId,userProvince,null);
                    // 避免出现政策分类总数少于推荐分类数的情况
                    int realPush=Math.min(pushLimit,push.size());
                    push=push.subList(0,realPush);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                Set<String> labels=userFace.keySet();
                // 按照标签比例完成每个标签的推荐个数
                for (String label:labels){
                    float score=userFace.get(label);
                    List<Map<String,Object>> pushLabel=new ArrayList<>();
                    try {
                        pushLabel=esSearch.boolPolicyQuery(client, policyIndex,null,null, label, null,
                                null, needUserNum, null,userId,userProvince,userFace);
                        // 避免出现政策分类总数少于推荐分类数的情况
                        int pushLabelNum=Math.min((int) (userLimit*score),pushLabel.size());
                        pushLabel=pushLabel.subList(0,pushLabelNum);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    push.addAll(pushLabel);
                }
            }
        }
        //关键词取value为前10位且大于2的关键词，因为关键词过长也会导致检索变慢
        else {
            String key = userKey.keySet().toString();
            System.out.println("用户推荐关键词："+key);
            //time==0，按照用户关键词+用户标签+省份获取
            //更好的方式：基于下方的标签比例，对每个标签的用户浏览、收藏提取对应的政策关键词，根据这些关键词获取此标签内的推荐
            try {
                push=esSearch.boolPolicyQuery(client, policyIndex,null,null, null, null,
                        null, needUserNum,key,userId,userProvince,userFace);
                // 避免出现政策分类总数少于推荐分类数的情况
                int realPush=Math.min(pushLimit,push.size());
                push=push.subList(0,realPush);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return DataResult.data(push);
    }

    /**
     * 政策溯源页面的政策推荐
     * 同理于搜索接口，face为空可视用户为游客
     * @param userId userId==-1表示游客访问，按照定位省份推荐，否则为用户访问，直接接入用户推荐接口
     */
    public DataResult pushResourcePolicy(long userId,String userProvince,int pageNo,int pageSize){
        List<Map<String,Object>> push=new ArrayList<>();
        Map<String, Float> userFace = allFace.get(userId);
        System.out.println("用户id"+userId+",用户画像"+userFace);
        //游客和暂无记录的用户：按照省份推荐，时间排序
        //==null表示游客，=={}表示无画像
        if(userFace==null||userFace.isEmpty()){
            try {
                push=esSearch.boolPolicyQuery(client,policyIndex,null,userProvince,null,null,null,notUserNum,null,userId,userProvince,null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //存在记录的用户：按照省份+分类推荐
        else {
            try {
                push=esSearch.boolPolicyQuery(client,policyIndex,null,userProvince,null,null,null,needUserNum,null,userId,userProvince,userFace);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        List<Map<String, Object>> page = getPage(pageNo, pageSize, push,highLimit);
        return DataResult.data(page);
    }

    /**
     * 将bool查询的结果，根据当前页号和页面大小返回指定的集合
     * 同时注意对应的内容长度等
     * @param pageNo 当前页号
     * @param pageSize 页面大小
     * @param limit 正文高亮的限制字数
     */
    public List<Map<String, Object>> getPage(int pageNo,int pageSize,List<Map<String, Object>> oldList,int limit) {
        //总条数
        int total = oldList.size();
        if (total==0){
            return null;
        }
        //过滤结果中存在空标题/空内容的政策，并转换为需要的Map格式
        List<Map<String,Object>> list=new ArrayList<>();
        for(Map<String,Object> l:oldList){
            Map<String,List<String>> h=stringToMap(l.get("highlight").toString(),String.valueOf(l.get("policyTitle")),String.valueOf(l.get("textContent")));
            if(h!=null){
                //这里list.remove(l)会有异常，比较急没仔细看，就直接改变逻辑了
                // https://www.cnblogs.com/loong-hon/p/10256686.html
                list.add(l);
            }
        }
        //总页数
        total=list.size();
        int pageSum = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
        if(list.isEmpty()){
            return null;
        }
        //分页，将其中的数据修改成希望展示在前端的数据格式
        List<Map<String, Object>> searchList = list.stream().skip((long) (pageNo - 1) * pageSize).limit(pageSize).
                collect(Collectors.toList());
        List<Map<String, Object>> resultList=new ArrayList<>();
        for (Map<String, Object> search:searchList){
            //searchList始终不为空，如果其中的search为空就说明查询结果为空，或者页面没有填满
            if(search==null){
                break;
            }
            Map<String, Object> result=new HashMap<>();
            Map<String,List<String>> highlight=stringToMap(search.get("highlight").toString(),String.valueOf(search.get("policyTitle")),String.valueOf(search.get("textContent")));
            //将高亮中的标题显示在搜索列表中（高亮合并，截取前100，注意<font>标签的匹配（难实现的话可以考虑直接只合并整句））
            assert highlight != null;
            List<String> policyTitle = highlight.get("policyTitle");
            String showTitle="";
            for(String text:policyTitle){
                showTitle=showTitle.concat(text);
//                if(showTitle.length()+text.length()<100){
//
//                }
//                else{
//                    showTitle=text.substring(0,100-showTitle.length());
//                    break;
//                }
            }
            result.put("policyTitle",showTitle);
            //将高亮中的正文显示在搜索列表中
            List<String> textContent = highlight.get("textContent");
            String showContent="";
            for(String text:textContent){
                //直接加，显示交给前端，避免<font>标签截断
                showContent=showContent.concat(text);
//                if(showContent.length()+text.length()<limit){
//                    showContent=showContent.concat(text);
//                }
//                else{
//                    showContent=text.substring(0,limit-showContent.length());
//                    break;
//                }
            }
            result.put("textContent",showContent);
            //存入分数
            result.put("score",search.get("score"));
            //存入政策相关信息
            result.put("policySource",search.get("policySource"));
            result.put("pubTime",search.get("pubTime"));
            result.put("label",search.get("label"));
            result.put("policyId",search.get("policyId"));
            result.put("province",search.get("province"));
            result.put("policyType",search.get("policyType"));
            result.put("pubAgencyFullname",search.get("pubAgencyFullname"));
            result.put("policyGrade",search.get("policyGrade"));
            Policy policy=getPolicy(Long.parseLong(search.get("policyId").toString()));
            result.put("views",policy.getViews());
            //存入结果总页数
            result.put("pageSum",pageSum);
            resultList.add(result);
        }
        return resultList;
    }

    /**
     * 将string格式的Map<String,List<String>还原
     */
    private Map<String,List<String>> stringToMap(String str,String notHighTitle,String notHighContent) {
        String policyTitle,textContent;
        //去除首尾{}符号
        str = str.substring(1, str.length() - 1);
        //说明高亮为空，返回原文
        if(str.length()==0){
            policyTitle = notHighTitle;
            textContent=notHighContent;
        }
        else{
            //记录这textContent字符串的首次出现位置,则以它为界就可以划分list
            int i = str.indexOf("textContent=");
            //i==0说明policyTitle不存在高亮,highlight中只有textContent字段
            if(i==0){
                policyTitle = notHighTitle;
                textContent = str.substring(13,str.length()-1);
            }
            //i==-1表示textContent=不存在，即正文不存在高亮，highlight中只有policyTitle字段
            else if(i==-1){
                policyTitle = str.substring(13,str.length()-1);
                textContent=notHighContent;
            }
            //否则就是两个都存在的情况
            else {
                //去除字符串本身和[]符号
                policyTitle = str.substring(13, i-3);
                textContent= str.substring(i+13,str.length()-1);
            }
        }
        //如果标题或正文为空/过短，考虑用户体验直接不显示此结果
        if(policyTitle.length()<5||textContent.length()<5){
            return null;
        }
        Map<String,List<String>> map = new HashMap<>();
        map.put("policyTitle",List.of(policyTitle));
        map.put("textContent",List.of(textContent));
        return map;
    }

    /**
     * 删除ArrayList中重复元素，保持顺序
     */
    public void removeDuplicateWithOrder(List<String> list) {
        Set<String> set = new HashSet<>();
        List<String> newList = new ArrayList<>();
        for (String element : list) {
            //如果这个元素可以添加到set中，说明是一个新的元素，将它加入到newList中
            if (set.add(element)) {
                newList.add(element);
            }
        }
        list.clear();
        list.addAll(newList);
    }

    /**
     * 提取前1000条政策标题关键词，作为初始联想词
     */
    public void initSuggestWord(){
        Map<String,Integer> result=new HashMap<>();
        List<Policy> policyList= list();
        for (Policy policy:policyList){
            List<Word> words= WordSegmenter.seg(policy.getPolicyTitle());
            System.out.println(words);
            for(Word word:words){
                //过滤极短分词（没有太大参考意义）
                if(word.toString().length()>2){
                    //如果关键词已存在，次数加一（可能后面优化suggest时有用）
                    if(result.containsKey(word.toString())){
                        int num=result.get(word.toString())+1;
                        result.put(word.toString(),num);
                    }
                    //否则将关键词加入suggest中
                    else {
                        addSuggestToEs(word.toString());
                        result.put(word.toString(),1);
                    }
                }
            }
        }
    }
}
