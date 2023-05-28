package org.project.es.common.util;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import com.alibaba.fastjson.JSONObject;
/**
 * 将json文件装换为json对象
 * @author Administrator
 */
public class JsonUtil {
    public static JSONObject fileToJson(String fileName) {
        JSONObject json = null;
        try (
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        ) {
            assert is != null;
            json = JSONObject.parseObject(IOUtils.toString(is, "utf-8"));
        } catch (Exception e) {
            System.out.println(fileName + "文件读取异常" + e);
        }
        return json;
    }
    public static void main(String[] args) {
        String fileName = "doc/policy.json";
        JSONObject json = JsonUtil.fileToJson(fileName);
        System.out.println(json);
    }
}