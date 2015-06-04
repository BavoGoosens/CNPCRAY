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
    private TaskStation taskStation;
    private boolean isDelivered = false;
    private int hop;

    public Task(Point origin, Point destination, long pickupDuration, TaskStation orderingTaskStation) {
        super(destination, pickupDuration, TimeWindow.ALWAYS, pickupDuration, TimeWindow.ALWAYS, 1);
        this.taskStation = orderingTaskStation;
        this.origin = origin;
    }

    public Point getPosition() {
        return this.getRoadModel().getPosition(this);
    }

    public Point getOrigin() {
        return this.origin;
    }

    public CNPAgent getAgent() {
        return this.agent;
    }

    public TaskStation getTaskStation() {
        return this.taskStation;
    }

    public void pickUp(CNPAgent agent) {
        this.agent = agent;
    }

    public void drop() {
        this.isDelivered = true;
        this.agent = null;
    }
    
    public void hop(){
        this.hop -= 1; 
    }
    
    public boolean canHop(){
        return this.hop >= 0;
    }

    public boolean hasBeenAssigned() {
        return this.agent != null;
    }

    public boolean exists() {
        try {
            this.getRoadModel().getPosition(this);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
        this.roadModel = Optional.of(roadModel);
        this.pdpModel = Optional.of(pdpModel);
        roadModel.addObjectAt(this, origin);
    }
}
