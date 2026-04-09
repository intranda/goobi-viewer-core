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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.DefaultQueueListener;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.jms.JMSException;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;

/**
 * JSF backing bean for monitoring and managing the viewer's internal message queue.
 */
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

    @Inject
    private transient SocketBean socketBean;

    @Inject
    private transient MessageQueueManager messageBroker;

    private TableDataProvider<ViewerMessage> lazyModelViewerHistory;

    /**
     * Creates a new MessageQueueBean instance.
     */
    public MessageQueueBean() {
        this.initMessageBrokerStart();

    }

    /**
     * Creates a new MessageQueueBean instance.
     *
     * @param broker message queue manager to use
     */
    public MessageQueueBean(MessageQueueManager broker) {
        this.messageBroker = broker;
        this.initMessageBrokerStart();
    }

    /**
     * init.
     */
    @PostConstruct
    public void init() {

        if (this.messageBrokerStart) {

            try {
                connection = messageBroker.getConnection();
                queueSession = connection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
            } catch (JMSException e) {
                log.error(e);
            }
            if (lazyModelViewerHistory == null) {
                lazyModelViewerHistory = initLazyModel();
            }
        }

    }

    /**
     * close.
     *
     * @throws jakarta.jms.JMSException if any.
     */
    @PreDestroy
    public void close() {
        log.debug("MessageQueueBean.close()");
        for (DefaultQueueListener listener : getListeners()) {
            listener.close();
        }
        try {
            if (this.queueSession != null) {
                this.queueSession.close();
            }
        } catch (JMSException e) {
            log.warn("Error closing queue session", e);
        }
        try {
            if (this.connection != null) {
                this.connection.close();
            }
        } catch (JMSException e) {
            // During shutdown the ActiveMQ transport may already be closed (EOFException /
            // TransportDisposedIOException) before the JMS client gets to send a close frame.
            // This is a known race condition and harmless — the connection is gone either way.
            if (this.connection != null && this.connection.isTransportFailed()) {
                log.debug("Connection was already disposed at shutdown");
            } else {
                log.warn("Error closing connection", e);
            }
        }
    }

    /**
     * getQueueContent.
     *
     * @return a map of queue names to their current message counts
     */
    public Map<String, Integer> getQueueContent() {
        Map<String, Integer> fastQueueContent = new TreeMap<>();
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            for (String queueName : MessageQueueManager.QUEUE_NAMES) {
                fastQueueContent.putAll(messageBroker.countMessagesInQueue(queueName));
            }
        }
        return fastQueueContent;
    }

    public int getTotalMessagesInQueueCount() {
        return getQueueContent().values().stream().mapToInt(i -> i).sum();
    }

    /**
     * pauseQueue.
     */
    public void pauseQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            paused = true;
            for (String queueName : MessageQueueManager.QUEUE_NAMES) {
                paused &= this.messageBroker.pauseQueue(queueName);
            }
            updateMessageQueueState();
        }
    }

    /**
     * resumeQueue.
     */
    public void resumeQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            paused = false;
            for (String queueName : MessageQueueManager.QUEUE_NAMES) {
                // 'false' indicates an error during resume -> still paused
                paused |= !this.messageBroker.resumeQueue(queueName);
            }
            updateMessageQueueState();
        }
    }

    /**
     * clearQueue.
     */
    public void clearQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            for (String queueName : MessageQueueManager.QUEUE_NAMES) {
                this.messageBroker.clearQueue(queueName);
            }
            updateMessageQueueState();
        }
    }

    /**
     * initMessageBrokerStart.
     */
    public void initMessageBrokerStart() {
        this.messageBrokerStart = DataManager.getInstance().getConfiguration().isStartInternalMessageBroker();
    }

    /**
     * Delete a single message from the goobi_slow queue.
     * 
     * @param ticket to delete
     */

    /**
     * Get a list of all active messages in the goobi_slow queue.
     *
     * @return list of all currently active {@link ViewerMessage} objects in the queue
     */
    public List<ViewerMessage> getActiveQueryMesssages() {
        return getQueryMessages(this.messageType);
    }

    /**
     * getQueryMessages.
     *
     * @param messageType message type to filter by
     * @return List<ViewerMessage>
     */
    public List<ViewerMessage> getQueryMessages(String messageType) {

        if (this.messageBroker != null && DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            return this.messageBroker.getWaitingMessages(messageType);
        }

        return new ArrayList<>();
    }

    /**
     * Remove all active messages of a given type from the queue.
     *
     * @param type message type to remove
     */
    public void removeMessagesFromQueue(String type) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            this.messageBroker.deleteMessages(type);
            updateMessageQueueState();
        }

    }

    /**
     * Deletes a single message from the queue.
     *
     * @param ticket message to delete from queue
     */
    public void deleteMessage(ViewerMessage ticket) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            this.messageBroker.deleteMessage(ticket);
            updateMessageQueueState();
        }

    }

    /**
     * Getter for the field <code>messageType</code>.
     *
     * @return the message type currently used as filter
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Setter for the field <code>messageType</code>.
     *
     * @param messageType message type to set as filter
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * isMessageBrokerStart.
     *
     * @return true if the message broker has been started, false otherwise
     */
    public boolean isMessageBrokerStart() {
        return messageBrokerStart;
    }

    /**
     * isPaused.
     *
     * @return true if message queue processing is currently paused, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }

    private static TableDataProvider<ViewerMessage> initLazyModel() {
        TableDataProvider<ViewerMessage> model = new TableDataProvider<>(new TableDataSource<ViewerMessage>() {

            @Override
            public List<ViewerMessage> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                    Map<String, String> filters) {
                String useSortField = sortField;
                SortOrder useSortOrder = sortOrder;
                if (StringUtils.isBlank(useSortField)) {
                    useSortField = "id";
                    useSortOrder = SortOrder.DESCENDING;
                }
                List<ViewerMessage> ret;
                try {
                    ret = DataManager.getInstance().getDao().getViewerMessages(first, pageSize, useSortField, useSortOrder.asBoolean(), filters);
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
                //empty
            }
        });
        model.setEntriesPerPage(AdminBean.DEFAULT_ROWS_PER_PAGE);
        model.getFilter("all");
        return model;
    }

    /**
     * Getter for the field <code>lazyModelViewerHistory</code>.
     *
     * @return the TableDataProvider for the viewer message history
     */
    public TableDataProvider<ViewerMessage> getLazyModelViewerHistory() {
        return lazyModelViewerHistory;
    }

    /**
     * updateMessageQueueState.
     */
    public void updateMessageQueueState() {
        this.socketBean.send("{'action':'update', 'subject':'messageQueueState'}");

        cleanOldMessages();
    }

    private static void cleanOldMessages() {
        try {
            int deleteAfterDays = DataManager.getInstance().getConfiguration().getActiveMQMessagePurgeInterval();
            if (deleteAfterDays > 0) {
                LocalDateTime before = LocalDateTime.now().minusDays(deleteAfterDays);
                DataManager.getInstance().getDao().deleteViewerMessagesBefore(before);
            }
        } catch (DAOException e) {
            log.error(e);
        }

    }

    /**
     * getListeners.
     *
     * @return a list of active message queue listeners registered with the message broker
     */
    public List<DefaultQueueListener> getListeners() {
        return Optional.ofNullable(this.messageBroker).map(broker -> broker.getListeners()).orElse(Collections.emptyList());
    }

    /**
     * restartAllListeners.
     */
    public void restartAllListeners() {
        getListeners().forEach(l -> {
            try {
                l.restartLoop();
            } catch (JMSException e) {
                log.error("Error restarting message listener for queue {}", l.getQueueType(), e);
            }
        });
        updateMessageQueueState();
    }

    public MessageQueueState getMessageQueueState() {
        if (!DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            return MessageQueueState.INACTIVE;
        } else if (this.messageBroker == null || !this.messageBroker.isQueueRunning()) {
            return MessageQueueState.STOPPED;
        } else if (this.isPaused()) {
            return MessageQueueState.PAUSED;
        } else {
            return MessageQueueState.RUNNING;
        }
    }

    public static enum MessageQueueState {
        INACTIVE,
        STOPPED,
        RUNNING,
        PAUSED;
    }
}
