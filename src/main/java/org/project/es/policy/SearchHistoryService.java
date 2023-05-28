package org.project.es.policy;

import cn.fabrice.common.pojo.DataResult;
import cn.fabrice.jfinal.service.BaseService;
import com.jfinal.kit.Kv;
import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.Word;
import org.project.es.common.module.UserSearchHistory;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Administrator
 */
public class SearchHistoryService extends BaseService<UserSearchHistory> {

    public SearchHistoryService(){
        super("policy.",UserSearchHistory.class,"user_search_policy");
    }

    public void addSearchHistory(long userId,String search){
        Kv cond = Kv.by("id", userId).set("search", search);
        UserSearchHistory userHistory = get(cond, "getUserExistSearch");
        if(userHistory==null){
            userHistory =new UserSearchHistory();
            userHistory.setUserId(BigInteger.valueOf(userId));
            userHistory.setSearchContent(search);
            userHistory.save();
        }
    }

    /**
     * 获取与搜索框中输入内容匹配的用户历史记录
     * 如果为null则返回用户所有历史记录（见sql语句）
     */
    public List<String > getUserSearch(long userId,String value){
        List<String> result=new ArrayList<>();
        List<UserSearchHistory> historyList;
        if(value==null){
            Kv cond = Kv.by("id", userId);
            historyList = list(cond, "getUserAllSearch");
        }
        else {
            Kv cond = Kv.by("id", userId).set("value", value);
            historyList= list(cond, "getUserSearch");
        }
        for (UserSearchHistory userSearchHistory:historyList){
            result.add(userSearchHistory.getSearchContent());
        }
        return result;
    }

    /**
     * 使用word分词器，对所有搜索记录进行分词，形成词语Map用于生成词云
     * @return 返回词语Map
     */
    public DataResult getCutWord(){
        Map<String,Integer> result=new HashMap<>();
        List<UserSearchHistory> allSearch = list("getAllSearch");
        //分词存入map
        for (UserSearchHistory searchHistory:allSearch){
            List<Word> words=WordSegmenter.seg(searchHistory.getSearchContent());
            System.out.println(words);
            for(Word word:words){
                int num=result.getOrDefault(word.toString(),0)+1;
                result.put(word.toString(),num);
            }
        }
        //转换为前端需要的格式
        Set<String> resultSet=result.keySet();
        List<Map<String,Object>> resultList=new ArrayList<>();
        for(String set:resultSet){
            Map<String,Object> r=new HashMap<>();
            r.put("name",set);
            r.put("value",result.get(set));
            resultList.add(r);
        }
        return DataResult.data(resultList);
    }
}
