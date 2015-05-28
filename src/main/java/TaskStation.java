import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.HashMap;
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
    long lastReceiveTime = 0;
    private double broadcastThreshold = 5000;
    private final Point position;
    private List<Task> stillToBeAssignedTasks = new ArrayList<Task>();

    private final int minimumBids = 2;

    public TaskStation(RandomGenerator rng, Point point, PDPModel pdpModel, RoadModel roadModel){
        this.pdpmodel = Optional.of(pdpModel);
        this.position = point;
        this.rng = rng;
        this.roadModel = Optional.of(roadModel);
        device = Optional.absent();
        range = rng.nextDouble();
        reliability = rng.nextDouble();
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
        double toss = rng.nextDouble();
        if (toss >= 0 && toss <= 0.002){
            //RandomLy generate new Tasks
            Point ori =this.roadModel.get().getRandomPosition(rng);
            Task t = new Task(ori, this.position, 10);
            this.stillToBeAssignedTasks.add(t);
            this.pdpmodel.get().register(t);
            this.roadModel.get().register(t);
            this.device.get().broadcast(TaskMessages.TASK_READY );
        }
        // Response from workers => choose best one if offer
        if (this.device.get().getUnreadCount() > 0){
            lastReceiveTime = timeLapse.getStartTime();
            ImmutableList<Message> answers = this.device.get().getUnreadMessages();
            int index = rng.nextInt(answers.size());
            Message answer = answers.get(index);
            this.assignTask((CNPAgent) answer.getSender());
        }
        // if there are tasks left and there is no response in the given timeframe
        // rebroadcast

    }

    @Override
    public void afterTick(TimeLapse timeLapse) {

    }

    private int getNumberOfBids() {
        List<CNPAgent> bidders = new ArrayList<CNPAgent>();
        for (Message message: this.device.get().getUnreadMessages()) {
            if (!bidders.contains(message.getSender())) {
                bidders.add((CNPAgent) message.getSender());
            }
        }
        return bidders.size();
    }

    private void assignTask(CNPAgent agent) {
        if (this.stillToBeAssignedTasks.size() > 0) {
            Task task = this.stillToBeAssignedTasks.get(0);
            agent.assignTask(task);
        }
    }

    private void declareTaskManager(CNPAgent agent) {
        if (this.stillToBeAssignedTasks.size() > 0) {
            Task task = this.stillToBeAssignedTasks.get(0);
            agent.declareTaskManager(task);
        }
    }

    enum TaskMessages implements MessageContents {
        NICE_TO_MEET_YOU, WHERE_IS_EVERYBODY, TASK_READY, TASK_ASSIGNED, TASK_OFFER ;
    }
}
