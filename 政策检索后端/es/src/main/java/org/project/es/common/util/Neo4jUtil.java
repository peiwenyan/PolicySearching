package org.project.es.common.util;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import java.util.ArrayList;
import java.util.List;

import static org.project.es.common.App.driver;


/**
 * @author Administrator
 */
public class Neo4jUtil{

    /**
     * 获取推荐词集合
     * @param word 查询词
     * @return 返回推荐词集合 List<String>
     */
    public List<String> listQueryWords(String word) {
        Session session = driver.session();
        // 查询
        String neo4jCql = "match (n:Policy)-[]->(n1:Policy) where '" + word + "'contains n.name return n1";
        Result result = session.run(neo4jCql);
        List<String> queryList = new ArrayList<>();
        while (result.hasNext()){
            Record record = result.next();
            //查询记录值,每个记录中包含一个节点
            List<Value> valueList = record.values();
            for (Value value : valueList){
                Node node = value.asNode();
                // 去掉双引号
                queryList.add(node.get("name").toString().replace("\"",""));
            }
        }
        session.close();
        return queryList;
    }
}
