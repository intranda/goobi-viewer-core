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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.activemq.ActiveMQConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.DefaultQueueListener;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import jakarta.jms.JMSException;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;

/**
 * <p>
 * MessageQueueBean class.
 * </p>
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
    private transient MessageQueueManager messageBroker;

    @Inject
    @Push
    private PushContext messageQueueState;
    @Inject
    @Push
    private PushContext messageQueueStateForHeader;

    private TableDataProvider<ViewerMessage> lazyModelViewerHistory;

    /**
     * <p>
     * Constructor for MessageQueueBean.
     * </p>
     */
    public MessageQueueBean() {
        this.initMessageBrokerStart();

    }

    /**
     * <p>
     * Constructor for MessageQueueBean.
     * </p>
     *
     * @param broker a {@link io.goobi.viewer.controller.mq.MessageQueueManager} object
     */
    public MessageQueueBean(MessageQueueManager broker) {
        this.messageBroker = broker;
        this.initMessageBrokerStart();
    }

    /**
     * <p>
     * init.
     * </p>
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
     * <p>
     * close.
     * </p>
     *
     * @throws jakarta.jms.JMSException if any.
     */
    @PreDestroy
    public void close() throws JMSException {
        if (this.queueSession != null) {
            this.queueSession.close();
        }
        if (this.connection != null) {
            this.connection.close();
        }
    }

    /**
     * <p>
     * getQueueContent.
     * </p>
     *
     * @return a {@link java.util.Map} object
     */
    public Map<String, Integer> getQueueContent() {
        Map<String, Integer> fastQueueContent = new TreeMap<>();
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            fastQueueContent.putAll(messageBroker.countMessagesInQueue(MessageQueueManager.QUEUE_NAME_VIEWER));
            fastQueueContent.putAll(messageBroker.countMessagesInQueue(MessageQueueManager.QUEUE_NAME_PDF));
        }
        return fastQueueContent;
    }

    public int getTotalMessagesInQueueCount() {
        return getQueueContent().values().stream().mapToInt(i -> i).sum();
    }

    /**
     * <p>
     * pauseQueue.
     * </p>
     */
    public void pauseQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            paused = this.messageBroker.pauseQueue(MessageQueueManager.QUEUE_NAME_VIEWER)
                    && this.messageBroker.pauseQueue(MessageQueueManager.QUEUE_NAME_PDF);
            updateMessageQueueState();
        }
    }

    /**
     * <p>
     * resumeQueue.
     * </p>
     */
    public void resumeQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            paused = !(this.messageBroker.resumeQueue(MessageQueueManager.QUEUE_NAME_VIEWER)
                    && this.messageBroker.resumeQueue(MessageQueueManager.QUEUE_NAME_PDF));
            updateMessageQueueState();
        }
    }

    /**
     * <p>
     * clearQueue.
     * </p>
     */
    public void clearQueue() {
        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            this.messageBroker.clearQueue(MessageQueueManager.QUEUE_NAME_VIEWER);
            this.messageBroker.clearQueue(MessageQueueManager.QUEUE_NAME_PDF);
            updateMessageQueueState();
        }
    }

    /**
     * <p>
     * initMessageBrokerStart.
     * </p>
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
     * @return List<ViewerMessage>
     */
    public List<ViewerMessage> getActiveQueryMesssages() {
        return getQueryMessages(this.messageType);
    }

    /**
     * <p>
     * getQueryMessages.
     * </p>
     *
     * @param messageType a {@link java.lang.String} object
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
     * @param type a {@link java.lang.String} object
     */
    public void removeMessagesFromQueue(String type) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            this.messageBroker.deleteMessages(type);
        }

    }

    /**
     * Delete a single message from the queue
     *
     * @param ticket a {@link io.goobi.viewer.controller.mq.ViewerMessage} object
     */
    public void deleteMessage(ViewerMessage ticket) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            this.messageBroker.deleteMessage(ticket);
        }

    }

    /**
     * <p>
     * Getter for the field <code>messageType</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * <p>
     * Setter for the field <code>messageType</code>.
     * </p>
     *
     * @param messageType a {@link java.lang.String} object
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * <p>
     * isMessageBrokerStart.
     * </p>
     *
     * @return a boolean
     */
    public boolean isMessageBrokerStart() {
        return messageBrokerStart;
    }

    /**
     * <p>
     * isPaused.
     * </p>
     *
     * @return a boolean
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
     * <p>
     * Getter for the field <code>lazyModelViewerHistory</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.managedbeans.tabledata.TableDataProvider} object
     */
    public TableDataProvider<ViewerMessage> getLazyModelViewerHistory() {
        return lazyModelViewerHistory;
    }

    /**
     * <p>
     * updateMessageQueueState.
     * </p>
     */
    public void updateMessageQueueState() {
        messageQueueState.send("update");
        messageQueueStateForHeader.send("update");
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
     * <p>
     * getListeners.
     * </p>
     *
     * @return a {@link java.util.List} object
     */
    public List<DefaultQueueListener> getListeners() {
        return Optional.ofNullable(this.messageBroker).map(broker -> broker.getListeners()).orElse(Collections.emptyList());
    }

    /**
     * <p>
     * restartAllListeners.
     * </p>
     */
    public void restartAllListeners() {
        getListeners().forEach(l -> {
            try {
                l.restartLoop();
            } catch (JMSException e) {
                log.error("Error restarting message listener for queue {}: {}", l.getQueueType(), e.toString());
            }
        });
    }

    public MessageQueueState getMessageQueueState() {
        if (this.messageBroker == null || !this.messageBroker.isQueueRunning()) {
            return MessageQueueState.STOPPED;
        } else if (this.isPaused()) {
            return MessageQueueState.PAUSED;
        } else {
            return MessageQueueState.RUNNING;
        }
    }

    public static enum MessageQueueState {
        STOPPED,
        RUNNING,
        PAUSED;
    }
}
