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

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;

@WebListener
public class StartQueueBrokerListener implements ServletContextListener {

    private static final Logger log = LogManager.getLogger(StartQueueBrokerListener.class);

    @Inject
    transient private MessageQueueManager messageBroker;

    public StartQueueBrokerListener() {
        //noop
    }

    public StartQueueBrokerListener(MessageQueueManager broker) {
        this.messageBroker = broker;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        if (DataManager.getInstance().getConfiguration().isStartInternalMessageBroker()) {
            if (this.messageBroker == null) {
                log.error("No MessageQueueManager injected. Maybe class could not be initialized");
            } else if (!this.messageBroker.hasConfig()) {
                log.warn("ActiveMQ configuration file {} could not be loaded",
                        ActiveMQConfig.getConfigResource(MessageQueueManager.ACTIVE_MQ_CONFIG_FILENAME));
            } else if (this.messageBroker.initializeMessageServer()) {
                log.info("Successfully started ActiveMQ");
            } else {
                log.error("ActiveMQ not initialized!");
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        this.messageBroker.closeMessageServer();
    }

}
