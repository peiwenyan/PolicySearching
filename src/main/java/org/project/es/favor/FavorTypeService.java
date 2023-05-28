package org.project.es.favor;

import cn.fabrice.jfinal.service.BaseService;
import com.jfinal.kit.Kv;
import org.project.es.common.module.User;
import org.project.es.common.module.UserFavoritesType;

import java.util.List;

/**
 * @author Administrator
 */
public class FavorTypeService extends BaseService<UserFavoritesType> {
    public FavorTypeService() {
        super("favor.", UserFavoritesType.class, "user_favorites_type");
    }

    /**
     * 获取某个用户的全部收藏夹
     */
    public List<UserFavoritesType> getUserFavorType(long userId){
        Kv cond= Kv.by("user_id",userId);
        return list(cond,"getUserFavorType");
    }
}
