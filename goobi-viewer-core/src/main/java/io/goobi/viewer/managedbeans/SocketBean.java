package io.goobi.viewer.managedbeans;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class SocketBean {

    private static final Logger logger = LogManager.getLogger(SocketBean.class);

    private static final Long MIN_IDLE_TIME = 2l;

    private final AtomicBoolean shouldSend = new AtomicBoolean(false);

    @Inject
    @Push
    private PushContext backgroundTasksState;

    public SocketBean() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(createRunnable(), 0, MIN_IDLE_TIME, TimeUnit.SECONDS);
    }

    public void send(String message) {
        shouldSend.set(true);
    }

    private Runnable createRunnable() {
        return () -> {
            if (shouldSend.getAndSet(false)) {
                System.out.println("SENDING UPDATE");
                sendMessage("update");
            }
        };
    }

    private void sendMessage(String message) {
        this.backgroundTasksState.send(message);

    }

}
