import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

/**
 * Created by bavo en michiel.
 */
public class Task extends Parcel implements TickListener {

    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;

    private final Point origin;
    private CNPAgent agent = null;
    private TaskStation taskStation;
    private boolean isDelivered = false;
    private int hop = maximumNumberOfHops;
    private static final int maximumNumberOfHops = 10;
    private int failTime = 500000;

    public Task(Point origin, Point destination, long pickupDuration, TaskStation orderingTaskStation) {
        super(destination, pickupDuration, TimeWindow.ALWAYS, pickupDuration, TimeWindow.ALWAYS, 1);
        this.taskStation = orderingTaskStation;
        this.origin = origin;
        this.hop = maximumNumberOfHops;
    }

    public Point getPosition() {
        return this.getRoadModel().getPosition(this);
    }

    public Point getOrigin() {
        return this.origin;
    }

    public int getHops() {
        return maximumNumberOfHops - this.hop;
    }

    public CNPAgent getAgent() {
        return this.agent;
    }

    public TaskStation getTaskStation() {
        return this.taskStation;
    }

    public void pickUp(CNPAgent agent) { this.agent = agent;
    }

    public void drop() {
        this.isDelivered = true;
        this.agent = null;
        CNPCray.stopSim();
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

    @Override
    public void tick(TimeLapse timeLapse) {
        failTime -= 1;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        if (failTime <= 0){
            CNPCray.stats.dataUpdate(this.toString(), "count", "failed parcel", 1 );
            this.roadModel.get().unregister(this);
            this.pdpModel.get().unregister(this);
            CNPCray.stopSim();
        }
    }
}
