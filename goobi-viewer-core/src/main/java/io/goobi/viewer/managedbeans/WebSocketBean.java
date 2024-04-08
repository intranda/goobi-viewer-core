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
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
import org.omnifaces.util.Faces;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import io.goobi.viewer.controller.mq.MessageQueueManager;

@Named
@ViewScoped
public class WebSocketBean implements Serializable {

    private static final long serialVersionUID = 9068383748390523908L;

    private static final Logger logger = LogManager.getLogger(WebSocketBean.class);

    @Inject
    @Push
    private PushContext pushChannel;
    @Inject
    private ServletContext context;
    @Inject
    private transient MessageQueueManager queueManager;
    private transient Scheduler scheduler = null;

    public WebSocketBean() {
        try {
            this.scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            logger.error("Error getting quartz scheduler", e);
        }
    }

    public Map<String, String> receiveMessage(String... parameters) {
        return Arrays.stream(parameters).map(p -> {
            return new String[] { p, Faces.getRequestParameter(p) };
        }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

    }

    public void sendMessage(Object message) {
        logger.debug("sending message '{}'", message);
        pushChannel.send(message).stream().forEach(f -> logger.debug("message sent"));
    }

}
