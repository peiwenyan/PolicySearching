package org.project.es.es;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.project.es.common.module.Policy;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Administrator
 * https://blog.csdn.net/wangxiaozhonga/article/details/127965918
 */
public class EsDoc {
    public static String suggestIndex="suggest_index";
    public static void main(String[] args) throws IOException {
        // 创建客户端对象
        RestHighLevelClient client = EsIndex.setTimeOut();
        getDoc(client);
        // 关闭客户端连接
        client.close();
    }

    /**
     * 创建文档
     * 将object存入指定索引index，并指定唯一标识id
     * @param client 客户端连接
     */
    public static void createDoc(RestHighLevelClient client,String index,String id,Object object) throws IOException {
        // 新增文档 - 请求对象
        IndexRequest request = new IndexRequest();
        // 设置索引及唯一性标识,suggest_index没有设置id，因此让其默认自行创建
        if(suggestIndex.equals(index)) {
            request.index(index);
        } else {
            request.index(index).id(id);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        // 直接传递给未创建的索引后可以直接创建索引，同时索引的字段类型和传递的值一致
        String productJson = objectMapper.writeValueAsString(object);
        // 添加文档数据, 数据格式为Json格式
        request.source(productJson, XContentType.JSON);
        // 客户端发送请求，获取响应对象，这一句有问题，无法解析返回值，这里是发送的过程，如果去掉这一句，user就不会存入es
        // --解决：java使用的maven客户端版本和本地客户端版本不一致，本地版本较高，因此java无法解析，修改本地版本8.0.0--7.10.2后无报错
        // --es的数据存到哪里去了？？（不在数据库）
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        // 打印结果信息
        System.out.println("_index: " + response.getIndex());
        System.out.println("id: " + response.getId());
        System.out.println("_result: " + response.getResult());
        System.out.println();
    }

    /**
     * 修改文档
     * @param client 客户端连接
     */
    public static void updateDoc(RestHighLevelClient client) throws IOException {
        // 修改文档 - 请求对象
        UpdateRequest request = new UpdateRequest();
        // 配置修改参数
        request.index("user_index").id("1");
        // 设置请求体，对数据进行修改
        request.doc(XContentType.JSON, "sex", "女");
        // 客户端发送请求，获取响应对象
        UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
        System.out.println("_index: " + response.getIndex());
        System.out.println("_id: " + response.getId());
        System.out.println("_result: " + response.getResult());
    }

    /**
     * 查询文档
     * @param client 客户端连接
     */
    public static void getDoc(RestHighLevelClient client) throws IOException {
        // 创建请求对象
        GetRequest request = new GetRequest().index("user_index").id("1");
        // 客户端发送请求，获取响应对象
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        // 打印结果信息
        System.out.println("_index: " + response.getIndex());
        System.out.println("_type: " + response.getType());
        System.out.println("_id: " + response.getId());
        System.out.println("source: " + response.getSourceAsString());
    }

    /**
     * 删除文档
     * @param client 客户端连接
     */
    public static void deleteDoc(RestHighLevelClient client) throws IOException {
        // 创建请求对象
        DeleteRequest request = new DeleteRequest().index("user_index").id("1");
        // 客户端发送请求，获取响应对象
        DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        // 打印信息
        System.out.println(response.toString());
    }

    /**
     * 批量新增
     * 指定为Policy类型
     * @param client 客户端连接
     */
    public static void bulkCreateDoc(RestHighLevelClient client,String index, List<Policy> list) throws IOException {
        // 创建批量新增请求对象
        BulkRequest request = new BulkRequest();
        // 将列表中的数据依次添加到request中
        for(Policy policy:list){
            IndexRequest indexRequest=new IndexRequest().index(index).id(String.valueOf(policy.getId()));
            // 将Policy类构建为json格式
            ObjectMapper objectMapper = new ObjectMapper();
            String productJson = objectMapper.writeValueAsString(policy);
            // 添加构建好的json格式文档数据
            indexRequest.source(productJson, XContentType.JSON);
            request.add(indexRequest);
        }
        // 客户端发送请求，获取响应对象
        BulkResponse responses = client.bulk(request, RequestOptions.DEFAULT);
        // 打印结果信息
        System.out.println("took: " + responses.getTook());
        System.out.println("items: " + Arrays.toString(responses.getItems()));
    }

    /**
     * 批量删除
     * @param client 客户端连接
     */
    public static void bulkDeleteDoc(RestHighLevelClient client) throws IOException {
        // 创建批量删除请求对象
        BulkRequest request = new BulkRequest();
        request.add(new DeleteRequest().index("user_index").id("1001"));
        request.add(new DeleteRequest().index("user_index").id("1002"));
        request.add(new DeleteRequest().index("user_index").id("1003"));
        // 客户端发送请求，获取响应对象
        BulkResponse responses = client.bulk(request, RequestOptions.DEFAULT);
        // 打印结果信息
        System.out.println("took: " + responses.getTook());
        System.out.println("items: " + Arrays.toString(responses.getItems()));
    }
}
