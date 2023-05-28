package org.project.es.favor;

import cn.fabrice.jfinal.service.BaseService;
import com.jfinal.aop.Inject;
import com.jfinal.kit.Kv;
import org.project.es.common.module.UserFavorites;
import org.project.es.common.module.UserFavoritesType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 */
public class FavorsService extends BaseService<UserFavorites> {
    @Inject
    FavorTypeService favorTypeService;

    public FavorsService() {
        super("favor.", UserFavorites.class, "user_favorites");
    }

    /**
     * 获取某个用户的全部收藏
     */
    public List<UserFavorites> getUserFavor(long userId){
        List<UserFavorites> result=new ArrayList<>();
        List<UserFavoritesType> userFavoritesTypeList=favorTypeService.getUserFavorType(userId);
        // 对每个收藏夹，获取其中的全部收藏
        for(UserFavoritesType userFavoritesType:userFavoritesTypeList){
            BigInteger typeId=userFavoritesType.getId();
            Kv cond= Kv.by("user_favorites_type_id",typeId);
            List<UserFavorites> favorites=list(cond,"getTypeUserFavor");
            result.addAll(favorites);
        }
        return result;
    }
}
