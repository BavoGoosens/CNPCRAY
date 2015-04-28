import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by bavo and michiel
 */
public class CNPAgent extends FIPACNP implements TickListener, MovingRoadUser {

    private final RandomGenerator rng;
    private Optional<GraphRoadModel> roadModel;
    private Optional<Point> destination;
    private Queue<Point> path;

    CNPAgent(RandomGenerator r) {
        rng = r;
        roadModel = Optional.absent();
        destination = Optional.absent();
        path = new LinkedList<>();
    }

    @Override
    public void initRoadUser(RoadModel model) {
        roadModel = Optional.of((GraphRoadModel) model);

        Point p;
        /*while (roadModel.get().isOccupied(p = model.getRandomPosition(rng))) {}*/
        p = model.getRandomPosition(rng);
        roadModel.get().addObjectAt(this, p);
    }

    @Override
    public double getSpeed() {
        return 1;
    }

    void nextDestination() {
        destination = Optional.of(roadModel.get().getRandomPosition(rng));
        path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
                destination.get()));
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (!destination.isPresent()) {
            nextDestination();
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(destination.get())) {
            nextDestination();
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

}