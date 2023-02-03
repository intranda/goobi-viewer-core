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

import java.util.Arrays;
import java.util.Optional;

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

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

public class DefaultQueueListener {

    private static final Logger log = LogManager.getLogger(DefaultQueueListener.class);

    private final MessageBroker messageBroker;
    private Thread thread;
    private ActiveMQConnection conn;
    private MessageConsumer consumer;
    private volatile boolean shouldStop = false;

    public DefaultQueueListener(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void register(String username, String password, String queueType) throws JMSException {
        ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory("vm://localhost");
        connFactory.setTrustedPackages(Arrays.asList("io.goobi.viewer.managedbeans", "io.goobi.viewer.model.job.mq"));
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
                            optTicket = Optional.of(ViewerMessage.parseJSON(tm.getText()));
                        }
                        if (message instanceof BytesMessage) {
                            BytesMessage bm = (BytesMessage) message;
                            byte[] bytes = new byte[(int) bm.getBodyLength()];
                            bm.readBytes(bytes);
                            optTicket = Optional.of(ViewerMessage.parseJSON(new String(bytes)));
                        }
                        if (optTicket.isPresent()) {
                            log.debug("Handling ticket {}", optTicket.get());
                            ViewerMessage ticket = optTicket.get();
                            try {
                                ViewerMessage retry = DataManager.getInstance().getDao().getViewerMessageByMessageID(message.getJMSMessageID());
                                if (retry != null) {
                                    ticket = retry;
                                    ticket.setRetryCount(ticket.getRetryCount() + 1);
                                }
                            } catch (DAOException e) {
                                log.error(e);
                            }

                            ticket.setMessageId(message.getJMSMessageID());

                            try {
                                MessageStatus result = messageBroker.handle(ticket);
                                
                                if (result != MessageStatus.ERROR) {
                                    //acknowledge message, it is done
                                    message.acknowledge();
                                } else {
                                    //error or wait => put back to queue and retry by redeliveryPolicy
                                    sess.recover();
                                }
                            } catch (Exception t) {
                                log.error("Error handling ticket " + message.getJMSMessageID() + ": ", t);
                                sess.recover();
                            } finally {
                                MessageBroker.notifyMessageQueueStateUpdate();
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
    
}
