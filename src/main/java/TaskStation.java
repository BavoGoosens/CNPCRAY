import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bavo and michiel.
 */
public class TaskStation implements CommUser, RoadUser, TickListener {

    private Optional<RoadModel> roadModel;
    private Optional<PDPModel> pdpmodel;
    private final RandomGenerator rng;
    private Optional<CommDevice> device;
    private final double range;
    private final double reliability;
    private double retransmission = 100;
    private final Point position;
    private List<Task> stillToBeAssignedTasks = new ArrayList<Task>();
    private int previousSize = -1;
    private int taskCount;
    private String name;
    public boolean fixedratio = false;
    private int taskManagerRatio = 3;
    private long hurryUp = 500000;

    public TaskStation(String name, RandomGenerator rng, Point point, PDPModel pdpModel, RoadModel roadModel){
        this.name = name;
        this.pdpmodel = Optional.of(pdpModel);
        this.position = point;
        this.rng = rng;
        this.roadModel = Optional.of(roadModel);
        device = Optional.absent();
        range = rng.nextDouble();
        reliability = rng.nextDouble();
        this.taskCount = 200;
    }

    @Override
    public Optional<Point> getPosition() {
        Point p = this.roadModel.get().getPosition(this);
        return Optional.of(p);
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(10);
        device = Optional.of(commDeviceBuilder
                .setReliability(1)
                .build());
    }

    @Override
    public void initRoadUser(RoadModel roadModel) {
        this.roadModel = Optional.of((RoadModel) roadModel);
        Point p;
        /*while (roadModel.get().isOccupied(p = model.getRandomPosition(rng))) {}*/
        p = roadModel.getRandomPosition(rng);
        this.roadModel.get().addObjectAt(this, this.position);
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        this.retransmission -= 1;
        double toss = rng.nextDouble();
        if (toss >= 0 && toss <= 0.001 && this.taskCount > 0){
            //RandomLy generate new Tasks
            Point ori =this.roadModel.get().getRandomPosition(rng);
            Task t = new Task(ori, this.position, 10, this);
            CNPCray.stats.dataUpdate(t.toString(),"task", "time_of_creation", timeLapse.getTime());
            this.stillToBeAssignedTasks.add(t);
            this.pdpmodel.get().register(t);
            this.roadModel.get().register(t);
            this.device.get().broadcast(
                    new TaskMessageContents(TaskMessageContents.TaskMessage.TASK_MANAGER_NEEDED)
            );
            CNPCray.stats.dataUpdate(this.name, "communitcation", "broadcast", 1);
            this.taskCount--;
        }

       /* if (this.stillToBeAssignedTasks.size() != this.previousSize) {
            previousSize = this.stillToBeAssignedTasks.size();
            if (this.stillToBeAssignedTasks.size() > 0) {
                System.out.println(this.toString()+": Searching task manager for "
                        +this.stillToBeAssignedTasks.size()+" tasks. Broadcast messages.");
            } else {
                System.out.println(this.toString()+": No tasks available. Not searching or broadcasting.");
            }
        }*/
        List<Integer> assignedTasks = new ArrayList<Integer>();
        for (int i = 0; i < this.stillToBeAssignedTasks.size(); i++) {
            if (this.searchForTaskManager(this.stillToBeAssignedTasks.get(i), timeLapse.getTime())) {
                assignedTasks.add(i);
            }
        }
        for (int i: assignedTasks) {
            this.stillToBeAssignedTasks.remove(i);
        }
        // Response from workers => choose best one if offer
       /* if (this.device.get().getUnreadCount() > 0){
            ImmutableList<Message> answers = this.device.get().getUnreadMessages();
            for (Message m: answers){
                if (! stillToBeAssignedTasks.isEmpty()){
                    this.declareTaskManager((CNPAgent) m.getSender());
                }
            }
        }*/
        // if there are tasks left and there is no response in the given timeframe
        // rebroadcast
        if (this.retransmission <= 0 && this.stillToBeAssignedTasks.size() > 0 &&ratioOn()){
            this.retransmission = 100;
            this.device.get().broadcast(
                    new TaskMessageContents(TaskMessageContents.TaskMessage.TASK_MANAGER_NEEDED)
            );
        }

    }

    private boolean ratioOn(){
        return !fixedratio || (fixedratio && this.taskManagerRatio >= 0);
    }

    public boolean searchForTaskManager(Task task, long time) {
        boolean assigned = false;
        for (Message message: this.device.get().getUnreadMessages()) {
            CommUser sender = message.getSender();
            if (sender instanceof CNPAgent &&
                    message.getContents().equals(TaskMessageContents.TaskMessage.WANT_TO_BE_TASK_MANAGER)) {
                CNPAgent agent = (CNPAgent) sender;
                try {
                    agent.declareTaskManager(task, time);
                    assigned = true;
                    System.out.println(this.toString()+": "+agent.toString()+" assigned task manager.");
                    if (this.fixedratio)
                        this.taskManagerRatio -= 1;
                    break;
                } catch(IllegalStateException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        return assigned;
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        if (taskCount <= 0 && hurryUp >= 0){
            this.hurryUp -= 1;
        } else if (hurryUp <= 0){
            CNPCray.stopSim(this.name);
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void taskDone() {
        this.taskManagerRatio += 1;
    }
}
