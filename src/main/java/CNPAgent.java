import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
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
public class CNPAgent extends Vehicle implements CommUser {

    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpModel;
    private Optional<CommDevice> device;
    private Optional<Point> destination;
    private Queue<Point> path;

    private final double range;
    private final double reliability;
    private final RandomGenerator rng;

    private long lastReceiveTime = 0;
    private BatteryStation batteryStation;
    private Optional<Point> destinationAfterCharging = Optional.absent();
    private long energy;
    private int charging = -1;

    private final static double speed = 50.0D;
    private final static long moveCost = 5;
    private final static long fullEnergy = 30000;
    private final static int chargingTime = 1000;

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
    public void initRoadPDP(RoadModel roadModel, PDPModel pdpModel) {
        this.roadModel = Optional.of(roadModel);
        this.pdpModel = Optional.of(pdpModel);

        Point p;
        p = roadModel.getRandomPosition(rng);
        roadModel.addObjectAt(this, p);
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    @Override
    public void tickImpl(TimeLapse timeLapse) {
        if (this.charging > 0) {
            this.charging--;
        } else if (this.energy - moveCost >= 0) {
            this.move(timeLapse);
            this.decreaseEnergyWith(moveCost);
        }
    }

    public double getEnergyPercentage() {
        return Math.round(((double) this.energy / fullEnergy)*100);
    }

    public Optional<Point> getDestination() {
        return this.destination;
    }

    private void move(TimeLapse timeLapse) {
        if (!destination.isPresent()) {
            this.setNextDestination();
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(destination.get())) {
            if (this.batteryStation != null) {
                this.batteryStation.loadBattery(this);
                this.batteryStation = null;
                this.charging = chargingTime;
            }
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
        if (this.destinationAfterCharging.isPresent()) {
            System.out.println("Now going to the previous destination: "+this.destinationAfterCharging.get());
            this.destination = this.destinationAfterCharging;
            this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this,
                    this.destination.get()));
            this.destinationAfterCharging = Optional.absent();
        } else {
            this.destination = Optional.of(this.roadModel.get().getRandomPosition(this.rng));
            this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this,
                    this.destination.get()));
            System.out.println("New assignment: "+destination.get());
            System.out.println("Battery: "+this.getEnergyPercentage()+"%");
            long energyNeeded = (this.path.size()-1)*moveCost*72;
            long energyAfterJob = this.energy - energyNeeded;
            System.out.println("Battery after assignment: "+Math.round((((double) energyAfterJob / fullEnergy)*100))+"%");
            if (energyAfterJob < 6000) {
                System.out.println("Charge battery first");
                this.destinationAfterCharging = this.destination;
                this.batteryStation = ((CNPRoadModel) this.roadModel.get()).getNearestBatteryStation(this.getPosition().get());
                this.destination = Optional.of(this.batteryStation.getPosition().get());
                this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this, this.destination.get()));
            }
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