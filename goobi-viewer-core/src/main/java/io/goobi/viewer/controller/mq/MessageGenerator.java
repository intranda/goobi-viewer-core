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
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.goobi.viewer.controller.DataManager;

public class MessageGenerator {

    private MessageGenerator() {
    }

    public static ViewerMessage generateSimpleMessage(String ticketType) {
        return new ViewerMessage(ticketType);
    }

    /**
     * submits an Object to an internal queue. Be sure that someone really consumes the queue, the argument "queueType" is not checked for sanity
     * 
     * @param ticket
     * @param queueType
     * @return id of the generated message
     * @throws JMSException
     * @throws JsonProcessingException
     */
    public static String submitInternalMessage(Object ticket, String queueType, String ticketType, String identifier)
            throws JMSException, JsonProcessingException {

        ActiveMQConnectionFactory connFactory = new ActiveMQConnectionFactory();
        connFactory.setTrustedPackages(Arrays.asList("io.goobi.viewer.managedbeans", "io.goobi.viewer.model.job.mq"));

        Connection conn = connFactory.createConnection(DataManager.getInstance().getConfiguration().getActiveMQUsername(),
                DataManager.getInstance().getConfiguration().getActiveMQPassword());
        String messageId = submitTicket(ticket, queueType, conn, ticketType, identifier);
        conn.close();
        return messageId;
    }

    private static String submitTicket(Object ticket, String queueName, Connection conn, String ticketType, String identifier)
            throws JMSException, JsonProcessingException {
        Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination dest = sess.createQueue(queueName);
        MessageProducer producer = sess.createProducer(dest);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        TextMessage message = sess.createTextMessage();
        // we set a random UUID here, because otherwise tickets will not be processed in parallel in an SQS fifo queue.
        // we still need a fifo queue for message deduplication, though.
        // See: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-additional-fifo-queue-recommendations.html
        message.setStringProperty("JMSXGroupID", UUID.randomUUID().toString());

        message.setText(new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(ticket));
        message.setStringProperty("JMSType", ticketType);
        message.setStringProperty("identifier", identifier);
        producer.send(message);
        return message.getJMSMessageID();
    }

}
