import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by bavo and michiel
 */
public abstract class CNPAgent extends Vehicle implements CommUser {

    protected Optional<RoadModel> roadModel;
    protected Optional<PDPModel> pdpModel;
    protected Optional<CommDevice> device;
    protected Optional<Point> destination;
    protected Queue<Point> path;
    protected CommDeviceBuilder commDeviceBuilder;

    protected final double range;
    protected final double reliability;
    protected final RandomGenerator rng;
    protected final String name;

    protected long lastReceiveTime = 0;
    protected Optional<BatteryStation> batteryStation = Optional.absent();
    protected Optional<TaskStation> taskStation = Optional.absent();
    protected Optional<Point> destinationAfterCharging = Optional.absent();
    protected Optional<Task> assignedTask = Optional.absent();
    protected Optional<Task> carryingTask = Optional.absent();
    protected Optional<Task> taskManagerTask = Optional.absent();
    protected Optional<CNPAgent> followAgent = Optional.absent();
    protected long proposalGiven = 0;

    protected List<CNPAgent> possibleWorkers = new ArrayList<>();
    protected Map<CNPAgent, Double> proposals = new HashMap<>();

    protected long energy;
    protected long charging = -1;
    protected long energyBeforeCharging = 0;
    protected long energyToCharge = 0;

    protected int retransmission = 0;

    protected final static double speed = 50.0D;
    protected final static long moveCost = 3;
    protected final static long sendCost = 0;
    protected final static long broadcastCost = 0;
    protected final static long fullEnergy = 30000;
    protected final static int chargingFactor = 100;
    protected final static int workersNeeded = 3;
    protected final static long proposalTimeOut = 1001;


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

        this.range = 2;
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
    public abstract void tickImpl(TimeLapse timeLapse);


    public boolean isWaitingForProposals() {
        return this.proposalGiven > 0;
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

    protected boolean canAcceptNewTasks() {
        return this.charging < 0 && !this.assignedTask.isPresent()
                && !this.carryingTask.isPresent() && !this.batteryStation.isPresent()
                && !this.taskManagerTask.isPresent() && !this.followAgent.isPresent();
    }

    protected boolean canBeTaskManager() {
        return this.charging < 0 && !this.taskStation.isPresent() && !this.assignedTask.isPresent()
                && !this.carryingTask.isPresent()
                && !this.taskManagerTask.isPresent() && !this.followAgent.isPresent();
    }

    protected boolean canBeWorkerFor(CNPAgent agent) {
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

    protected double calculateProposal(Point p){
        double manhattan = sqrt(pow((this.getPosition().get().x - p.x),2) + pow((this.getPosition().get().y - p.y), 2));
        double energyCost = (this.roadModel.get().getShortestPathTo(this, p).size() - 1) * moveCost * 72;
        double chargeChance = (100 - this.getEnergyPercentage()) + 0.000000001234;
        return (manhattan + energyCost) * chargeChance;
    }

    protected void interpretMessage(Message m, long time) {
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
                if (this.possibleWorkers.size() >= this.workersNeeded() && !this.isWaitingForProposals()) {
                    for (CNPAgent worker: this.possibleWorkers) {
                        this.send(TaskMessageContents.TaskMessage.GIVE_PROPOSAL, this.taskManagerTask.get(), worker);
                    }
                    System.out.println(this.toString() + ": Proposals needed from possible workers. (" + time + ")");
                    this.proposalGiven = time;
                    /*CNPAgent worker = this.possibleWorkers.get(0);
                    this.send(TaskMessageContents.TaskMessage.WORKER_ASSIGNED, this.taskManagerTask.get(), worker);
                    this.possibleWorkers.remove(0);
                    for (CNPAgent otherWorker: this.possibleWorkers) {
                        this.send(TaskMessageContents.TaskMessage.WORKER_DECLINED, otherWorker);
                    }
                    this.possibleWorkers.clear();
                    this.taskManagerTask = Optional.absent();*/
                }
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.GIVE_PROPOSAL) && this.canBeWorkerFor(cnpAgent)) {
                TaskMessageContents contents = (TaskMessageContents) m.getContents();
                double proposal = calculateProposal(contents.getTask().getPosition());
                this.send(TaskMessageContents.TaskMessage.PROPOSAL, proposal, cnpAgent);
                System.out.println(this.toString() + ": Proposal for " + cnpAgent.toString() + ": " + proposal);
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.PROPOSAL) && this.isTaskManager()) {
                TaskMessageContents contents = (TaskMessageContents) m.getContents();
                double proposal = contents.getProposal();
                this.proposals.put(cnpAgent, proposal);
            }
            else if (m.getContents().equals(TaskMessageContents.TaskMessage.LEAVING) && this.isTaskManager()) {
                this.possibleWorkers.remove(cnpAgent);
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_ASSIGNED) && this.canBeWorkerFor(cnpAgent)) {
                TaskMessageContents contents = (TaskMessageContents) m.getContents();
                this.assignTask(contents.getTask());
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_DECLINED)) {
                this.followAgent = Optional.absent();
            }
        }
    }

    protected abstract int workersNeeded();

    protected abstract void move(TimeLapse timeLapse);

    protected void decreaseEnergyWith(long energyDecrease) {
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

    protected abstract void setNextDestination( Point destiny);

    protected boolean inRange(Point p) {
        Point position = this.getPosition().get();
        double distance = sqrt(pow(position.x - p.x, 2) + pow(position.y - p.y, 2));
        return distance < this.range;
    }

    public abstract void declareTaskManager(Task task);

    public abstract void assignTask(Task task);

    protected void send(TaskMessageContents contents, CommUser recipient) {
        this.device.get().send(contents, recipient);
    }

    protected void send(TaskMessageContents.TaskMessage message, CommUser recipient) {
        if (this.getEnergy() - sendCost >= 0) {
            TaskMessageContents contents = new TaskMessageContents(message);
            this.send(contents, recipient);
            this.decreaseEnergyWith(sendCost);
        }
    }

    protected void send(TaskMessageContents.TaskMessage message, Task task, CommUser recipient) {
        TaskMessageContents contents = new TaskMessageContents(message, task);
        this.send(contents, recipient);
    }

    protected void send(TaskMessageContents.TaskMessage message, double proposal, CommUser recipient) {
        TaskMessageContents contents = new TaskMessageContents(message, proposal);
        this.send(contents, recipient);
    }

    protected void broadcast(TaskMessageContents contents) {
        if (this.getEnergy() - broadcastCost >= 0) {
            this.device.get().broadcast(contents);
            this.decreaseEnergyWith(broadcastCost);
        }
    }

    protected void broadcast(TaskMessageContents.TaskMessage message) {
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
        this.commDeviceBuilder = commDeviceBuilder;
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