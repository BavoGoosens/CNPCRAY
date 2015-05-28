import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

/**
 * Created by bavo en michiel.
 */
public class Task extends Parcel {

    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;

    private final Point origin;
    private CNPAgent agent = null;

    public Task(Point origin, Point destination, long pickupDuration) {
        super(destination, pickupDuration, TimeWindow.ALWAYS, pickupDuration, TimeWindow.ALWAYS, 1);
        this.origin = origin;
    }

    public Point getPosition() {
        return this.getRoadModel().getPosition(this);
    }

    public CNPAgent getAgent() {
        return this.agent;
    }

    public void pickUp(CNPAgent agent) {
        this.agent = agent;
    }

    public void drop() {
        this.agent = null;
    }

    public boolean isAssigned() {
        return this.agent != null;
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
        this.roadModel = Optional.of(roadModel);
        this.pdpModel = Optional.of(pdpModel);
        roadModel.addObjectAt(this, origin);
    }
}
