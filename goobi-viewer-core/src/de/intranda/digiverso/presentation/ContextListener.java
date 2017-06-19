/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
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
package de.intranda.digiverso.presentation;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.model.user.LicenseType;
import de.intranda.digiverso.presentation.model.user.Role;

public class ContextListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ContextListener.class);

    //    static {
    // ImageIO.scanForPlugins();
    //    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Launching {}", Helper.getVersion());
        DataManager.getInstance();
        // Add a "member" role, if not yet in the database
        try {
            if (DataManager.getInstance().getDao().getRole("member") == null) {
                logger.info("Role 'member' does not exist yet, adding...");
                if (!DataManager.getInstance().getDao().addRole(new Role("member"))) {
                    logger.error("Could not add static role 'member'.");
                }
            }
            LicenseType.addStaticLicenseTypesToDB();
        } catch (DAOException e) {
            logger.error(e.getMessage());
        }
        //        createResources();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            DataManager.getInstance().getDao().shutdown();
        } catch (DAOException e) {
            logger.error(e.getMessage());
        }

        // Shut all loggers down to prevent memory leaks when re-deploying the context
        LogManager.shutdown();
    }
}
