import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.*;
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

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

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
    private final String name;

    private long lastReceiveTime = 0;
    private Optional<BatteryStation> batteryStation = Optional.absent();
    private Optional<TaskStation> taskStation = Optional.absent();
    private Optional<Point> destinationAfterCharging = Optional.absent();
    private Optional<Task> assignedTask = Optional.absent();
    private Optional<Task> carryingTask = Optional.absent();
    private Optional<Task> taskManagerTask = Optional.absent();
    private Optional<CNPAgent> followAgent = Optional.absent();

    private List<CNPAgent> possibleWorkers = new ArrayList<>();

    private long energy;
    private long charging = -1;
    private long energyBeforeCharging = 0;
    private long energyToCharge = 0;

    private int retransmission = 0;

    private final static double speed = 50.0D;
    private final static long moveCost = 3;
    private final static long sendCost = 0;
    private final static long broadcastCost = 0;
    private final static long fullEnergy = 30000;
    private final static int chargingFactor = 100;
    private final static int workersNeeded = 3;

    CNPAgent(String name, RandomGenerator r) {
        this(name, r, fullEnergy);
    }

    CNPAgent(String name, RandomGenerator r, long energy) {
        this.name = name;
        this.rng = r;
        this.energy = energy;
        this.roadModel = Optional.absent();
        this.destination = Optional.absent();
        this.device = Optional.absent();
        this.path = new LinkedList<>();

        this.range = this.rng.nextDouble();
        this.reliability = 1;
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
            // if charging: wait and do nothing
            this.charging--;
        } else if (this.energy - moveCost >= 0) {
            // if enough energy: move to destination
            this.move(timeLapse);
            this.decreaseEnergyWith(moveCost);
        }

        for (Message m: this.device.get().getUnreadMessages()) {
            interpretMessage(m);
        }

        if (this.taskManagerTask.isPresent() && this.possibleWorkers.size() < workersNeeded) {
            this.retransmission--;
            if (this.retransmission < 0) {
                this.retransmission = 100;
                this.broadcast(TaskMessageContents.TaskMessage.WORKER_NEEDED);
            }
        }
    }

    private void interpretMessage(Message m) {
        CommUser sender = m.getSender();
        if (sender instanceof TaskStation) {
            TaskStation taskStation = (TaskStation) sender;
            if (m.getContents().equals(TaskMessageContents.TaskMessage.TASK_MANAGER_NEEDED) && this.canBeTaskManager()) {
                this.setNextDestination(taskStation.getPosition().get());
                this.taskStation = Optional.of(taskStation);
            }
        } else if (sender instanceof CNPAgent) {
            CNPAgent cnpAgent = (CNPAgent) sender;
            if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_NEEDED) && this.canAcceptNewTasks()) {
                this.send(TaskMessageContents.TaskMessage.WANT_TO_BE_WORKER, sender);
                this.followAgent = Optional.of(cnpAgent);
                this.destination = Optional.absent();
                this.taskStation = Optional.absent();
            } else if (m.getContents().equals((TaskMessageContents.TaskMessage.WANT_TO_BE_WORKER)) && this.isTaskManager()) {
                this.possibleWorkers.add(cnpAgent);
                if (this.possibleWorkers.size() >= workersNeeded) {
                    CNPAgent worker = this.possibleWorkers.get(0);
                    this.send(TaskMessageContents.TaskMessage.WORKER_ASSIGNED, this.taskManagerTask.get(), worker);
                    this.possibleWorkers.remove(0);
                    for (CNPAgent otherWorker: this.possibleWorkers) {
                        this.send(TaskMessageContents.TaskMessage.WORKER_DECLINED, otherWorker);
                    }
                    this.possibleWorkers.clear();
                    this.taskManagerTask = Optional.absent();
                }
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.LEAVING) && this.isTaskManager()) {
                this.possibleWorkers.remove(cnpAgent);
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_ASSIGNED) && this.canBeWorkerFor(cnpAgent)) {
                TaskMessageContents contents = (TaskMessageContents) m.getContents();
                this.assignTask(contents.getTask());
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_DECLINED)) {
                this.followAgent = Optional.absent();
            }
        }
    }

    public boolean isExecutingTask() {
        return this.carryingTask.isPresent() || this.assignedTask.isPresent();
    }

    public boolean isGoingToTaskStation() {
        return this.taskStation.isPresent();
    }

    public boolean isGoingToRecharge() {
        return this.batteryStation.isPresent();
    }

    public boolean isFollowingATaskManager() {
        return this.followAgent.isPresent();
    }

    private boolean canAcceptNewTasks() {
        return this.charging < 0 && !this.assignedTask.isPresent()
                && !this.carryingTask.isPresent() && !this.batteryStation.isPresent()
                && !this.taskManagerTask.isPresent() && !this.followAgent.isPresent();
    }

    private boolean canBeTaskManager() {
        return this.charging < 0 && !this.taskStation.isPresent() && !this.assignedTask.isPresent()
                && !this.carryingTask.isPresent()
                && !this.taskManagerTask.isPresent() && !this.followAgent.isPresent();
    }

    private boolean canBeWorkerFor(CNPAgent agent) {
        return this.followAgent.isPresent() && this.followAgent.get().equals(agent);
    }

    public boolean isTaskManager() {
        return this.taskManagerTask.isPresent();
    }

    public int getNumberOfPossibleWorkers() {
        return this.possibleWorkers.size();
    }

    public long getEnergyPercentage() {
        if (this.charging >= 0) {
            long alreadyCharged = this.energyToCharge - this.charging*chargingFactor;
            return Math.round(((double) (this.energyBeforeCharging+alreadyCharged) / fullEnergy)*100);
        }
        return Math.round(((double) this.energy / fullEnergy) * 100);
    }

    public Optional<Point> getDestination() {
        return this.destination;
    }

    private double calculateProposal(Point p){
        double manhattan = sqrt(pow((this.getPosition().get().x - p.x),2) + pow((this.getPosition().get().y - p.y), 2));
        double energyCost = (this.roadModel.get().getShortestPathTo(this, p).size() - 1) * moveCost * 72;
        double chargeChance = (100 - this.getEnergyPercentage()) + 0.000000001234;
        return (manhattan + energyCost) * chargeChance;
    }

    private void move(TimeLapse timeLapse) {
        if (this.followAgent.isPresent()) {
            this.path = new LinkedList<>(
                    this.roadModel.get().getShortestPathTo(
                            this, this.followAgent.get().getPosition().get()
                    )
            );
            if (this.getEnergyPercentage() < 30) {
                this.send(TaskMessageContents.TaskMessage.LEAVING, this.followAgent.get());
                this.followAgent = Optional.absent();
            }
        } else if (!destination.isPresent()) {
            // geen destination present random rondbewegen.
            this.setNextDestination(null);
        } else if (this.taskStation.isPresent()) {
            // Agent is naar taskstation aan het bewegen en komt in range dus gaat offer maken.
            if (this.inRange(this.taskStation.get().getPosition().get())) {
                this.send(TaskMessageContents.TaskMessage.WANT_TO_BE_TASK_MANAGER, this.taskStation.get());
                this.taskStation = Optional.absent();
                this.setNextDestination(null);
            }
        }

        roadModel.get().followPath(this, path, timeLapse);

        if (this.destination.isPresent() && roadModel.get().getPosition(this).equals(destination.get())) {
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
                try {
                    this.pdpModel.get().pickup(this, this.carryingTask.get(), timeLapse);
                } catch (IllegalArgumentException e) {
/*                    System.out.println("Agent at position "+this.getPosition().get()+" tried to pickup task "+
                            this.carryingTask.get().toString()+" at position "
                            +this.carryingTask.get().getPosition()+" but it's not located there.");*/
                    throw e;
                }
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
        if (destiny != null) {
            //System.out.println("Now going to the destination: " + destiny);
            this.destination = Optional.of(destiny);
            this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this,
                    destiny));
        }else {
            if (this.destinationAfterCharging.isPresent()) {
                //System.out.println("Now going to the destination: " + this.destinationAfterCharging.get());
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
        //System.out.println("New assignment: " + destination.get());
        //System.out.println("Battery: " + this.getEnergyPercentage() + "%");
        long energyNeeded = (this.path.size() - 1) * moveCost * 72;
        long energyAfterJob = this.energy - energyNeeded;
        //System.out.println("Battery after assignment: " + Math.round((((double) energyAfterJob / fullEnergy) * 100)) + "%");
        if (energyAfterJob < 4000) {
            //System.out.println("Charge battery first");
            this.destinationAfterCharging = this.destination;
            this.batteryStation = Optional.of(((CNPRoadModel) this.roadModel.get()).getNearestBatteryStation(this.getPosition().get()));
            this.destination = Optional.of(this.batteryStation.get().getPosition().get());
            this.path = new LinkedList<>(this.roadModel.get().getShortestPathTo(this, this.destination.get()));
        }
    }

    private boolean inRange(Point p) {
        Point position = this.getPosition().get();
        double distance = sqrt(pow(position.x - p.x, 2) + pow(position.y - p.y, 2));
        return distance < range*2;
    }

    public void declareTaskManager(Task task) {
        if (!this.canBeTaskManager()) throw new IllegalStateException("Agent cannot be task manager at this moment. " +
                "Try with another agent or try again later.");
        this.taskManagerTask = Optional.of(task);
    }

    public void assignTask(Task task) {
        if (this.followAgent.isPresent()) {
            this.assignedTask = Optional.of(task);
            /*
            Wanneer de robot op weg is naar een taskstation moet hij dit stoppen
            en de pickup-task opnemen.
             */
            this.taskStation = Optional.absent();
            this.followAgent = Optional.absent();
            this.setNextDestination(task.getPosition());
        } else
            throw new IllegalStateException("WHAT NU WEER");
    }

    private void send(TaskMessageContents contents, CommUser recipient) {
        this.device.get().send(contents, recipient);
    }

    private void send(TaskMessageContents.TaskMessage message, CommUser recipient) {
        if (this.getEnergy() - sendCost >= 0) {
            TaskMessageContents contents = new TaskMessageContents(message);
            this.send(contents, recipient);
            this.decreaseEnergyWith(sendCost);
        }
    }

    private void send(TaskMessageContents.TaskMessage message, Task task, CommUser recipient) {
        TaskMessageContents contents = new TaskMessageContents(message, task);
        this.send(contents, recipient);
    }

    private void broadcast(TaskMessageContents contents) {
        if (this.getEnergy() - broadcastCost >= 0) {
            this.device.get().broadcast(contents);
            this.decreaseEnergyWith(broadcastCost);
        }
    }

    private void broadcast(TaskMessageContents.TaskMessage message) {
        TaskMessageContents contents = new TaskMessageContents(message);
        this.broadcast(contents);
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

    @Override
    public String toString() {
        return this.name;
    }


}