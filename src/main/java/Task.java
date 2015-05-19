import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * Created by bavo en michiel.
 */
public class Task extends Parcel {

    private final Point origin;

    public Task(Point origin, Point destination, long pickupDuration) {
        super(destination, pickupDuration, TimeWindow.ALWAYS, pickupDuration, TimeWindow.ALWAYS, 1);
        this.origin = origin;
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
        roadModel.addObjectAt(this, origin);
    }
}
