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
    private CNPAgent agent = null;

    public Task(Point origin, Point destination, long pickupDuration) {
        super(destination, pickupDuration, TimeWindow.ALWAYS, pickupDuration, TimeWindow.ALWAYS, 1);
        this.origin = origin;
    }

    public CNPAgent getAgent() {
        return this.agent;
    }

    public void setAgent(CNPAgent agent) {
        this.agent = agent;
    }

    public boolean isAssigned() {
        return this.agent != null;
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
        roadModel.addObjectAt(this, origin);
    }
}
