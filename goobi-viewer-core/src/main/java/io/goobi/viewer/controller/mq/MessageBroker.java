package io.goobi.viewer.controller.mq;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.activemq.memory.buffer.MessageQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Startup;
import org.reflections.Reflections;

/**
 * Manages handling of messages by their respective MessageHandlers. Main method is {@link #handle(ViewerMessage message)}
 * which accepts a {@link ViewerMessage} and calls a {@link MessageHandler} instance to process the message, returning a
 * {@link MessageStatus} result.
 * #handle may either be called directly to handle the message instantly, or from a {@link MessageQueue
 * 
 * @author florian
 *
 */
@Singleton
@Startup
public class MessageBroker {

    private static final Logger logger = LogManager.getLogger(MessageBroker.class);
    
    private final Map<String, MessageHandler<MessageStatus>> instances;
    
    public MessageBroker() {
        this.instances = generateTicketHandlers();
    }
    
    public MessageBroker(Map<String, MessageHandler<MessageStatus>> instances) {
        this.instances = instances;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map<String, MessageHandler<MessageStatus>> generateTicketHandlers() {
        Map<String, MessageHandler<MessageStatus>> handlers = new HashMap<>();
        Set<Class<? extends MessageHandler>> ticketHandlers = new Reflections("io.goobi.viewer.model.job.mq.*").getSubTypesOf(MessageHandler.class);
        for (Class<? extends MessageHandler> clazz : ticketHandlers) {
            try {
                MessageHandler<MessageStatus> handler = clazz.getDeclaredConstructor().newInstance();
                handlers.put(handler.getMessageHandlerName(), handler);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException
                    | SecurityException e) {
                logger.error(e);
            }
        }
        return handlers;
    }
}
