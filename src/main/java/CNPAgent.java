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
    protected long assignedTaskManager = 0;
    protected long startedWaitingForTaskManagerTask = 0;

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
    protected final static long taskManagerTimeOut = 20000000;
    protected final static long taskManagerTaskTimeOut = 1001;


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
            interpretMessage(m, timeLapse.getTime());
        }

        if (this.isTaskManager()) {
            if (this.isWaitingForProposals() && timeLapse.getTime() - this.proposalGiven >= proposalTimeOut) {
                if (this.proposals.isEmpty()) {
                    this.possibleWorkers.clear();
                    this.proposalGiven = 0;
                    System.out.println(this.toString()+": No proposals returned. Search for new workers.");
                } else if (this.startedWaitingForTaskManagerTask > 0 && timeLapse.getTime() - this.startedWaitingForTaskManagerTask >= taskManagerTaskTimeOut) {
                    System.out.println(this.toString()+": No task manager task returned, start over...");
                    this.startedWaitingForTaskManagerTask = 0;
                } else {
                    double bestProposal = Double.MAX_VALUE;
                    CNPAgent bestAgent = null;
                    for (CNPAgent agent: this.proposals.keySet()) {
                        double proposal = this.proposals.get(agent);
                        if (proposal < bestProposal) {
                            bestProposal = proposal;
                            bestAgent = agent;
                        }
                    }
                    if (bestAgent.isTaskManager()) {
                        this.send(TaskMessageContents.TaskMessage.GIVE_TASK_MANAGER_TASK, bestAgent);
                        System.out.println(this.toString()+": "+bestAgent.toString()+" is assigned as worker (chosen out of " + this.proposals.size() + " proposals), " +
                                "but wait for task manager task first.");
                        this.startedWaitingForTaskManagerTask = timeLapse.getTime();
                    } else {
                        this.send(TaskMessageContents.TaskMessage.WORKER_ASSIGNED, this.taskManagerTask.get(), bestAgent);
                        System.out.println(this.toString() + ": " + bestAgent.toString() + " assigned as worker (chosen out of " + this.proposals.size() + " proposals).");
                        this.taskManagerTask = Optional.absent();
                    }
                    this.possibleWorkers.remove(bestAgent);
                    for (CNPAgent otherWorker: this.possibleWorkers) {
                        this.send(TaskMessageContents.TaskMessage.WORKER_DECLINED, otherWorker);
                    }
                    this.possibleWorkers.clear();
                    this.proposals.clear();
                    this.proposalGiven = 0;
                }
            }
            if (this.possibleWorkers.size() < workersNeeded && !this.isFollowingATaskManager()) {
                this.retransmission--;
                if (this.retransmission < 0) {
                    this.retransmission = 100;
                    this.broadcast(TaskMessageContents.TaskMessage.WORKER_NEEDED);
                }
            }
        }
    }

    protected void move(TimeLapse timeLapse) {
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
                    System.out.println("Agent at position "+this.getPosition().get()+" tried to pickup task "+
                            this.carryingTask.get().toString()+" at position "
                            +this.carryingTask.get().getPosition()+" but it's not located there.");
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

    protected void setNextDestination( Point destiny) {
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

    public void declareTaskManager(Task task, long time) {
        if (!this.canBeTaskManager()) throw new IllegalStateException("Agent cannot be task manager at this moment. " +
                "Try with another agent or try again later.");
        this.taskManagerTask = Optional.of(task);
        this.assignedTaskManager = time;
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

    protected boolean canAcceptNewTasks(long time) {
        return (
                    this.charging < 0 && !this.assignedTask.isPresent() &&
                    !this.carryingTask.isPresent() && !this.batteryStation.isPresent() &&
                    !this.taskManagerTask.isPresent() && !this.followAgent.isPresent()
                )
                                        ||
                (
                        this.isTaskManager() &&
                        time - this.assignedTaskManager >= taskManagerTimeOut &&
                        !this.followAgent.isPresent()
                );
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
        double manhattan = sqrt(pow((this.getPosition().get().x - p.x), 2) + pow((this.getPosition().get().y - p.y), 2));
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
            if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_NEEDED) && this.canAcceptNewTasks(time)) {
                if (this.isTaskManager()) {
                    System.out.println(this.toString() + ": I don't want to be a task manager anymore. Bid for worker.");
                }
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
                    System.out.println(this.toString() + ": Proposals needed from possible workers.");
                    this.proposalGiven = time;
                }
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.GIVE_PROPOSAL) && this.canBeWorkerFor(cnpAgent)) {
                TaskMessageContents contents = (TaskMessageContents) m.getContents();
                double proposal = 0;
                try {
                    proposal = calculateProposal(contents.getTask().getPosition());
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }

                this.send(TaskMessageContents.TaskMessage.PROPOSAL, proposal, cnpAgent);
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
                this.taskManagerTask = Optional.absent();
                for (CNPAgent possibleWorker: this.possibleWorkers) {
                    this.send(TaskMessageContents.TaskMessage.WORKER_DECLINED, possibleWorker);
                }
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.WORKER_DECLINED)) {
                this.followAgent = Optional.absent();
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.GIVE_TASK_MANAGER_TASK) &&
                    this.isTaskManager() && this.canBeWorkerFor(cnpAgent)) {
                this.send(TaskMessageContents.TaskMessage.TASK_MANAGER_TASK, this.taskManagerTask.get(), cnpAgent);
            } else if (m.getContents().equals(TaskMessageContents.TaskMessage.TASK_MANAGER_TASK) && cnpAgent.isTaskManager() && this.isTaskManager()) {
                System.out.println(this.toString()+": Task from "+cnpAgent.toString()+" received. Can now be assigned as worker. I take over his task.");
                TaskMessageContents contents = (TaskMessageContents) m.getContents();
                this.send(TaskMessageContents.TaskMessage.WORKER_ASSIGNED, this.taskManagerTask.get(), cnpAgent);
                this.taskManagerTask = Optional.of(contents.getTask());
            }
        }
    }

    protected abstract int workersNeeded();

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

    protected boolean inRange(Point p) {
        Point position = this.getPosition().get();
        double distance = sqrt(pow(position.x - p.x, 2) + pow(position.y - p.y, 2));
        return distance < this.range;
    }

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

    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof CNPAgent)) return false;
        CNPAgent agent = (CNPAgent) otherObject;
        return agent.toString().equals(this.toString());
    }


}