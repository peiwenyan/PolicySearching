package org.project.es.face;

import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import org.apache.commons.collections.map.HashedMap;
import org.project.es.common.module.UserBrowseHistory;
import org.project.es.common.module.UserFavorites;
import org.project.es.favor.FavorsService;
import org.project.es.favor.HistoryService;
import org.project.es.policy.PolicyService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 根据用户浏览记录、收藏得到其中的政策所属分类，
 * 按照权重（收藏>浏览），得到这个用户对应的分类画像，根据这个结果以实现个性化推荐
 * 分类信息：['税务', '销售', '旅游', '金融贸易', '政务', '交通/物流/供应链', '服务业', '行政', '汽车', '低保', '民生', '医疗健康', '体育', '脱贫',
 *         '信息安全', '建筑', '政府', '社会', '节能减排', '互联网', '扶贫', '文化', '农业', '信息化', '物流', '招标', '其他', '工业', '渔业', '环境',
 *         '教育', '食品安全', '社会保障', '交通', '社保', '科技', '经济', '节能环保', '党建', '法律', '投资', '财政', '违法犯罪', '行业', '武汉']
 *
 * @author Administrator
 */
public class FaceService {
    /**
     * 当前工作：
     * 1.整理标注，完成政策分类预测，并合并分类和本体
     * 2.修改政策表的字段，添加分类字段，将预测结果导入
     * 3.从mysql将政策表导入es，让搜索的分类字段可以工作
     * 4，实现下方的用户行为画像
     * 5，根据画像对搜索的score重新计算并排序（这个可能会很慢，如果这样需要考虑是否能修改es内置的评分规则），实现个性化搜索
     * 6.接口：政策文章下方的推荐：按照分类。。
     * 7.接口：政策溯源，高亮查询，内置搜索textContent
     * 8.接口：个性化推荐：按照用户画像的分类，成比例推荐
     */
    @Inject
    FavorsService favorsService;
    @Inject
    PolicyService policyService;
    @Inject
    HistoryService historyService;

    /**
     * 设定收藏和浏览对于用户画像的影响
     */
    float favorScore= (float) 0.6,historyScore= (float) 0.4;
    public List<String> labels= List.of("建筑","招标","农业","文化","交通/物流/供应链", "节能环保", "税务", "服务业", "旅游", "法律", "教育", "工业", "经济", "金融贸易",
            "科学技术", "政务", "社会保障", "医疗健康", "其他");
    /**
     * num表示个性化指数，数值越大，搜索结果越偏向个性化
     */
    public double labelNum=4;
    public double maxLabelScore=1.5;
    public double minLabelScore=1.0;
    /**
     * 遍历用户收藏，从中取出所有的收藏政策的分类信息
     * 1.从收藏夹表找到某个用户的所有收藏夹id
     * 2，根据收藏夹id从收藏表中找到所有收藏的id
     * 3.根据id找到政策分类，完成用户收藏的画像
     */
    public Map<String,Float> getLabelInFavor(long userId){
        List<UserFavorites> userFavoritesList=favorsService.getUserFavor(userId);
        Map<String, Float> labelScore=new HashMap<>();
        // 对每条收藏，获取其分类
        for(UserFavorites userFavorites:userFavoritesList){
            String label=policyService.getPolicy(Long.parseLong(String.valueOf(userFavorites.getPolicyId()))).getLabel();
            if(labelScore.containsKey(label)){
                Float score=labelScore.get(label)+1;
                labelScore.put(label,score);
            }
            else {
                labelScore.put(label, 1F);
            }
        }
        return getScore(labelScore);
    }

    /**
     * 遍历用户浏览记录，从中取出所有浏览政策的分类信息
     */
    public Map<String, Float> getLabelInHistory(long userId){
        List<UserBrowseHistory> userBrowseHistories=historyService.getUserHistory(userId);
        Map<String, Float> labelScore=new HashMap<>();
        // 对每条浏览，获取其分类
        for(UserBrowseHistory userBrowseHistory:userBrowseHistories){
            String label=policyService.getPolicy(Long.parseLong(String.valueOf(userBrowseHistory.getPolicyId()))).getLabel();
            if(labelScore.containsKey(label)){
                Float score=labelScore.get(label)+1;
                labelScore.put(label,score);
            }
            else {
                labelScore.put(label, 1F);
            }
        }
        return getScore(labelScore);
    }

    /**
     * 根据浏览和收藏的分类信息，按照权重得到用户的分类信息，即用户画像
     */
    public Map<String,Float> getUserLabel(long userId){
        if(userId==-1){
            return null;
        }
        Map<String,Float> favor=getLabelInFavor(userId);
        Map<String,Float> history=getLabelInHistory(userId);
        Map<String,Float> full=new HashMap<>();
        Set<String> label1=favor.keySet();
        Set<String> label2=history.keySet();
        Set<String> labels=setMerge(label1,label2);
        //按照浏览、收藏权重计算分数
        for(String label:labels){
            float fullScore= (float) 0;
            if (favor.get(label)!=null){
                fullScore+=favor.get(label)*favorScore;
            }
            if (history.get(label)!=null){
                fullScore+=history.get(label)*historyScore;
            }
            full.put(label,fullScore);
        }
        //总分合为1（例如浏览或收藏某一个为空时，fullScore的总分就会变为对应的favorScore、historyScore）
        return getScore(full);
    }

    public Map<String,Float> getScore(Map<String,Float> map){
        Set<String> labels=map.keySet();
        //计算总分
        Float full= (float) 0;
        for(String label:labels){
            full+=map.get(label);
        }
        for(String label:labels){
            Float score=map.get(label)/full;
            map.put(label,score);
        }
        return map;

    }
    public static Set<String> setMerge(Set<String> set1, Set<String> set2){
        Set<String> full = new HashSet<>(set1);
        full.addAll(set2);
        return full;
    }
}
