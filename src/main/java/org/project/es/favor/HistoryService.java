package org.project.es.favor;

import cn.fabrice.jfinal.service.BaseService;
import com.jfinal.kit.Kv;
import org.project.es.common.module.UserBrowseHistory;

import java.util.List;

/**
 * @author Administrator
 */
public class HistoryService extends BaseService<UserBrowseHistory> {
    public HistoryService(){
        super("favor.",UserBrowseHistory.class,"user_browse_history");
    }

    public List<UserBrowseHistory> getUserHistory(long userId){
        Kv cond= Kv.by("user_id",userId);
        return list(cond,"getUserHistory");
    }
}
