package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.util.Faces;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import io.goobi.viewer.controller.mq.MessageQueueManager;

@Named
@ViewScoped
public class WebSocketBean implements Serializable {

    private static final long serialVersionUID = 9068383748390523908L;

    private static final Logger logger = LogManager.getLogger(WebSocketBean.class);

    @Inject
    @Push
    private PushContext pushChannel;
    @Inject
    private ServletContext context;
    @Inject
    private transient MessageQueueManager queueManager;
    private transient Scheduler scheduler = null;


    public WebSocketBean() {
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            logger.error("Error getting quartz scheduler", e);
        }
    }

    public Map<String, String> receiveMessage(String...parameters) {
        return Arrays.stream(parameters).map(p -> {
            return new String[] {p, Faces.getRequestParameter(p)};
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));
        
    }
    
    public void sendMessage(Object message) {
        logger.debug("sending message '{}'", message);
        pushChannel.send(message).stream().forEach(f -> logger.debug("message sent"));
    }


}
