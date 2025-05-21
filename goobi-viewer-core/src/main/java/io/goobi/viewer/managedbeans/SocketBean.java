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

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Application scoped bean which handles socket updates for indexer and background tasks status. Only notifies the socket chanell at most every 2
 * seconds to avoid concurrent subsequent ajax request
 */
@Named
@ApplicationScoped
public class SocketBean {

    private static final Logger logger = LogManager.getLogger(SocketBean.class);

    private static final Long MIN_IDLE_TIME = 2l;

    private final AtomicBoolean shouldSend = new AtomicBoolean(false);

    @Inject
    @Push
    private PushContext backgroundTasksState;

    /**
     * Default constructor. Instantiates a fixed schedule thread
     */
    public SocketBean() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(createRunnable(), 0, MIN_IDLE_TIME, TimeUnit.SECONDS);
    }

    /**
     * Send an "update" message to the socket channel
     * 
     * @param message
     */
    public void send(String message) {
        shouldSend.set(true);
    }

    private Runnable createRunnable() {
        return () -> {
            if (shouldSend.getAndSet(false)) {
                sendMessage("update");
            }
        };
    }

    private void sendMessage(String message) {
        this.backgroundTasksState.send(message);

    }

}
