import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
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
public class CNPAgent extends FIPACNP implements TickListener, MovingRoadUser, CommUser {

    private Optional<GraphRoadModel> roadModel;
    private Optional<CommDevice> device;
    private Optional<Point> destination;
    private Queue<Point> path;
    private final double range;
    private final double reliability;
    private final RandomGenerator rng;
    private long lastReceiveTime = 0;
    private long energy;
    private final static long moveCost = 10;
    private final static long fullEnergy = 100000;

    CNPAgent(RandomGenerator r) {
        this(r, fullEnergy);
    }

    CNPAgent(RandomGenerator r, long energy) {
        rng = r;
        this.energy = energy;
        roadModel = Optional.absent();
        destination = Optional.absent();
        path = new LinkedList<>();
        device = Optional.absent();

        range = rng.nextDouble();
        reliability = rng.nextDouble();
    }

    public long getEnergy() {
        return this.energy;
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
        return 50.0D;
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (this.energy - moveCost >= 0) {
            this.move(timeLapse);
            this.decreaseEnergyWith(moveCost);
        }
    }

    private void move(TimeLapse timeLapse) {
        if (!destination.isPresent()) {
            this.setNextDestination();
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(destination.get())) {
            this.setNextDestination();
        }
    }

    private void decreaseEnergyWith(long energyDecrease) {
        if (this.energy - energyDecrease < 0) {
            throw new IllegalArgumentException("The agent does not have enough energy.");
        }
        this.energy -= energyDecrease;
    }

    public long loadFullBattery() {
        long energyIncrease = fullEnergy - this.energy;
        this.energy = fullEnergy;
        return energyIncrease;
    }

    private void setNextDestination() {
        destination = Optional.of(roadModel.get().getRandomPosition(rng));
        path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
                destination.get()));
        long energyNeeded = path.size()*moveCost*72;
        System.out.println("Energy needed: "+energyNeeded);
        System.out.println("Battery: "+energy);
        if (energyNeeded > this.energy) {
            System.out.println("Find a battery station...");
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }

    @Override
    public Optional<Point> getPosition() {
        Point p = this.roadModel.get().getPosition(this);
        return Optional.of(p);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        if (range >= 0) {
            commDeviceBuilder.setMaxRange(2);
        }
        device = Optional.of(commDeviceBuilder
                .setReliability(reliability)
                .build());
    }


}