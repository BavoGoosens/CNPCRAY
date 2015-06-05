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
public class PropagateCNPAgent extends CNPAgent {

    private static final int workersNeeded = 0;
    private long proposalInitiated = 0;

    private static final long proposalTimeOut = 6001;

    PropagateCNPAgent(String name, RandomGenerator r) {
        this(name, r, fullEnergy);
    }

    PropagateCNPAgent(String name, RandomGenerator r, long energy) {
        super(name, r, energy);
    }

    @Override
    protected int workersNeeded() {
        return workersNeeded;
    }

    @Override
    public void tickImpl(TimeLapse timeLapse) {
        for (Message m: this.device.get().getUnreadMessages()) {
            interpretMessage(m, timeLapse.getTime());
        }
        if (this.charging >= 0) {
            // if charging: wait and do nothing
            this.charging--;
        } else if (this.energy - moveCost >= 0) {
            // if enough energy: move to destination
            this.move(timeLapse);
            this.decreaseEnergyWith(moveCost);

        }
        if (timeLapse.getTime() - this.proposalInitiated >= proposalTimeOut) {
            this.proposalInitiated = 0;
        }
        if (this.isTaskManager())
            this.taskManagerTick(timeLapse);
    }

    @Override
    protected void taskManagerTick(TimeLapse timeLapse) {
        if (!this.taskManagerTask.get().exists()) {
            this.taskManagerTask = Optional.absent();
        } else {
            this.retransmit();
        }
    }

    @Override
    protected void retransmit() {
        if (this.taskManagerTask.get().canHop()) {
            this.retransmission--;
            if (this.retransmission < 0) {
                this.retransmission = 100;
                this.broadcast(TaskMessageContents.TaskMessage.GIVE_PROPOSAL, this.taskManagerTask.get());
                CNPCray.stats.dataUpdate(this.name, "communication", "give_proposal retransmit", 1);
                //System.out.println(this.toString()+": Transmitting...");
            }
        } else {
            System.out.println(this.toString() + ": Maximum number of hops reached.");
        }
    }

    @Override
    protected boolean canAcceptNewTasks(long time) {
        return this.charging < 0 &&
                !this.carryingTask.isPresent() && !this.batteryStation.isPresent() && !this.taskManagerTask.isPresent()
                && this.proposalInitiated <= 0; // als de proposalInitiated groter is dan 0, dan wil het zeggen
                                                // dat hij nog aan het onderhandelen kan zijn
    }

    @Override
    protected void interpretMessage(Message m, long time) {
        CommUser sender = m.getSender();
        if (sender instanceof TaskStation) {
            TaskStation taskStation = (TaskStation) sender;
            if (m.getContents().equals(TaskMessageContents.TaskMessage.TASK_MANAGER_NEEDED) && canBeTaskManager()) {
                this.setNextDestination(taskStation.getPosition().get());
                this.taskStation = Optional.of(taskStation);
            }
        } else if (sender instanceof CNPAgent) {
            CNPAgent cnpAgent = (CNPAgent) sender;
            TaskMessageContents content = (TaskMessageContents) m.getContents();
            if (content.equals(TaskMessageContents.TaskMessage.GIVE_PROPOSAL) && canAcceptNewTasks(time)) {
                if (!content.getTask().hasBeenAssigned()) {
                    // dit kan gebeuren als de manager heeft gebroadcast en
                    // vlak daarna de taak toch zelf heeft opgepikt
                    // give proposal zal ontvangen worden, maar is niet meer relevant
                    double proposal = Double.MAX_VALUE;
                    try {
                        proposal = calculateProposal(content.getTask().getPosition());
                    } catch (IllegalArgumentException e) {
                        throw e;
                    }
                    this.send(TaskMessageContents.TaskMessage.PROPOSAL, proposal, cnpAgent);
                    CNPCray.stats.dataUpdate(this.name, "communication", "proposal", 1);
                    System.out.println(this.toString() + ": My proposal for " + cnpAgent.toString() + ": " + proposal);
                    this.proposalInitiated = time;
                }
            } else if (content.equals(TaskMessageContents.TaskMessage.PROPOSAL) && isTaskManager()) {
                double proposal = content.getProposal();
                if (proposal < this.calculateProposal(this.taskManagerTask.get().getPosition())) {
                    this.send(TaskMessageContents.TaskMessage.TASK_MANAGER_ASSIGNED, this.taskManagerTask.get(), cnpAgent);
                    this.taskManagerTask.get().hop();
                    CNPCray.stats.dataUpdate(this.name, "count", "passed along", 1);
                    CNPCray.stats.dataUpdate(this.taskManagerTask.get().toString(), "timing", "hop", time);
                    this.taskManagerTask = Optional.absent();
                    System.out.println(this.toString()+": Proposal from "+cnpAgent.toString()+" is better. Propagate task.");
                } else {
                    CNPCray.stats.dataUpdate(this.name, "count", "i am better", 1);
                    System.out.println(this.toString()+": Proposal from "+cnpAgent.toString()+" is not better. Do not propagate task.");
                }
            } else if (content.equals(TaskMessageContents.TaskMessage.TASK_MANAGER_ASSIGNED)) {
                this.proposalInitiated = 0;
                this.batteryStation = Optional.absent();
                this.declareTaskManager(content.getTask(), time);
                System.out.println(this.toString() + ": Task propagation succeeded. Task received from " + cnpAgent.toString());
            }
        }
    }

    @Override
    protected void destinationReached(TimeLapse timeLapse) {
        if (this.batteryStation.isPresent()) {
            // if battery station
            this.energyBeforeCharging = this.energy;
            try {
                long energyLoaded = this.batteryStation.get().loadBattery(this);
                this.energyToCharge = energyLoaded;
                this.batteryStation = Optional.absent();
                this.charging = Math.round(energyLoaded / chargingFactor);
                this.setNextDestination(null);
            } catch (IllegalArgumentException e) {
                System.out.println("The Mask");
                this.setNextDestination(this.batteryStation.get().getPosition().get());
                throw e;
            }
        } else if (this.taskManagerTask.isPresent()) {
            // aangekomen op plaats naar een taak
            this.carryingTask = this.taskManagerTask;
            this.taskManagerTask = Optional.absent();
            try {
                this.pdpModel.get().pickup(this, this.carryingTask.get(), timeLapse);
            } catch (IllegalArgumentException e) {
                System.out.println(this.toString()+" at position "+this.getPosition().get()+" tried to pickup task "+
                        this.carryingTask.get().toString()+" at position "
                        +this.carryingTask.get().getPosition()+" but it's not located there.");
                throw e;
            }
            this.carryingTask.get().pickUp(this);
            CNPCray.stats.dataUpdate(this.carryingTask.get().toString(), "timing", "pick up", timeLapse.getTime());
            CNPCray.stats.dataUpdate(this.carryingTask.get().toString(), "count", "hops", this.carryingTask.get().getHops());
            System.out.println(this.toString()+": Pick up parcel after "+this.carryingTask.get().getHops()+" hops.");
            this.setNextDestination(this.carryingTask.get().getDestination());
        } else if (this.carryingTask.isPresent()) {
            // aangekomen op eindbestemming
            this.pdpModel.get().deliver(this, this.carryingTask.get(), timeLapse);
            if (this.carryingTask.get().getTaskStation().fixedratio)
                this.carryingTask.get().getTaskStation().taskDone();
            this.carryingTask.get().drop();
            CNPCray.stats.dataUpdate(this.name, "count", "agent deliver", 1);
            CNPCray.stats.dataUpdate(this.carryingTask.get().toString(), "timing", "deliver", timeLapse.getTime());
            this.carryingTask = Optional.absent();
            this.setNextDestination(null);
        } else {
            // geen taken ontvangen of echte goals gezet => zet op null om random verder te blijven bewegen.
            this.setNextDestination(null);
        }
    }

    @Override
    public void declareTaskManager(Task task, long time) {
        if (!this.canBeTaskManager())
            throw new IllegalStateException("Agent cannot be task manager at this moment. " +
                "Try with another agent or try again later.");
        this.taskStation = Optional.absent();
        CNPCray.stats.dataUpdate(this.name, "count", "became taskmanager", 1);
        this.taskManagerTask = Optional.of(task);
        this.setNextDestination(task.getPosition());
    }

    @Override
    protected boolean canBeTaskManager() {
        return this.charging < 0
                && !this.carryingTask.isPresent()
                && !this.taskManagerTask.isPresent()
                && !this.batteryStation.isPresent();
    }

    @Override
    protected void broadcast(TaskMessageContents contents) {
        if (this.getEnergy() - broadcastCost >= 0) {
            this.device.get().broadcast(contents);
            this.decreaseEnergyWith(broadcastCost);
        }
    }

    protected void broadcast(TaskMessageContents.TaskMessage message, Task task) {
        TaskMessageContents contents = new TaskMessageContents(message, task);
        this.broadcast(contents);
    }

}