import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Created by bavo and michiel.
 */
public class TaskStation implements CommUser, RoadUser {
    @Override
    public Optional<Point> getPosition() {
        return null;
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {

    }

    @Override
    public void initRoadUser(RoadModel roadModel) {

    }
}
