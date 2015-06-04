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
public class StandardCNPAgent extends CNPAgent {

    private final static int workersNeeded = 1;

    StandardCNPAgent(String name, RandomGenerator r) {
        this(name, r, fullEnergy);
    }

    StandardCNPAgent(String name, RandomGenerator r, long energy) {
        super(name, r, energy);
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
            interpretMessage(m, timeLapse.getTime());
        }

        if (this.isTaskManager()) {
            if (this.isWaitingForProposals() && timeLapse.getTime() - this.proposalGiven >= proposalTimeOut) {
                if (this.proposals.isEmpty()) {
                    this.possibleWorkers.clear();
                    this.proposalGiven = 0;
                    System.out.println(this.toString()+": No proposals returned. Search for new workers. ("+timeLapse.getTime()+")");
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
                    this.send(TaskMessageContents.TaskMessage.WORKER_ASSIGNED, this.taskManagerTask.get(), bestAgent);
                    this.possibleWorkers.remove(bestAgent);
                    for (CNPAgent otherWorker: this.possibleWorkers) {
                        this.send(TaskMessageContents.TaskMessage.WORKER_DECLINED, otherWorker);
                    }
                    System.out.println(this.toString()+": "+bestAgent.toString()+" assigned as worker.");
                    this.possibleWorkers.clear();
                    this.proposals.clear();
                    this.proposalGiven = 0;
                    this.taskManagerTask = Optional.absent();
                }
            }
            if (this.possibleWorkers.size() < workersNeeded) {
                this.retransmission--;
                if (this.retransmission < 0) {
                    this.retransmission = 100;
                    this.broadcast(TaskMessageContents.TaskMessage.WORKER_NEEDED);
                }
            }
        }
    }

    @Override
    protected int workersNeeded() {
        return StandardCNPAgent.workersNeeded;
    }


    @Override
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

    @Override
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

    @Override
    public void declareTaskManager(Task task) {
        if (!this.canBeTaskManager()) throw new IllegalStateException("Agent cannot be task manager at this moment. " +
                "Try with another agent or try again later.");
        this.taskManagerTask = Optional.of(task);
    }

    @Override
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
}