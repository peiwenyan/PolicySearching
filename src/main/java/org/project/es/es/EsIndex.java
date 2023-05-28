package org.project.es.es;


import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.project.es.common.util.JsonUtil;

import java.io.IOException;

/**
 * @author Administrator
 */
public class EsIndex {
    public static void main(String[] args) throws IOException {
        // 创建客户端对象
        RestHighLevelClient client = setTimeOut();
        // 关闭客户端连接
        client.close();
    }

    /**
     * 创建政策索引
     * @param client 客户端连接
     */
    public static void createPolicyIndex(RestHighLevelClient client) throws IOException {
        // 1.创建索引 - 请求对象
        String index="policy_index";
        CreateIndexRequest request=addIndexSetting(index);
        // 3.使用工具类将json文件转换为json对象，添加到请求对象中
        JSONObject mapping = JsonUtil.fileToJson("doc/policy.json");
        request.mapping(mapping);
        // 4.发送请求，获取响应
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        // 5.输出响应状态
        System.out.println("操作状态 = " + response.isAcknowledged());
    }

    /**
     * 创建政策索引
     * @param client 客户端连接
     */
    public static void createSuggestIndex(RestHighLevelClient client) throws IOException {
        // 1.创建索引 - 请求对象
        String index="suggest_index";
        CreateIndexRequest request=addIndexSetting(index);
        // 3.使用工具类将json文件转换为json对象，添加到请求对象中
        JSONObject mapping = JsonUtil.fileToJson("doc/suggest.json");
        request.mapping(mapping);
        // 4.发送请求，获取响应
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        // 5.输出响应状态
        System.out.println("操作状态 = " + response.isAcknowledged());
    }



    /**
     * 查看索引
     * @param client 客户端连接
     */
    public static java.util.Map<String, org.elasticsearch.cluster.metadata.MappingMetadata> getIndex(RestHighLevelClient client, String index) throws IOException {
        // 查询索引 - 请求对象
        GetIndexRequest request = new GetIndexRequest(index);
        // 发送请求，获取响应
        GetIndexResponse response=client.indices().get(request, RequestOptions.DEFAULT);
        return response.getMappings();
    }

    /**
     * 删除索引
     * @param client 客户端连接
     */
    public static void deleteIndex(RestHighLevelClient client,String index) throws IOException {
        // 删除索引 - 请求对象
        DeleteIndexRequest request = new DeleteIndexRequest(index);
        // 发送请求，获取响应
        AcknowledgedResponse response = client.indices().delete(request, RequestOptions.DEFAULT);
        // 操作结果
        System.out.println("操作结果: " + response.isAcknowledged());
    }

    /**
     * 创建连接并修改超时时间
     * @return 返回客户端连接
     */
    public static RestHighLevelClient setTimeOut() {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost("47.115.208.248", 9200, "http"))
                        .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                            // 该方法接收一个RequestConfig.Builder对象，对该对象进行修改后然后返回。
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(
                                    RequestConfig.Builder requestConfigBuilder) {
                                return requestConfigBuilder.setConnectTimeout(5000 * 1000) // 连接超时（默认为1秒）
                                        .setSocketTimeout(6000 * 1000);// 套接字超时（即客户端的超时限制默认30秒）
                            }
                        }));
    }

    /**
     * 添加索引的基本配置信息
     * https://www.knowledgedict.com/tutorial/elasticsearch-query.html
     */
    public static CreateIndexRequest addIndexSetting(String index){
        CreateIndexRequest request = new CreateIndexRequest(index);
        // 2.设置setting，也就是索引的基本配置信息，将setting添加到请求对象中
        Settings setting = Settings.builder()
                //设置分片数,主分片数量一旦设置后就不能修改了（即索引内容将被分成几个分片并在分片上分别执行查询后综合返回结果，注意不要设置太多小分片）
                .put("index.number_of_shards", 1)
                //索引的刷新时间间隔,索引更新多久才对搜索可见(即数据写入es到可以搜索到的时间间隔，设置越小越靠近实时，但是索引的速度会明显下降，),
                // 默认为1秒，如果我们对实时搜索没有太大的要求，反而更注重索引的速度，那么我们就应该设置的稍微大一些，这里设置30s
                .put("index.refresh_interval", "30s")
                //每个节点上允许最多分片数
                .put("index.routing.allocation.total_shards_per_node", 3)
                //将数据同步到磁盘的频率,为了保证性能，插入ES的数据并不会立刻落盘，而是首先存放在内存当中，
                // 等到条件成熟后触发flush操作，内存中的数据才会被写入到磁盘当中
                .put("index.translog.sync_interval", "30s")
                //每个主分片拥有的副本数,副本数量可以修改（即备份）
                .put("index.number_of_replicas", 0)
                //一次最多获取多少条记录
                .put("index.max_result_window", "10000000")
                .build();
        request.settings(setting);
        return request;
    }
}
