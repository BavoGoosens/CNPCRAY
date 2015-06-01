import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
    private Optional<BatteryStation> batteryStation = Optional.absent();
    private Optional<TaskStation> taskStation = Optional.absent();
    private Optional<Point> destinationAfterCharging = Optional.absent();
    private Optional<Task> assignedTask = Optional.absent();
    private Optional<Task> carryingTask = Optional.absent();
    private Optional<Task> taskToBeAssigned = Optional.absent();
    private List<Task> completedTasks = new ArrayList<Task>();

    private long energy;
    private long charging = -1;
    private long energyBeforeCharging = 0;
    private long energyToCharge = 0;

    private final static double speed = 50.0D;
    private final static long moveCost = 3;
    private final static long fullEnergy = 30000;
    private final static int chargingFactor = 100;

    CNPAgent(RandomGenerator r) {
        this(r, fullEnergy);
    }

    CNPAgent(RandomGenerator r, long energy) {
        this.rng = r;
        this.energy = energy;
        this.roadModel = Optional.absent();
        this.destination = Optional.absent();
        this.device = Optional.absent();
        this.path = new LinkedList<>();

        this.range = this.rng.nextDouble();
        this.reliability = this.rng.nextDouble();
        setCapacity(1);
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
        if (this.charging >= 0) {
            this.charging--;
        } else if (this.energy - moveCost >= 0) {
            this.move(timeLapse);
            this.decreaseEnergyWith(moveCost);
        }
        if (this.device.get().getUnreadCount() > 0){
            ImmutableList<Message> received = this.device.get().getUnreadMessages();
            Message m = received.get(0);
            CommUser sender = m.getSender();
            if (sender.getClass().equals(TaskStation.class)) {
                TaskStation task = (TaskStation) sender;
                Point taskpos = this.roadModel.get().getPosition(task);
                if (this.canAcceptNewTasks()) {
                    this.setNextDestination(taskpos);
                    this.taskStation = Optional.of(task);
                }
            }
        }
    }

    public boolean isExecutingTask() {
        return this.taskStation.isPresent() || this.carryingTask.isPresent() || this.assignedTask.isPresent();
    }

    public boolean isGoingToRecharge() {
        return this.batteryStation.isPresent();
    }

    private boolean canAcceptNewTasks() {
        return this.charging < 0 && !this.assignedTask.isPresent()
                && !this.carryingTask.isPresent() && !this.batteryStation.isPresent();
    }

    public double getEnergyPercentage() {
        if (this.charging >= 0) {
            long alreadyCharged = this.energyToCharge - this.charging*chargingFactor;
            return Math.round(((double) (this.energyBeforeCharging+alreadyCharged) / fullEnergy)*100);
        }
        return Math.round(((double) this.energy / fullEnergy)*100);
    }

    public Optional<Point> getDestination() {
        return this.destination;
    }

    private void move(TimeLapse timeLapse) {
        if (!destination.isPresent()) {
            // geen destination present random rondbewegen.
            this.setNextDestination(null);
        } else if (this.taskStation.isPresent()) {
            // Agent is naar taskstation aan het bewegen en komt in range dus gaat offer maken.
            if (this.inRange(this.taskStation.get().getPosition().get())) {
                this.device.get().send(TaskStation.TaskMessages.TASK_OFFER, this.taskStation.get());
                this.taskStation = Optional.absent();
                this.setNextDestination(null);
            }
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (roadModel.get().getPosition(this).equals(destination.get())) {
            // De destination is bereikt.
            if (this.batteryStation.isPresent()) {
                // if battery station
                this.energyBeforeCharging = this.energy;
                long energyLoaded = this.batteryStation.get().loadBattery(this);
                this.energyToCharge = energyLoaded;
                this.batteryStation = Optional.absent();
                this.charging = Math.round(energyLoaded / chargingFactor);
                this.setNextDestination(null);
            } else if (this.assignedTask.isPresent()) {
                // aangekomen op plaats naar een taal
                this.carryingTask = this.assignedTask;
                this.assignedTask = Optional.absent();
                this.pdpModel.get().pickup(this, this.carryingTask.get(), timeLapse);
                this.carryingTask.get().pickUp(this);
                this.setNextDestination(this.carryingTask.get().getDestination());
            } else if (this.carryingTask.isPresent()) {
                // aangekomen op eindbestemming
                this.pdpModel.get().deliver(this, this.carryingTask.get(), timeLapse);
                this.carryingTask.get().drop();
                this.carryingTask = Optional.absent();
                this.setNextDestination(null);
            } else {
                // geen taken ontvangen of echte goals gezet => zet op null om random verder te blijven bewegen.
                this.setNextDestination(null);
            }
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

    private void setNextDestination( Point destiny) {
        if (destiny != null){
            System.out.println("Now going to the destination: " + destiny);
            this.destination = Optional.of(destiny);
            this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this,
                    destiny));
        }else {
            if (this.destinationAfterCharging.isPresent()) {
                System.out.println("Now going to the destination: " + this.destinationAfterCharging.get());
                this.destination = this.destinationAfterCharging;
                this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this,
                        this.destination.get()));
                this.destinationAfterCharging = Optional.absent();
            } else {
                this.destination = Optional.of(this.roadModel.get().getRandomPosition(this.rng));
                this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this,
                        this.destination.get()));
            }
        }
        System.out.println("New assignment: " + destination.get());
        System.out.println("Battery: " + this.getEnergyPercentage() + "%");
        long energyNeeded = (this.path.size() - 1) * moveCost * 72;
        long energyAfterJob = this.energy - energyNeeded;
        System.out.println("Battery after assignment: " + Math.round((((double) energyAfterJob / fullEnergy) * 100)) + "%");
        if (energyAfterJob < 4000) {
            System.out.println("Charge battery first");
            this.destinationAfterCharging = this.destination;
            this.batteryStation = Optional.of(((CNPRoadModel) this.roadModel.get()).getNearestBatteryStation(this.getPosition().get()));
            this.destination = Optional.of(this.batteryStation.get().getPosition().get());
            this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this, this.destination.get()));
        }
    }

    private boolean inRange(Point p) {
        Point position = this.getPosition().get();
        double distance = Math.sqrt(Math.pow(position.x - p.x, 2) + Math.pow(position.y - p.y, 2));
        return distance < range*2;
    }

    public void declareTaskManager(Task task) {
        this.taskToBeAssigned = Optional.of(task);
        this.device.get().broadcast(TaskStation.TaskMessages.TASK_READY);
    }

    public void assignTask(Task task) {
        if (this.canAcceptNewTasks()) {
            this.assignedTask = Optional.of(task);
            this.setNextDestination(task.getPosition());
        } else
            throw new IllegalStateException("WHAT NU WEER");
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