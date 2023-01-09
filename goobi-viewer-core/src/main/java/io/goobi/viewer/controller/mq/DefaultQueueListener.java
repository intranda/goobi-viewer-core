/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.goobi.viewer.controller.mq;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultQueueListener {

    private static final Logger log = LogManager.getLogger(DefaultQueueListener.class);

    private Thread thread;
    private ActiveMQConnection conn;
    private MessageConsumer consumer;
    private volatile boolean shouldStop = false;

    private static Map<String, MessageHandler<ReturnValue>> instances = new HashMap<>();

    public void register(String username, String password, String queueType) throws JMSException {
        ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory("vm://localhost");
        connFactory.setTrustedPackages(Arrays.asList("io.goobi.managedbeans", "io.goobi.viewer.model.job.mq"));
        conn = (ActiveMQConnection) connFactory.createConnection(username, password);
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setAll(0);
        conn.setPrefetchPolicy(prefetchPolicy);
        RedeliveryPolicy policy = conn.getRedeliveryPolicy();
        policy.setMaximumRedeliveries(0);
        final Session sess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        final Destination dest = sess.createQueue(queueType);

        consumer = sess.createConsumer(dest);

        Runnable run = new Runnable() {
            @Override
            public void run() {
                while (!shouldStop) {
                    try {
                        Message message = consumer.receive();
                        Optional<ViewerMessage> optTicket = Optional.empty();
                        if (message instanceof TextMessage) {
                            TextMessage tm = (TextMessage) message;
                            optTicket = Optional.of(new ObjectMapper().readValue(tm.getText(), ViewerMessage.class));
                        }
                        if (message instanceof BytesMessage) {
                            BytesMessage bm = (BytesMessage) message;
                            byte[] bytes = new byte[(int) bm.getBodyLength()];
                            bm.readBytes(bytes);
                            optTicket = Optional.of(new ObjectMapper().readValue(new String(bytes), ViewerMessage.class));
                        }
                        if (optTicket.isPresent()) {
                            log.debug("Handling ticket {}", optTicket.get());
                            try {
                                ReturnValue result = handleMessage(optTicket.get());
                                if (result != ReturnValue.ERROR) {
                                    //acknowledge message, it is done
                                    message.acknowledge();
                                } else {
                                    //error or wait => put back to queue and retry by redeliveryPolicy
                                    sess.recover();
                                }
                            } catch (Throwable t) {
                                log.error("Error handling ticket " + message.getJMSMessageID() + ": ", t);
                                sess.recover();
                            }
                        }
                    } catch (JMSException | JsonProcessingException e) {
                        if (!shouldStop) {
                            // back off a little bit, maybe we have a problem with the connection or we are shutting down
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException e1) {
                                Thread.currentThread().interrupt();
                            }
                            if (!shouldStop) {
                                log.error(e);
                            }
                        }
                    }
                }

            }
        };
        thread = new Thread(run);
        thread.setDaemon(true);

        conn.start();
        thread.start();
    }

    private ReturnValue handleMessage(ViewerMessage message) {
        if (!instances.containsKey(message.getTaskName())) {
            getInstalledTicketHandler();
        }
        MessageHandler<ReturnValue> handler = instances.get(message.getTaskName());
        if (handler == null) {
            return ReturnValue.ERROR;
        }
        return handler.call(message);
    }

    public void close() throws JMSException {
        this.shouldStop = true;
        this.consumer.close();
        this.conn.close();
        try {
            this.thread.join(1000);
        } catch (InterruptedException e) {
            log.error(e);
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void getInstalledTicketHandler() {
        instances = new HashMap<>();
        Set<Class<? extends MessageHandler>> ticketHandlers = new Reflections("io.goobi.viewer.model.job.mq.*").getSubTypesOf(MessageHandler.class);
        for (Class<? extends MessageHandler> clazz : ticketHandlers) {
            try {
                MessageHandler<ReturnValue> handler = clazz.getDeclaredConstructor().newInstance();
                instances.put(handler.getMessageHandlerName(), handler);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException
                    | SecurityException e) {
                log.error(e);
            }
        }
    }
}
