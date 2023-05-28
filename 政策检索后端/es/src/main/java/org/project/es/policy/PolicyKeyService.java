package org.project.es.policy;

import cn.fabrice.jfinal.service.BaseService;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import org.project.es.common.module.Policy;
import org.project.es.common.module.PolicyKey;
import org.project.es.common.module.UserBrowseHistory;
import org.project.es.common.module.UserFavorites;
import org.project.es.favor.FavorsService;
import org.project.es.favor.HistoryService;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
public class PolicyKeyService extends BaseService<PolicyKey> {
    @Inject
    FavorsService favorsService;
    @Inject
    HistoryService historyService;
    /**
     * 出现两次及以上的关键词作为用户关键词，最终取resultNum个关键词
     */
    int keyNum= 2;
    int resultNum=10;

    public PolicyKeyService() {
        super("policy.", PolicyKey.class, "policy_key");
    }

    public String getPolicyKey(long policyId){
        Kv cond= Kv.by("id",policyId);
        PolicyKey policyKey=get(cond,"getPolicyKey");
        if(policyKey==null){
            return null;
        }
        return policyKey.getKeyList();
    }

    /**
     * 获取用户全部收藏夹中的全部政策对应的关键词
     */
    public Map<String,Integer> getUserFavorKey(long userId){
        List<UserFavorites> favoritesList=favorsService.getUserFavor(userId);
        // 统计关键词的出现次数，出现次数>=keyNum的关键词作为用户关键词
        Map<String,Integer> keyMap=new HashMap<>();
        for(UserFavorites userFavorites:favoritesList){
            BigInteger policyId=userFavorites.getPolicyId();
            putKey(policyId,keyMap);
        }
        return keyMap;
    }

    /**
     * 获取用户浏览记录中的全部政策对应的关键词
     */
    public Map<String,Integer> getUserHistoryKey(long userId){
        List<UserBrowseHistory> historyList=historyService.getUserHistory(userId);
        Map<String,Integer> keyMap=new HashMap<>();
        for(UserBrowseHistory history:historyList){
            BigInteger policyId=history.getPolicyId();
            putKey(policyId,keyMap);
        }
        return keyMap;
    }

    /**
     * 综合得到用户关键词（根据一定的标准，暂定：次数>=keyNum，浏览收藏1:1直接相加）
     */
    public Map<String,Integer> getUserKey(long userId){
        Map<String,Integer> favorKey=getUserFavorKey(userId);
        Map<String,Integer> historyKey=getUserHistoryKey(userId);
        Map<String, Integer> result=new HashMap<>();
        Set<String> favorSet=favorKey.keySet();
        Set<String> historySet=historyKey.keySet();
        //合并两者关键词，并只保留其中value超过keyNum的关键词
        for(String favor:favorSet){
            result.put(favor,favorKey.get(favor));
        }
        for(String history:historySet){
            //从结果中查看history是否曾经在favorSet中出现过，如果出现则result中history的value为二者之和
            int favorValue = result.getOrDefault(history, 0);
            int resultValue=historyKey.get(history)+favorValue;
            if (resultValue>keyNum){
                result.put(history,resultValue);
            }
            else {
                result.remove(history);
            }
        }
        return subMapByValue(result,resultNum);
    }

    /**
     * 根据政策id将政策的关键词存入字典
     */
    public void putKey(BigInteger policyId,Map<String,Integer> keyMap){
        String key=getPolicyKey(policyId.longValue());
        if(key!=null){
            // 去除字符串列表中的不合适字符
            key=key.replace("[","");
            key=key.replace("]","");
            key=key.replace("'","");
            key=key.replace(" ","");
            // 将关键词列表存入map中(同时统计关键词出现次数)
            String[] keys= key.split(",");
            for(String k:keys){
                if(keyMap.containsKey(k)){
                    int value= keyMap.get(k) +1;
                    keyMap.put(k, value);
                }
                else {
                    keyMap.put(k, 1);
                }
            }
        }
    }

    /**
     * Map按照整数型的value进行降序排序，当value相同时，按照key的长度进行排序
     * https://blog.csdn.net/Elliot_Elliot/article/details/121657128
     */
    public static LinkedHashMap<String, Integer> sortMap(Map<String, Integer> map) {
        return map.entrySet().stream().sorted(((item1, item2) -> {
            int compare = item2.getValue().compareTo(item1.getValue());
            if (compare == 0) {
                if (item1.getKey().length() < item2.getKey().length()) {
                    compare = 1;
                } else if (item1.getKey().length() > item2.getKey().length()) {
                    compare = -1;
                }
            }
            return compare;
        })).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * 按照map中value的大小，获取排名前n位的map结果
     * 即：map按value排序后取前n位
     */
    public Map<String,Integer> subMapByValue(Map<String,Integer> map,int n){
        if(n>=map.size()){
            return map;
        }
        map=sortMap(map);
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        Map<String,Integer> result=new HashMap<>();
        //取出n个,在迭代器没有遍历完、没有取到n个数据时不会停止
        while (iterator.hasNext()&&n>0){
            Map.Entry<String, Integer> next = iterator.next();
            result.put(next.getKey(),next.getValue());
            n--;
        }
       return result;
    }
}
