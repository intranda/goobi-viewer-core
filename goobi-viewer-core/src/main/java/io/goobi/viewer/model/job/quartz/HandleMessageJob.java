package io.goobi.viewer.model.job.quartz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private final MessageBroker messageBroker;
    private final String cronSchedulerExpression;
    private final Map<String, String> params = new HashMap<>();
    private final boolean runInQueue;
    
    public HandleMessageJob(TaskType taskType, String cronSchedulerExpression, MessageBroker messageBroker) {
        this.taskType = taskType;
        this.cronSchedulerExpression = cronSchedulerExpression;
        this.messageBroker = messageBroker;
        this.runInQueue = true;
    }
    
    public HandleMessageJob(TaskType taskType, MessageBroker messageBroker) {
        this.taskType = taskType;
        this.cronSchedulerExpression = "";
        this.messageBroker = messageBroker;
        this.runInQueue = false;
    }
    
    @Override
    public String getJobName() {
        return taskType.name();
    }
    
    @Override
    public String getCronExpression() {
        return cronSchedulerExpression;
    }

    public void setParam(String name, String value) {
        this.params.put(name, value);
    }
    
    public String getParam(String name) {
        return this.params.get(name);
    }
    
    public boolean isRunInQueue() {
        return runInQueue;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    @Override
    public void execute() {
        ViewerMessage message = MessageGenerator.generateSimpleMessage(this.taskType.name());
        this.params.forEach((key, value) -> {
            message.getProperties().put(key, value);
        });
        if(isRunInQueue()) {
            this.messageBroker.addToQueue(message);
        } else {            
            this.messageBroker.handle(message);
        }
    }

}
