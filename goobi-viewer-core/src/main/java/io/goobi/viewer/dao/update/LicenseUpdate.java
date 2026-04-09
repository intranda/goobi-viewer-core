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

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.License.AccessType;
import io.goobi.viewer.model.security.LicenseRightsHolder;

/**
 * Database migration step that migrates existing licenses to the new
 * {@link io.goobi.viewer.model.security.LicenseRightsHolder} model and removes obsolete columns
 * from the {@code licenses} table.
 */
public class LicenseUpdate implements IModelUpdate {

    private static final Logger logger = LogManager.getLogger(LicenseUpdate.class);

    private static final String TABLE_NAME_LICENSES = "licenses";

    /** {@inheritDoc} */
    @Override
    public boolean update(IDAO dao, CMSTemplateManager templateManager) throws DAOException, SQLException {
        return performUpdates(dao);
    }

    /**
     * performUpdates.
     *
     * @param dao a {@link io.goobi.viewer.dao.IDAO} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws SQLException if in case of errors
     */
    private static boolean performUpdates(IDAO dao) throws DAOException, SQLException {
        boolean ret = false;
        // Remove obsolete core license type for crowdsourcing campaigns
        int count = 0;
        for (License license : dao.getAllLicenses()) {
            if (license.getLicensees().isEmpty()) {
                license.addLicensee();
            }
            boolean update = false;
            LicenseRightsHolder lrh = license.getLicensees().get(0);
            if (license.getUser() != null) {
                lrh.setType(AccessType.USER);
                lrh.setUser(license.getUser());
                license.setUser(null);
                update = true;
            } else if (license.getUserGroup() != null) {
                lrh.setType(AccessType.USER_GROUP);
                lrh.setUserGroup(license.getUserGroup());
                license.setUserGroup(null);
                update = true;
            } else if (license.getIpRange() != null) {
                lrh.setType(AccessType.IP_RANGE);
                lrh.setIpRange(license.getIpRange());
                license.setIpRange(null);
                update = true;
            } else if (license.getClient() != null) {
                lrh.setType(AccessType.CLIENT);
                lrh.setClient(license.getClient());
                license.setClient(null);
                update = true;
            }
            if (update && dao.updateLicense(license)) {
                count++;
            }

        }
        if (count > 0) {
            logger.info("Updated {} licenses.", count);
            ret = true;
        }

        // Remove obsolete cols
        //        if (dao.columnsExists(TABLE_NAME_LICENSES, "user_id")) {
        //            dao.executeUpdate("ALTER TABLE `" + TABLE_NAME_LICENSES + "` DROP COLUMN `user_id`;");
        //            ret = true;
        //        }
        //        if (dao.columnsExists(TABLE_NAME_LICENSES, "user_group_id")) {
        //            dao.executeUpdate("ALTER TABLE `" + TABLE_NAME_LICENSES + "` DROP COLUMN `user_group_id`;");
        //            ret = true;
        //        }
        //        if (dao.columnsExists(TABLE_NAME_LICENSES, "ip_range_id")) {
        //            dao.executeUpdate("ALTER TABLE `" + TABLE_NAME_LICENSES + "` DROP COLUMN `ip_range_id`;");
        //            ret = true;
        //        }
        //        if (dao.columnsExists(TABLE_NAME_LICENSES, "client_id")) {
        //            dao.executeUpdate("ALTER TABLE `" + TABLE_NAME_LICENSES + "` DROP COLUMN `client_id`;");
        //            ret = true;
        //        }
        if (dao.columnsExists(TABLE_NAME_LICENSES, "primary_type")) {
            dao.executeUpdate("ALTER TABLE `" + TABLE_NAME_LICENSES + "` DROP COLUMN `primary_type`;");
            ret = true;
        }
        if (dao.columnsExists(TABLE_NAME_LICENSES, "secondary_type")) {
            dao.executeUpdate("ALTER TABLE `" + TABLE_NAME_LICENSES + "` DROP COLUMN `secondary_type`;");
            ret = true;
        }

        return ret;
    }
}
