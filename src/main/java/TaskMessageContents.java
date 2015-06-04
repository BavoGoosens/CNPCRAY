import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Created by parallels on 6/3/15.
 */
public class TaskMessageContents implements MessageContents {

    public enum TaskMessage {
        TASK_MANAGER_NEEDED, WANT_TO_BE_TASK_MANAGER,
        TASK_MANAGER_ASSIGNED, TASK_MANAGER_DECLINED,
        WORKER_NEEDED, WANT_TO_BE_WORKER,
        WORKER_ASSIGNED, WORKER_DECLINED,
        LEAVING, GIVE_PROPOSAL, PROPOSAL,
        GIVE_TASK_MANAGER_TASK, TASK_MANAGER_TASK
    }

    private final TaskMessage message;
    private final Task task;
    private final double proposal;

    public TaskMessageContents(TaskMessage message) {
        if (message.equals(TaskMessage.TASK_MANAGER_ASSIGNED)
                || message.equals(TaskMessage.WORKER_ASSIGNED) || message.equals(TaskMessage.GIVE_PROPOSAL)
                || message.equals(TaskMessage.TASK_MANAGER_TASK)) {
            throw new IllegalStateException("You need to give a task when the message is "+message.toString());
        }
        if (message.equals(TaskMessage.PROPOSAL)) throw new IllegalStateException("You need to give a proposal when the message is "+message.toString());
        this.message = message;
        this.task = null;
        this.proposal = -1;
    }

    public TaskMessageContents(TaskMessage message, double proposal) {
        if (!message.equals(TaskMessage.PROPOSAL)) throw new IllegalStateException("You cannot give a proposal when the message is "+message.toString());
        this.proposal = proposal;
        this.message = message;
        this.task = null;
    }

    public TaskMessageContents(TaskMessage message, Task task) {
        if (!message.equals(TaskMessage.TASK_MANAGER_ASSIGNED) &&
                !message.equals(TaskMessage.WORKER_ASSIGNED) &&
                !message.equals(TaskMessage.GIVE_PROPOSAL) && !message.equals(TaskMessage.TASK_MANAGER_TASK)) {
            throw new IllegalStateException("You cannot give a task when the message is "+message.toString());
        }
        this.message = message;
        this.task = task;
        this.proposal = -1;
    }

    public TaskMessage getMessage() { return this.message; }
    public Task getTask() { return this.task; }
    public double getProposal() { return this.proposal; }

    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof TaskMessage) {
            TaskMessage taskMessage = (TaskMessage) otherObject;
            return this.getMessage().equals(taskMessage);
        }
        if (otherObject instanceof TaskMessageContents) {
            TaskMessageContents taskMessageContents = (TaskMessageContents) otherObject;
            return this.getMessage().equals(taskMessageContents.getMessage());
        }
        return false;
    }

}
