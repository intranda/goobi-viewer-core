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

package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletContext;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.jmx.QueueViewMBean;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.StartQueueBrokerListener;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;

@Named
@ApplicationScoped
public class MessageQueueBean implements Serializable {

    private static final Logger log = LogManager.getLogger(MessageQueueBean.class);

    private static final long serialVersionUID = -8818079687932871359L;

    private transient ActiveMQConnection connection;
    private transient QueueSession queueSession;

    private boolean messageBrokerStart;

    private String messageType;

    private boolean paused;

    private TableDataProvider<ViewerMessage> lazyModelViewerHistory;

    public MessageQueueBean() {
        this.initMessageBrokerStart();

    }

    @PostConstruct
    public void init() throws DAOException {

        if (this.messageBrokerStart) {

            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
            connectionFactory.setTrustedPackages(Arrays.asList("io.goobi.managedbeans", "io.goobi.viewer.model.job.mq"));

            try {
                connection =
                        (ActiveMQConnection) connectionFactory.createConnection(DataManager.getInstance().getConfiguration().getActiveMQUsername(),
                                DataManager.getInstance().getConfiguration().getActiveMQPassword());
                queueSession = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
            } catch (JMSException e) {
                log.error(e);
            }
            if (lazyModelViewerHistory == null) {
                lazyModelViewerHistory = initLazyModel();
            }
        }

    }

    public Map<String, Integer> getQueueContent() {
        Map<String, Integer> fastQueueContent = new TreeMap<>();
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            try {
                connection.start();
                Queue queue = queueSession.createQueue("viewer");
                QueueBrowser browser = queueSession.createBrowser(queue);
                Enumeration<?> messagesInQueue = browser.getEnumeration();
                while (messagesInQueue.hasMoreElements()) {
                    ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();

                    String type = queueMessage.getStringProperty("JMSType");
                    if (fastQueueContent.containsKey(type)) {
                        fastQueueContent.put(type, fastQueueContent.get(type) + 1);
                    } else {
                        fastQueueContent.put(type, 1);
                    }
                }
                browser.close();
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }
        return fastQueueContent;
    }

    public void pauseQueue() {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            try {

                FacesContext facesContext = FacesContext.getCurrentInstance();
                ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
                StartQueueBrokerListener listener = (StartQueueBrokerListener) servletContext.getAttribute("BrokerService");

                BrokerService broker = listener.getBroker();

                ObjectName queueViewMBeanName =
                        new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=viewer");

                QueueViewMBean mbean =
                        (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
                mbean.pause();
                paused = true;
            } catch (MalformedObjectNameException e) {
                log.error(e);
            }
        }
    }

    public void resumeQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            try {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
                StartQueueBrokerListener listener = (StartQueueBrokerListener) servletContext.getAttribute("BrokerService");

                BrokerService broker = listener.getBroker();

                ObjectName queueViewMBeanName =
                        new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=viewer");

                QueueViewMBean mbean =
                        (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);
                mbean.resume();
                paused = false;
            } catch (MalformedObjectNameException e) {
                log.error(e);
            }
        }
    }

    public void clearQueue() {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            try {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
                StartQueueBrokerListener listener = (StartQueueBrokerListener) servletContext.getAttribute("BrokerService");

                BrokerService broker = listener.getBroker();

                ObjectName queueViewMBeanName =
                        new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost,destinationType=Queue,destinationName=viewer");

                QueueViewMBean mbean =
                        (QueueViewMBean) broker.getManagementContext().newProxyInstance(queueViewMBeanName, QueueViewMBean.class, true);

                mbean.purge();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    public void initMessageBrokerStart() {
        this.messageBrokerStart = DataManager.getInstance().getConfiguration().isStartInternalMessageBroker();
    }

    /**
     * Delete a single message from the goobi_slow queue
     * 
     * @param ticket to delete
     */

    /**
     * Get a list of all active messages in the goobi_slow queue
     */

    public List<ViewerMessage> getActiveQueryMesssages() {

        List<ViewerMessage> answer = new ArrayList<>();
        if (!DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            return answer;
        }
        if (StringUtils.isNotBlank(messageType)) {
            try {
                connection.start();
                Queue queue = queueSession.createQueue("viewer");
                QueueBrowser browser = queueSession.createBrowser(queue, "JMSType = '" + messageType + "'");
                Enumeration<?> messagesInQueue = browser.getEnumeration();
                while (messagesInQueue.hasMoreElements() && answer.size() < 100) {
                    ActiveMQTextMessage queueMessage = (ActiveMQTextMessage) messagesInQueue.nextElement();
                    ViewerMessage ticket = new ObjectMapper().readValue(queueMessage.getText(), ViewerMessage.class);
                    ticket.setMessageId(queueMessage.getJMSMessageID());

                    answer.add(ticket);
                }
                browser.close();
                connection.stop();
            } catch (JMSException | JacksonException e) {
                log.error(e);
            }
        }
        return answer;
    }

    /**
     * Remove all active messages of a given type from the queue
     * 
     */
    public void removeMessagesFromQueue() {
        if (StringUtils.isNotBlank(messageType)) {
            try {
                Queue queue = queueSession.createQueue("viewer");
                MessageConsumer consumer = queueSession.createConsumer(queue, "JMSType='" + messageType + "'");
                connection.start();
                Message message = consumer.receiveNoWait();
                while (message != null) {
                    message.acknowledge();
                    message = consumer.receiveNoWait();
                }
                connection.stop();
            } catch (JMSException e) {
                log.error(e);
            }
        }

    }

    /**
     * Delete a single message from the queue
     * 
     * @param ticket
     */

    public void deleteMessage(ViewerMessage ticket) {
        try {
            connection.start();
            Queue queue = queueSession.createQueue("viewer");
            QueueReceiver receiver = queueSession.createReceiver(queue, "JMSMessageID='" + ticket.getMessageId() + "'");
            Message message = receiver.receiveNoWait();

            if (message != null) {
                message.acknowledge();
            }
            connection.stop();
        } catch (JMSException e) {
            log.error(e);
        }
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isMessageBrokerStart() {
        return messageBrokerStart;
    }

    public boolean isPaused() {
        return paused;
    }

    private TableDataProvider<ViewerMessage> initLazyModel() {
        TableDataProvider<ViewerMessage> model = new TableDataProvider<>(new TableDataSource<ViewerMessage>() {

            private Optional<Long> numCreatedPages = Optional.empty();

            @SuppressWarnings("unchecked")
            @Override
            public List<ViewerMessage> getEntries(int first, int pageSize, String sortField, SortOrder sortOrder,
                    Map<String, String> filters) {
                if (StringUtils.isBlank(sortField)) {
                    sortField = "id";
                    sortOrder = SortOrder.DESCENDING;
                }
                List<ViewerMessage> ret;
                try {
                    ret = DataManager.getInstance().getDao().getViewerMessages(first, pageSize, sortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    log.error(e);
                    return Collections.emptyList();
                }

                return ret;
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getViewerMessageCount(filters);
                } catch (DAOException e) {
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numCreatedPages = Optional.empty();
            }
        });
        model.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
        return model;
    }

    public TableDataProvider<ViewerMessage> getLazyModelViewerHistory() {
        return lazyModelViewerHistory;
    }

}
