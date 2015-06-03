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
        LEAVING
    }

    private final TaskMessage message;
    private final Task task;

    public TaskMessageContents(TaskMessage message) {
        this(message, null);
        if (message.equals(TaskMessage.TASK_MANAGER_ASSIGNED)
                || message.equals(TaskMessage.WORKER_ASSIGNED)) {
            throw new IllegalStateException("You need to give a task when the message is "+message.toString());
        }
    }

    public TaskMessageContents(TaskMessage message, Task task) {
        this.message = message;
        this.task = task;
    }

    public TaskMessage getMessage() { return this.message; }
    public Task getTask() { return this.task; }

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
