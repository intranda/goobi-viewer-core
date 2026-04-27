/**
 * This file is part of the Goobi viewer Connector - OAI-PMH and SRU interfaces for digital objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.connector;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.connector.utils.Utils;

/**
 * <p>
 * ContextListener class.
 * </p>
 *
 */
public class ContextListener implements ServletContextListener {

    private static final Logger logger = LogManager.getLogger(ContextListener.class);

    /**
     * {@inheritDoc}
     * 
     * @should set version correctly
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("{}", Utils.formatVersionString(Utils.getVersion()));
        io.goobi.viewer.controller.DataManager.getInstance().setConnectorVersion(Utils.getVersion());
    }

    /** {@inheritDoc} */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Shut all loggers down to prevent memory leaks when re-deploying the context
        LogManager.shutdown();
    }
}
