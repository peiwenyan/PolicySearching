package org.project.es.policy;

import cn.fabrice.common.pojo.BaseResult;
import cn.fabrice.common.pojo.DataResult;
import cn.fabrice.jfinal.annotation.Param;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.core.Controller;
import com.jfinal.core.Path;
import org.project.es.common.util.ExcelUtil;
import org.project.es.face.FaceService;

/**
 * @author Administrator
 * 待处理：
 * 以下是二次排序功能：
 * 7.根据提取关键词对政策进行相关推荐 ×
 * 8.按照用户填写的行业、城市进行更好的推荐（二次排序） √行业没有考虑形成行业-标签映射表，将标签添加到用户画像中
 * 二次排序：用户画像（分类标签，关键词）、拓展词、用户行业（考虑形成行业-分类的映射，将这个转换到用户画像中）、用户城市
 * 9.为了优化检索速度，可以忽略那些不作为检索要求的字段，例如省份id等，这个可以通过检索获取policy_id--根据policy_id从policy表中得到对应policy信息的形式获取
 * 但这样有可能policy_id在mysql中的检索时间较长，反而得不偿失，这个需要后面测试
 * 10.需要使用label_num字段（因为二次排序不知道怎么根据标签内容排序，只能转为数字的方式进行加分，这里因为有其他的二次排序要求，应该是不能将就了，还是要想办法搞清楚二次排序）
 * 目前数据更新导入方式：（1）使用查询方法创建新表，从result.xlsx导入 （2）修改labels分类列表，调用标签编号方法 （3）删除索引-创建索引-导入索引 （4）尝试搜索
 *
 * 1.font标签匹配，不能只返回一节 √考虑前端截取？
 * 2.政策溯源下方的推荐 √但内容待思考，根据城市没什么太大意义--热搜
 * --游客：根据城市推荐
 * --用户：用户个性推荐
 * --pageNo，pageSize
 * 3.政策结果的评分
 * --搜索：修改用户个性化的权重，城市匹配 √待优化
 * --溯源：文本匹配度
 * 4.政策溯源：返回高亮文本而不是整篇文本 √
 * 5.搜索结果限定条数 √
 * 6.知识图谱返回拓展词，收取拓展词进行二次搜索 √
 * 7.返回结果列表中过滤为空的结果 √
 * 8.注意MySQL的insert数据问题，避免重复插入。。。 √
 * 9.缓存池记录用户画像结果
 * 10.联想词库初始化：https://z.itpub.net/article/detail/0DA9670797AD12A85436E97AE2D065EC √粗粒度分词？
 * 11.es同义词：http://www.javashuo.com/article/p-kngzwejd-cu.html
 */
@Path("/policy")
public class PolicyController extends Controller {
    /**
     * 参考文档：
     * 使用explain解释评分：https://blog.csdn.net/qq_46416934/article/details/124241670
     * 衰减查询缺失值：https://github.com/elastic/elasticsearch/pull/34533
     * java添加多种自定义评分规则：https://blog.csdn.net/xiaoll880214/article/details/86716393
     * 解释评分的具体内容含义：
     * 官方自定义评分文档：https://www.elastic.co/guide/en/elasticsearch/reference/8.7/query-dsl-function-score-query.html
     */
    @Inject
    PolicyService policyService;
    @Inject
    PolicyKeyService policyKeyService;
    @Inject
    SearchHistoryService searchHistoryService;
    @Inject
    ExcelUtil excelUtil;
    @Inject
    FaceService faceService;

    /**
     * 添加标签对应的num，用于检索匹配分类
     */
    public void setLabelNum(){
        renderJson(policyService.setProvinceNum());
    }

    public void setPolicyId(){
        excelUtil.setPolicyId();
    }

    /**
     * 第一次处理：8万多条数据，但id不是数据对应的当前行，而是对应15万数据中的某一行，因此index_id有15万多--对应id
     * 数据库无法存储，
     * 1.可能是policy_body超过longtext的长度，但也有可能是其他字段溢出，因此考虑使用python读取文件，计算每个字段的最大长度
     * 2.如果还不行，考虑到mysql的单行存储长度有限制--但longtext等长文本在mysql中会单独存储，在对应行中只占用几个字节，因此这个溢出的概率感觉不大
     * 主要考虑1的尝试解决（PS：成功解决！！！是其他字段溢出的问题）
     *
     * 关于longtext：
     * https://www.yii666.com/blog/312514.html
     * 可以存储2^32个字节长度，注意这里是字节长度，而不是字符长度（varchar(n)这里的n是字符长度）
     * 在 utf8mb4字符集下英文占用1个字节长度，一般汉字占3-4个字节长度。
     * 那么utf8mb4 字符集下，大约能存 2^30个汉字
     */
    public void addPolicy(){
        renderJson(policyService.addPolicyToEsOne());
    }

    /**
     * 注意key是驼峰形式而不是_格式（对应Policy.class）
     */
    public void termSearch(String key,String value){
        policyService.addSuggestToEs(value);
        renderJson(policyService.termSearch(key,value));
    }

    public void matchSearch(String key,String value){
        policyService.addSuggestToEs(value);
        renderJson(policyService.matchSearch(key,value));
    }

    public void fuzzySearch(String key,String value){
        policyService.addSuggestToEs(value);
        renderJson(policyService.fuzzySearch(key,value));
    }

    public void lightSearch(String key,String value){
        policyService.addSuggestToEs(value);
        renderJson(policyService.lightSearch(key,value));
    }


    @Param(name = "userId", required = true)
    @Param(name = "pageNo", required = true)
    @Param(name = "pageSize", required = true)
    @Param(name = "userProvince",required = true)
    public void boolSearch(long userId,String policyGrade,String province, String label,String policyType,
                           String pubAgency,int time,String value,int pageNo,int pageSize,String userProvince){
        policyService.addSuggestToEs(value);
        if(userId!=-1&&value!=null){
            searchHistoryService.addSearchHistory(userId,value);
        }
        renderJson(policyService.boolSearch(policyGrade,province, label, policyType,
                pubAgency, time, value,userId,pageNo,pageSize,userProvince));
    }

    public void suggestSearch(long userId,String value){
        renderJson(policyService.suggestSearch(userId,value));
    }

    public void expandSearch(String value){
        renderJson(policyService.expandSearch(value));
    }

    public void createPolicyIndex(String index){
        renderJson(policyService.createPolicyIndex());
    }

    public void createSuggestIndex(String index){
        renderJson(policyService.createSuggestIndex());
    }

    public void deleteIndex(String index){
        renderJson(policyService.deleteIndex(index));
    }

    public void getIndex(String index){
        renderJson(policyService.getIndex(index));
    }

    public void policyResource(int pageNo,int pageSize,String content){
        renderJson(policyService.policyResource(pageNo,pageSize,content));
    }

    public void pushUnderPolicy(long policyId){
        renderJson(policyService.pushUnderPolicy(policyId));
    }

    public void pushResourcePolicy(long userId,String userProvince,int pageNo,int pageSize){
        renderJson(policyService.pushResourcePolicy(userId,userProvince,pageNo,pageSize));
    }

    public void pushUserPolicy(long userId,String userProvince){
        renderJson(policyService.pushUserPolicy(userId,userProvince));
    }

    public void getCutWord(){
        renderJson(searchHistoryService.getCutWord());
    }

    public void getUserFace(long userId){
        renderJson(policyService.setFace(userId));
    }

    public void getUserFavorKey(){
        policyKeyService.getUserKey(1);
    }

    public void initSuggestWord(){
        policyService.initSuggestWord();
    }
}
