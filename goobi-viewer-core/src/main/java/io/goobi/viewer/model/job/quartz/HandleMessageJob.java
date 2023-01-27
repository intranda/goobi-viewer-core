package io.goobi.viewer.model.job.quartz;

import org.quartz.Job;

import io.goobi.viewer.controller.mq.MessageBroker;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;

public class HandleMessageJob extends AbstractViewerJob implements IViewerJob, Job {

    private final TaskType taskType;
    private final MessageBroker messageBroker;
    private final String cronSchedulerExpression;
    
    public HandleMessageJob(TaskType taskType, String cronSchedulerExpression, MessageBroker messageBroker) {
        this.taskType = taskType;
        this.cronSchedulerExpression = cronSchedulerExpression;
        this.messageBroker = messageBroker;
    }
    
    @Override
    public String getJobName() {
        return taskType.name();
    }
    
    @Override
    public String getCronExpression() {
        return cronSchedulerExpression;
    }

    @Override
    public void execute() {
        
        this.messageBroker.handle(message);
    }

}
