package org.project.es.face;

import cn.fabrice.common.pojo.DataResult;
import com.jfinal.aop.Clear;
import com.jfinal.aop.Inject;
import com.jfinal.core.Controller;
import com.jfinal.core.Path;
import org.project.es.favor.FavorsService;

/**
 * @author Administrator
 */
@Path("/face")
public class FaceController extends Controller {
    @Inject
    FavorsService favorsService;
    @Inject
    FaceService faceService;

    public void getFavor(){
        renderJson(favorsService.getUserFavor(1));
    }

    public void getLabel(){
        long userId=1;
        renderJson(DataResult.data(faceService.getUserLabel(userId)));
    }
}
