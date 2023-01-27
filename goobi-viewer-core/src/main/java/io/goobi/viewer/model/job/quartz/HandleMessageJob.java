package io.goobi.viewer.model.job.quartz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.quartz.Job;

import io.goobi.viewer.controller.mq.MessageBroker;
import io.goobi.viewer.controller.mq.MessageGenerator;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;

/**
 * Cronjob 
 * @author florian
 *
 */
public class HandleMessageJob extends AbstractViewerJob implements IViewerJob, Job {

    private final TaskType taskType;
    private final String cronSchedulerExpression;
    
    public HandleMessageJob(TaskType taskType, String cronSchedulerExpression, MessageBroker messageBroker) {
        this.taskType = taskType;
        this.cronSchedulerExpression = cronSchedulerExpression;
    }
    
    public HandleMessageJob() {
        this.taskType = null;
        this.cronSchedulerExpression = "";
    }
    
    @Override
    public String getJobName() {
        return Optional.ofNullable(taskType).map(TaskType::name).orElse("");
    }
    
    @Override
    public String getCronExpression() {
        return cronSchedulerExpression;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    @Override
    public void execute(Map<String, Object> params, MessageBroker messageBroker) {
        TaskType type = (TaskType)params.get("taskType");
        boolean runInQueue = (boolean) params.get("runInQueue");
        ViewerMessage message = MessageGenerator.generateSimpleMessage(type.name());
        params.forEach((key, value) -> {
            message.getProperties().put(key, value.toString());
        });
        if(runInQueue) {
            messageBroker.addToQueue(message);
        } else {            
            messageBroker.handle(message);
        }
    }

}
