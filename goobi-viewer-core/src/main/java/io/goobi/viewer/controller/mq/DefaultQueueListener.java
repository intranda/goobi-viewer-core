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

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;

public class DefaultQueueListener {

    private static final Logger log = LogManager.getLogger(DefaultQueueListener.class);

    private final MessageQueueManager messageBroker;
    private Thread thread;
    private volatile boolean shouldStop = false;

    public DefaultQueueListener(MessageQueueManager messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void register(String queueType) throws JMSException {
        ActiveMQConnection conn = this.messageBroker.getConnection();
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setAll(0);
        conn.setPrefetchPolicy(prefetchPolicy);
        RedeliveryPolicy policy = conn.getRedeliveryPolicy();
        policy.setMaximumRedeliveries(0);
        thread = new Thread(() -> startMessageLoop(queueType, conn));
        thread.setDaemon(true);
        thread.start();
    }

    void startMessageLoop(String queueType, ActiveMQConnection conn) {
        try {
            conn.start();
            startListener(queueType, conn);
            log.info("Exiting listener thread for message queue {}: ", queueType);
        } catch (JMSException e) {
            log.error("Error starting listener for queue {}. Aborting listerner startup", e.toString(), e);
        }
    }

    void startListener(String queueType, ActiveMQConnection conn) throws JMSException {
        try (Session sess = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                MessageConsumer consumer = sess.createConsumer(sess.createQueue(queueType));) {
            while (!shouldStop) {
                waitForMessage(sess, consumer);
                if (Thread.interrupted()) {
                    log.info("Queue listener for queue {} interrupted: exiting listener", queueType);
                    return;
                }
            }
        }
    }

    void waitForMessage(Session sess, MessageConsumer consumer) {
        try {
            Message message = consumer.receive();
            ViewerMessage ticket = null;
            if (message instanceof TextMessage) {
                TextMessage tm = (TextMessage) message;
                ticket = ViewerMessage.parseJSON(tm.getText());
            }
            if (message instanceof BytesMessage) {
                BytesMessage bm = (BytesMessage) message;
                byte[] bytes = new byte[(int) bm.getBodyLength()];
                bm.readBytes(bytes);
                ticket = ViewerMessage.parseJSON(new String(bytes));
            }
            if (ticket != null) {
                handleTicket(sess, message, ticket);
            }
        } catch (JMSException | JsonProcessingException e) {
            if (!shouldStop) {
                // back off a little bit, maybe we have a problem with the connection or we are shutting down
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!shouldStop) {
                    log.error("Message listener has encountered an error. Attempting to resume listener", e);
                }
            }
        }
    }

    void handleTicket(final Session sess, Message message, ViewerMessage ticket) throws JMSException {
        log.debug("Handling ticket {}", ticket);
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
            log.error("Error handling ticket {}: ", message.getJMSMessageID(), t);
            sess.recover();
        } finally {
            MessageQueueManager.notifyMessageQueueStateUpdate();
        }
    }

    public void close() {
        this.shouldStop = true;
        log.info("Stopping MessageQueue listener...");
        try {
            this.thread.join(1000);
        } catch (InterruptedException e) {
            log.error(e);
            Thread.currentThread().interrupt();
        }
    }

}
