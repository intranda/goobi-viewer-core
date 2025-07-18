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
package io.goobi.viewer.dao.update;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.user.UserTools;

/**
 * Management tool to updated deprecated viewer database setups to the one required by the viewer. to be run at viewer start, right after initializing
 * the dao Appplies a fixed list of {@link IModelUpdate IModelUpdates} to the {@link io.goobi.viewer.dao.IDAO} which are responsible to make the
 * required changes
 *
 * @author florian
 */
public class DatabaseUpdater {

    private static final Logger logger = LogManager.getLogger(DatabaseUpdater.class);

    private final IDAO dao;
    private final CMSTemplateManager templateManager;

    /**
     * <p>
     * Constructor for DatabaseUpdater.
     * </p>
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     * @param templateManager
     */
    public DatabaseUpdater(IDAO dao, CMSTemplateManager templateManager) {
        this.dao = dao;
        this.templateManager = templateManager;
    }

    /**
     * <p>
     * update.
     * </p>
     */
    public void update() {
        List<IModelUpdate> updates = instantiateUpdater();
        for (IModelUpdate update : updates) {
            try {
                if (update.update(dao, templateManager)) {
                    logger.info("Successfully updated database using {}", update.getClass().getSimpleName());
                }
            } catch (SQLException | DAOException e) {
                logger.error("Failed to update database using {}", update.getClass().getSimpleName(), e);
            }
        }

        try {
            //Initialize CMSTemplateManager with the exisitng ServletContext
            //CMSTemplateManager.getInstance(sce.getServletContext());
            // Add a "member" role, if not yet in the database
            if (DataManager.getInstance().getDao().getRole("member") == null) {
                logger.info("Role 'member' does not exist yet, adding...");
                if (!DataManager.getInstance().getDao().addRole(new Role("member"))) {
                    logger.error("Could not add static role 'member'.");
                }
            }
            // Add core license type
            LicenseType.addCoreLicenseTypesToDB();
            // Add anonymous user
            UserTools.checkAndCreateAnonymousUser();
            // add general clientapplication (representing all clients)
            DataManager.getInstance().getClientManager().addGeneralClientApplicationToDB();
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static List<IModelUpdate> instantiateUpdater() {
        List<IModelUpdate> updaterList = new ArrayList<>();
        Set<Class<? extends IModelUpdate>> classList = new Reflections("io.goobi.viewer.dao.update.*").getSubTypesOf(IModelUpdate.class);
        for (Class<? extends IModelUpdate> clazz : classList) {
            try {
                IModelUpdate updater = clazz.getDeclaredConstructor().newInstance();
                updaterList.add(updater);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException
                    | SecurityException e) {
                logger.error(e);
            }
        }
        return updaterList;
    }

}
