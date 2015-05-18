import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import javax.swing.text.Position;

/**
 * Created by bavo and michiel.
 */
public class BatteryStation implements CommUser, RoadUser{

    private Optional<GraphRoadModel> roadModel;
    private final RandomGenerator rng;
    private Optional<CommDevice> device;
    private final double range;
    private final double reliability;
    private final Point position;

    public  BatteryStation(RandomGenerator rng, Point p){
        this.position = p;
        this.rng = rng;
        roadModel = Optional.absent();
        device = Optional.absent();
        range = rng.nextDouble();
        reliability = rng.nextDouble();
    }

    @Override
    public Optional<Point> getPosition() {
        Point p = this.roadModel.get().getPosition(this);
        return Optional.of(p);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(6);
        device = Optional.of(commDeviceBuilder
                .setReliability(0.3)
                .build());
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {
        this.roadModel = Optional.of((GraphRoadModel) roadModel);
        Point p;
        /*while (roadModel.get().isOccupied(p = model.getRandomPosition(rng))) {}*/
        p = roadModel.getRandomPosition(rng);
        this.roadModel.get().addObjectAt(this, this.position);
    }
}
